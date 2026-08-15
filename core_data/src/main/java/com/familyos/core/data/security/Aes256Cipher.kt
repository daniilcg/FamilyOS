package com.familyos.core.data.security

import android.util.Base64
import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AES-256-GCM helper for local document encryption.
 *
 * Ciphertext layout: `IV (12 bytes) || ciphertext+tag`.
 */
@Singleton
class Aes256Cipher @Inject constructor() {

    private val random = SecureRandom()

    /**
     * Generates a fresh 256-bit AES key.
     */
    fun generateKey(): ByteArray {
        val key = ByteArray(KEY_SIZE_BYTES)
        random.nextBytes(key)
        return key
    }

    /**
     * Encrypts [plain] with [keyBytes] using AES-256-GCM.
     */
    fun encrypt(plain: ByteArray, keyBytes: ByteArray): ByteArray {
        require(keyBytes.size == KEY_SIZE_BYTES) { "AES-256 key must be 32 bytes" }
        val iv = ByteArray(IV_SIZE_BYTES)
        random.nextBytes(iv)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, toKey(keyBytes), GCMParameterSpec(TAG_BITS, iv))
        val encrypted = cipher.doFinal(plain)
        return ByteBuffer.allocate(iv.size + encrypted.size)
            .put(iv)
            .put(encrypted)
            .array()
    }

    /**
     * Decrypts [payload] previously produced by [encrypt].
     */
    fun decrypt(payload: ByteArray, keyBytes: ByteArray): ByteArray {
        require(keyBytes.size == KEY_SIZE_BYTES) { "AES-256 key must be 32 bytes" }
        require(payload.size > IV_SIZE_BYTES) { "Ciphertext too short" }
        val buffer = ByteBuffer.wrap(payload)
        val iv = ByteArray(IV_SIZE_BYTES)
        buffer.get(iv)
        val encrypted = ByteArray(buffer.remaining())
        buffer.get(encrypted)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, toKey(keyBytes), GCMParameterSpec(TAG_BITS, iv))
        return cipher.doFinal(encrypted)
    }

    /**
     * Encodes raw bytes as URL-safe Base64 without wrapping.
     */
    fun encodeBase64(bytes: ByteArray): String =
        Base64.encodeToString(bytes, Base64.NO_WRAP or Base64.URL_SAFE)

    /**
     * Decodes URL-safe Base64 into raw bytes.
     */
    fun decodeBase64(value: String): ByteArray =
        Base64.decode(value, Base64.NO_WRAP or Base64.URL_SAFE)

    private fun toKey(keyBytes: ByteArray): SecretKey = SecretKeySpec(keyBytes, ALGORITHM)

    companion object {
        private const val ALGORITHM = "AES"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val KEY_SIZE_BYTES = 32
        private const val IV_SIZE_BYTES = 12
        private const val TAG_BITS = 128
    }
}
