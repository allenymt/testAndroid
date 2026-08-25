package com.vdian.android.lib.testforgradle.nfc

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class Ntag424MockServerVerifierTest {

    @Test
    fun verify_matchesRealCardZeroKeyCapture() {
        val verifier = Ntag424MockServerVerifier(
            key1 = ByteArray(16),
            key2 = ByteArray(16)
        )
        val url = "https://wdb-applink.weidian.com?" +
            "e=F9059AC5EB4B02AC9F6E3CB6DE0712C6&c=0B2902187FF92A61"

        val result = verifier.verify(url)

        assertArrayEquals(hex("04187262E51690"), result.uid)
        assertEquals(3, result.readCounter)
    }

    @Test
    fun verify_decryptsPiccDataValidatesMacAndRejectsReplay() {
        val uid = hex("04187262E51690")
        val counter = 3
        val url = buildUrl(uid, counter)
        val verifier = Ntag424MockServerVerifier()

        val result = verifier.verify(url)

        assertArrayEquals(uid, result.uid)
        assertEquals(counter, result.readCounter)
        expectFailure("重放") { verifier.verify(url) }
    }

    @Test
    fun verify_rejectsInvalidMacBeforeAdvancingReplayState() {
        val uid = hex("04187262E51690")
        val validUrl = buildUrl(uid, 7)
        val invalidUrl = validUrl.replace(Regex("c=[0-9A-F]{16}"), "c=0000000000000000")
        val verifier = Ntag424MockServerVerifier()

        expectFailure("SDMMAC") { verifier.verify(invalidUrl) }
        assertEquals(7, verifier.verify(validUrl).readCounter)
    }

    @Test
    fun verify_rejectsDuplicateDynamicParameters() {
        val url = buildUrl(hex("04187262E51690"), 9)

        expectFailure("重复参数") {
            Ntag424MockServerVerifier().verify("$url&e=${"0".repeat(32)}")
        }
    }

    @Test
    fun demoKeys_matchIssuedCardConfiguration() {
        assertArrayEquals(hex("112233445566778899AABBCCDDEEFF00"), Ntag424DemoKeys.key0)
        assertArrayEquals(hex("A1B2C3D4E5F6071829303A4B5C6D7E8F"), Ntag424DemoKeys.key1)
        assertArrayEquals(hex("000102030405060708090A0B0C0D0E0F"), Ntag424DemoKeys.key2)
        assertArrayEquals(hex("00112233445566778899AABBCCDDEEFF"), Ntag424DemoKeys.valueForVersion(0, 1))
        assertArrayEquals(hex("0102030405060708090A0B0C0D0E0F10"), Ntag424DemoKeys.valueForVersion(1, 1))
        assertArrayEquals(hex("102030405060708090A0B0C0D0E0F000"), Ntag424DemoKeys.valueForVersion(2, 1))
    }

    @Test
    fun demoChangeKeyData_matchesNtag424Encoding() {
        assertArrayEquals(
            Ntag424DemoKeys.key0 + byteArrayOf(0x02),
            Ntag424DemoKeys.buildChangeKeyData(
                keyNumber = 0,
                oldKey = Ntag424DemoKeys.transportKey,
                newKey = Ntag424DemoKeys.key0
            )
        )
        assertArrayEquals(
            Ntag424DemoKeys.transportKey + byteArrayOf(0x00),
            Ntag424DemoKeys.buildChangeKeyData(
                keyNumber = 0,
                oldKey = Ntag424DemoKeys.key0,
                newKey = Ntag424DemoKeys.transportKey,
                newVersion = 0
            )
        )
        val key1Data = Ntag424DemoKeys.buildChangeKeyData(
            keyNumber = 1,
            oldKey = Ntag424DemoKeys.transportKey,
            newKey = Ntag424DemoKeys.key1
        )
        assertArrayEquals(Ntag424DemoKeys.key1, key1Data.copyOfRange(0, 16))
        assertEquals(21, key1Data.size)
        assertEquals(0x02, key1Data[16].toInt() and 0xFF)
        assertArrayEquals(hex("AA87C986"), key1Data.copyOfRange(17, 21))
    }

    @Test
    fun demoCompositePersonalization_ordersKeysAndSupportsRetryAfterPartialCompletion() {
        assertEquals(
            listOf(1, 2, 0),
            Ntag424DemoKeys.planPersonalization(mapOf(0 to 0, 1 to 0, 2 to 0))
        )
        assertEquals(
            listOf(1, 0),
            Ntag424DemoKeys.planPersonalization(mapOf(0 to 0, 1 to 1, 2 to 2))
        )
        assertEquals(
            emptyList<Int>(),
            Ntag424DemoKeys.planPersonalization(mapOf(0 to 2, 1 to 2, 2 to 2))
        )
        expectFailure("不支持") {
            Ntag424DemoKeys.planPersonalization(mapOf(0 to 0, 1 to 3, 2 to 0))
        }
    }

    @Test
    fun demoCompositePersonalization_migratesLegacyVersion1Keys() {
        assertEquals(
            listOf(1, 2, 0),
            Ntag424DemoKeys.planPersonalization(mapOf(0 to 1, 1 to 1, 2 to 1))
        )
    }

    @Test
    fun demoTransportRestore_ordersKeysAndSkipsAlreadyRestoredKeys() {
        assertEquals(
            listOf(1, 2, 0),
            Ntag424DemoKeys.planTransportRestore(mapOf(0 to 2, 1 to 2, 2 to 2))
        )
        assertEquals(
            listOf(2, 0),
            Ntag424DemoKeys.planTransportRestore(mapOf(0 to 1, 1 to 0, 2 to 2))
        )
        assertEquals(
            emptyList<Int>(),
            Ntag424DemoKeys.planTransportRestore(mapOf(0 to 0, 1 to 0, 2 to 0))
        )
    }

    private fun buildUrl(uid: ByteArray, counter: Int): String {
        val plainPiccData = byteArrayOf(0xC7.toByte()) + uid + byteArrayOf(
            counter.toByte(),
            (counter ushr 8).toByte(),
            (counter ushr 16).toByte()
        ) + hex("8735BD8238")
        val encryptedPiccData = Ntag424DnaEv2Crypto.encryptCbc(
            Ntag424DemoKeys.key2,
            ByteArray(16),
            plainPiccData
        )
        val sessionVector = hex("3CC300010080") + uid + byteArrayOf(
            counter.toByte(),
            (counter ushr 8).toByte(),
            (counter ushr 16).toByte()
        )
        val sessionKey = Ntag424DnaEv2Crypto.cmac(Ntag424DemoKeys.key1, sessionVector)
        val mac = Ntag424DnaEv2Crypto.truncateMac(
            Ntag424DnaEv2Crypto.cmac(sessionKey, ByteArray(0))
        )
        return "https://wdb-applink.weidian.com?" +
            "targetUrl=weidianbuyer%3A%2F%2Fwdb%2Fnfc%3FencryptId%3DN2.01%26version%3D1" +
            "&e=${encryptedPiccData.toHex()}&c=${mac.toHex()}"
    }

    private fun expectFailure(message: String, block: () -> Unit) {
        try {
            block()
            fail("Expected failure containing $message")
        } catch (error: IllegalStateException) {
            check(error.message.orEmpty().contains(message))
        }
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02X".format(it) }

    private fun hex(value: String): ByteArray =
        value.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
}
