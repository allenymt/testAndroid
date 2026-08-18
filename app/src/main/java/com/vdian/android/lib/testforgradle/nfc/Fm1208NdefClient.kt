package com.vdian.android.lib.testforgradle.nfc

import android.net.Uri
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.util.Log
import java.io.Closeable
import java.io.IOException

/** Reads and writes an FM1208 card personalized as an NFC Forum Type 4 NDEF tag. */
class Fm1208NdefClient private constructor(private val isoDep: IsoDep) : Closeable {

    data class Content(
        val uri: Uri?,
        val applicationPackage: String?,
        val recordCount: Int
    )

    fun read(): Content {
        Log.i(LOG_TAG, "$LOG_PREFIX read started")
        val capability = selectNdefFile()
        if (capability.readAccess != OPEN_ACCESS) {
            throw IOException("NDEF file requires unsupported read authentication")
        }
        val nlen = readBytes(0, 2, capability.maxReadSize)
        val messageLength = unsignedShort(nlen)
        val capacity = capability.maxNdefFileSize - 2
        if (messageLength == 0) return Content(null, null, 0)
        if (messageLength > capacity) {
            throw IOException("NDEF length $messageLength exceeds card capacity $capacity")
        }
        val message = NdefMessage(readBytes(2, messageLength, capability.maxReadSize))
        var uri: Uri? = null
        var applicationPackage: String? = null
        message.records.forEach { record ->
            if (uri == null && record.tnf == NdefRecord.TNF_WELL_KNOWN &&
                record.type.contentEquals(NdefRecord.RTD_URI)) {
                uri = record.toUri()
            }
            if (record.tnf == NdefRecord.TNF_EXTERNAL_TYPE &&
                record.type.contentEquals(ANDROID_PACKAGE_TYPE)) {
                applicationPackage = String(record.payload, Charsets.UTF_8)
            }
        }
        return Content(uri, applicationPackage, message.records.size).also {
            Log.i(
                LOG_TAG,
                "$LOG_PREFIX read completed records=${it.recordCount}, uriHost=${it.uri?.host}, package=${it.applicationPackage}"
            )
        }
    }

    fun writeBusinessData(
        url: String = DEFAULT_BUSINESS_URL,
        applicationPackage: String = DEFAULT_APPLICATION_PACKAGE
    ) {
        val uri = Uri.parse(url)
        require(uri.scheme == "https" && !uri.host.isNullOrBlank()) { "A valid HTTPS URL is required" }
        require(applicationPackage.isNotBlank()) { "Application package must not be blank" }
        val message = NdefMessage(
            arrayOf(
                NdefRecord.createUri(uri),
                NdefRecord.createApplicationRecord(applicationPackage)
            )
        ).toByteArray()
        Log.i(
            LOG_TAG,
            "$LOG_PREFIX write started uriHost=${uri.host}, package=$applicationPackage, ndefBytes=${message.size}"
        )

        val capability = selectNdefFile()
        if (capability.writeAccess != OPEN_ACCESS) {
            throw IOException("NDEF file is read-only or requires unsupported write authentication")
        }
        val capacity = capability.maxNdefFileSize - 2
        if (message.size > capacity) {
            throw IOException("NDEF message needs ${message.size} bytes, card capacity is $capacity")
        }

        // Clear NLEN first so an interrupted update is never exposed as a valid partial message.
        updateBytes(0, byteArrayOf(0x00, 0x00), capability.maxWriteSize)
        updateBytes(2, message, capability.maxWriteSize)
        updateBytes(0, byteArrayOf((message.size ushr 8).toByte(), message.size.toByte()), capability.maxWriteSize)
        Log.i(LOG_TAG, "$LOG_PREFIX write completed ndefBytes=${message.size}")
    }

    private fun selectNdefFile(): Type4Capability {
        Log.i(LOG_TAG, "$LOG_PREFIX step=1 select NDEF application aid=D2760000850101")
        try {
            exchange(Fm1208Type4Protocol.SELECT_NDEF_APPLICATION, "select NDEF application")
        } catch (error: IOException) {
            if (error.message?.contains("status=6A82") == true) {
                val mfProbe = probeFmcosMasterFile()
                throw IOException(
                    "Standard NDEF application is absent; FMCOS MF probe: $mfProbe. " +
                        "The card requires Type 4 NDEF personalization.",
                    error
                )
            }
            throw error
        }
        Log.i(LOG_TAG, "$LOG_PREFIX step=2 select Capability Container file=E103")
        exchange(Fm1208Type4Protocol.SELECT_CAPABILITY_CONTAINER, "select Capability Container")
        val header = readBytes(0, 2, DEFAULT_TRANSFER_SIZE)
        val ccLength = unsignedShort(header)
        Log.i(LOG_TAG, "$LOG_PREFIX step=3 Capability Container declaredLength=$ccLength")
        if (ccLength < MINIMUM_CC_LENGTH) throw IOException("Invalid Capability Container length: $ccLength")
        val capability = Fm1208Type4Protocol.parseCapabilityContainer(
            readBytes(0, ccLength, DEFAULT_TRANSFER_SIZE)
        )
        Log.i(
            LOG_TAG,
            "$LOG_PREFIX step=4 parsed CC ndefFile=${capability.ndefFileId.toString(16).uppercase()}, " +
                "capacity=${capability.maxNdefFileSize}, mLe=${capability.maxReadSize}, " +
                "mLc=${capability.maxWriteSize}, readAccess=${capability.readAccess.toString(16)}, " +
                "writeAccess=${capability.writeAccess.toString(16)}"
        )
        exchange(Fm1208Type4Protocol.selectFile(capability.ndefFileId), "select NDEF file")
        Log.i(LOG_TAG, "$LOG_PREFIX step=5 selected NDEF file")
        return capability
    }

