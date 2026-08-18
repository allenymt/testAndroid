package com.vdian.android.lib.testforgradle.nfc

import java.io.IOException

internal data class Type4Capability(
    val ndefFileId: Int,
    val maxNdefFileSize: Int,
    val maxReadSize: Int,
    val maxWriteSize: Int,
    val readAccess: Int,
    val writeAccess: Int
)

internal object Fm1208Type4Protocol {
    val SELECT_NDEF_APPLICATION = byteArrayOf(
        0x00, 0xA4.toByte(), 0x04, 0x00, 0x07,
        0xD2.toByte(), 0x76, 0x00, 0x00, 0x85.toByte(), 0x01, 0x01, 0x00
    )
    val SELECT_CAPABILITY_CONTAINER = selectFile(0xE103)
    val SELECT_FMCOS_MASTER_FILE = byteArrayOf(
        0x00, 0xA4.toByte(), 0x00, 0x00, 0x02, 0x3F, 0x00
    )

    fun selectFile(fileId: Int): ByteArray = byteArrayOf(
        0x00, 0xA4.toByte(), 0x00, 0x0C, 0x02,
        (fileId ushr 8).toByte(), fileId.toByte()
    )

    fun readBinary(offset: Int, length: Int): ByteArray {
        require(offset in 0..0xFFFF) { "Offset out of range: $offset" }
        require(length in 1..256) { "Read length must be 1..256" }
        return byteArrayOf(
            0x00, 0xB0.toByte(), (offset ushr 8).toByte(), offset.toByte(),
            if (length == 256) 0x00 else length.toByte()
        )
    }

    fun updateBinary(offset: Int, data: ByteArray): ByteArray {
        require(offset in 0..0xFFFF) { "Offset out of range: $offset" }
        require(data.isNotEmpty() && data.size <= 255) { "Write length must be 1..255" }
        return byteArrayOf(
            0x00, 0xD6.toByte(), (offset ushr 8).toByte(), offset.toByte(), data.size.toByte()
        ) + data
    }

    fun responseData(response: ByteArray, operation: String): ByteArray {
        if (response.size < 2) throw IOException("$operation returned an incomplete APDU response")
        val status = ((response[response.size - 2].toInt() and 0xFF) shl 8) or
            (response.last().toInt() and 0xFF)
        if (status != 0x9000) {
            throw IOException("$operation failed, status=${status.toString(16).uppercase().padStart(4, '0')}")
        }
        return response.copyOf(response.size - 2)
    }

    fun parseCapabilityContainer(bytes: ByteArray): Type4Capability {
        require(bytes.size >= 7) { "Capability Container is shorter than its header" }
        val declaredLength = unsignedShort(bytes, 0)
        require(declaredLength in 7..bytes.size) { "Invalid Capability Container length: $declaredLength" }
        val maxRead = unsignedShort(bytes, 3)
        val maxWrite = unsignedShort(bytes, 5)
        require(maxRead > 0 && maxWrite > 0) { "Invalid Type 4 transfer limits" }

        var offset = 7
        while (offset < declaredLength) {
            val type = bytes[offset].toInt() and 0xFF
            if (type == 0x00) {
                offset++
                continue
            }
            if (type == 0xFE) break
            require(offset + 1 < declaredLength) { "Incomplete Capability Container TLV" }
            val length = bytes[offset + 1].toInt() and 0xFF
            val valueOffset = offset + 2
            require(valueOffset + length <= declaredLength) { "Capability Container TLV exceeds file length" }
            if (type == 0x04 && length == 6) {
                return Type4Capability(
                    ndefFileId = unsignedShort(bytes, valueOffset),
                    maxNdefFileSize = unsignedShort(bytes, valueOffset + 2),
                    maxReadSize = maxRead,
                    maxWriteSize = maxWrite,
                    readAccess = bytes[valueOffset + 4].toInt() and 0xFF,
                    writeAccess = bytes[valueOffset + 5].toInt() and 0xFF
                )
            }
            offset = valueOffset + length
        }
        throw IllegalArgumentException("NDEF File Control TLV was not found")
    }

    private fun unsignedShort(bytes: ByteArray, offset: Int): Int =
        ((bytes[offset].toInt() and 0xFF) shl 8) or (bytes[offset + 1].toInt() and 0xFF)
}
