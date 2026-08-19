package com.vdian.android.lib.testforgradle.nfc

import org.junit.Assert.assertArrayEquals
import org.junit.Test

class Ntag424DnaEv2CryptoTest {
    private val zeroKey = ByteArray(16)
    private val zeroIv = ByteArray(16)

    @Test
    fun authenticationCrypto_matchesNxpAn12196Vector() {
        val encryptedRndB = hex("A04C124213C186F22399D33AC2A30215")
        val rndB = hex("B9E2FC789B64BF237CCCAA20EC7E6E48")
        val rndA = hex("13C5DB8A5930439FC3DEF9A4C675360F")

        assertArrayEquals(rndB, Ntag424DnaEv2Crypto.decryptCbc(zeroKey, zeroIv, encryptedRndB))
        assertArrayEquals(
            hex("35C3E05A752E0144BAC0DE51C1F22C56B34408A23D8AEA266CAB947EA8E0118D"),
            Ntag424DnaEv2Crypto.encryptCbc(
                zeroKey,
                zeroIv,
                rndA + Ntag424DnaEv2Crypto.rotateLeft(rndB)
            )
        )
        assertArrayEquals(
            hex("9D00C4DFC5DB8A5930439FC3DEF9A4C675360F13000000000000000000000000"),
            Ntag424DnaEv2Crypto.decryptCbc(
                zeroKey,
                zeroIv,
                hex("3FA64DB5446D1F34CD6EA311167F5E4985B89690C04A05F17FA7AB2F08120663")
            )
        )
    }

    @Test
    fun sessionKeys_matchNxpAn12196Vector() {
        val rndA = hex("13C5DB8A5930439FC3DEF9A4C675360F")
        val rndB = hex("B9E2FC789B64BF237CCCAA20EC7E6E48")

        assertArrayEquals(
            hex("1309C877509E5A215007FF0ED19CA564"),
            Ntag424DnaEv2Crypto.deriveSessionKey(zeroKey, hex("A55A"), rndA, rndB)
        )
        assertArrayEquals(
            hex("4C6626F5E72EA694202139295C7A7FC7"),
            Ntag424DnaEv2Crypto.deriveSessionKey(zeroKey, hex("5AA5"), rndA, rndB)
        )
    }

    @Test
    fun secureMessagingCrypto_matchesChangeFileSettingsVector() {
        val sessionEncKey = hex("1309C877509E5A215007FF0ED19CA564")
        val sessionMacKey = hex("4C6626F5E72EA694202139295C7A7FC7")
        val transactionId = hex("9D00C4DF")
        val counter = hex("0100")
        val settings = hex("4000E0C1F121200000430000430000")
        val iv = Ntag424DnaEv2Crypto.encryptBlock(
            sessionEncKey,
            hex("A55A") + transactionId + counter + ByteArray(8)
        )

        assertArrayEquals(hex("3E27082AB2ACC1EF55C57547934E9962"), iv)
        val encryptedSettings = Ntag424DnaEv2Crypto.encryptCbc(
            sessionEncKey,
            iv,
            Ntag424DnaEv2Crypto.pad80(settings)
        )
        assertArrayEquals(hex("61B6D97903566E84C3AE5274467E89EA"), encryptedSettings)
        assertArrayEquals(
            hex("D799B7C1A0EF7A04"),
            Ntag424DnaEv2Crypto.truncateMac(
                Ntag424DnaEv2Crypto.cmac(
                    sessionMacKey,
                    hex("5F") + counter + transactionId + hex("02") + encryptedSettings
                )
            )
        )
    }

    private fun hex(value: String): ByteArray = value.chunked(2)
        .map { it.toInt(16).toByte() }
        .toByteArray()
}
