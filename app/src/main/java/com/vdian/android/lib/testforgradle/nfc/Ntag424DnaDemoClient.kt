package com.vdian.android.lib.testforgradle.nfc

import android.net.Uri
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.io.IOException

/** Read-only NTAG 424 DNA diagnostics. Reading an SDM NDEF advances its read counter. */
internal class Ntag424DnaDemoClient private constructor(private val isoDep: IsoDep) : Closeable {

    data class Report(
        val version: Ntag424Version,
        val ndefSettings: Ntag424FileSettings?,
        val ndefUri: Uri?,
        val ndefApplicationPackage: String?,
        val ndefRecordCount: Int?,
        val originalitySignature: ByteArray?,
        val warnings: List<String>
    ) {
        fun toDisplayText(): String = buildString {
            appendLine(if (version.looksLikeNtag424Dna) "识别结果: NTAG 424 DNA" else "识别结果: 未确认是 NTAG 424 DNA")
            appendLine("硬件: vendor=%02X type=%02X storage=%02X version=%02X".format(
                version.hardwareVendorId,
                version.hardwareType,
                version.hardwareStorageSize,
                version.hardwareMajorVersion
            ))
            appendLine("软件版本: ${version.softwareMajorVersion}.${version.softwareMinorVersion}")
            appendLine("芯片 UID: ${version.uid?.toHex(":") ?: "响应未提供"}")
            if (version.productionWeek != null && version.productionYear != null) {
                appendLine("生产批次: week=${version.productionWeek}, year=%02X".format(version.productionYear))
            }
            ndefSettings?.let { settings ->
                appendLine("NDEF 文件: ${settings.fileSize} bytes, SDM=${if (settings.sdmEnabled) "已开启" else "未开启"}")
                appendLine(
                    "权限: R=${settings.readKey.asAccess()} W=${settings.writeKey.asAccess()} " +
                        "RW=${settings.readWriteKey.asAccess()} Change=${settings.changeKey.asAccess()}"
                )
                if (settings.sdmEnabled) {
                    appendLine(
                        "SDM: MetaRead=${settings.sdmMetaReadKey?.asAccess()}, " +
                            "FileRead=${settings.sdmFileReadKey?.asAccess()}, " +
                            "CtrRet=${settings.sdmCounterRetrieveKey?.asAccess()}"
                    )
                }
            }
            appendLine("NDEF records: ${ndefRecordCount ?: "读取失败"}")
            appendLine("NDEF URI: ${ndefUri ?: "无"}")
            appendLine("NDEF pkg: ${ndefApplicationPackage ?: "无"}")
            ndefUri?.let { uri ->
                val dynamicFields = uri.queryParameterNames.associateWith { uri.getQueryParameter(it) }
                    .filterKeys { it.lowercase() in SDM_PARAMETER_NAMES }
                if (dynamicFields.isNotEmpty()) appendLine("疑似 SDM 字段: $dynamicFields")
            }
            appendLine("Originality Signature: ${originalitySignature?.toHex() ?: "未取得"}")
            appendLine("注意: Demo 只展示签名；生产环境还必须使用 NXP 公钥验证签名，并在后端验证 SDMMAC/计数器。")
            warnings.forEach { appendLine("提示: $it") }
        }.trim()
    }

    fun inspect(): Report {
        Log.i(LOG_TAG, "$LOG_PREFIX inspection started")
        isoExchange(Ntag424DnaProtocol.SELECT_NDEF_APPLICATION, "select NDEF application")
        Log.i(LOG_TAG, "$LOG_PREFIX NDEF application selected")
        val version = Ntag424DnaProtocol.parseVersion(
            nativeExchange(Ntag424DnaProtocol.GET_VERSION, "get version")
        )
        Log.i(
            LOG_TAG,
            "$LOG_PREFIX version parsed, recognized=${version.looksLikeNtag424Dna}, " +
                "hwMajor=${version.hardwareMajorVersion.toString(16)}, " +
                "sw=${version.softwareMajorVersion}.${version.softwareMinorVersion}"
        )
        val warnings = mutableListOf<String>()
        val settings = capture("读取 NDEF 文件设置", warnings) {
            Ntag424DnaProtocol.parseFileSettings(
                nativeExchange(Ntag424DnaProtocol.getFileSettings(NDEF_FILE_NUMBER), "get NDEF file settings")
            )
        }
        Log.i(
            LOG_TAG,
            "$LOG_PREFIX file settings read, available=${settings != null}, " +
                "sdmEnabled=${settings?.sdmEnabled}"
        )
        val signature = captureOriginalitySignature(warnings)
        Log.i(LOG_TAG, "$LOG_PREFIX originality signature read, available=${signature != null}")
        val ndef = capture("读取 NDEF", warnings) { readNdef() }
        Log.i(
            LOG_TAG,
            "$LOG_PREFIX NDEF read, available=${ndef != null}, records=${ndef?.recordCount}, " +
                "uriHost=${ndef?.uri?.host}, package=${ndef?.applicationPackage}, warnings=${warnings.size}"
        )
        return Report(
            version = version,
            ndefSettings = settings,
            ndefUri = ndef?.uri,
            ndefApplicationPackage = ndef?.applicationPackage,
            ndefRecordCount = ndef?.recordCount,
            originalitySignature = signature,
            warnings = warnings
        )
    }

