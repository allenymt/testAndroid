package com.vdian.android.lib.testforgradle.nfc

import java.io.IOException

internal data class Ntag424Version(
    val hardwareVendorId: Int,
    val hardwareType: Int,
    val hardwareMajorVersion: Int,
    val hardwareStorageSize: Int,
    val softwareMajorVersion: Int,
    val softwareMinorVersion: Int,
    val uid: ByteArray?,
    val productionWeek: Int?,
    val productionYear: Int?
) {
    val looksLikeNtag424Dna: Boolean
        get() = hardwareVendorId == 0x04 && hardwareType == 0x04 &&
            hardwareMajorVersion == 0x30 && hardwareStorageSize == 0x11
}

internal data class Ntag424FileSettings(
    val fileType: Int,
    val fileOption: Int,
    val communicationMode: Int,
    val readKey: Int,
    val writeKey: Int,
    val readWriteKey: Int,
    val changeKey: Int,
    val fileSize: Int,
    val sdmEnabled: Boolean,
    val sdmOptions: Int?,
    val sdmMetaReadKey: Int?,
    val sdmFileReadKey: Int?,
    val sdmCounterRetrieveKey: Int?,
    val sdmConfiguration: ByteArray?
)

internal enum class Ntag424WriteMode { PLAIN, FULL }

internal data class Ntag424SdmDisablePlan(
    val disableFileSettings: Boolean,
    val cleanedUrl: String?
)

internal object Ntag424DnaProtocol {
    val SELECT_NDEF_APPLICATION = byteArrayOf(
        0x00, 0xA4.toByte(), 0x04, 0x00, 0x07,
        0xD2.toByte(), 0x76, 0x00, 0x00, 0x85.toByte(), 0x01, 0x01, 0x00
    )
    val GET_VERSION = nativeCommand(0x60)
    val READ_ORIGINALITY_SIGNATURE = nativeCommand(0x3C, byteArrayOf(0x00))
    val ADDITIONAL_FRAME = nativeCommand(0xAF)

    fun getFileSettings(fileNumber: Int): ByteArray {
        require(fileNumber in 0..0x1F) { "File number must be 0..31" }
        return nativeCommand(0xF5, byteArrayOf(fileNumber.toByte()))
    }

    fun writeModeFor(settings: Ntag424FileSettings): Ntag424WriteMode = when (settings.communicationMode) {
        0 -> Ntag424WriteMode.PLAIN
        3 -> Ntag424WriteMode.FULL
        else -> throw IllegalArgumentException(
            "Unsupported NDEF file communication mode: ${settings.communicationMode}"
        )
    }

    fun buildSdmFileSettings(
        currentSettings: Ntag424FileSettings,
        piccDataOffset: Int,
        macOffset: Int
    ): ByteArray {
        val accessRights = (currentSettings.readKey shl 12) or
            (currentSettings.writeKey shl 8) or
            (currentSettings.readWriteKey shl 4) or currentSettings.changeKey
        return byteArrayOf(
            (currentSettings.fileOption or 0x40).toByte(),
            accessRights.toByte(),
            (accessRights ushr 8).toByte(),
            0xC1.toByte(),
            0xF1.toByte(), 0x21
        ) + piccDataOffset.toLittleEndian24() + macOffset.toLittleEndian24() +
            macOffset.toLittleEndian24()
    }

    fun buildWriteProtectionFileSettings(
        currentSettings: Ntag424FileSettings,
        protected: Boolean
    ): ByteArray {
        val writeKey = if (protected) DEMO_KEY_NUMBER else FREE_ACCESS
        val accessRights = (currentSettings.readKey shl 12) or
            (writeKey shl 8) or (writeKey shl 4) or currentSettings.changeKey
        val base = byteArrayOf(
            currentSettings.fileOption.toByte(),
            accessRights.toByte(),
            (accessRights ushr 8).toByte()
        )
        return if (currentSettings.sdmEnabled) {
            base + checkNotNull(currentSettings.sdmConfiguration) {
                "SDM configuration is missing"
            }
        } else {
            base
        }
    }

    fun buildDisabledSdmFileSettings(currentSettings: Ntag424FileSettings): ByteArray {
        val accessRights = (currentSettings.readKey shl 12) or
            (currentSettings.writeKey shl 8) or
            (currentSettings.readWriteKey shl 4) or currentSettings.changeKey
        return byteArrayOf(
            (currentSettings.fileOption and 0x40.inv()).toByte(),
            accessRights.toByte(),
            (accessRights ushr 8).toByte()
        )
    }

