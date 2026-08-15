package com.familyos.core.data.auth

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * PBKDF2-HMAC-SHA256 password hashing for local auth credentials.
 */
object PasswordHasher {

    private const val ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val ITERATIONS = 120_000
    private const val SALT_BYTES = 16
    private const val KEY_BYTES = 32

    private val secureRandom = SecureRandom()

    /** Generates a new cryptographically secure salt (Base64). */
    fun newSalt(): String {
        val salt = ByteArray(SALT_BYTES)
        secureRandom.nextBytes(salt)
        return Base64.getEncoder().encodeToString(salt)
    }

    /** Returns Base64-encoded PBKDF2 hash of [password] with the given Base64 [salt]. */
    fun hash(password: String, salt: String): String {
        val saltBytes = Base64.getDecoder().decode(salt)
        val spec = PBEKeySpec(
            password.toCharArray(),
            saltBytes,
            ITERATIONS,
            KEY_BYTES * 8,
        )
        val factory = SecretKeyFactory.getInstance(ALGORITHM)
        val hash = factory.generateSecret(spec).encoded
        spec.clearPassword()
        return Base64.getEncoder().encodeToString(hash)
    }

    /** Constant-time verification of [password] against stored [salt] + [expectedHash]. */
    fun verify(password: String, salt: String, expectedHash: String): Boolean {
        val actual = hash(password, salt)
        return constantTimeEquals(actual, expectedHash)
    }

    private fun constantTimeEquals(a: String, b: String): Boolean {
        val aBytes = a.toByteArray(Charsets.UTF_8)
        val bBytes = b.toByteArray(Charsets.UTF_8)
        if (aBytes.size != bBytes.size) return false
        var diff = 0
        for (i in aBytes.indices) {
            diff = diff or (aBytes[i].toInt() xor bBytes[i].toInt())
        }
        return diff == 0
    }
}
