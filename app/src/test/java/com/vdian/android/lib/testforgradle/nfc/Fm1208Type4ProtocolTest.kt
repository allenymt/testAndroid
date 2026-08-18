package com.vdian.android.lib.testforgradle.nfc

import java.io.IOException
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class Fm1208Type4ProtocolTest {

    @Test
    fun parseCapabilityContainer_findsNdefFileAndTransferLimits() {
        val capability = Fm1208Type4Protocol.parseCapabilityContainer(
            byteArrayOf(
                0x00, 0x0F, 0x20, 0x00, 0x3B, 0x00, 0x34,
                0x04, 0x06, 0xE1.toByte(), 0x04, 0x01, 0x00, 0x00, 0x00
            )
        )

        assertEquals(0xE104, capability.ndefFileId)
        assertEquals(256, capability.maxNdefFileSize)
        assertEquals(59, capability.maxReadSize)
        assertEquals(52, capability.maxWriteSize)
        assertEquals(0, capability.readAccess)
        assertEquals(0, capability.writeAccess)
    }

    @Test
    fun commands_useShortIso7816ApduEncoding() {
        assertArrayEquals(
            byteArrayOf(0x00, 0xA4.toByte(), 0x00, 0x00, 0x02, 0x3F, 0x00),
            Fm1208Type4Protocol.SELECT_FMCOS_MASTER_FILE
        )
        assertArrayEquals(
            byteArrayOf(0x00, 0xB0.toByte(), 0x01, 0x02, 0x20),
            Fm1208Type4Protocol.readBinary(0x0102, 32)
        )
        assertArrayEquals(
            byteArrayOf(0x00, 0xD6.toByte(), 0x01, 0x02, 0x02, 0x12, 0x34),
            Fm1208Type4Protocol.updateBinary(0x0102, byteArrayOf(0x12, 0x34))
        )
    }

    @Test
    fun defaultBusinessData_targetsDemoApp() {
        assertEquals("https://yl.test.com/nfc", Fm1208NdefClient.DEFAULT_BUSINESS_URL)
        assertEquals(
            "com.vdian.android.lib.testforgradle",
            Fm1208NdefClient.DEFAULT_APPLICATION_PACKAGE
        )
    }

    @Test
    fun fmcosTlv_findsNestedMasterFileMetadata() {
        val fci = hex("6F15840E315041592E5359532E4444463031A503880101")

        assertEquals("1PAY.SYS.DDF01", FmcosTlv.find(fci, 0x84)?.toString(Charsets.US_ASCII))
        assertArrayEquals(byteArrayOf(0x01), FmcosTlv.find(fci, 0x88))
    }

    @Test
    fun fmcosTlv_findsApplicationInDirectoryRecord() {
        val record = hex("61114F09A00000000386980701500450424F43")

        assertArrayEquals(hex("A00000000386980701"), FmcosTlv.find(record, 0x4F))
        assertEquals("PBOC", FmcosTlv.find(record, 0x50)?.toString(Charsets.US_ASCII))
    }

    @Test
    fun fmcosStatus_treatsMissingDirectoryAndMissingRecordAsEmpty() {
        assertEquals(true, FmcosStatus.isDirectoryEnd(0x6A82))
        assertEquals(true, FmcosStatus.isDirectoryEnd(0x6A83))
        assertEquals(false, FmcosStatus.isDirectoryEnd(0x6982))
    }

    @Test
    fun testCardProfile_matchesArticleApduExamples() {
        assertArrayEquals(hex("800E000000"), Fm1208TestCardProtocol.ERASE_MASTER_FILE)
        assertArrayEquals(
            hex("80E00000073F00B001F0FFFF"),
            Fm1208TestCardProtocol.CREATE_MASTER_KEY_FILE
        )
        assertArrayEquals(
            hex("80D401000DF9F0F0AA88FFFFFFFFFFFFFFFF"),
            Fm1208TestCardProtocol.WRITE_DEFAULT_KEY
        )
        assertArrayEquals(
            hex("80E03F010D380FFFF0FA95FFFF4144463031"),
            Fm1208TestCardProtocol.CREATE_APPLICATION
        )
        assertArrayEquals(
            hex("80E0000307280400FAFAFFFF"),
            Fm1208TestCardProtocol.CREATE_BUSINESS_FILE
        )
    }

    @Test
    fun externalAuthenticate_encryptsChallengeAndZeroPaddingWithDes() {
        val command = Fm1208TestCardProtocol.externalAuthenticate(
            challenge = hex("01234567"),
            key = hex("133457799BBCDFF1")
        )

        assertArrayEquals(hex("008200000854290DBB651898AC"), command)
    }

    @Test
    fun externalAuthenticate_supportsExperimentalAllFfTestKey() {
        assertArrayEquals(
            hex("0082000008D5F2E7D767E0A1EF"),
            Fm1208TestCardProtocol.externalAuthenticate(hex("01234567"))
        )
    }

    @Test
    fun businessMessageEncoding_usesTwoByteLengthAndRoundTrips() {
        val message = ByteArray(500) { (it and 0xFF).toByte() }

        val stored = Fm1208TestCardProtocol.encodeBusinessMessage(message)

        assertEquals(0x01, stored[0].toInt() and 0xFF)
        assertEquals(0xF4, stored[1].toInt() and 0xFF)
        assertArrayEquals(message, Fm1208TestCardProtocol.decodeBusinessMessage(stored))
    }

    @Test(expected = IOException::class)
    fun responseData_rejectsNonSuccessStatus() {
        Fm1208Type4Protocol.responseData(byteArrayOf(0x6A, 0x82.toByte()), "select file")
    }

    private fun hex(value: String): ByteArray = value.chunked(2)
        .map { it.toInt(16).toByte() }
        .toByteArray()
}
