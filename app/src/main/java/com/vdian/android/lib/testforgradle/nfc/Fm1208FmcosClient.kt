package com.vdian.android.lib.testforgradle.nfc

import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.SystemClock
import android.util.Log
import java.io.Closeable
import java.io.IOException

/** FMCOS inspector and guarded client for the experimental ADF01 test-card profile. */
class Fm1208FmcosClient private constructor(private val isoDep: IsoDep) : Closeable {

    data class DirectoryEntry(
        val recordNumber: Int,
        val aid: String?,
        val label: String?,
        val rawData: String
    )

    data class Profile(
        val masterFileName: String?,
        val directorySfi: Int?,
        val applications: List<DirectoryEntry>
    )

    data class InitializationResult(
        val completedSteps: Int,
        val verifiedMessageBytes: Int
    )

    fun inspect(): Profile {
        val mfData = exchange(Fm1208Type4Protocol.SELECT_FMCOS_MASTER_FILE, "select FMCOS MF")
        val masterFileName = FmcosTlv.find(mfData, 0x84)?.toString(Charsets.US_ASCII)
        val directorySfi = FmcosTlv.find(mfData, 0x88)?.firstOrNull()?.toInt()?.and(0xFF)
        Log.i(
            LOG_TAG,
            "$LOG_PREFIX MF selected name=$masterFileName, directorySfi=$directorySfi"
        )
        val applications = directorySfi?.let(::readDirectory) ?: emptyList()
        return Profile(masterFileName, directorySfi, applications)
    }

    fun readBusinessMessageOrNull(): ByteArray? {
        if (!selectBusinessApplication()) return null
        authenticate("read business application")
        exchange(Fm1208TestCardProtocol.SELECT_BUSINESS_FILE, "select FMCOS business EF 0003")
        val lengthBytes = readBytes(0, 2)
        val length = ((lengthBytes[0].toInt() and 0xFF) shl 8) or (lengthBytes[1].toInt() and 0xFF)
        if (length == 0) return byteArrayOf()
        if (length > Fm1208TestCardProtocol.BUSINESS_FILE_SIZE - 2) {
            throw IOException("Invalid FMCOS business message length: $length")
        }
        return readBytes(2, length).also {
            Log.i(LOG_TAG, "$LOG_PREFIX business read completed messageBytes=${it.size}")
        }
    }

    fun writeBusinessMessage(message: ByteArray) {
        val stored = Fm1208TestCardProtocol.encodeBusinessMessage(message)
        Log.i(LOG_TAG, "$LOG_PREFIX business write started messageBytes=${message.size}")
        if (!selectBusinessApplication()) {
            throw IOException("FMCOS test application ADF01 is absent; initialize the test card first")
        }
        authenticate("write business application")
        exchange(Fm1208TestCardProtocol.SELECT_BUSINESS_FILE, "select FMCOS business EF 0003")

        // A zero length prevents a partial update from being exposed as a complete message.
        updateBytes(0, byteArrayOf(0x00, 0x00))
        updateBytes(2, stored.copyOfRange(2, stored.size))
        updateBytes(0, stored.copyOfRange(0, 2))
        Log.i(LOG_TAG, "$LOG_PREFIX business write completed messageBytes=${message.size}")
    }

    fun initializeTestCard(message: ByteArray): InitializationResult {
        Fm1208TestCardProtocol.encodeBusinessMessage(message)
        var completedSteps = 0

        fun runStep(command: ByteArray, operation: String, sensitive: Boolean = false) {
            exchange(command, operation, sensitive)
            completedSteps++
            Log.i(LOG_TAG, "$LOG_PREFIX initialize step=$completedSteps completed operation=$operation")
        }

        runStep(Fm1208Type4Protocol.SELECT_FMCOS_MASTER_FILE, "initialize: select FMCOS MF")
        authenticate("initialize: authorize MF erase")
        completedSteps++
        runStep(Fm1208TestCardProtocol.ERASE_MASTER_FILE, "initialize: erase MF children")
        runStep(Fm1208Type4Protocol.SELECT_FMCOS_MASTER_FILE, "initialize: reselect FMCOS MF")
        runStep(Fm1208TestCardProtocol.CREATE_MASTER_KEY_FILE, "initialize: create MF KEY EF")
        runStep(
            Fm1208TestCardProtocol.WRITE_DEFAULT_KEY,
            "initialize: write MF test key",
            sensitive = true
        )
        runStep(Fm1208TestCardProtocol.CREATE_APPLICATION, "initialize: create ADF01")
        runStep(Fm1208TestCardProtocol.SELECT_APPLICATION_BY_NAME, "initialize: select ADF01")
        runStep(Fm1208TestCardProtocol.CREATE_APPLICATION_KEY_FILE, "initialize: create ADF KEY EF")
        runStep(
            Fm1208TestCardProtocol.WRITE_DEFAULT_KEY,
            "initialize: write ADF test key",
            sensitive = true
        )
        runStep(Fm1208TestCardProtocol.CREATE_BUSINESS_FILE, "initialize: create business EF 0003")

        writeBusinessMessage(message)
        completedSteps++
        val verified = readBusinessMessageOrNull()
            ?: throw IOException("FMCOS business application disappeared after initialization")
        if (!verified.contentEquals(message)) {
            throw IOException("FMCOS verification failed after initialization")
        }
        completedSteps++
        return InitializationResult(completedSteps, verified.size).also {
            Log.i(
                LOG_TAG,
                "$LOG_PREFIX initialization completed steps=${it.completedSteps}, " +
                    "verifiedMessageBytes=${it.verifiedMessageBytes}"
            )
        }
    }

