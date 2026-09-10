package com.vdian.android.lib.testforgradle.nfc

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

internal data class HkNtag424Record(
    val batchId: String,
    val recordId: String,
    val version: Int,
    val key0: String,
    val key1: String,
    val key2: String,
    val url: String,
    val packageName: String,
    val uid: String? = null,
    val physicalSerialNo: String? = null,
    val lastCounter: Long? = null
) {
    val key0Bytes: ByteArray get() = key0.hexToBytes()
    val key1Bytes: ByteArray get() = key1.hexToBytes()
    val key2Bytes: ByteArray get() = key2.hexToBytes()

    fun completed(uid: String, physicalSerialNo: String): HkNtag424Record = copy(
        uid = uid,
        physicalSerialNo = physicalSerialNo
    )
}

internal data class HkNtag424Assignment(
    val index: Int,
    val record: HkNtag424Record,
    val physicalSerialNo: String
)

internal class HkNtag424RecordRepository(private val context: Context) {
    fun load(): List<HkNtag424Record> = context.assets.open(ASSET_NAME).bufferedReader().use {
        parse(it.readText())
    }

    companion object {
        const val ASSET_NAME = "hk_ntag424_mock_records.json"
        private val RECORD_LIST_TYPE = object : TypeToken<List<HkNtag424Record>>() {}.type

        fun parse(json: String): List<HkNtag424Record> {
            val records: List<HkNtag424Record> = Gson().fromJson(json, RECORD_LIST_TYPE)
                ?: error("Mock 生产数据不能为空")
            require(records.isNotEmpty()) { "Mock 生产数据不能为空" }
            require(records.map { it.recordId }.distinct().size == records.size) { "recordId 不能重复" }
            require(records.map { it.key1.uppercase() }.distinct().size == records.size) { "Key 1 不能重复" }
            records.forEach { record ->
                require(record.batchId.isNotBlank() && record.recordId.isNotBlank()) { "批次和 recordId 不能为空" }
                require(record.version in 1..0xFF) { "${record.recordId} 的 version 无效" }
                record.key0Bytes
                record.key1Bytes
                record.key2Bytes
                require(Ntag424DnaProtocol.hasSdmPlaceholders(record.url)) {
                    "${record.recordId} 的 URL 必须包含外层 e/c 占位符"
                }
                require(record.packageName.isNotBlank()) { "${record.recordId} 的 packageName 不能为空" }
            }
            return records
        }
    }
}

