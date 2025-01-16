package com.vdian.android.lib.testforgradle.nfc

import android.content.Intent
import android.net.Uri
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.util.Log

/**
 * @author yulun
 * @since 2025-01-15 19:06
 * 常见应用总结
 * 文本传输：使用 Text Record。
 * 链接传输：使用 URI Record。
 * 自定义数据格式：使用 MIME Type Record，适合传递 JSON 或图片。
 * 复合数据：使用 Smart Poster，可组合多个记录。
 * todo
 * 同时处理ACTION_NDEF_DISCOVERED 和 ACTION_TECH_DISCOVERED
 */
class BuyerNFCUtil {
    object Companion {

        private val URI_PREFIX_MAP: MutableMap<Byte, String> = HashMap()

        // 预先定义已知Uri前缀
        init {
            URI_PREFIX_MAP[0x00.toByte()] = ""
            URI_PREFIX_MAP[0x01.toByte()] = "http://www."
            URI_PREFIX_MAP[0x02.toByte()] = "https://www."
            URI_PREFIX_MAP[0x03.toByte()] = "http://"
            URI_PREFIX_MAP[0x04.toByte()] = "https://"
            URI_PREFIX_MAP[0x05.toByte()] = "tel:"
            URI_PREFIX_MAP[0x06.toByte()] = "mailto:"
            URI_PREFIX_MAP[0x07.toByte()] = "ftp://anonymous:anonymous@"
            URI_PREFIX_MAP[0x08.toByte()] = "ftp://ftp."
            URI_PREFIX_MAP[0x09.toByte()] = "ftps://"
            URI_PREFIX_MAP[0x0A.toByte()] = "sftp://"
            URI_PREFIX_MAP[0x0B.toByte()] = "smb://"
            URI_PREFIX_MAP[0x0C.toByte()] = "nfs://"
            URI_PREFIX_MAP[0x0D.toByte()] = "ftp://"
            URI_PREFIX_MAP[0x0E.toByte()] = "dav://"
            URI_PREFIX_MAP[0x0F.toByte()] = "news:"
            URI_PREFIX_MAP[0x10.toByte()] = "telnet://"
            URI_PREFIX_MAP[0x11.toByte()] = "imap:"
            URI_PREFIX_MAP[0x12.toByte()] = "rtsp://"
            URI_PREFIX_MAP[0x13.toByte()] = "urn:"
            URI_PREFIX_MAP[0x14.toByte()] = "pop:"
            URI_PREFIX_MAP[0x15.toByte()] = "sip:"
            URI_PREFIX_MAP[0x16.toByte()] = "sips:"
            URI_PREFIX_MAP[0x17.toByte()] = "tftp:"
            URI_PREFIX_MAP[0x18.toByte()] = "btspp://"
            URI_PREFIX_MAP[0x19.toByte()] = "btl2cap://"
            URI_PREFIX_MAP[0x1A.toByte()] = "btgoep://"
            URI_PREFIX_MAP[0x1B.toByte()] = "tcpobex://"
            URI_PREFIX_MAP[0x1C.toByte()] = "irdaobex://"
            URI_PREFIX_MAP[0x1D.toByte()] = "file://"
            URI_PREFIX_MAP[0x1E.toByte()] = "urn:epc:id:"
            URI_PREFIX_MAP[0x1F.toByte()] = "urn:epc:tag:"
            URI_PREFIX_MAP[0x20.toByte()] = "urn:epc:pat:"
            URI_PREFIX_MAP[0x21.toByte()] = "urn:epc:raw:"
            URI_PREFIX_MAP[0x22.toByte()] = "urn:epc:"
            URI_PREFIX_MAP[0x23.toByte()] = "urn:nfc:"
        }

        fun readNFCInfo(intent: Intent): String {
            val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
            tag?.let {
                val ndef = Ndef.get(it)
                ndef.let {
                    return "${ndef.type}\n max size: ${ndef.maxSize} bytes\n\n"
                }
            }
            return ""
        }

        /**
         * 读取NFC标签Uri
         */
        fun readNfcUri(intent: Intent): List<Uri> {
            val uriList = mutableListOf<Uri>()
            if (NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action) {
                val rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
                val messageSize = rawMsgs?.size ?: 0
                for (i in 0 until messageSize) {
                    val ndefMessage = rawMsgs?.get(i) as? NdefMessage
                    ndefMessage?.let {
                        try {
                            val recordSize = it.records.size
                            for (j in 0 until recordSize) {
                                val ndefRecord = it.records[j]
                                val uri = parse(ndefRecord)
                                uriList.add(uri)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
            return uriList
        }

        /**
         * 解析NdefRecord中Uri数据
         */
        private fun parse(record: NdefRecord): Uri {
            return when (record.tnf) {
                NdefRecord.TNF_WELL_KNOWN -> parseWellKnown(record)
                NdefRecord.TNF_ABSOLUTE_URI -> parseAbsolute(record)
                //  android package 不过这种应该不用处理，因为是针对Android应用的，也就是App已经被卸载了
                NdefRecord.TNF_EXTERNAL_TYPE -> parseExternal(record)
                else -> throw IllegalArgumentException("Unknown TNF ${record.tnf}")
            }
        }

        /**
         * 处理绝对的Uri
         */
        private fun parseAbsolute(ndefRecord: NdefRecord): Uri {
            val payload = ndefRecord.payload
            return Uri.parse(String(payload, Charsets.UTF_8))
        }

        /**
         * 处理额外的数据
         */
        private fun parseExternal(ndefRecord: NdefRecord): Uri {
            val payload = ndefRecord.payload
            return Uri.parse(String(payload, Charsets.UTF_8))
        }

        /**
         * 处理已知类型的Uri
         */
        private fun parseWellKnown(ndefRecord: NdefRecord): Uri {
            if (!ndefRecord.type.contentEquals(NdefRecord.RTD_URI)) return Uri.EMPTY
            val payload = ndefRecord.payload
            val prefix = URI_PREFIX_MAP[payload[0].toByte()] ?: ""
            val prefixBytes = prefix.toByteArray(Charsets.UTF_8)
            val fullUri = ByteArray(prefixBytes.size + payload.size - 1)
            System.arraycopy(prefixBytes, 0, fullUri, 0, prefixBytes.size)
            System.arraycopy(payload, 1, fullUri, prefixBytes.size, payload.size - 1)
            return Uri.parse(String(fullUri, Charsets.UTF_8))
        }
    }
}