    fun writeDataPlain(fileNumber: Int, offset: Int, data: ByteArray): ByteArray {
        require(fileNumber in 0..0x1F) { "File number must be 0..31" }
        require(offset in 0..0xFFFFFF) { "Offset out of range" }
        require(data.isNotEmpty() && data.size <= 0xF8) { "Plain write length must be 1..248" }
        val body = byteArrayOf(fileNumber.toByte()) + offset.toLittleEndian24() +
            data.size.toLittleEndian24() + data
        return byteArrayOf(0x90.toByte(), 0x8D.toByte(), 0x00, 0x00, body.size.toByte()) +
            body + byteArrayOf(0x00)
    }

    /** Appends SDM fields without discarding the business query already encoded in the URI. */
    fun buildSdmUrl(originalUrl: String, encryptedPiccData: String, mac: String): String {
        require(encryptedPiccData.isNotEmpty()) { "Encrypted PICC data must not be empty" }
        require(mac.isNotEmpty()) { "SDM MAC must not be empty" }
        val fragmentStart = originalUrl.indexOf('#').let { if (it < 0) originalUrl.length else it }
        val base = originalUrl.substring(0, fragmentStart)
        val fragment = originalUrl.substring(fragmentStart)
        val query = base.substringAfter('?', "")
        require(query.split('&').filter { it.isNotEmpty() }.none { parameter ->
            val name = parameter.substringBefore('=').lowercase()
            name == "e" || name == "c"
        }) { "URL already contains an SDM e or c parameter" }
        val separator = if (base.contains('?')) "&" else "?"
        return "$base${separator}e=$encryptedPiccData&c=$mac$fragment"
    }

    /** Removes only top-level SDM fields while preserving encoded nested queries and fragments. */
    fun removeSdmParameters(url: String): String {
        val fragmentStart = url.indexOf('#').let { if (it < 0) url.length else it }
        val withoutFragment = url.substring(0, fragmentStart)
        val fragment = url.substring(fragmentStart)
        val queryStart = withoutFragment.indexOf('?')
        if (queryStart < 0) return url
        val base = withoutFragment.substring(0, queryStart)
        val retainedParameters = withoutFragment.substring(queryStart + 1)
            .split('&')
            .filter { it.isNotEmpty() && !it.isSdmParameter() }
        return if (retainedParameters.isEmpty()) {
            base + fragment
        } else {
            "$base?${retainedParameters.joinToString("&")}$fragment"
        }
    }

    fun hasSdmPlaceholders(url: String): Boolean {
        val fragmentStart = url.indexOf('#').let { if (it < 0) url.length else it }
        val query = url.substring(0, fragmentStart).substringAfter('?', "")
        val parameters = query.split('&').filter { it.isNotEmpty() }
        val piccData = parameters.singleOrNull { it.parameterName().equals("e", true) }
            ?.substringAfter('=', "")
        val mac = parameters.singleOrNull { it.parameterName().equals("c", true) }
            ?.substringAfter('=', "")
        return piccData == "0".repeat(32) && mac == "0".repeat(16)
    }

    fun planSdmDisable(sdmEnabled: Boolean, currentUrl: String?): Ntag424SdmDisablePlan {
        val cleanedUrl = currentUrl?.let(::removeSdmParameters)
            ?.takeIf { it != currentUrl }
        val canRetryUrlCleanup = currentUrl?.let(::hasSdmPlaceholders) == true
        check(sdmEnabled || canRetryUrlCleanup) {
            "SDM 尚未开启，URL 中也没有可清理的 SDM 占位参数"
        }
        return Ntag424SdmDisablePlan(
            disableFileSettings = sdmEnabled,
            cleanedUrl = cleanedUrl
        )
    }

    private fun String.isSdmParameter(): Boolean =
        parameterName().let { it.equals("e", true) || it.equals("c", true) }

    private fun String.parameterName(): String = substringBefore('=')

    /** Uses RFC 3986 query-safe characters to keep the SDM NDEF within the 256-byte file. */
    fun compactUrlForSdm(url: String): String = url.replace(
        Regex("%3A%2F%2F", RegexOption.IGNORE_CASE),
        "://"
    )

    fun selectFile(fileId: Int): ByteArray = byteArrayOf(
        0x00, 0xA4.toByte(), 0x00, 0x0C, 0x02,
        (fileId ushr 8).toByte(), fileId.toByte()
    )

    fun readBinary(offset: Int, length: Int): ByteArray {
        require(offset in 0..0xFFFF) { "Offset out of range" }
        require(length in 1..256) { "Read length must be 1..256" }
        return byteArrayOf(
            0x00, 0xB0.toByte(), (offset ushr 8).toByte(), offset.toByte(),
            if (length == 256) 0 else length.toByte()
        )
    }

