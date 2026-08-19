package com.vdian.android.lib.testforgradle.nfc

import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

internal object Ntag424DnaEv2Crypto {
    private const val BLOCK_SIZE = 16
    private const val CMAC_RB = 0x87

    fun encryptCbc(key: ByteArray, iv: ByteArray, data: ByteArray): ByteArray =
        cipher("AES/CBC/NoPadding", Cipher.ENCRYPT_MODE, key, iv).doFinal(data)

    fun decryptCbc(key: ByteArray, iv: ByteArray, data: ByteArray): ByteArray =
        cipher("AES/CBC/NoPadding", Cipher.DECRYPT_MODE, key, iv).doFinal(data)

    fun encryptBlock(key: ByteArray, data: ByteArray): ByteArray =
        cipher("AES/ECB/NoPadding", Cipher.ENCRYPT_MODE, key).doFinal(data)

    fun cmac(key: ByteArray, message: ByteArray): ByteArray {
        require(key.size == BLOCK_SIZE) { "AES-128 key must be 16 bytes" }
        val firstSubKey = doubleBlock(encryptBlock(key, ByteArray(BLOCK_SIZE)))
        val secondSubKey = doubleBlock(firstSubKey)
        val completeLastBlock = message.isNotEmpty() && message.size % BLOCK_SIZE == 0
        val blockCount = maxOf(1, (message.size + BLOCK_SIZE - 1) / BLOCK_SIZE)
        val lastBlock = ByteArray(BLOCK_SIZE)
        val lastOffset = (blockCount - 1) * BLOCK_SIZE
        if (completeLastBlock) {
            message.copyInto(lastBlock, endIndex = message.size, startIndex = lastOffset)
            xorInPlace(lastBlock, firstSubKey)
        } else {
            if (lastOffset < message.size) message.copyInto(lastBlock, endIndex = message.size, startIndex = lastOffset)
            lastBlock[message.size - lastOffset] = 0x80.toByte()
            xorInPlace(lastBlock, secondSubKey)
        }

        var state = ByteArray(BLOCK_SIZE)
        for (block in 0 until blockCount - 1) {
            val input = message.copyOfRange(block * BLOCK_SIZE, (block + 1) * BLOCK_SIZE)
            xorInPlace(input, state)
            state = encryptBlock(key, input)
        }
        xorInPlace(lastBlock, state)
        return encryptBlock(key, lastBlock)
    }

    fun truncateMac(fullMac: ByteArray): ByteArray {
        require(fullMac.size == BLOCK_SIZE)
        return ByteArray(8) { index -> fullMac[index * 2 + 1] }
    }

    fun deriveSessionKey(key: ByteArray, label: ByteArray, rndA: ByteArray, rndB: ByteArray): ByteArray {
        require(label.size == 2 && rndA.size == 16 && rndB.size == 16)
        val mixed = ByteArray(6) { index -> (rndA[index + 2].toInt() xor rndB[index].toInt()).toByte() }
        val vector = label + byteArrayOf(0x00, 0x01, 0x00, 0x80.toByte()) +
            rndA.copyOfRange(0, 2) + mixed + rndB.copyOfRange(6, 16) + rndA.copyOfRange(8, 16)
        return cmac(key, vector)
    }

    fun pad80(data: ByteArray): ByteArray {
        val paddedSize = ((data.size / BLOCK_SIZE) + 1) * BLOCK_SIZE
        return ByteArray(paddedSize).also {
            data.copyInto(it)
            it[data.size] = 0x80.toByte()
        }
    }

    fun rotateLeft(data: ByteArray): ByteArray = data.copyOfRange(1, data.size) + data[0]

    fun rotateRight(data: ByteArray): ByteArray = byteArrayOf(data.last()) + data.copyOfRange(0, data.size - 1)

    private fun doubleBlock(input: ByteArray): ByteArray {
        val output = ByteArray(input.size)
        var carry = 0
        for (index in input.indices.reversed()) {
            val value = input[index].toInt() and 0xFF
            output[index] = ((value shl 1) or carry).toByte()
            carry = value ushr 7
        }
        if (carry != 0) output[output.lastIndex] = (output.last().toInt() xor CMAC_RB).toByte()
        return output
    }

    private fun xorInPlace(target: ByteArray, other: ByteArray) {
        target.indices.forEach { target[it] = (target[it].toInt() xor other[it].toInt()).toByte() }
    }

    private fun cipher(
        transformation: String,
        mode: Int,
        key: ByteArray,
        iv: ByteArray? = null
    ): Cipher = Cipher.getInstance(transformation).apply {
        val keySpec = SecretKeySpec(key, "AES")
        if (iv == null) init(mode, keySpec) else init(mode, keySpec, IvParameterSpec(iv))
    }
}