    private fun probeFmcosMasterFile(): String {
        val command = Fm1208Type4Protocol.SELECT_FMCOS_MASTER_FILE
        Log.i(LOG_TAG, "$LOG_PREFIX probe select FMCOS MF file=3F00 apdu=${command.toCommandLog()}")
        return try {
            val response = isoDep.transceive(command)
            Log.i(LOG_TAG, "$LOG_PREFIX probe FMCOS MF response=${response.toResponseLog()}")
            val status = response.statusWord()
            when (status) {
                0x9000 -> "present, FCI=${response.copyOf(response.size - 2).toResponseLog()}"
                0x6A81 -> "unavailable or card locked, status=6A81"
                else -> "status=${status.toString(16).uppercase().padStart(4, '0')}"
            }
        } catch (probeError: Exception) {
            Log.e(LOG_TAG, "$LOG_PREFIX probe FMCOS MF failed", probeError)
            "probe failed: ${probeError.message ?: probeError.javaClass.simpleName}"
        }
    }

    private fun readBytes(startOffset: Int, length: Int, transferLimit: Int): ByteArray {
        if (length == 0) return byteArrayOf()
        val result = ByteArray(length)
        var copied = 0
        while (copied < length) {
            val chunkSize = minOf(length - copied, transferLimit, MAX_READ_SIZE)
            val chunk = exchange(
                Fm1208Type4Protocol.readBinary(startOffset + copied, chunkSize),
                "read binary at ${startOffset + copied}"
            )
            if (chunk.isEmpty() || chunk.size > chunkSize) throw IOException("Card returned an invalid read length")
            chunk.copyInto(result, copied)
            copied += chunk.size
        }
        return result
    }

    private fun updateBytes(startOffset: Int, data: ByteArray, transferLimit: Int) {
        var written = 0
        while (written < data.size) {
            val chunkSize = minOf(data.size - written, transferLimit, MAX_WRITE_SIZE)
            val chunk = data.copyOfRange(written, written + chunkSize)
            exchange(
                Fm1208Type4Protocol.updateBinary(startOffset + written, chunk),
                "update binary at ${startOffset + written}"
            )
            written += chunkSize
        }
    }

    private fun exchange(command: ByteArray, operation: String): ByteArray {
        Log.i(LOG_TAG, "$LOG_PREFIX -> $operation apdu=${command.toCommandLog()}")
        return try {
            val response = isoDep.transceive(command)
            Log.i(LOG_TAG, "$LOG_PREFIX <- $operation response=${response.toResponseLog()}")
            Fm1208Type4Protocol.responseData(response, operation)
        } catch (error: Exception) {
            Log.e(LOG_TAG, "$LOG_PREFIX !! $operation failed apdu=${command.toCommandLog()}", error)
            throw error
        }
    }

    override fun close() {
        isoDep.close()
    }

    companion object {
        const val DEFAULT_BUSINESS_URL =
            "https://yl.test.com/nfc"
        const val DEFAULT_APPLICATION_PACKAGE = "com.vdian.android.lib.testforgradle"

        private const val LOG_TAG = "Fm1208NdefClient"
        private const val LOG_PREFIX = "[FM1208-APDU]"
        private val ANDROID_PACKAGE_TYPE = "android.com:pkg".toByteArray(Charsets.US_ASCII)
        private const val OPEN_ACCESS = 0x00
        private const val MINIMUM_CC_LENGTH = 15
        private const val DEFAULT_TRANSFER_SIZE = 0xFF
        private const val MAX_READ_SIZE = 0x100
        private const val MAX_WRITE_SIZE = 0xFF

        fun connect(tag: Tag): Fm1208NdefClient {
            Log.i(
                LOG_TAG,
                "$LOG_PREFIX connect uid=${tag.id.toHex()}, tech=${tag.techList.joinToString()}, " +
                    "historicalBytes=${IsoDep.get(tag)?.historicalBytes.toHex()}, " +
                    "hiLayerResponse=${IsoDep.get(tag)?.hiLayerResponse.toHex()}"
            )
            val isoDep = IsoDep.get(tag)
                ?: throw IllegalArgumentException("Card does not expose ISO-DEP (ISO/IEC 14443-4)")
            isoDep.timeout = 5000
            isoDep.connect()
            Log.i(
                LOG_TAG,
                "$LOG_PREFIX connected timeout=${isoDep.timeout}, maxTransceiveLength=${isoDep.maxTransceiveLength}, " +
                    "extendedLength=${isoDep.isExtendedLengthApduSupported}"
            )
            return Fm1208NdefClient(isoDep)
        }

        private fun unsignedShort(bytes: ByteArray): Int =
            ((bytes[0].toInt() and 0xFF) shl 8) or (bytes[1].toInt() and 0xFF)

        private fun ByteArray?.toHex(): String =
            this?.joinToString("") { "%02X".format(it) } ?: "null"

        private fun ByteArray.toCommandLog(): String {
            val isUpdateBinary = size >= 2 && this[1] == 0xD6.toByte()
            return if (isUpdateBinary && size > 5) {
                "${copyOfRange(0, 5).toHex()} [${size - 5} data bytes omitted]"
            } else {
                toHex()
            }
        }

        private fun ByteArray.toResponseLog(): String = if (size <= MAX_LOGGED_RESPONSE_BYTES) {
            toHex()
        } else {
            "${copyOfRange(0, MAX_LOGGED_RESPONSE_BYTES).toHex()}... [$size bytes total]"
        }

        private fun ByteArray.statusWord(): Int {
            if (size < 2) return -1
            return ((this[size - 2].toInt() and 0xFF) shl 8) or (last().toInt() and 0xFF)
        }

        private const val MAX_LOGGED_RESPONSE_BYTES = 64
    }
}
