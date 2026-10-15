package com.vdian.android.lib.testforgradle.nfc

import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class HkNfcConfigJsonTest {
    @Test
    fun writesUidOnlyToSelectedRow() {
        val source = """[{"url":"https://example.com/a","packageName":"com.example.app"},{"url":"https://example.com/b","packageName":"com.example.app"}]"""
        val result = JsonParser().parse(HkNfcConfigJson.withRfid(source, 1, "04abcdef123456")).asJsonArray
        assertFalse(result[0].asJsonObject.has("rfid"))
        assertEquals(listOf("04ABCDEF123456"), HkNfcConfigJson.rfidsAt(result.toString(), 1))
        assertEquals("https://example.com/b", result[1].asJsonObject["url"].asString)
    }

    @Test
    fun preservesRfidWhenUnchangedConfigurationMoves() {
        val source = """[{"key1":"new"},{"key1":"old"}]"""
        val old = """[{"key1":"old","rfid":"04112233445566"}]"""
        val result = JsonParser().parse(HkNfcConfigJson.merge(source, old)).asJsonArray
        assertEquals(emptyList<String>(), HkNfcConfigJson.rfidsAt(result.toString(), 0))
        assertEquals(listOf("04112233445566"), HkNfcConfigJson.rfidsAt(result.toString(), 1))
    }

    @Test
    fun changedConfigurationDoesNotInheritOldUid() {
        val result = JsonParser().parse(HkNfcConfigJson.merge(
            """[{"key1":"new"}]""", """[{"key1":"old","rfid":"04112233445566"}]"""
        )).asJsonArray
        assertEquals(emptyList<String>(), HkNfcConfigJson.rfidsAt(result.toString(), 0))
    }

    @Test
    fun appendsMultipleCardsAndDeduplicatesCaseInsensitively() {
        val first = HkNfcConfigJson.withRfid("[{}]", 0, "04ABCDEF123456")
        val second = HkNfcConfigJson.withRfid(first, 0, "04112233445566")
        val repeated = HkNfcConfigJson.withRfid(second, 0, "04abcdef123456")
        assertEquals(listOf("04ABCDEF123456", "04112233445566"), HkNfcConfigJson.rfidsAt(repeated, 0))
        assertEquals(true, JsonParser().parse(repeated).asJsonArray[0].asJsonObject["rfid"].isJsonArray)
    }

    @Test
    fun migratesLegacyStringAndPreservesItWhenAppending() {
        val result = HkNfcConfigJson.withRfid("""[{"rfid":"04abcdef123456"}]""", 0, "04112233445566")
        assertEquals(listOf("04ABCDEF123456", "04112233445566"), HkNfcConfigJson.rfidsAt(result, 0))
    }

    @Test
    fun mergesUidArraysWithoutLosingOrDuplicatingCards() {
        val source = """[{"key1":"a","rfid":["04abcdef123456"]}]"""
        val previous = """[{"key1":"a","rfid":["04ABCDEF123456","04112233445566"]}]"""
        val result = HkNfcConfigJson.merge(source, previous)
        assertEquals(listOf("04ABCDEF123456", "04112233445566"), HkNfcConfigJson.rfidsAt(result, 0))
    }

    @Test
    fun ntag215ParserAcceptsWrittenBackRfid() {
        val json = HkNfcConfigJson.withRfid(
            """[{"url":"https://example.com","packageName":"com.example.app"}]""", 0, "04112233445566"
        )
        assertEquals("https://example.com", HkNtag215Data.parse(json).single().url)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsRandomRfUid() {
        HkNfcConfigJson.withRfid("[{}]", 0, "08112233")
    }
}