internal class HkNtag424ProductionStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    init {
        migrateLegacyResult()
    }

    fun assignmentFor(
        index: Int,
        records: List<HkNtag424Record>,
        requireUnused: Boolean
    ): HkNtag424Assignment = synchronized(LOCK) {
        require(index in records.indices) { "所选生产记录不存在" }
        val record = records[index]
        val historicalRecord = historyInternal().firstOrNull { it.recordId == record.recordId }
        if (requireUnused) check(historicalRecord == null && record.recordId !in issuedRecordIds()) {
            "${record.recordId} 已完成写入，不能重复生产"
        }
        val serialNo = historicalRecord?.physicalSerialNo ?:
            "No.${nextSerialNumber().toString().padStart(3, '0')}"
        HkNtag424Assignment(index, record, serialNo)
    }

    fun complete(assignment: HkNtag424Assignment, uid: String): HkNtag424Record = synchronized(LOCK) {
        val history = historyInternal()
        val issuedUids = preferences.getStringSet(KEY_ISSUED_UIDS, emptySet()).orEmpty() +
            preferences.getStringSet(LEGACY_COMPLETED_UIDS, emptySet()).orEmpty()
        check(issuedUids.none { it.equals(uid, ignoreCase = true) }) {
            "UID $uid 已完成生产，拒绝重复分配 SN"
        }
        val completed = assignment.record.completed(uid, assignment.physicalSerialNo)
        val issuedRecordIds = issuedRecordIds()
        check(
            preferences.edit()
                .putString(KEY_HISTORY, gson.toJson(history + completed))
                .putStringSet(KEY_ISSUED_UIDS, issuedUids + uid.uppercase())
                .putStringSet(KEY_ISSUED_RECORD_IDS, issuedRecordIds + assignment.record.recordId)
                .putInt(KEY_NEXT_SERIAL_NUMBER, serialNumber(assignment.physicalSerialNo) + 1)
                .commit()
        ) { "写卡成功，但本地生产进度保存失败，请停止继续生产" }
        completed
    }

    fun history(key1: String? = null): List<HkNtag424Record> = synchronized(LOCK) {
        val normalized = key1?.trim()?.takeIf { it.isNotEmpty() }
        historyInternal().filter { normalized == null || it.key1.equals(normalized, ignoreCase = true) }
            .asReversed()
    }

    fun deleteHistory(recordId: String): Boolean = synchronized(LOCK) {
        val history = historyInternal()
        val retained = history.filterNot { it.recordId == recordId }
        if (retained.size == history.size) return@synchronized false
        check(preferences.edit().putString(KEY_HISTORY, gson.toJson(retained)).commit()) {
            "删除历史记录失败"
        }
        true
    }

    fun toJson(record: HkNtag424Record): String = gson.toJson(record)

    fun isIssued(recordId: String): Boolean = synchronized(LOCK) { recordId in issuedRecordIds() }

    private fun historyInternal(): List<HkNtag424Record> {
        val json = preferences.getString(KEY_HISTORY, null) ?: return emptyList()
        val type = object : TypeToken<List<HkNtag424Record>>() {}.type
        return gson.fromJson<List<HkNtag424Record>>(json, type).orEmpty()
    }

    private fun issuedRecordIds(): Set<String> =
        preferences.getStringSet(KEY_ISSUED_RECORD_IDS, emptySet()).orEmpty()

    private fun nextSerialNumber(): Int {
        if (preferences.contains(KEY_NEXT_SERIAL_NUMBER)) {
            return preferences.getInt(KEY_NEXT_SERIAL_NUMBER, 1)
        }
        val historyNext = historyInternal().mapNotNull { record ->
            record.physicalSerialNo?.substringAfter("No.")?.toIntOrNull()
        }.maxOrNull()?.plus(1) ?: 1
        val legacyNext = preferences.getInt(LEGACY_NEXT_INDEX, 0) + 1
        return maxOf(historyNext, legacyNext)
    }

    private fun migrateLegacyResult() = synchronized(LOCK) {
        if (preferences.contains(KEY_HISTORY)) return@synchronized
        val legacyJson = preferences.getString(LEGACY_LAST_RESULT, null) ?: return@synchronized
        val record = runCatching { gson.fromJson(legacyJson, HkNtag424Record::class.java) }.getOrNull()
            ?.takeIf { !it.uid.isNullOrBlank() && !it.physicalSerialNo.isNullOrBlank() }
            ?: return@synchronized
        val nextSerial = serialNumber(checkNotNull(record.physicalSerialNo)) + 1
        preferences.edit()
            .putString(KEY_HISTORY, gson.toJson(listOf(record)))
            .putStringSet(KEY_ISSUED_RECORD_IDS, setOf(record.recordId))
            .putStringSet(KEY_ISSUED_UIDS, setOf(checkNotNull(record.uid).uppercase()))
            .putInt(KEY_NEXT_SERIAL_NUMBER, nextSerial)
            .commit()
    }

    private fun serialNumber(value: String): Int =
        value.substringAfter("No.").toIntOrNull() ?: error("SN 格式无效: $value")

    companion object {
        private const val PREFERENCES_NAME = "hk_ntag424_production"
        private const val KEY_NEXT_SERIAL_NUMBER = "next_serial_number"
        private const val KEY_HISTORY = "history"
        private const val KEY_ISSUED_UIDS = "issued_uids"
        private const val KEY_ISSUED_RECORD_IDS = "issued_record_ids"
        private const val LEGACY_COMPLETED_UIDS = "completed_uids"
        private const val LEGACY_LAST_RESULT = "last_result"
        private const val LEGACY_NEXT_INDEX = "next_index"
        private val LOCK = Any()
    }
}

private fun String.hexToBytes(): ByteArray {
    require(length == 32 && all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) {
        "AES key 必须是 32 位 HEX"
    }
    return chunked(2).map { it.toInt(16).toByte() }.toByteArray()
}