    private data class NdefContent(val uri: Uri?, val applicationPackage: String?, val recordCount: Int)

    private fun readNdef(): NdefContent {
        isoExchange(Ntag424DnaProtocol.selectFile(CC_FILE_ID), "select CC file")
        val ccHeader = isoExchange(Ntag424DnaProtocol.readBinary(0, 2), "read CC length")
        require(ccHeader.size == 2) { "Invalid CC length response" }
        val ccLength = ((ccHeader[0].toInt() and 0xFF) shl 8) or
            (ccHeader[1].toInt() and 0xFF)
        val cc = isoExchange(Ntag424DnaProtocol.readBinary(0, ccLength), "read CC")
        val (ndefFileId, maxFileSize) = Ntag424DnaProtocol.parseCapabilityContainer(cc)
        isoExchange(Ntag424DnaProtocol.selectFile(ndefFileId), "select NDEF file")
        val nlenBytes = isoExchange(Ntag424DnaProtocol.readBinary(0, 2), "read NLEN")
        require(nlenBytes.size == 2) { "Invalid NLEN response" }
        val nlen = ((nlenBytes[0].toInt() and 0xFF) shl 8) or
            (nlenBytes[1].toInt() and 0xFF)
        require(nlen <= maxFileSize - 2) { "NDEF length exceeds file capacity" }
        if (nlen == 0) return NdefContent(null, null, 0)
        val bytes = ByteArrayOutputStream(nlen)
        var offset = 0
        while (offset < nlen) {
            val chunkSize = minOf(nlen - offset, MAX_READ_LENGTH)
            val chunk = isoExchange(
                Ntag424DnaProtocol.readBinary(offset + 2, chunkSize),
                "read NDEF at $offset"
            )
            require(chunk.size == chunkSize) { "NDEF read returned ${chunk.size}, expected $chunkSize" }
            bytes.write(chunk)
            offset += chunkSize
        }
        val message = NdefMessage(bytes.toByteArray())
        val uri = message.records.firstOrNull {
            it.tnf == NdefRecord.TNF_WELL_KNOWN && it.type.contentEquals(NdefRecord.RTD_URI)
        }?.toUri()
        val applicationPackage = message.records.firstOrNull {
            it.tnf == NdefRecord.TNF_EXTERNAL_TYPE && it.type.contentEquals(ANDROID_PACKAGE_TYPE)
        }?.payload?.toString(Charsets.UTF_8)
        return NdefContent(uri, applicationPackage, message.records.size)
    }

    private fun nativeExchange(command: ByteArray, operation: String): ByteArray {
        val output = ByteArrayOutputStream()
        val commandCode = command[1].toInt() and 0xFF
        var nextCommand = command
        var frames = 0
        while (true) {
            val response = transceive(nextCommand, operation)
            if (response.size < 2 || response[response.size - 2] != 0x91.toByte()) {
                throw IOException("$operation returned an invalid native response")
            }
            output.write(response, 0, response.size - 2)
            val status = response.last().toInt() and 0xFF
            when {
                Ntag424DnaProtocol.isSuccessfulNativeStatus(
                    commandCode,
                    status,
                    output.size()
                ) -> return output.toByteArray()
                status == 0xAF -> {
                    if (++frames > MAX_ADDITIONAL_FRAMES) throw IOException("$operation returned too many frames")
                    nextCommand = Ntag424DnaProtocol.ADDITIONAL_FRAME
                }
                else -> throw IOException("$operation failed, status=91${status.toString(16).uppercase().padStart(2, '0')}")
            }
        }
    }

