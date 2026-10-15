package com.vdian.android.lib.testforgradle.nfc

import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NfcA

internal data class HkNtag215ReadResult(
    val uid: String,
    val capacity: Int,
    val writable: Boolean,
    val message: NdefMessage?
) {
    val urls: List<String> get() = message?.records.orEmpty().filter {
        (it.tnf == NdefRecord.TNF_WELL_KNOWN && it.type.contentEquals(NdefRecord.RTD_URI)) ||
            it.tnf == NdefRecord.TNF_ABSOLUTE_URI
    }.mapNotNull { it.toUri()?.toString() }
    val packages: List<String> get() = message?.records.orEmpty().filter {
        it.tnf == NdefRecord.TNF_EXTERNAL_TYPE &&
            it.type.contentEquals("android.com:pkg".toByteArray(Charsets.US_ASCII))
    }.map { it.payload.toString(Charsets.UTF_8) }
}

internal object HkNtag215Reader {
    fun read(tag: Tag): HkNtag215ReadResult {
        val nfca = checkNotNull(NfcA.get(tag)) { "当前卡片不支持 NFC-A" }
        nfca.use {
            it.connect()
            check(HkNtag215Data.isNtag215(it.transceive(byteArrayOf(0x60)))) {
                "当前卡片不是 NTAG215"
            }
        }
        val ndef = checkNotNull(Ndef.get(tag)) { "卡片尚未格式化为 NDEF" }
        return ndef.use {
            it.connect()
            HkNtag215ReadResult(
                uid = tag.id.joinToString(":") { value -> "%02X".format(value) },
                capacity = it.maxSize,
                writable = it.isWritable,
                message = it.ndefMessage
            )
        }
    }
}
