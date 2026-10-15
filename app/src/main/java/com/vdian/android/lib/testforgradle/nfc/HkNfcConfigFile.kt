package com.vdian.android.lib.testforgradle.nfc

import android.content.Context
import android.util.AtomicFile
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.File

/** Writable copies of the packaged configuration; only the selected row's rfid is updated. */
internal class HkNfcConfigFile(private val context: Context, private val assetName: String) {
    private val file get() = AtomicFile(File(context.filesDir, assetName))

    fun load(): String = synchronized(LOCK) {
        val source = context.assets.open(assetName).bufferedReader().use { it.readText() }
        val previous = if (file.baseFile.exists()) file.openRead().bufferedReader().use { it.readText() } else null
        val merged = HkNfcConfigJson.merge(source, previous)
        save(merged)
        merged
    }

    fun writeRfid(index: Int, rfid: String) = synchronized(LOCK) {
        val current = file.openRead().bufferedReader().use { it.readText() }
        save(HkNfcConfigJson.withRfid(current, index, rfid))
    }

    fun read(): String = synchronized(LOCK) {
        file.openRead().bufferedReader().use { it.readText() }
    }

    private fun save(json: String) {
        val target = file
        val stream = target.startWrite()
        try {
            stream.write(json.toByteArray(Charsets.UTF_8))
            target.finishWrite(stream)
        } catch (error: Exception) {
            target.failWrite(stream)
            throw error
        }
    }

    companion object {
        private val LOCK = Any()
    }
}

internal object HkNfcConfigJson {
    private val gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()

    fun merge(source: String, previous: String?): String {
        val rows = JsonParser().parse(source).asJsonArray
        val oldRows = previous?.let { JsonParser().parse(it).asJsonArray } ?: JsonArray()
        val used = mutableSetOf<Int>()
        rows.forEach { row ->
            val oldIndex = (0 until oldRows.size()).firstOrNull {
                it !in used && configuration(oldRows[it].asJsonObject) == configuration(row.asJsonObject)
            }
            val ids = rfids(row.asJsonObject)
            if (oldIndex != null) {
                used += oldIndex
                ids.addAll(rfids(oldRows[oldIndex].asJsonObject))
            }
            row.asJsonObject.add("rfid", uidArray(ids))
        }
        return gson.toJson(rows)
    }

    fun withRfid(json: String, index: Int, rfid: String): String {
        validateUid(rfid)
        val rows = JsonParser().parse(json).asJsonArray
        require(index in 0 until rows.size()) { "所选 JSON 配置不存在" }
        val row = rows[index].asJsonObject
        row.add("rfid", uidArray(rfids(row) + rfid.uppercase()))
        return gson.toJson(rows)
    }

    fun rfidsAt(json: String, index: Int): List<String> {
        val rows = JsonParser().parse(json).asJsonArray
        require(index in 0 until rows.size()) { "所选 JSON 配置不存在" }
        return rfids(rows[index].asJsonObject).distinct()
    }

    private fun rfids(row: JsonObject): MutableList<String> {
        val value = row.get("rfid") ?: return mutableListOf()
        if (value.isJsonNull) return mutableListOf()
        val values = if (value.isJsonArray) value.asJsonArray.toList() else listOf(value)
        return values.map {
            require(it.isJsonPrimitive && it.asJsonPrimitive.isString) { "rfid 必须是 UID 字符串数组" }
            it.asString.also(::validateUid).uppercase()
        }.toMutableList()
    }

    private fun uidArray(ids: List<String>): JsonArray = JsonArray().also { result ->
        ids.distinct().forEach { result.add(it) }
    }

    private fun validateUid(uid: String) {
        require(uid.matches(Regex("[0-9A-Fa-f]{14}")) && uid.any { it != '0' }) {
            "RFID 必须是有效 7-byte UID 的 14 位 HEX"
        }
    }

    private fun configuration(row: JsonObject): JsonObject = JsonObject().also { result ->
        row.entrySet().filter { it.key != "rfid" }.forEach { result.add(it.key, it.value) }
    }
}
