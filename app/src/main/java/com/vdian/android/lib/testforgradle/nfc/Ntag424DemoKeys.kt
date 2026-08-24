package com.vdian.android.lib.testforgradle.nfc

import java.util.zip.CRC32

/** Fixed keys for protocol testing on demo cards. Never use these values in production. */
internal object Ntag424DemoKeys {
    const val PERSONALIZED_VERSION = 0x01
    val transportKey = ByteArray(16)
    val key0 = hex("00112233445566778899AABBCCDDEEFF")
    val key1 = hex("0102030405060708090A0B0C0D0E0F10")
    val key2 = hex("102030405060708090A0B0C0D0E0F000")

    fun valueForVersion(keyNumber: Int, version: Int): ByteArray = when (version) {
        0 -> transportKey
        PERSONALIZED_VERSION -> personalizedValue(keyNumber)
        else -> error("不支持 AppKey $keyNumber version $version")
    }

    fun personalizedValue(keyNumber: Int): ByteArray = when (keyNumber) {
        0 -> key0
        1 -> key1
        2 -> key2
        else -> error("AppKey $keyNumber 尚未配置固定 Demo key value")
    }

    fun planPersonalization(initialVersions: Map<Int, Int>): List<Int> {
        require(initialVersions.keys == setOf(0, 1, 2)) { "必须提供 AppKey 0、1、2 的版本" }
        initialVersions.forEach { (keyNumber, version) ->
            check(version == 0 || version == PERSONALIZED_VERSION) {
                "AppKey $keyNumber version $version 不支持复合 Demo 操作"
            }
        }
        return listOf(1, 2, 0).filter { initialVersions.getValue(it) == 0 }
    }

    fun buildChangeKeyData(
        keyNumber: Int,
        oldKey: ByteArray,
        newKey: ByteArray,
        newVersion: Int = PERSONALIZED_VERSION
    ): ByteArray {
        require(keyNumber in 0..4)
        require(oldKey.size == 16 && newKey.size == 16)
        require(newVersion in 0..0xFF)
        val version = byteArrayOf(newVersion.toByte())
        if (keyNumber == 0) return newKey + version
        val xor = ByteArray(16) { index ->
            (oldKey[index].toInt() xor newKey[index].toInt()).toByte()
        }
        return xor + version + jamCrc32(newKey)
    }

    private fun jamCrc32(data: ByteArray): ByteArray {
        val value = CRC32().apply { update(data) }.value xor 0xFFFFFFFFL
        return byteArrayOf(
            value.toByte(),
            (value ushr 8).toByte(),
            (value ushr 16).toByte(),
            (value ushr 24).toByte()
        )
    }

    private fun hex(value: String): ByteArray =
        value.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
}
