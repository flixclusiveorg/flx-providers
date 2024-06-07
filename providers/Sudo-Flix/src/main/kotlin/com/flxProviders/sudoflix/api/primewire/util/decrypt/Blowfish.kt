package com.flxProviders.sudoflix.api.primewire.util.decrypt

import com.flxProviders.sudoflix.api.primewire.util.decrypt.BlowfishConstants.P_ARRAY
import com.flxProviders.sudoflix.api.primewire.util.decrypt.BlowfishConstants.S_BOX0
import com.flxProviders.sudoflix.api.primewire.util.decrypt.BlowfishConstants.S_BOX1
import com.flxProviders.sudoflix.api.primewire.util.decrypt.BlowfishConstants.S_BOX2
import com.flxProviders.sudoflix.api.primewire.util.decrypt.BlowfishConstants.S_BOX3
import kotlin.math.ceil

internal class Blowfish(key: String) {
    private val sBox0: IntArray = S_BOX0.clone()
    private val sBox1: IntArray = S_BOX1.clone()
    private val sBox2: IntArray = S_BOX2.clone()
    private val sBox3: IntArray = S_BOX3.clone()
    private val pArray: IntArray = P_ARRAY.clone()
    @Suppress("SpellCheckingInspection")
    private val keyStr = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/="
    // private val iv = "abc12345"

    init {
        generateSubKeys(key)
    }

    private fun generateSubKeys(key: String) {
        var temp: Int
        var keyIndex = 0

        // XOR the key with the P-array
        for ((pIndex, _) in (0 until 18).withIndex()) {
            temp = 0
            for (j in 0 until 4) {
                temp = fixNegative((temp shl 8) or key[keyIndex].code)
                keyIndex = (keyIndex + 1) % key.length
            }
            pArray[pIndex] = xor(pArray[pIndex], temp)
        }

        var tempSubKey = Pair(0, 0)

        // Encipher the P-array
        for (i in 0 until 18 step 2) {
            tempSubKey = encipher(tempSubKey.first, tempSubKey.second)
            pArray[i] = tempSubKey.first
            pArray[i + 1] = tempSubKey.second
        }

        // Encipher the S-boxes
        for (i in 0 until 256 step 2) {
            tempSubKey = encipher(tempSubKey.first, tempSubKey.second)
            sBox0[i] = tempSubKey.first
            sBox0[i + 1] = tempSubKey.second
        }
        for (i in 0 until 256 step 2) {
            tempSubKey = encipher(tempSubKey.first, tempSubKey.second)
            sBox1[i] = tempSubKey.first
            sBox1[i + 1] = tempSubKey.second
        }
        for (i in 0 until 256 step 2) {
            tempSubKey = encipher(tempSubKey.first, tempSubKey.second)
            sBox2[i] = tempSubKey.first
            sBox2[i + 1] = tempSubKey.second
        }
        for (i in 0 until 256 step 2) {
            tempSubKey = encipher(tempSubKey.first, tempSubKey.second)
            sBox3[i] = tempSubKey.first
            sBox3[i + 1] = tempSubKey.second
        }
    }


    @Suppress("unused")
    private fun encrypt(e: String): String {
        val root = utf8Decode(e)
        var encrypted = ""
        val blockSize = 8
        val paddingChar = "\u0000"
        val numBlocks = ceil(e.length.toDouble() / blockSize.toDouble()).toInt()

        for (i in 0 until numBlocks) {
            var block = root.substring(blockSize * i, blockSize)

            if (block.length < blockSize) {
                block += paddingChar.repeat(blockSize - block.length)
            }

            var (left, right) = split64by32(block)
            val enciphered = encipher(left, right)
            left = enciphered.first
            right = enciphered.second

            encrypted += num2block32(left) + num2block32(right)
        }

        return encrypted
    }

    fun decrypt(input: String): String {
        val numBlocks = ceil(input.length.toDouble() / 8.0).toInt()

        var decrypted = ""

        for (i in 0 until numBlocks) {
            val block = input.substring(8 * i, 8 * (i + 1))

            if (block.length < 8) {
                throw IllegalArgumentException("[PrimeWire]> Invalid block size")
            }

            val (left, right) = split64by32(block)
            val (decipheredLeft, decipheredRight) = decipher(left, right)

            decrypted += num2block32(decipheredLeft) + num2block32(decipheredRight)
        }

        return utf8Encode(decrypted)
    }

