package com.vdian.android.lib.testforgradle.nfc

import android.nfc.NdefMessage
import android.nfc.NdefRecord
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HkNtag215ReaderTest {
    @Test
    fun separatesUriAndApplicationRecordWithoutDecodingUrl() {
        val url = "https://wdb-applink.weidian.com?targetUrl=weidianbuyer%3A%2F%2Fwdb%2Fnfc%3FencryptId%3Da%2Bb"
        val result = HkNtag215ReadResult("04:01", 492, true, NdefMessage(arrayOf(
            NdefRecord.createUri(url),
            NdefRecord.createApplicationRecord("com.koudai.weidian.buyer")
        )))
        assertEquals(listOf(url), result.urls)
        assertEquals(listOf("com.koudai.weidian.buyer"), result.packages)
    }

    @Test
    fun emptyNdefHasNoUrlOrPackage() {
        val result = HkNtag215ReadResult("04:01", 492, true, null)
        assertTrue(result.urls.isEmpty())
        assertTrue(result.packages.isEmpty())
    }
}
