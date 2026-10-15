package com.vdian.android.lib.testforgradle.nfc

import com.google.gson.JsonParser
import java.net.URI

internal data class HkNtag215Record(val url: String, val packageName: String)

internal object HkNtag215Data {
    const val ASSET_NAME = "hk_ntag215_records.json"

    fun parse(json: String): List<HkNtag215Record> {
        val records = JsonParser().parse(json).asJsonArray.mapIndexed { index, element ->
            val item = element.asJsonObject
            require(item.keySet() == setOf("url", "packageName")) {
                "配置 ${index + 1} 只允许包含 url 和 packageName"
            }
            require(item["url"].isJsonPrimitive && item["url"].asJsonPrimitive.isString &&
                item["packageName"].isJsonPrimitive && item["packageName"].asJsonPrimitive.isString) {
                "配置 ${index + 1} 的 URL 和包名必须是字符串"
            }
            HkNtag215Record(item["url"].asString, item["packageName"].asString)
        }
        require(records.isNotEmpty()) { "NTAG215 写卡配置不能为空" }
        records.forEachIndexed { index, record ->
            val uri = URI(record.url)
            require(uri.scheme in listOf("https", "http") && !uri.host.isNullOrBlank()) {
                "配置 ${index + 1} 的 URL 无效"
            }
            require(record.packageName.matches(Regex("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+"))) {
                "配置 ${index + 1} 的包名无效"
            }
        }
        return records
    }

    fun isNtag215(version: ByteArray): Boolean = version.contentEquals(
        byteArrayOf(0x00, 0x04, 0x04, 0x02, 0x01, 0x00, 0x11, 0x03)
    )
}
