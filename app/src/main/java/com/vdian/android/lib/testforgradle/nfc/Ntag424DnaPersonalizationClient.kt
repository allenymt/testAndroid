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
import java.security.MessageDigest
import java.security.SecureRandom

/** Personalizes a recognized NTAG 424 DNA card with versioned, fixed Demo keys. */
internal class Ntag424DnaPersonalizationClient private constructor(
    private val isoDep: IsoDep,
    private val random: SecureRandom = SecureRandom()
) : Closeable {
    data class Result(val ndefBytes: Int, val piccDataOffset: Int, val macOffset: Int)
    data class WriteResult(val ndefBytes: Int, val recordCount: Int)
    data class WriteProtectionResult(val protected: Boolean, val writeKey: Int, val readWriteKey: Int)
    data class DisableSdmResult(val settingsChanged: Boolean, val urlParametersRemoved: Boolean)
    data class File03Result(val text: String, val payloadBytes: Int, val rawBytes: ByteArray)
    data class ChangeKeyResult(val keyNumber: Int, val oldVersion: Int, val newVersion: Int)
    data class RestoreKeysResult(
        val initialKeyVersions: Map<Int, Int>,
        val restoredKeys: List<Int>
    )
    data class CompositeSetupResult(
        val initialKeyVersions: Map<Int, Int>,
        val changedKeys: List<Int>,
        val writeResult: WriteResult,
        val sdmResult: Result
    )

    private data class Session(
        val transactionId: ByteArray,
        val encKey: ByteArray,
        val macKey: ByteArray,
        var commandCounter: Int = 0
    )

    fun writeBusinessNdefWithDemoKey0(url: String, applicationPackage: String): WriteResult {
        Log.w(LOG_TAG, "$LOG_PREFIX business NDEF write started with Demo Key 0")
        exchangeIso(Ntag424DnaProtocol.SELECT_NDEF_APPLICATION, "select NDEF application")
        requireRecognizedCard()
        val currentSettings = readFileSettings()
        check(!currentSettings.sdmEnabled) { "SDM 已开启；请先关闭 SDM 后再覆盖业务 NDEF" }
        check(canWriteWithDemoKey0(currentSettings)) { "File 02 写权限不允许 Demo Key 0 写入" }
        val message = createBusinessMessage(url, applicationPackage)
        val messageBytes = message.toByteArray()
        check(messageBytes.size + NLEN_SIZE <= currentSettings.fileSize) {
            "NDEF message 为 ${messageBytes.size} bytes，超过文件可用容量 ${currentSettings.fileSize - NLEN_SIZE} bytes"
        }

        val session = authenticateDemoKey(0)
        writeNdefAtomically(session, currentSettings, messageBytes, "write business NDEF")
        Log.w(
            LOG_TAG,
            "$LOG_PREFIX business NDEF write completed, ndefBytes=${messageBytes.size + NLEN_SIZE}, " +
                "records=${message.records.size}"
        )
        return WriteResult(messageBytes.size + NLEN_SIZE, message.records.size)
    }

    fun enableSdmWithDemoKey0(): Result {
        Log.w(LOG_TAG, "$LOG_PREFIX personalization started with Demo Key 0")
        exchangeIso(Ntag424DnaProtocol.SELECT_NDEF_APPLICATION, "select NDEF application")
        requireRecognizedCard()
        val currentSettings = readFileSettings()
        check(!currentSettings.sdmEnabled) { "SDM 已开启，拒绝重复个性化" }
        check(currentSettings.changeKey == 0) { "File 02 Change 权限不是 Key 0" }
        check(canWriteWithDemoKey0(currentSettings)) { "File 02 写权限不允许 Demo Key 0 写入" }
        val currentMessage = readCurrentNdefMessage()
        val template = buildTemplate(currentMessage, currentSettings.fileSize)

        val session = authenticateDemoKey(0)
        writeNdefAtomically(session, currentSettings, template.messageBytes, "write SDM NDEF template")
        changeFileSettingsFull(
            session,
            currentSettings,
            template.piccDataOffset,
            template.macOffset
        )
        Log.w(
            LOG_TAG,
            "$LOG_PREFIX personalization completed, ndefBytes=${template.fileBytesSize}, " +
                "piccDataOffset=${template.piccDataOffset}, macOffset=${template.macOffset}"
        )
        return Result(template.fileBytesSize, template.piccDataOffset, template.macOffset)
    }

    fun setWriteProtectionWithDemoKey(protected: Boolean): WriteProtectionResult {
        exchangeIso(Ntag424DnaProtocol.SELECT_NDEF_APPLICATION, "select NDEF application")
        requireRecognizedCard()
        val currentSettings = readFileSettings()
        check(currentSettings.changeKey == DEMO_KEY_NUMBER) {
            "File 02 Change 权限不是 Demo Key $DEMO_KEY_NUMBER"
        }
        val expectedKey = if (protected) DEMO_KEY_NUMBER else FREE_ACCESS
        check(currentSettings.writeKey != expectedKey || currentSettings.readWriteKey != expectedKey) {
            if (protected) "File 02 写保护已开启" else "File 02 写保护已移除"
        }

        val session = authenticateDemoKey(0)
        val data = Ntag424DnaProtocol.buildWriteProtectionFileSettings(currentSettings, protected)
        secureFullCommand(
            session,
            CHANGE_FILE_SETTINGS_COMMAND,
            byteArrayOf(NDEF_FILE_NUMBER.toByte()),
            data,
            if (protected) "enable File 02 write protection" else "disable File 02 write protection"
        )
        return WriteProtectionResult(protected, expectedKey, expectedKey)
    }

    fun disableSdmWithDemoKey(): DisableSdmResult {
        exchangeIso(Ntag424DnaProtocol.SELECT_NDEF_APPLICATION, "select NDEF application")
        requireRecognizedCard()
        val currentSettings = readFileSettings()
        check(currentSettings.changeKey == DEMO_KEY_NUMBER) {
            "File 02 Change 权限不是 Demo Key $DEMO_KEY_NUMBER"
        }
        val currentMessage = readCurrentNdefMessageOrNull()
        val uriRecordIndex = currentMessage?.records?.indexOfFirst {
            it.tnf == NdefRecord.TNF_WELL_KNOWN && it.type.contentEquals(NdefRecord.RTD_URI)
        } ?: -1
        val currentUrl = if (uriRecordIndex >= 0) {
            currentMessage?.records?.get(uriRecordIndex)?.toUri()?.toString()
                ?: error("当前 NDEF URI 无法解析")
        } else {
            null
        }
        val disablePlan = Ntag424DnaProtocol.planSdmDisable(currentSettings.sdmEnabled, currentUrl)
        val cleanedMessage = disablePlan.cleanedUrl?.let { cleanedUrl ->
            val records = checkNotNull(currentMessage).records.copyOf()
            records[uriRecordIndex] = NdefRecord.createUri(Uri.parse(cleanedUrl))
            NdefMessage(records)
        }
        check(cleanedMessage == null || canWriteWithDemoKey0(currentSettings)) {
            "File 02 写权限不允许 Demo Key 0 清理 SDM URL 参数；SDM 设置尚未改变"
        }

        val session = authenticateDemoKey(0)
        if (disablePlan.disableFileSettings) {
            secureFullCommand(
                session,
                CHANGE_FILE_SETTINGS_COMMAND,
                byteArrayOf(NDEF_FILE_NUMBER.toByte()),
                Ntag424DnaProtocol.buildDisabledSdmFileSettings(currentSettings),
                "disable SDM on NDEF file"
            )
        }
        cleanedMessage?.let {
            val messageBytes = it.toByteArray()
            check(messageBytes.size + NLEN_SIZE <= currentSettings.fileSize) {
                "清理 SDM 参数后的 NDEF 超过 File 02 容量"
            }
            writeNdefAtomically(session, currentSettings, messageBytes, "remove SDM URL parameters")
        }
        return DisableSdmResult(disablePlan.disableFileSettings, cleanedMessage != null)
    }

    fun writeFile03DemoData(text: String): File03Result {
        val payload = text.toByteArray(Charsets.UTF_8)
        check(payload.size <= FILE_03_SIZE - FILE_03_LENGTH_SIZE) {
            "File 03 测试文本最多 ${FILE_03_SIZE - FILE_03_LENGTH_SIZE} bytes UTF-8"
        }
        exchangeIso(Ntag424DnaProtocol.SELECT_NDEF_APPLICATION, "select NDEF application")
        requireRecognizedCard()
        val settings = readFileSettings(FILE_03_NUMBER)
        validateFile03Settings(settings)
        val fileData = ByteArray(FILE_03_SIZE)
        fileData[0] = (payload.size ushr 8).toByte()
        fileData[1] = payload.size.toByte()
        payload.copyInto(fileData, FILE_03_LENGTH_SIZE)
        val writeKey = requireDemoAccessKey(settings.writeKey, settings.readWriteKey, "写入")
        val session = authenticateDemoKey(writeKey)
        writeFull(session, FILE_03_NUMBER, 0, fileData, "write File 03 demo data")
        return File03Result(text, payload.size, fileData)
    }

    fun readFile03DemoData(): File03Result {
        exchangeIso(Ntag424DnaProtocol.SELECT_NDEF_APPLICATION, "select NDEF application")
        requireRecognizedCard()
        val settings = readFileSettings(FILE_03_NUMBER)
        validateFile03Settings(settings)
        val readKey = requireDemoAccessKey(settings.readKey, settings.readWriteKey, "读取")
        val session = authenticateDemoKey(readKey)
        val raw = readFull(session, FILE_03_NUMBER, 0, FILE_03_SIZE, "read File 03 demo data")
        val payloadSize = ((raw[0].toInt() and 0xFF) shl 8) or (raw[1].toInt() and 0xFF)
        check(payloadSize in 0..FILE_03_SIZE - FILE_03_LENGTH_SIZE) {
            "File 03 Demo 数据长度无效: $payloadSize"
        }
        val payload = raw.copyOfRange(FILE_03_LENGTH_SIZE, FILE_03_LENGTH_SIZE + payloadSize)
        return File03Result(payload.toString(Charsets.UTF_8), payloadSize, raw)
    }

    fun changeDemoKey(keyNumber: Int): ChangeKeyResult {
        require(keyNumber in 0..2) { "Demo 仅支持修改 Key 0、1、2" }
        exchangeIso(Ntag424DnaProtocol.SELECT_NDEF_APPLICATION, "select NDEF application")
        requireRecognizedCard()
        val oldVersion = readKeyVersion(keyNumber)
        check(oldVersion != Ntag424DemoKeys.PERSONALIZED_VERSION) {
            "AppKey $keyNumber 已是 Demo version ${Ntag424DemoKeys.PERSONALIZED_VERSION}"
        }
        val oldKey = Ntag424DemoKeys.valueForVersion(keyNumber, oldVersion)
        val newKey = Ntag424DemoKeys.personalizedValue(keyNumber)
        return changeKey(keyNumber, oldVersion, oldKey, Ntag424DemoKeys.PERSONALIZED_VERSION, newKey)
    }

    fun restoreDemoKeysToTransport(): RestoreKeysResult {
        exchangeIso(Ntag424DnaProtocol.SELECT_NDEF_APPLICATION, "select NDEF application")
        requireRecognizedCard()
        val initialVersions = (0..2).associateWith(::readKeyVersion)
        val keysToRestore = Ntag424DemoKeys.planTransportRestore(initialVersions)

        val restoredKeys = mutableListOf<Int>()
        keysToRestore.forEach { keyNumber ->
            // The previous write-back verification leaves an EV2 session active on that key.
            // Reselect before the next plain GetKeyVersion/AuthenticateEV2First sequence.
            exchangeIso(
                Ntag424DnaProtocol.SELECT_NDEF_APPLICATION,
                "reselect application before restoring AppKey $keyNumber"
            )
            val oldVersion = initialVersions.getValue(keyNumber)
            val oldKey = Ntag424DemoKeys.valueForVersion(keyNumber, oldVersion)
            changeKey(keyNumber, oldVersion, oldKey, TRANSPORT_KEY_VERSION, Ntag424DemoKeys.transportKey)
            restoredKeys += keyNumber
        }
        return RestoreKeysResult(initialVersions, restoredKeys)
    }

    fun personalizeDemoKeysWriteNdefAndEnableSdm(
        url: String,
        applicationPackage: String
    ): CompositeSetupResult {
        exchangeIso(Ntag424DnaProtocol.SELECT_NDEF_APPLICATION, "select NDEF application")
        requireRecognizedCard()
        val initialVersions = (0..2).associateWith(::readKeyVersion)
        val keysToChange = Ntag424DemoKeys.planPersonalization(initialVersions)
        val initialSettings = readFileSettings()
        check(!initialSettings.sdmEnabled) { "File 02 已开启 SDM；复合操作要求初始状态为关闭" }
        check(initialSettings.changeKey == DEMO_KEY_NUMBER) {
            "File 02 Change 权限不是 Demo Key $DEMO_KEY_NUMBER"
        }
        check(canWriteWithDemoKey0(initialSettings)) { "File 02 写权限不允许 Demo Key 0 写入" }
        val messageBytes = createBusinessMessage(url, applicationPackage).toByteArray()
        check(messageBytes.size + NLEN_SIZE <= initialSettings.fileSize) {
            "NDEF message 为 ${messageBytes.size} bytes，超过文件可用容量 " +
                "${initialSettings.fileSize - NLEN_SIZE} bytes"
        }

        val changedKeys = mutableListOf<Int>()
        listOf(1, 2, 0).forEach { keyNumber ->
            if (keyNumber in keysToChange) {
                changeDemoKey(keyNumber)
                changedKeys += keyNumber
            } else {
                exchangeIso(
                    Ntag424DnaProtocol.SELECT_NDEF_APPLICATION,
                    "reselect application before validating AppKey $keyNumber"
                )
                authenticateDemoKey(keyNumber)
            }
        }

        val writeResult = writeBusinessNdefWithDemoKey0(url, applicationPackage)
        val sdmResult = enableSdmWithDemoKey0()
        return CompositeSetupResult(initialVersions, changedKeys, writeResult, sdmResult)
    }

    private fun changeKey(
        keyNumber: Int,
        oldVersion: Int,
        oldKey: ByteArray,
        newVersion: Int,
        newKey: ByteArray
    ): ChangeKeyResult {
        val session = authenticateDemoKey(0)
        secureFullCommand(
            session,
            CHANGE_KEY_COMMAND,
            byteArrayOf(keyNumber.toByte()),
            Ntag424DemoKeys.buildChangeKeyData(keyNumber, oldKey, newKey, newVersion),
            "change AppKey $keyNumber to version $newVersion",
            allowEmptyResponseMac = keyNumber == 0
        )
        exchangeIso(Ntag424DnaProtocol.SELECT_NDEF_APPLICATION, "reselect application after ChangeKey")
        val actualVersion = readKeyVersion(keyNumber)
        check(actualVersion == newVersion) {
            "AppKey $keyNumber version 写后校验失败: expected=$newVersion, actual=$actualVersion"
        }
        authenticateDemoKey(keyNumber)
        return ChangeKeyResult(keyNumber, oldVersion, newVersion)
    }

    private fun authenticateDemoKey(keyNumber: Int): Session {
        require(keyNumber in 0..4) { "Invalid AppKey number: $keyNumber" }
        val keyVersion = readKeyVersion(keyNumber)
        val authenticationKey = Ntag424DemoKeys.valueForVersion(keyNumber, keyVersion)
        Log.i(LOG_TAG, "$LOG_PREFIX AuthenticateEV2First started, keyNo=$keyNumber")
        val firstCommand = byteArrayOf(
            0x90.toByte(), 0x71, 0x00, 0x00, 0x02, keyNumber.toByte(), 0x00, 0x00
        )
        val first = transceive(firstCommand, "authenticate EV2 first part")
        requireStatus(first, 0x91AF, "authenticate EV2 first part")
        val encryptedRndB = first.copyOf(first.size - 2)
        check(encryptedRndB.size == 16) { "Invalid encrypted RndB length" }
        val rndB = Ntag424DnaEv2Crypto.decryptCbc(authenticationKey, ZERO_IV, encryptedRndB)
        val rndA = ByteArray(16).also(random::nextBytes)
        val challenge = rndA + Ntag424DnaEv2Crypto.rotateLeft(rndB)
        val encryptedChallenge = Ntag424DnaEv2Crypto.encryptCbc(authenticationKey, ZERO_IV, challenge)
        val secondCommand = byteArrayOf(0x90.toByte(), 0xAF.toByte(), 0x00, 0x00, 0x20) +
            encryptedChallenge + byteArrayOf(0x00)
        val second = transceive(secondCommand, "authenticate EV2 second part")
        requireStatus(second, 0x9100, "authenticate EV2 second part")
        val decrypted = Ntag424DnaEv2Crypto.decryptCbc(
            authenticationKey,
            ZERO_IV,
            second.copyOf(second.size - 2)
        )
        check(decrypted.size == 32) { "Invalid EV2 authentication response length" }
        val returnedRndA = Ntag424DnaEv2Crypto.rotateRight(decrypted.copyOfRange(4, 20))
        check(MessageDigest.isEqual(rndA, returnedRndA)) { "Card failed EV2 RndA verification" }
        val transactionId = decrypted.copyOfRange(0, 4)
        val encKey = Ntag424DnaEv2Crypto.deriveSessionKey(authenticationKey, ENC_LABEL, rndA, rndB)
        val macKey = Ntag424DnaEv2Crypto.deriveSessionKey(authenticationKey, MAC_LABEL, rndA, rndB)
        Log.i(
            LOG_TAG,
            "$LOG_PREFIX AuthenticateEV2First succeeded, keyNo=$keyNumber, " +
                "keyVersion=$keyVersion, TI=${transactionId.toHex()}"
        )
        return Session(transactionId, encKey, macKey)
    }

    private fun readKeyVersion(keyNumber: Int): Int {
        val command = byteArrayOf(
            0x90.toByte(), GET_KEY_VERSION_COMMAND.toByte(), 0x00, 0x00, 0x01,
            keyNumber.toByte(), 0x00
        )
        val response = transceive(command, "get AppKey $keyNumber version")
        requireStatus(response, 0x9100, "get AppKey $keyNumber version")
        check(response.size == 3) { "AppKey $keyNumber version response length is invalid" }
        return response[0].toInt() and 0xFF
    }

    private fun writeNdefAtomically(
        session: Session,
        settings: Ntag424FileSettings,
        messageBytes: ByteArray,
        operation: String
    ) {
        val mode = Ntag424DnaProtocol.writeModeFor(settings)
        writeBytes(session, mode, 0, byteArrayOf(0, 0), "$operation: clear NLEN")
        val maxChunkSize = when (mode) {
            Ntag424WriteMode.PLAIN -> MAX_PLAIN_WRITE_LENGTH
            Ntag424WriteMode.FULL -> MAX_FULL_WRITE_LENGTH
        }
        messageBytes.forEachChunk(maxChunkSize) { offset, chunk ->
            writeBytes(session, mode, offset + NLEN_SIZE, chunk, "$operation at ${offset + NLEN_SIZE}")
        }
        val nlen = byteArrayOf((messageBytes.size ushr 8).toByte(), messageBytes.size.toByte())
        writeBytes(session, mode, 0, nlen, "$operation: commit NLEN")
    }

    private fun writeBytes(
        session: Session,
        mode: Ntag424WriteMode,
        offset: Int,
        data: ByteArray,
        operation: String
    ) {
        when (mode) {
            Ntag424WriteMode.PLAIN -> writePlain(session, offset, data, operation)
            Ntag424WriteMode.FULL -> writeFull(session, offset, data, operation)
        }
    }

    private fun writeFull(session: Session, offset: Int, data: ByteArray, operation: String) =
        writeFull(session, NDEF_FILE_NUMBER, offset, data, operation)

    private fun writeFull(
        session: Session,
        fileNumber: Int,
        offset: Int,
        data: ByteArray,
        operation: String
    ) {
        val header = byteArrayOf(fileNumber.toByte()) + littleEndian24(offset) +
            littleEndian24(data.size)
        secureFullCommand(
            session,
            WRITE_DATA_COMMAND,
            header,
            data,
            operation
        )
    }

    private fun readFull(
        session: Session,
        fileNumber: Int,
        offset: Int,
        length: Int,
        operation: String
    ): ByteArray {
        val header = byteArrayOf(fileNumber.toByte()) + littleEndian24(offset) + littleEndian24(length)
        val counter = littleEndian16(session.commandCounter)
        val commandMac = Ntag424DnaEv2Crypto.truncateMac(
            Ntag424DnaEv2Crypto.cmac(
                session.macKey,
                byteArrayOf(READ_DATA_COMMAND.toByte()) + counter + session.transactionId + header
            )
        )
        val body = header + commandMac
        val apdu = byteArrayOf(
            0x90.toByte(), READ_DATA_COMMAND.toByte(), 0x00, 0x00, body.size.toByte()
        ) + body + byteArrayOf(0x00)
        val response = transceive(apdu, operation)
        requireStatus(response, 0x9100, operation)
        check(response.size >= 10) { "$operation response MAC is missing" }
        val encryptedData = response.copyOfRange(0, response.size - 10)
        val responseMac = response.copyOfRange(response.size - 10, response.size - 2)
        session.commandCounter++
        val expectedMac = Ntag424DnaEv2Crypto.truncateMac(
            Ntag424DnaEv2Crypto.cmac(
                session.macKey,
                byteArrayOf(0x00) + littleEndian16(session.commandCounter) +
                    session.transactionId + encryptedData
            )
        )
        check(MessageDigest.isEqual(responseMac, expectedMac)) { "$operation response MAC verification failed" }
        check(encryptedData.isNotEmpty() && encryptedData.size % 16 == 0) {
            "$operation encrypted response length is invalid"
        }
        val ivInput = byteArrayOf(0x5A, 0xA5.toByte()) + session.transactionId +
            littleEndian16(session.commandCounter) + ByteArray(8)
        val iv = Ntag424DnaEv2Crypto.encryptBlock(session.encKey, ivInput)
        val decrypted = Ntag424DnaEv2Crypto.decryptCbc(session.encKey, iv, encryptedData)
        check(decrypted.size >= length) { "$operation returned ${decrypted.size} bytes, expected $length" }
        return decrypted.copyOf(length)
    }

    private fun writePlain(session: Session, offset: Int, data: ByteArray, operation: String) {
        val response = transceive(
            Ntag424DnaProtocol.writeDataPlain(NDEF_FILE_NUMBER, offset, data),
            "$operation (plain)"
        )
        requireStatus(response, 0x9100, "$operation (plain)")
        // The PICC advances CmdCtr for every successful Plain command while authenticated.
        session.commandCounter++
        Log.i(
            LOG_TAG,
            "$LOG_PREFIX plain NDEF chunk succeeded, offset=$offset, bytes=${data.size}, " +
                "cmdCtr=${session.commandCounter}"
        )
    }

    private fun changeFileSettingsFull(
        session: Session,
        currentSettings: Ntag424FileSettings,
        piccDataOffset: Int,
        macOffset: Int
    ) {
        val data = Ntag424DnaProtocol.buildSdmFileSettings(
            currentSettings,
            piccDataOffset,
            macOffset
        )
        secureFullCommand(
            session,
            CHANGE_FILE_SETTINGS_COMMAND,
            byteArrayOf(NDEF_FILE_NUMBER.toByte()),
            data,
            "enable SDM on NDEF file"
        )
    }

    private fun secureFullCommand(
        session: Session,
        command: Int,
        header: ByteArray,
        data: ByteArray,
        operation: String,
        allowEmptyResponseMac: Boolean = false
    ) {
        val counter = littleEndian16(session.commandCounter)
        val ivInput = byteArrayOf(0xA5.toByte(), 0x5A) + session.transactionId + counter + ByteArray(8)
        val iv = Ntag424DnaEv2Crypto.encryptBlock(session.encKey, ivInput)
        val encryptedData = Ntag424DnaEv2Crypto.encryptCbc(
            session.encKey,
            iv,
            Ntag424DnaEv2Crypto.pad80(data)
        )
        val commandMac = Ntag424DnaEv2Crypto.truncateMac(
            Ntag424DnaEv2Crypto.cmac(
                session.macKey,
                byteArrayOf(command.toByte()) + counter + session.transactionId + header + encryptedData
            )
        )
        val body = header + encryptedData + commandMac
        check(body.size <= 255) { "$operation APDU is too large" }
        val apdu = byteArrayOf(
            0x90.toByte(), command.toByte(), 0x00, 0x00, body.size.toByte()
        ) + body + byteArrayOf(0x00)
        val response = transceive(apdu, operation)
        requireStatus(response, 0x9100, operation)
        if (allowEmptyResponseMac && response.size == 2) {
            session.commandCounter++
            Log.i(LOG_TAG, "$LOG_PREFIX $operation succeeded; authenticated key changed")
            return
        }
        check(response.size >= 10) { "$operation response MAC is missing" }
        val responseData = response.copyOfRange(0, response.size - 10)
        val responseMac = response.copyOfRange(response.size - 10, response.size - 2)
        session.commandCounter++
        val expectedMac = Ntag424DnaEv2Crypto.truncateMac(
            Ntag424DnaEv2Crypto.cmac(
                session.macKey,
                byteArrayOf(0x00) + littleEndian16(session.commandCounter) +
                    session.transactionId + responseData
            )
        )
        check(MessageDigest.isEqual(responseMac, expectedMac)) { "$operation response MAC verification failed" }
        Log.i(LOG_TAG, "$LOG_PREFIX $operation succeeded, cmdCtr=${session.commandCounter}")
    }

    private fun readVersion(): Ntag424Version {
        val data = ArrayList<Byte>()
        var command = Ntag424DnaProtocol.GET_VERSION
        repeat(3) { frame ->
            val response = transceive(command, "get version part ${frame + 1}")
            val expected = if (frame < 2) 0x91AF else 0x9100
            requireStatus(response, expected, "get version part ${frame + 1}")
            data += response.copyOf(response.size - 2).toList()
            command = Ntag424DnaProtocol.ADDITIONAL_FRAME
        }
        return Ntag424DnaProtocol.parseVersion(data.toByteArray())
    }

    private fun readFileSettings(fileNumber: Int = NDEF_FILE_NUMBER): Ntag424FileSettings {
        val response = transceive(
            Ntag424DnaProtocol.getFileSettings(fileNumber),
            "get file $fileNumber settings"
        )
        requireStatus(response, 0x9100, "get file $fileNumber settings")
        return Ntag424DnaProtocol.parseFileSettings(response.copyOf(response.size - 2))
    }

    private fun validateFile03Settings(settings: Ntag424FileSettings) {
        check(settings.fileSize == FILE_03_SIZE) {
            "File 03 容量应为 $FILE_03_SIZE bytes，实际为 ${settings.fileSize}"
        }
        check(settings.communicationMode == FULL_COMMUNICATION_MODE) {
            "File 03 Demo 仅支持 Full communication mode"
        }
    }

    private fun requireDemoAccessKey(primaryKey: Int, readWriteKey: Int, operation: String): Int =
        when {
            primaryKey in 0..4 -> primaryKey
            readWriteKey in 0..4 -> readWriteKey
            else -> error("File 03 $operation 权限未配置为 Demo AppKey 0..4")
        }

    private fun readCurrentNdefMessage(): NdefMessage =
        readCurrentNdefMessageOrNull() ?: error("当前 NDEF 为空")

    private fun readCurrentNdefMessageOrNull(): NdefMessage? {
        exchangeIso(Ntag424DnaProtocol.selectFile(NDEF_FILE_ID), "select NDEF file")
        val nlenBytes = isoData(
            transceive(Ntag424DnaProtocol.readBinary(0, 2), "read current NLEN"),
            "read current NLEN"
        )
        check(nlenBytes.size == 2) { "Invalid current NLEN response" }
        val nlen = ((nlenBytes[0].toInt() and 0xFF) shl 8) or
            (nlenBytes[1].toInt() and 0xFF)
        check(nlen in 0..NDEF_FILE_SIZE - 2) { "当前 NDEF 长度无效: $nlen" }
        if (nlen == 0) {
            Log.i(LOG_TAG, "$LOG_PREFIX current NDEF is empty")
            return null
        }
        val bytes = ByteArrayOutputStream(nlen)
        var offset = 0
        while (offset < nlen) {
            val chunkSize = minOf(nlen - offset, MAX_READ_LENGTH)
            val chunk = isoData(
                transceive(
                    Ntag424DnaProtocol.readBinary(offset + 2, chunkSize),
                    "read current NDEF at $offset"
                ),
                "read current NDEF at $offset"
            )
            check(chunk.size == chunkSize) { "Current NDEF read returned an invalid length" }
            bytes.write(chunk)
            offset += chunkSize
        }
        return NdefMessage(bytes.toByteArray()).also {
            Log.i(LOG_TAG, "$LOG_PREFIX current NDEF read, records=${it.records.size}, bytes=$nlen")
        }
    }

    private data class Template(val messageBytes: ByteArray, val piccDataOffset: Int, val macOffset: Int) {
        val fileBytesSize: Int get() = messageBytes.size + NLEN_SIZE
    }

    private fun buildTemplate(currentMessage: NdefMessage, maxFileSize: Int): Template {
        val piccPlaceholder = "0".repeat(32)
        val macPlaceholder = "0".repeat(16)
        val uriRecordIndex = currentMessage.records.indexOfFirst {
            it.tnf == NdefRecord.TNF_WELL_KNOWN && it.type.contentEquals(NdefRecord.RTD_URI)
        }
        check(uriRecordIndex >= 0) { "当前 NDEF 不包含 URI record，无法保留原业务链接" }
        val originalUrl = currentMessage.records[uriRecordIndex].toUri()?.toString()
            ?: error("当前 NDEF URI 无法解析")
        check(currentMessage.records.size == 2 && uriRecordIndex == 0) {
            "当前 NDEF 必须是两条 records，且第一条为 URL"
        }
        val packageRecord = currentMessage.records[1]
        check(
            packageRecord.tnf == NdefRecord.TNF_EXTERNAL_TYPE &&
                packageRecord.type.contentEquals(ANDROID_PACKAGE_TYPE) &&
                packageRecord.payload.isNotEmpty()
        ) { "当前 NDEF 第二条不是有效的 android.com:pkg record" }
        val containsCompleteTemplate = validateSdmTemplateIfPresent(
            originalUrl,
            piccPlaceholder,
            macPlaceholder
        )
        val sdmUrl = if (containsCompleteTemplate) {
            originalUrl
        } else {
            val compactUrl = Ntag424DnaProtocol.compactUrlForSdm(originalUrl)
            Ntag424DnaProtocol.buildSdmUrl(compactUrl, piccPlaceholder, macPlaceholder)
        }
        val records = currentMessage.records.copyOf()
        records[uriRecordIndex] = NdefRecord.createUri(Uri.parse(sdmUrl))
        val message = NdefMessage(records).toByteArray()
        val fileBytes = byteArrayOf((message.size ushr 8).toByte(), message.size.toByte()) + message
        val piccDataOffset = findPlaceholderValueOffset(fileBytes, "e", piccPlaceholder)
        val macOffset = findPlaceholderValueOffset(fileBytes, "c", macPlaceholder)
        check(fileBytes.size <= maxFileSize) {
            "保留原 NDEF 后 SDM 模板为 ${fileBytes.size} bytes，超过文件容量 $maxFileSize bytes"
        }
        Log.i(
            LOG_TAG,
            "$LOG_PREFIX SDM template preserves ${records.size} records, " +
                "originalUriLength=${originalUrl.length}, sdmUriLength=${sdmUrl.length}"
        )
        return Template(message, piccDataOffset, macOffset)
    }

    private fun validateSdmTemplateIfPresent(
        url: String,
        piccPlaceholder: String,
        macPlaceholder: String
    ): Boolean {
        val outer = Uri.parse(url)
        val outerValues = outer.sdmValues()
        if (outerValues == null) return false

        val (encryptedPiccData, mac) = outerValues
        check(encryptedPiccData == piccPlaceholder && mac == macPlaceholder) {
            "外层 URL 必须包含 32 个 0 的 e 和 16 个 0 的 c 占位符"
        }
        return true
    }

    private fun Uri.sdmValues(): Pair<String, String>? {
        val encryptedPiccData = getQueryParameters("e")
        val mac = getQueryParameters("c")
        check(encryptedPiccData.size <= 1 && mac.size <= 1) { "URL 中 e/c 参数必须唯一" }
        check(encryptedPiccData.isEmpty() == mac.isEmpty()) { "URL 必须同时包含 e 和 c" }
        return if (encryptedPiccData.isEmpty()) null else encryptedPiccData.single() to mac.single()
    }

    private fun findPlaceholderValueOffset(
        fileBytes: ByteArray,
        parameterName: String,
        placeholder: String
    ): Int {
        val fileText = fileBytes.toString(Charsets.ISO_8859_1)
        val markers = listOf(
            "$parameterName=$placeholder",
            "$parameterName%3D$placeholder"
        )
        val matches = markers.mapNotNull { marker ->
            fileText.indexOf(marker, ignoreCase = true)
                .takeIf { it >= 0 }
                ?.let { it + marker.length - placeholder.length }
        }
        check(matches.size == 1) { "SDM URL 的 $parameterName 占位符缺失或不唯一" }
        return matches.single()
    }

    private fun createBusinessMessage(url: String, applicationPackage: String): NdefMessage {
        val uri = Uri.parse(url)
        require(uri.scheme == "https" && !uri.host.isNullOrBlank()) { "请输入有效的 HTTPS URL" }
        require(applicationPackage.matches(PACKAGE_NAME_PATTERN)) { "请输入有效的 Android 包名" }
        return NdefMessage(
            arrayOf(
                NdefRecord.createUri(uri),
                NdefRecord.createApplicationRecord(applicationPackage)
            )
        )
    }

    private fun requireRecognizedCard() {
        check(readVersion().looksLikeNtag424Dna) { "标签不是已识别的 NTAG 424 DNA" }
    }

    private fun canWriteWithDemoKey0(settings: Ntag424FileSettings): Boolean =
        settings.writeKey == 0 || settings.writeKey == FREE_ACCESS ||
            settings.readWriteKey == 0 || settings.readWriteKey == FREE_ACCESS

    private fun exchangeIso(command: ByteArray, operation: String) {
        requireStatus(transceive(command, operation), 0x9000, operation)
    }

    private fun isoData(response: ByteArray, operation: String): ByteArray {
        requireStatus(response, 0x9000, operation)
        return response.copyOf(response.size - 2)
    }

    private fun transceive(command: ByteArray, operation: String): ByteArray {
        Log.i(LOG_TAG, "$LOG_PREFIX -> $operation ${command.toSafeLog()}")
        return isoDep.transceive(command).also { Log.i(LOG_TAG, "$LOG_PREFIX <- $operation ${it.toHex()}") }
    }

    private fun requireStatus(response: ByteArray, expected: Int, operation: String) {
        if (response.size < 2) throw IOException("$operation returned an incomplete response")
        val actual = ((response[response.size - 2].toInt() and 0xFF) shl 8) or
            (response.last().toInt() and 0xFF)
        if (actual != expected) {
            throw IOException("$operation failed, status=${actual.toString(16).uppercase().padStart(4, '0')}")
        }
    }

    override fun close() {
        isoDep.close()
        Log.i(LOG_TAG, "$LOG_PREFIX personalization ISO-DEP connection closed")
    }

    private inline fun ByteArray.forEachChunk(
        maxChunkSize: Int,
        action: (offset: Int, chunk: ByteArray) -> Unit
    ) {
        var offset = 0
        while (offset < size) {
            val end = minOf(offset + maxChunkSize, size)
            action(offset, copyOfRange(offset, end))
            offset = end
        }
    }

    companion object {
        private const val LOG_TAG = "Ntag424Personalize"
        private const val LOG_PREFIX = "[NTAG424-SDM-CONFIG]"
        private const val NDEF_FILE_NUMBER = 2
        private const val NDEF_FILE_ID = 0xE104
        private const val NDEF_FILE_SIZE = 256
        private const val NLEN_SIZE = 2
        private const val MAX_READ_LENGTH = 0xF0
        private const val MAX_PLAIN_WRITE_LENGTH = 0xF8
        private const val MAX_FULL_WRITE_LENGTH = 0xE0
        private const val WRITE_DATA_COMMAND = 0x8D
        private const val READ_DATA_COMMAND = 0xAD
        private const val CHANGE_KEY_COMMAND = 0xC4
        private const val TRANSPORT_KEY_VERSION = 0x00
        private const val GET_KEY_VERSION_COMMAND = 0x64
        private const val CHANGE_FILE_SETTINGS_COMMAND = 0x5F
        private const val DEMO_KEY_NUMBER = 0
        private const val FILE_03_NUMBER = 3
        private const val FILE_03_SIZE = 128
        private const val FILE_03_LENGTH_SIZE = 2
        private const val FULL_COMMUNICATION_MODE = 3
        private const val FREE_ACCESS = 0x0E
        private val ZERO_IV = ByteArray(16)
        private val ENC_LABEL = byteArrayOf(0xA5.toByte(), 0x5A)
        private val MAC_LABEL = byteArrayOf(0x5A, 0xA5.toByte())
        private val PACKAGE_NAME_PATTERN = Regex("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+")
        private val ANDROID_PACKAGE_TYPE = "android.com:pkg".toByteArray(Charsets.US_ASCII)

        fun connect(tag: Tag): Ntag424DnaPersonalizationClient {
            val isoDep = IsoDep.get(tag) ?: throw IllegalArgumentException("标签不支持 ISO-DEP")
            isoDep.timeout = 5000
            isoDep.connect()
            return Ntag424DnaPersonalizationClient(isoDep)
        }

        private fun littleEndian16(value: Int): ByteArray =
            byteArrayOf(value.toByte(), (value ushr 8).toByte())

        private fun littleEndian24(value: Int): ByteArray =
            byteArrayOf(value.toByte(), (value ushr 8).toByte(), (value ushr 16).toByte())

        private fun ByteArray.indexOf(needle: ByteArray): Int =
            indices.firstOrNull { start ->
                start + needle.size <= size && needle.indices.all { this[start + it] == needle[it] }
            } ?: -1

        private fun ByteArray.countOccurrences(needle: ByteArray): Int = indices.count { start ->
            start + needle.size <= size && needle.indices.all { this[start + it] == needle[it] }
        }

        private fun ByteArray.toSafeLog(): String =
            if (size > 24) "${copyOfRange(0, 5).toHex()} [${size - 6} protected bytes] 00" else toHex()

        private fun ByteArray.toHex(separator: String = ""): String =
            joinToString(separator) { "%02X".format(it) }

        private fun hex(value: String): ByteArray = value.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }
}