    private fun readDirectory(sfi: Int): List<DirectoryEntry> {
        if (sfi !in 1..30) throw IOException("Invalid FMCOS directory SFI: $sfi")
        val entries = mutableListOf<DirectoryEntry>()
        for (recordNumber in 1..MAX_DIRECTORY_RECORDS) {
            val command = byteArrayOf(
                0x00, 0xB2.toByte(), recordNumber.toByte(), ((sfi shl 3) or 0x04).toByte(), 0x00
            )
            val response = transceive(command, "read DIR record $recordNumber")
            val status = response.statusWord()
            if (FmcosStatus.isDirectoryEnd(status)) {
                Log.i(
                    LOG_TAG,
                    "$LOG_PREFIX DIR enumeration stopped status=${"%04X".format(status)}, records=${entries.size}"
                )
                break
            }
            val data = Fm1208Type4Protocol.responseData(response, "read DIR record $recordNumber")
            entries += DirectoryEntry(
                recordNumber = recordNumber,
                aid = FmcosTlv.find(data, 0x4F)?.toHex(),
                label = FmcosTlv.find(data, 0x50)?.toString(Charsets.UTF_8),
                rawData = data.toHex()
            )
        }
        Log.i(LOG_TAG, "$LOG_PREFIX DIR read completed records=${entries.size}")
        return entries
    }

    private fun selectBusinessApplication(): Boolean {
        val operation = "select FMCOS test application ADF01"
        val response = transceive(Fm1208TestCardProtocol.SELECT_APPLICATION_BY_NAME, operation)
        return when (response.statusWord()) {
            0x9000 -> {
                Fm1208Type4Protocol.responseData(response, operation)
                true
            }
            0x6A82 -> false
            else -> {
                Fm1208Type4Protocol.responseData(response, operation)
                false
            }
        }
    }

    private fun authenticate(operation: String) {
        val challenge = exchange(Fm1208TestCardProtocol.GET_CHALLENGE, "$operation: get challenge")
        if (challenge.size != 4) {
            throw IOException("$operation returned a ${challenge.size}-byte challenge; expected 4")
        }
        exchange(
            Fm1208TestCardProtocol.externalAuthenticate(challenge),
            "$operation: external authenticate",
            sensitive = true
        )
        Log.i(LOG_TAG, "$LOG_PREFIX authentication completed operation=$operation")
    }

    private fun readBytes(startOffset: Int, length: Int): ByteArray {
        val result = ByteArray(length)
        var copied = 0
        while (copied < length) {
            val chunkSize = minOf(
                length - copied,
                Fm1208TestCardProtocol.MAX_TRANSFER_SIZE
            )
            val chunk = exchange(
                Fm1208Type4Protocol.readBinary(startOffset + copied, chunkSize),
                "read FMCOS business EF at ${startOffset + copied}"
            )
            if (chunk.isEmpty() || chunk.size > chunkSize) {
                throw IOException("Card returned an invalid FMCOS read length")
            }
            chunk.copyInto(result, copied)
            copied += chunk.size
        }
        return result
    }

    private fun updateBytes(startOffset: Int, data: ByteArray) {
        var written = 0
        while (written < data.size) {
            val chunkSize = minOf(
                data.size - written,
                Fm1208TestCardProtocol.MAX_TRANSFER_SIZE
            )
            val chunk = data.copyOfRange(written, written + chunkSize)
            exchange(
                Fm1208Type4Protocol.updateBinary(startOffset + written, chunk),
                "update FMCOS business EF at ${startOffset + written}",
                sensitive = true
            )
            written += chunkSize
        }
    }