    private fun isoExchange(command: ByteArray, operation: String): ByteArray {
        val response = transceive(command, operation)
        if (response.size < 2) throw IOException("$operation returned an incomplete response")
        val status = ((response[response.size - 2].toInt() and 0xFF) shl 8) or
            (response.last().toInt() and 0xFF)
        if (status != 0x9000 && status != 0x9100) {
            throw IOException("$operation failed, status=${status.toString(16).uppercase().padStart(4, '0')}")
        }
        return response.copyOf(response.size - 2)
    }

    private fun transceive(command: ByteArray, operation: String): ByteArray {
        Log.i(LOG_TAG, "$LOG_PREFIX -> $operation ${command.toHex()}")
        return isoDep.transceive(command).also {
            Log.i(LOG_TAG, "$LOG_PREFIX <- $operation ${it.toHex()} ")
        }
    }

    private inline fun <T> capture(label: String, warnings: MutableList<String>, action: () -> T): T? =
        try {
            action()
        } catch (error: Exception) {
            Log.w(LOG_TAG, "$LOG_PREFIX $label failed", error)
            warnings += "${label}失败: ${error.message ?: error.javaClass.simpleName}"
            null
        }

    private fun captureOriginalitySignature(warnings: MutableList<String>): ByteArray? =
        try {
            nativeExchange(Ntag424DnaProtocol.READ_ORIGINALITY_SIGNATURE, "read originality signature")
                .also { require(it.size == ORIGINALITY_SIGNATURE_LENGTH) { "签名长度应为 56，实际为 ${it.size}" } }
        } catch (error: Exception) {
            // Random ID is a permanent PICC configuration. Some cards reject ReadSignature
            // with 910B after it is enabled; this does not affect SDM/NDEF inspection.
            if (error.message?.contains("status=910B", ignoreCase = true) == true) {
                Log.i(LOG_TAG, "$LOG_PREFIX Originality Signature unavailable in Random UID mode")
                warnings += "Random UID 已开启，Originality Signature 当前不可读取（910B）"
            } else {
                Log.w(LOG_TAG, "$LOG_PREFIX 读取 Originality Signature failed", error)
                warnings += "读取 Originality Signature 失败: ${error.message ?: error.javaClass.simpleName}"
            }
            null
        }

    override fun close() {
        isoDep.close()
        Log.i(LOG_TAG, "$LOG_PREFIX ISO-DEP connection closed")
    }

    companion object {
        private const val LOG_TAG = "Ntag424DnaDemo"
        private const val LOG_PREFIX = "[NTAG424-DEMO]"
        private const val CC_FILE_ID = 0xE103
        private const val NDEF_FILE_NUMBER = 0x02
        private const val ORIGINALITY_SIGNATURE_LENGTH = 56
        private const val MAX_READ_LENGTH = 0xF0
        private const val MAX_ADDITIONAL_FRAMES = 8
        private val SDM_PARAMETER_NAMES = setOf("e", "c", "uid", "ctr", "picc_data", "cmac")
        private val ANDROID_PACKAGE_TYPE = "android.com:pkg".toByteArray(Charsets.US_ASCII)

        fun connect(tag: Tag): Ntag424DnaDemoClient {
            val isoDep = IsoDep.get(tag)
                ?: throw IllegalArgumentException("标签不支持 ISO-DEP，无法作为 NTAG 424 DNA 检测")
            Log.i(
                LOG_TAG,
                "$LOG_PREFIX connecting ISO-DEP, rfUid=${tag.id.toHex(":")}, " +
                    "maxTransceiveLength=${isoDep.maxTransceiveLength}"
            )
            isoDep.timeout = 5000
            isoDep.connect()
            Log.i(LOG_TAG, "$LOG_PREFIX ISO-DEP connected, timeoutMs=${isoDep.timeout}")
            return Ntag424DnaDemoClient(isoDep)
        }

        private fun Int.asAccess(): String = when (this) {
            0x0E -> "Free"
            0x0F -> "Never"
            else -> "Key $this"
        }

        private fun ByteArray.toHex(separator: String = ""): String =
            joinToString(separator) { "%02X".format(it) }
    }
}