    fun isSuccessfulNativeStatus(command: Int, status: Int, responseDataLength: Int): Boolean =
        status == 0x00 ||
            (command == READ_SIGNATURE_COMMAND && status == 0x90 &&
                responseDataLength == ORIGINALITY_SIGNATURE_LENGTH)

    fun parseVersion(data: ByteArray): Ntag424Version {
        require(data.size >= 28) { "GetVersion response is too short: ${data.size}" }
        val uid = data.copyOfRange(14, 21)
        return Ntag424Version(
            hardwareVendorId = data.u8(0),
            hardwareType = data.u8(1),
            hardwareMajorVersion = data.u8(3),
            hardwareStorageSize = data.u8(5),
            softwareMajorVersion = data.u8(10),
            softwareMinorVersion = data.u8(11),
            uid = uid,
            productionWeek = data.getOrNull(26)?.toInt()?.and(0x7F),
            productionYear = data.getOrNull(27)?.toInt()?.and(0xFF)
        )
    }

    fun parseFileSettings(data: ByteArray): Ntag424FileSettings {
        require(data.size >= 7) { "GetFileSettings response is too short: ${data.size}" }
        val fileOption = data.u8(1)
        val accessRights = data.u8(2) or (data.u8(3) shl 8)
        val sdmEnabled = fileOption and 0x40 != 0
        if (sdmEnabled) require(data.size >= 10) { "SDM file settings are incomplete" }
        val sdmRights = if (sdmEnabled) data.u8(8) or (data.u8(9) shl 8) else 0
        return Ntag424FileSettings(
            fileType = data.u8(0),
            fileOption = fileOption,
            communicationMode = fileOption and 0x03,
            readKey = accessRights ushr 12 and 0x0F,
            writeKey = accessRights ushr 8 and 0x0F,
            readWriteKey = accessRights ushr 4 and 0x0F,
            changeKey = accessRights and 0x0F,
            fileSize = data.u24LittleEndian(4),
            sdmEnabled = sdmEnabled,
            sdmOptions = if (sdmEnabled) data.u8(7) else null,
            sdmMetaReadKey = if (sdmEnabled) sdmRights ushr 12 and 0x0F else null,
            sdmFileReadKey = if (sdmEnabled) sdmRights ushr 8 and 0x0F else null,
            sdmCounterRetrieveKey = if (sdmEnabled) sdmRights and 0x0F else null,
            sdmConfiguration = if (sdmEnabled) data.copyOfRange(7, data.size) else null
        )
    }

    private const val DEMO_KEY_NUMBER = 0
    private const val FREE_ACCESS = 0x0E

    fun parseCapabilityContainer(data: ByteArray): Pair<Int, Int> {
        require(data.size >= 7) { "Capability Container is too short" }
        val declaredLength = data.u16BigEndian(0)
        require(declaredLength in 7..data.size) { "Invalid Capability Container length" }
        var offset = 7
        while (offset + 1 < declaredLength) {
            val type = data.u8(offset)
            if (type == 0x00) {
                offset++
                continue
            }
            if (type == 0xFE) break
            val length = data.u8(offset + 1)
            require(offset + 2 + length <= declaredLength) { "Invalid Capability Container TLV" }
            if (type == 0x04 && length == 6) {
                return data.u16BigEndian(offset + 2) to data.u16BigEndian(offset + 4)
            }
            offset += 2 + length
        }
        throw IOException("NDEF File Control TLV was not found")
    }

    private fun nativeCommand(command: Int, data: ByteArray = byteArrayOf()): ByteArray =
        if (data.isEmpty()) {
            byteArrayOf(0x90.toByte(), command.toByte(), 0x00, 0x00, 0x00)
        } else {
            byteArrayOf(0x90.toByte(), command.toByte(), 0x00, 0x00, data.size.toByte()) +
                data + byteArrayOf(0x00)
        }

    private fun ByteArray.u8(offset: Int): Int = this[offset].toInt() and 0xFF

    private fun ByteArray.u16BigEndian(offset: Int): Int = (u8(offset) shl 8) or u8(offset + 1)

    private fun ByteArray.u24LittleEndian(offset: Int): Int =
        u8(offset) or (u8(offset + 1) shl 8) or (u8(offset + 2) shl 16)

    private fun Int.toLittleEndian24(): ByteArray =
        byteArrayOf(toByte(), (this ushr 8).toByte(), (this ushr 16).toByte())

    private const val READ_SIGNATURE_COMMAND = 0x3C
    private const val ORIGINALITY_SIGNATURE_LENGTH = 56
}
