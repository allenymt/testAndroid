package com.vdian.android.lib.testforgradle.nfc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class HkNtag215DataTest {
    @Test
    fun assetContainsOnlyUrlAndPackageAndPreservesEncoding() {
        val asset = listOf(File("src/main/assets/${HkNtag215Data.ASSET_NAME}"),
            File("app/src/main/assets/${HkNtag215Data.ASSET_NAME}")).first { it.isFile }
        val records = HkNtag215Data.parse(asset.readText())
        assertEquals(1, records.size)
        assertEquals("com.koudai.weidian.buyer", records.single().packageName)
        assertTrue(records.single().url.contains("weidianbuyer%3A%2F%2F"))
        assertTrue(records.single().url.contains("D%2BtzR2Ol"))
        assertEquals(setOf("url", "packageName"),
            com.google.gson.JsonParser().parse(asset.readText()).asJsonArray[0].asJsonObject.keySet())
    }

    @Test
    fun recognizes215AndRejectsOtherNtagModels() {
        assertTrue(HkNtag215Data.isNtag215(byteArrayOf(0, 4, 4, 2, 1, 0, 0x11, 3)))
        assertFalse(HkNtag215Data.isNtag215(byteArrayOf(0, 4, 4, 2, 1, 0, 0x0F, 3)))
        assertFalse(HkNtag215Data.isNtag215(byteArrayOf(0, 4, 4, 2, 1, 0, 0x13, 3)))
        assertFalse(HkNtag215Data.isNtag215(byteArrayOf()))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsInvalidUrl() {
        HkNtag215Data.parse("""[{"url":"not-a-url","packageName":"com.example.app"}]""")
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsInvalidPackageName() {
        HkNtag215Data.parse("""[{"url":"https://example.com","packageName":"bad package"}]""")
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsMissingPackageName() {
        HkNtag215Data.parse("""[{"url":"https://example.com"}]""")
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsEmptyConfiguration() {
        HkNtag215Data.parse("[]")
    }
}
