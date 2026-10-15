package com.vdian.android.lib.testforgradle.nfc

internal object HkNtag424ReadUid {
    fun resolve(versionUid: ByteArray?, url: String?, records: List<HkNtag424Record>): String? {
        if (versionUid?.size == 7 && versionUid.any { it != 0.toByte() }) {
            return versionUid.toHex()
        }
        if (url == null) return null
        // A fresh in-memory verifier validates the profile's MAC without consuming
        // the real server's replay counter. Never guess a UID from decryption alone.
        return records.firstNotNullOfOrNull { record ->
            runCatching {
                Ntag424MockServerVerifier(key1 = record.key1Bytes, key2 = record.key2Bytes)
                    .verify(url).uid.toHex()
            }.getOrNull()
        }
    }

    private fun ByteArray.toHex() = joinToString("") { "%02X".format(it) }
}
