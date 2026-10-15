package com.vdian.android.lib.testforgradle.nfc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HkNtag424ReadUidTest {
    private val uid = byteArrayOf(4, 1, 2, 3, 4, 5, 6)
    private val record = HkNtag424Record(1, "00".repeat(16), "11".repeat(16),
        "22".repeat(16), "https://example.com", "com.example.app")

    @Test
    fun usesRealVersionUidWithoutKeysOrServer() {
        assertEquals("04010203040506", HkNtag424ReadUid.resolve(uid, null, emptyList()))
    }

    @Test
    fun randomUidModeExtractsRealUidFromLocallyValidatedSdm() {
        assertEquals("04010203040506", HkNtag424ReadUid.resolve(ByteArray(7), url(), listOf(record)))
    }

    @Test
    fun neverDisplaysZerosOrUidDecryptedWithWrongKeys() {
        assertNull(HkNtag424ReadUid.resolve(ByteArray(7), url(), emptyList()))
        assertNull(HkNtag424ReadUid.resolve(ByteArray(7), url(), listOf(record.copy(key1 = "33".repeat(16)))))
    }

    private fun url(): String {
        val counter = byteArrayOf(1, 0, 0)
        val plain = byteArrayOf(0xC7.toByte()) + uid + counter + ByteArray(5)
        val encrypted = Ntag424DnaEv2Crypto.encryptCbc(record.key2Bytes, ByteArray(16), plain)
        val vector = byteArrayOf(0x3C, 0xC3.toByte(), 0, 1, 0, 0x80.toByte()) + uid + counter
        val session = Ntag424DnaEv2Crypto.cmac(record.key1Bytes, vector)
        val mac = Ntag424DnaEv2Crypto.truncateMac(Ntag424DnaEv2Crypto.cmac(session, ByteArray(0)))
        return "https://example.com?e=${encrypted.hex()}&c=${mac.hex()}"
    }

    private fun ByteArray.hex() = joinToString("") { "%02X".format(it) }
}
