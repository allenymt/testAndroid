package com.vdian.android.lib.testforgradle.nfc

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

internal data class HkNtag424Record(
    val version: Int,
    val key0: String,
    val key1: String,
    val key2: String,
    val url: String,
    val packageName: String
) {
    val key0Bytes: ByteArray get() = key0.hexToBytes()
    val key1Bytes: ByteArray get() = key1.hexToBytes()
    val key2Bytes: ByteArray get() = key2.hexToBytes()
}

internal class HkNtag424RecordRepository(private val context: Context) {
    fun load(): List<HkNtag424Record> = parse(HkNfcConfigFile(context, ASSET_NAME).load())

    companion object {
        const val ASSET_NAME = "hk_ntag424_mock_records.json"
        private val RECORD_LIST_TYPE = object : TypeToken<List<HkNtag424Record>>() {}.type

        fun parse(json: String): List<HkNtag424Record> {
            val records: List<HkNtag424Record> = Gson().fromJson(json, RECORD_LIST_TYPE)
                ?: error("Mock 写卡数据不能为空")
            require(records.isNotEmpty()) { "Mock 写卡数据不能为空" }
            require(records.map { it.key1.uppercase() }.distinct().size == records.size) {
                "Key 1 不能重复"
            }
            records.forEachIndexed { index, record ->
                val label = "第 ${index + 1} 条写卡数据"
                require(record.version in 1..0xFF) { "$label 的 version 无效" }
                record.key0Bytes
                record.key1Bytes
                record.key2Bytes
                require(Ntag424DnaProtocol.hasSdmPlaceholders(record.url)) {
                    "$label 的 URL 必须包含外层 e/c 占位符"
                }
                require(record.packageName.isNotBlank()) { "$label 的 packageName 不能为空" }
            }
            return records
        }
    }
}

private fun String.hexToBytes(): ByteArray {
    require(length == 32 && all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) {
        "AES key 必须是 32 位 HEX"
    }
    return chunked(2).map { it.toInt(16).toByte() }.toByteArray()
}
