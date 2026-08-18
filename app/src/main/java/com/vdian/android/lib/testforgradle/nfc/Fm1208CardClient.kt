package com.vdian.android.lib.testforgradle.nfc

import android.net.Uri
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import android.util.Log
import java.io.IOException

/** Selects the Android NDEF path or the native FMCOS path from the Tag technologies. */
object Fm1208CardClient {

    sealed class Content {
        data class NdefContent(
            val uri: Uri?,
            val applicationPackage: String?,
            val recordCount: Int
        ) : Content()

        data class FmcosContent(
            val profile: Fm1208FmcosClient.Profile,
            val uri: Uri?,
            val applicationPackage: String?,
            val recordCount: Int
        ) : Content()
    }

    enum class CardType { NDEF, FMCOS }

    fun read(tag: Tag): Content {
        val ndef = Ndef.get(tag)
        if (ndef != null) {
            Log.i(LOG_TAG, "$LOG_PREFIX detected cardType=NDEF")
            return readNdef(ndef)
        }
        if (NdefFormatable.get(tag) != null) {
            Log.i(LOG_TAG, "$LOG_PREFIX detected cardType=NDEF_FORMATABLE")
            return Content.NdefContent(null, null, 0)
        }
        Log.i(LOG_TAG, "$LOG_PREFIX detected cardType=FMCOS")
        return Fm1208FmcosClient.connect(tag).use { client ->
            val profile = client.inspect()
            val businessMessage = client.readBusinessMessageOrNull()
            val businessContent = businessMessage
                ?.takeIf { it.isNotEmpty() }
                ?.let { parseNdefMessage(NdefMessage(it)) }
            Content.FmcosContent(
                profile = profile,
                uri = businessContent?.uri,
                applicationPackage = businessContent?.applicationPackage,
                recordCount = businessContent?.recordCount ?: 0
            )
        }
    }

    fun writeNdef(tag: Tag, url: String, applicationPackage: String): CardType {
        val message = createBusinessMessage(url, applicationPackage)
        Ndef.get(tag)?.let { ndef ->
            Log.i(LOG_TAG, "$LOG_PREFIX write cardType=NDEF")
            writeNdef(ndef, message)
            return CardType.NDEF
        }
        NdefFormatable.get(tag)?.let { formatable ->
            Log.i(LOG_TAG, "$LOG_PREFIX write cardType=NDEF_FORMATABLE")
            formatNdef(formatable, message)
            return CardType.NDEF
        }
        throw IOException(
            "检测到 FMCOS 卡片，请选择“写入 FMCOS 卡片”"
        )
    }

    fun writeFmcos(tag: Tag, url: String, applicationPackage: String): CardType {
        requireValidBusinessData(url, applicationPackage)
        if (Ndef.get(tag) != null || NdefFormatable.get(tag) != null) {
            throw IOException("检测到 NDEF 卡片，请选择“写入 NDEF 卡片”")
        }
        Fm1208FmcosClient.connect(tag).use { client ->
            client.writeBusinessMessage(createBusinessMessage(url, applicationPackage).toByteArray())
        }
        return CardType.FMCOS
    }

    fun initializeFmcosTestCard(
        tag: Tag,
        url: String,
        applicationPackage: String
    ): Fm1208FmcosClient.InitializationResult {
        requireValidBusinessData(url, applicationPackage)
        if (Ndef.get(tag) != null || NdefFormatable.get(tag) != null) {
            throw IOException("检测到 NDEF 卡片，拒绝执行 FMCOS 测试卡初始化")
        }
        val uid = tag.id.toHex()
        if (uid != Fm1208TestCardProtocol.EXPECTED_UID_HEX) {
            throw IOException(
                "测试卡 UID 不匹配，期望 ${Fm1208TestCardProtocol.EXPECTED_UID_HEX}，实际 $uid"
            )
        }
        val message = createBusinessMessage(url, applicationPackage).toByteArray()
        return Fm1208FmcosClient.connect(tag).use { client ->
            client.initializeTestCard(message)
        }
    }

    internal fun parseNdefMessage(message: NdefMessage): Content.NdefContent {
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
        return Content.NdefContent(uri, applicationPackage, message.records.size)
    }

    private fun readNdef(ndef: Ndef): Content.NdefContent {
        try {
            ndef.connect()
            val message = ndef.cachedNdefMessage ?: ndef.ndefMessage
                ?: return Content.NdefContent(null, null, 0)
            return parseNdefMessage(message)
        } finally {
            closeQuietly(ndef)
        }
    }

    private fun writeNdef(ndef: Ndef, message: NdefMessage) {
        try {
            ndef.connect()
            if (!ndef.isWritable) throw IOException("NDEF tag is read-only")
            if (message.byteArrayLength > ndef.maxSize) {
                throw IOException(
                    "NDEF message needs ${message.byteArrayLength} bytes, tag capacity is ${ndef.maxSize}"
                )
            }
            ndef.writeNdefMessage(message)
        } finally {
            closeQuietly(ndef)
        }
    }

    private fun formatNdef(formatable: NdefFormatable, message: NdefMessage) {
        try {
            formatable.connect()
            formatable.format(message)
        } finally {
            try {
                formatable.close()
            } catch (_: Exception) {
            }
        }
    }

    internal fun createBusinessMessage(url: String, applicationPackage: String): NdefMessage {
        requireValidBusinessData(url, applicationPackage)
        val uri = Uri.parse(url)
        return NdefMessage(
            arrayOf(
                NdefRecord.createUri(uri),
                NdefRecord.createApplicationRecord(applicationPackage)
            )
        )
    }

    private fun requireValidBusinessData(url: String, applicationPackage: String) {
        val uri = Uri.parse(url)
        require(uri.scheme == "https" && !uri.host.isNullOrBlank()) { "A valid HTTPS URL is required" }
        require(applicationPackage.isNotBlank()) { "Application package must not be blank" }
    }

    private fun closeQuietly(ndef: Ndef) {
        try {
            ndef.close()
        } catch (_: Exception) {
        }
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02X".format(it) }

    private val ANDROID_PACKAGE_TYPE = "android.com:pkg".toByteArray(Charsets.US_ASCII)
    private const val LOG_TAG = "Fm1208CardClient"
    private const val LOG_PREFIX = "[FM1208-APDU]"
}
