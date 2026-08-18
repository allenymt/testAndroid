package com.vdian.android.lib.testforgradle.nfc

import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

/** Experimental FMCOS profile reconstructed from the referenced FM1208 test-card article. */
internal object Fm1208TestCardProtocol {
    const val EXPECTED_UID_HEX = "77E9CC7D"
    const val APPLICATION_FILE_ID = 0x3F01
    const val BUSINESS_FILE_ID = 0x0003
    const val BUSINESS_FILE_SIZE = 0x0400
    const val MAX_TRANSFER_SIZE = 0xF0

    val ERASE_MASTER_FILE = hex("800E000000")
    val CREATE_MASTER_KEY_FILE = hex("80E00000073F00B001F0FFFF")
    val WRITE_DEFAULT_KEY = hex("80D401000DF9F0F0AA88FFFFFFFFFFFFFFFF")
    val CREATE_APPLICATION = hex("80E03F010D380FFFF0FA95FFFF4144463031")
    val SELECT_APPLICATION_BY_NAME = hex("00A40400054144463031")
    val CREATE_APPLICATION_KEY_FILE = hex("80E00000073F010095FAFFFF")
    val CREATE_BUSINESS_FILE = hex("80E0000307280400FAFAFFFF")
    val SELECT_BUSINESS_FILE = hex("00A40000020003")
    val GET_CHALLENGE = hex("0084000004")

    fun externalAuthenticate(challenge: ByteArray, key: ByteArray = defaultKey()): ByteArray {
        require(challenge.size == CHALLENGE_SIZE) { "FMCOS challenge must be 4 bytes" }
        require(key.size == DES_KEY_SIZE) { "FMCOS DES key must be 8 bytes" }
        val input = challenge + ByteArray(CHALLENGE_PADDING_SIZE)
        val cipher = Cipher.getInstance("DES/ECB/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "DES"))
        val cryptogram = cipher.doFinal(input)
        return byteArrayOf(0x00, 0x82.toByte(), 0x00, 0x00, cryptogram.size.toByte()) + cryptogram
    }

    fun encodeBusinessMessage(message: ByteArray): ByteArray {
        require(message.isNotEmpty()) { "Business message must not be empty" }
        require(message.size <= BUSINESS_FILE_SIZE - LENGTH_FIELD_SIZE) {
            "Business message needs ${message.size} bytes, capacity is ${BUSINESS_FILE_SIZE - LENGTH_FIELD_SIZE}"
        }
        return byteArrayOf((message.size ushr 8).toByte(), message.size.toByte()) + message
    }

    fun decodeBusinessMessage(stored: ByteArray): ByteArray {
        require(stored.size >= LENGTH_FIELD_SIZE) { "Business file is missing its length field" }
        val length = ((stored[0].toInt() and 0xFF) shl 8) or (stored[1].toInt() and 0xFF)
        require(length <= BUSINESS_FILE_SIZE - LENGTH_FIELD_SIZE) { "Invalid business message length: $length" }
        require(stored.size >= length + LENGTH_FIELD_SIZE) { "Business file content is incomplete" }
        return stored.copyOfRange(LENGTH_FIELD_SIZE, LENGTH_FIELD_SIZE + length)
    }

    fun defaultKey(): ByteArray = ByteArray(DES_KEY_SIZE) { 0xFF.toByte() }

    private fun hex(value: String): ByteArray = value.chunked(2)
        .map { it.toInt(16).toByte() }
        .toByteArray()

    private const val CHALLENGE_SIZE = 4
    private const val CHALLENGE_PADDING_SIZE = 4
    private const val DES_KEY_SIZE = 8
    private const val LENGTH_FIELD_SIZE = 2
}
