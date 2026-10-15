package com.vdian.android.lib.testforgradle.nfc

import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NfcA

internal object HkNtag215Writer {
    fun write(tag: Tag, record: HkNtag215Record): Int {
        val nfca = checkNotNull(NfcA.get(tag)) { "当前卡片不支持 NFC-A" }
        nfca.use {
            it.connect()
            check(HkNtag215Data.isNtag215(it.transceive(byteArrayOf(0x60)))) {
                "当前卡片不是 NTAG215，未执行写入"
            }
        }
        val message = NdefMessage(arrayOf(
            NdefRecord.createUri(record.url),
            NdefRecord.createApplicationRecord(record.packageName)
        ))
        val expected = message.toByteArray()
        val ndef = checkNotNull(Ndef.get(tag)) { "卡片尚未格式化为 NDEF，请先格式化后重试" }
        ndef.use {
            it.connect()
            check(it.isWritable) { "卡片 NDEF 不可写，可能已锁定或设置写保护" }
            check(expected.size <= it.maxSize) {
                "NDEF 数据 ${expected.size} bytes 超出卡片容量 ${it.maxSize} bytes"
            }
            it.writeNdefMessage(message)
            val actual = it.ndefMessage?.toByteArray()
            check(actual != null && expected.contentEquals(actual)) { "写入后的 NDEF 回读校验失败" }
        }
        return expected.size
    }
}
