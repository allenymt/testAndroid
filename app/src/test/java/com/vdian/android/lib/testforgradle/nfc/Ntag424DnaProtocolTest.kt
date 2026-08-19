package com.vdian.android.lib.testforgradle.nfc

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class Ntag424DnaProtocolTest {

    @Test
    fun commands_useNtag424WrappedNativeApdus() {
        assertArrayEquals(hex("9060000000"), Ntag424DnaProtocol.GET_VERSION)
        assertArrayEquals(hex("90F50000010200"), Ntag424DnaProtocol.getFileSettings(2))
        assertArrayEquals(hex("903C0000010000"), Ntag424DnaProtocol.READ_ORIGINALITY_SIGNATURE)
        assertArrayEquals(hex("90AF000000"), Ntag424DnaProtocol.ADDITIONAL_FRAME)
    }

    @Test
    fun parseVersion_recognizesNtag424DnaResponse() {
        val response = hex(
            "04040230001105" +
                "04040201001105" +
                "04112233445566A1A2A3A4A52219"
        )

        val version = Ntag424DnaProtocol.parseVersion(response)

        assertTrue(version.looksLikeNtag424Dna)
        assertEquals(0x30, version.hardwareMajorVersion)
        assertEquals(1, version.softwareMajorVersion)
        assertArrayEquals(hex("04112233445566"), version.uid)
        assertEquals(0x22, version.productionWeek)
        assertEquals(0x19, version.productionYear)
    }

    @Test
    fun parseFileSettings_extractsSdmAndAccessRights() {
        val settings = Ntag424DnaProtocol.parseFileSettings(
            hex("004000E0000100C1F1210000002B00003E0000")
        )

        assertEquals(256, settings.fileSize)
        assertEquals(0x40, settings.fileOption)
        assertTrue(settings.sdmEnabled)
        assertEquals(0x0E, settings.readKey)
        assertEquals(0x00, settings.writeKey)
        assertEquals(0x00, settings.readWriteKey)
        assertEquals(0x00, settings.changeKey)
        assertEquals(0x02, settings.sdmMetaReadKey)
        assertEquals(0x01, settings.sdmFileReadKey)
        assertEquals(0x01, settings.sdmCounterRetrieveKey)
    }

    @Test
    fun parseFileSettings_decodesRealCardAccessRightsLittleEndian() {
        val settings = Ntag424DnaProtocol.parseFileSettings(hex("0000E0EE000100"))

        assertEquals(0x0E, settings.readKey)
        assertEquals(0x0E, settings.writeKey)
        assertEquals(0x0E, settings.readWriteKey)
        assertEquals(0x00, settings.changeKey)
        assertFalse(settings.sdmEnabled)
    }

    @Test
    fun personalizationWrite_usesCurrentFileCommunicationMode() {
        val settings = Ntag424DnaProtocol.parseFileSettings(hex("0000E0EE000100"))

        assertEquals(Ntag424WriteMode.PLAIN, Ntag424DnaProtocol.writeModeFor(settings))
    }

    @Test
    fun buildSdmFileSettings_preservesExistingFileAccessRights() {
        val settings = Ntag424DnaProtocol.parseFileSettings(hex("0000E0EE000100"))

        val result = Ntag424DnaProtocol.buildSdmFileSettings(
            settings,
            piccDataOffset = 0x94,
            macOffset = 0xB7
        )

        assertArrayEquals(hex("40E0EEC1F121940000B70000B70000"), result)
    }

    @Test
    fun writeProtection_changesOnlyWriteRightsAndPreservesSdmConfiguration() {
        val settings = Ntag424DnaProtocol.parseFileSettings(
            hex("0040E0EE000100C1F121940000B70000B70000")
        )

        assertArrayEquals(
            hex("4000E0C1F121940000B70000B70000"),
            Ntag424DnaProtocol.buildWriteProtectionFileSettings(settings, protected = true)
        )
    }

    @Test
    fun removeWriteProtection_changesOnlyWriteRightsAndPreservesSdmConfiguration() {
        val settings = Ntag424DnaProtocol.parseFileSettings(
            hex("004000E0000100C1F121940000B70000B70000")
        )

        assertArrayEquals(
            hex("40E0EEC1F121940000B70000B70000"),
            Ntag424DnaProtocol.buildWriteProtectionFileSettings(settings, protected = false)
        )
    }

    @Test
    fun writeProtection_preservesSettingsWhenSdmIsDisabled() {
        val settings = Ntag424DnaProtocol.parseFileSettings(hex("0000E0EE000100"))

        assertArrayEquals(
            hex("0000E0"),
            Ntag424DnaProtocol.buildWriteProtectionFileSettings(settings, protected = true)
        )
        assertArrayEquals(
            hex("00E0EE"),
            Ntag424DnaProtocol.buildWriteProtectionFileSettings(settings, protected = false)
        )
    }

    @Test
    fun disableSdm_changesOnlySdmConfiguration() {
        val settings = Ntag424DnaProtocol.parseFileSettings(
            hex("004000E0000100C1F121940000B70000B70000")
        )

        assertArrayEquals(
            hex("0000E0"),
            Ntag424DnaProtocol.buildDisabledSdmFileSettings(settings)
        )
    }

    @Test
    fun writeDataPlain_matchesNxpCommandFormat() {
        assertArrayEquals(
            hex("908D000019010E0000120000FF0506E1050080828300000000000000000000"),
            Ntag424DnaProtocol.writeDataPlain(
                fileNumber = 1,
                offset = 14,
                data = hex("FF0506E10500808283000000000000000000")
            )
        )
    }

    @Test
    fun buildSdmUrl_preservesOriginalQuery() {
        val original = "https://wdb-applink.weidian.com?" +
            "targetUrl=https%3A%2F%2Fvmspub.weidian.com%2Fgaia%2F79497%2F" +
            "78f86a5b_bbk.html%3FshowTitle%3D0%26transparentStatusBar%3D1"
        val encryptedPiccData = "0".repeat(32)
        val mac = "0".repeat(16)

        val result = Ntag424DnaProtocol.buildSdmUrl(original, encryptedPiccData, mac)

        assertEquals(
            "$original&e=$encryptedPiccData&c=$mac",
            result
        )
    }

    @Test
    fun buildSdmUrl_insertsParametersBeforeFragment() {
        assertEquals(
            "https://example.com/path?e=AAAA&c=BBBB#section",
            Ntag424DnaProtocol.buildSdmUrl(
                "https://example.com/path#section",
                "AAAA",
                "BBBB"
            )
        )
    }

    @Test
    fun compactUrlForSdm_onlyDecodesNestedSchemeDelimiter() {
        assertEquals(
            "https://host?targetUrl=https://nested/path%3FshowTitle%3D0%26transparentStatusBar%3D1",
            Ntag424DnaProtocol.compactUrlForSdm(
                "https://host?targetUrl=https%3A%2F%2Fnested/path%3FshowTitle%3D0%26transparentStatusBar%3D1"
            )
        )
    }

    @Test
    fun buildSdmUrl_rejectsExistingDynamicParameters() {
        try {
            Ntag424DnaProtocol.buildSdmUrl("https://example.com?a=1&e=old", "AAAA", "BBBB")
            fail("Expected duplicate SDM parameter to be rejected")
        } catch (expected: IllegalArgumentException) {
            assertTrue(expected.message.orEmpty().contains("already contains"))
        }
    }

    @Test
    fun parseCapabilityContainer_findsNdefFile() {
        val (fileId, fileSize) = Ntag424DnaProtocol.parseCapabilityContainer(
            hex("000F20003B00340406E10401000000")
        )

        assertEquals(0xE104, fileId)
        assertEquals(256, fileSize)
    }

    @Test
    fun readSignatureResponse_acceptsRealCard9190Success() {
        val response = hex(
            "7A268D0725176746995DCBDB301F71B5FEE884FDC145ED5A5C6555CFF832EA97" +
                "415A71D6D8C99347A2B0635EE01585D80882FC01181697609190"
        )

        assertEquals(58, response.size)
        assertTrue(
            Ntag424DnaProtocol.isSuccessfulNativeStatus(
                command = 0x3C,
                status = response.last().toInt() and 0xFF,
                responseDataLength = response.size - 2
            )
        )
        assertFalse(Ntag424DnaProtocol.isSuccessfulNativeStatus(0x3C, 0x90, 55))
        assertFalse(Ntag424DnaProtocol.isSuccessfulNativeStatus(0x60, 0x90, 56))
    }

    private fun hex(value: String): ByteArray = value.chunked(2)
        .map { it.toInt(16).toByte() }
        .toByteArray()
}