    private fun exchange(
        command: ByteArray,
        operation: String,
        sensitive: Boolean = false
    ): ByteArray = Fm1208Type4Protocol.responseData(
        transceive(command, operation, sensitive),
        operation
    )

    private fun transceive(
        command: ByteArray,
        operation: String,
        sensitive: Boolean = false
    ): ByteArray {
        val commandLog = if (sensitive && command.size > 5) {
            "${command.copyOfRange(0, 5).toHex()} [${command.size - 5} data bytes omitted]"
        } else {
            command.toHex()
        }
        Log.i(LOG_TAG, "$LOG_PREFIX -> $operation apdu=$commandLog")
        return isoDep.transceive(command).also { response ->
            Log.i(LOG_TAG, "$LOG_PREFIX <- $operation response=${response.toHex()}")
        }
    }

    override fun close() {
        isoDep.close()
    }

    companion object {
        private const val LOG_TAG = "Fm1208FmcosClient"
        private const val LOG_PREFIX = "[FM1208-APDU]"
        private const val MAX_DIRECTORY_RECORDS = 32

        fun connect(tag: Tag): Fm1208FmcosClient {
            val isoDep = IsoDep.get(tag)
                ?: throw IllegalArgumentException("Card does not expose ISO-DEP (ISO/IEC 14443-4)")
            val startedAt = SystemClock.elapsedRealtime()
            Log.i(
                LOG_TAG,
                "$LOG_PREFIX connect started uid=${tag.id.toHex()}, tech=${tag.techList.joinToString()}, " +
                    "historicalBytes=${isoDep.historicalBytes.toHex()}, " +
                    "maxTransceiveLength=${isoDep.maxTransceiveLength}"
            )
            isoDep.timeout = 5000
            try {
                isoDep.connect()
            } catch (error: IOException) {
                val elapsed = SystemClock.elapsedRealtime() - startedAt
                Log.e(
                    LOG_TAG,
                    "$LOG_PREFIX connect failed uid=${tag.id.toHex()}, elapsedMs=$elapsed, " +
                        "isConnected=${isoDep.isConnected}; tag may have left the RF field or expired during app selection",
                    error
                )
                throw IOException(
                    "连接卡片失败：请先移开卡片，保持 App 在前台并重新贴卡，贴住直到操作完成",
                    error
                )
            }
            Log.i(
                LOG_TAG,
                "$LOG_PREFIX connected uid=${tag.id.toHex()}, " +
                    "elapsedMs=${SystemClock.elapsedRealtime() - startedAt}, timeout=${isoDep.timeout}, " +
                    "extendedLength=${isoDep.isExtendedLengthApduSupported}"
            )
            return Fm1208FmcosClient(isoDep)
        }

        private fun ByteArray.statusWord(): Int {
            if (size < 2) return -1
            return ((this[size - 2].toInt() and 0xFF) shl 8) or (last().toInt() and 0xFF)
        }

        private fun ByteArray.toHex(): String = joinToString("") { "%02X".format(it) }
    }
}

internal object FmcosStatus {
    fun isDirectoryEnd(status: Int): Boolean = status == 0x6A82 || status == 0x6A83
}

internal object FmcosTlv {
    fun find(data: ByteArray, expectedTag: Int): ByteArray? {
        var offset = 0
        while (offset < data.size) {
            val tagStart = offset
            var tag = data[offset++].toInt() and 0xFF
            if ((tag and 0x1F) == 0x1F) {
                if (offset >= data.size) return null
                tag = (tag shl 8) or (data[offset++].toInt() and 0xFF)
            }
            if (offset >= data.size) return null
            val firstLength = data[offset++].toInt() and 0xFF
            val length = if (firstLength and 0x80 == 0) {
                firstLength
            } else {
                val lengthBytes = firstLength and 0x7F
                if (lengthBytes !in 1..2 || offset + lengthBytes > data.size) return null
                var value = 0
                repeat(lengthBytes) { value = (value shl 8) or (data[offset++].toInt() and 0xFF) }
                value
            }
            if (offset + length > data.size) return null
            val value = data.copyOfRange(offset, offset + length)
            if (tag == expectedTag) return value
            if (tagStart != offset && tag in CONSTRUCTED_TAGS) {
                find(value, expectedTag)?.let { return it }
            }
            offset += length
        }
        return null
    }

    private val CONSTRUCTED_TAGS = setOf(0x61, 0x6F, 0xA5)
}