    private fun encipher(plaintext: Int, key: Int): Pair<Int, Int> {
        var temp: Int
        var cipherText = plaintext
        var cipherKey = key

        for (round in 0 until 16) {
            temp = xor(cipherText, pArray[round])
            cipherText = xor(substitute(temp), cipherKey)
            cipherKey = temp
        }

        temp = cipherText
        cipherText = cipherKey
        cipherKey = temp

        cipherKey = xor(cipherKey, pArray[16])
        cipherText = xor(cipherText, pArray[17])

        return Pair(cipherText, cipherKey)
    }

    private fun decipher(left: Int, right: Int): Pair<Int, Int> {
        var n: Int
        var e = left
        var t = right

        n = xor(e, pArray[17])
        e = xor(t, pArray[16])
        t = n

        for (r in 15 downTo 0) {
            n = e
            e = t
            t = n
            t = xor(substitute(e), t)
            e = xor(e, pArray[r])
        }

        return Pair(e, t)
    }

    private fun block32toNum(e: String): Int {
        return fixNegative(
            (e[0].code shl 24) or (e[1].code shl 16) or (e[2].code shl 8) or e[3].code
        )
    }

    private fun num2block32(e: Int): String {
        return charArrayOf(
            (e ushr 24).toChar(),
            ((e shl 8) ushr 24).toChar(),
            ((e shl 16) ushr 24).toChar(),
            ((e shl 24) ushr 24).toChar()
        ).concatToString()
    }

    private fun xor(e: Int, t: Int): Int {
        return fixNegative(e xor t)
    }

    private fun addMod32(e: Int, t: Int): Int {
        return fixNegative((e + t) or 0)
    }

    private fun fixNegative(e: Int): Int {
        return e ushr 0
    }

    private fun split64by32(e: String): Pair<Int, Int> {
        val t = e.substring(0, 4)
        val n = e.substring(4, 8)
        return Pair(block32toNum(t), block32toNum(n))
    }

    private fun substitute(value: Int): Int {
        val t = value ushr 24
        val n = (value shl 8) ushr 24
        val r = (value shl 16) ushr 24
        val i = (value shl 24) ushr 24

        var result = addMod32(sBox0[t], sBox1[n])
        result = xor(result, sBox2[r])
        result = addMod32(result, sBox3[i])

        return result
    }

    private fun utf8Decode(input: String): String {
        var decoded = ""
        for (i in input.indices) {
            val charCode = input[i].code
            when {
                charCode < 128 -> decoded += charCode.toChar()
                charCode in 128..2047 -> {
                    val firstCharCode = (charCode shr 6) or 192
                    val secondCharCode = (charCode and 63) or 128
                    decoded += firstCharCode.toChar()
                    decoded += secondCharCode.toChar()
                }
                else -> {
                    val firstCharCode = (charCode shr 12) or 224
                    val secondCharCode = ((charCode shr 6) and 63) or 128
                    val thirdCharCode = (charCode and 63) or 128
                    decoded += firstCharCode.toChar()
                    decoded += secondCharCode.toChar()
                    decoded += thirdCharCode.toChar()
                }
            }
        }
        return decoded
    }

    private fun utf8Encode(input: String): String {
        var encoded = ""
        var i = 0
        while (i < input.length) {
            val charCode = input[i].code
            when {
                charCode < 128 -> encoded += charCode.toChar()
                charCode in 192..223 -> {
                    val secondCharCode = input[++i].code
                    encoded += (((charCode and 31) shl 6) or (secondCharCode and 63)).toChar()
                }
                else -> {
                    val secondCharCode = input[++i].code
                    val thirdCharCode = input[++i].code
                    encoded += (((charCode and 15) shl 12) or ((secondCharCode and 63) shl 6) or (thirdCharCode and 63)).toChar()
                }
            }
            i++
        }
        return encoded
    }

    fun base64(e: String): String {
        var s = ""
        var l = 0
        val root = e.replace(Regex("[^A-Za-z0-9+/=]"), "")
        while (l < root.length) {
            val t = (keyStr.indexOf(root[l++]) shl 2) or (keyStr.indexOf(root[l]).shr(4))
            val i = keyStr.indexOf(root[l++])
            val n = ((i and 15) shl 4) or (keyStr.indexOf(root[l]).shr(2))
            val o = keyStr.indexOf(root[l++])
            val r = ((o and 3) shl 6) or keyStr.indexOf(root[l++])
            s += t.toChar()
            if (o != 64) {
                s += n.toChar()
            }
            if (r != 64) {
                s += r.toChar()
            }
        }
        return s
    }

}
