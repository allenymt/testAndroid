package com.vdian.android.lib.testforgradle.nfc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class HkNtag424MockRecordsTest {
    @Test
    fun records_haveUniqueIdentifiersAndValidKeys() {
        val records = loadRecords()

        assertTrue(records.isNotEmpty())
        assertEquals(records.size, records.map { it.recordId }.distinct().size)
        assertEquals(records.size, records.map { it.key1.uppercase() }.distinct().size)
        assertTrue(records.all { it.key0Bytes.size == 16 && it.key1Bytes.size == 16 && it.key2Bytes.size == 16 })
    }

    @Test
    fun records_useOuterSdmParameters() {
        loadRecords().forEach { record ->
            assertTrue(Ntag424DnaProtocol.hasSdmPlaceholders(record.url))
            assertEquals("com.koudai.weidian.buyer", record.packageName)
        }
    }

    private fun loadRecords(): List<HkNtag424Record> {
        val asset = listOf(
            File("src/main/assets/${HkNtag424RecordRepository.ASSET_NAME}"),
            File("app/src/main/assets/${HkNtag424RecordRepository.ASSET_NAME}")
        ).first { it.isFile }
        return HkNtag424RecordRepository.parse(asset.readText())
    }
}
