package com.vdian.android.lib.testforgradle.nfc

import java.net.URI
import java.net.URLDecoder
import java.security.MessageDigest

internal class Ntag424MockServerVerifier(
    private val key1: ByteArray = Ntag424DemoKeys.key1,
    private val key2: ByteArray = Ntag424DemoKeys.key2,
    private val replayStore: ReplayStore = InMemoryReplayStore()
) {
    data class Result(val uid: ByteArray, val readCounter: Int, val calculatedMac: ByteArray)

    interface ReplayStore {
        /** Atomically records a newer counter and rejects duplicate or older values. */
        fun accept(uid: ByteArray, readCounter: Int): Boolean
    }

    class InMemoryReplayStore : ReplayStore {
        private val latestCounters = mutableMapOf<String, Int>()

        @Synchronized
        override fun accept(uid: ByteArray, readCounter: Int): Boolean {
            val key = uid.joinToString("") { "%02X".format(it) }
            val previous = latestCounters[key]
            if (previous != null && readCounter <= previous) return false
            latestCounters[key] = readCounter
            return true
        }
    }

    init {
        require(key1.size == 16 && key2.size == 16)
    }

    fun verify(url: String, macInput: ByteArray = ByteArray(0)): Result {
        val parameters = parseUniqueParameters(url)
        val encryptedPiccData = parameters.requiredHex("e", 16)
        val receivedMac = parameters.requiredHex("c", 8)
        val plainPiccData = Ntag424DnaEv2Crypto.decryptCbc(
            key2,
            ByteArray(16),
            encryptedPiccData
        )
        check(plainPiccData.size == 16) { "PICCData 解密长度无效" }
        val tag = plainPiccData[0].toInt() and 0xFF
        check(tag and 0xC0 == 0xC0) { "PICCData 未同时包含 UID 和 SDMReadCtr" }
        check(tag and 0x07 == 0x07) { "PICCData UID 长度不是 7 bytes" }
        val uid = plainPiccData.copyOfRange(1, 8)
        val readCounter = (plainPiccData[8].toInt() and 0xFF) or
            ((plainPiccData[9].toInt() and 0xFF) shl 8) or
            ((plainPiccData[10].toInt() and 0xFF) shl 16)
        val sessionVector = hex("3CC300010080") + uid + byteArrayOf(
            readCounter.toByte(),
            (readCounter ushr 8).toByte(),
            (readCounter ushr 16).toByte()
        )
        val sessionMacKey = Ntag424DnaEv2Crypto.cmac(key1, sessionVector)
        val calculatedMac = Ntag424DnaEv2Crypto.truncateMac(
            Ntag424DnaEv2Crypto.cmac(sessionMacKey, macInput)
        )
        check(MessageDigest.isEqual(calculatedMac, receivedMac)) { "SDMMAC c 验证失败" }
        check(replayStore.accept(uid, readCounter)) {
            "SDMReadCtr=$readCounter 已使用或早于服务器记录，拒绝重放"
        }
        return Result(uid, readCounter, calculatedMac)
    }

    private fun parseUniqueParameters(url: String): Map<String, String> {
        val query = runCatching { URI(url).rawQuery }.getOrNull()
            ?: error("URL 缺少 query")
        val result = mutableMapOf<String, String>()
        query.split('&').filter { it.isNotEmpty() }.forEach { parameter ->
            val parts = parameter.split('=', limit = 2)
            val name = decode(parts[0]).lowercase()
            val value = decode(parts.getOrElse(1) { "" })
            check(name !in result) { "URL 包含重复参数: $name" }
            result[name] = value
        }
        return result
    }

    private fun Map<String, String>.requiredHex(name: String, byteCount: Int): ByteArray {
        val value = get(name) ?: error("URL 缺少 $name 参数")
        check(value.length == byteCount * 2 && value.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) {
            "$name 必须是 ${byteCount * 2} 个 hex 字符"
        }
        return hex(value)
    }

    private fun decode(value: String): String = URLDecoder.decode(value, Charsets.UTF_8.name())

    private fun ByteArray.toHex(): String = joinToString("") { "%02X".format(it) }

    private fun hex(value: String): ByteArray =
        value.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
}
