package com.familyos.core.domain.security

/**
 * AES-256 document encryption / decryption contract used by the documents vault.
 *
 * Implementations live in the data layer and must use Android Keystore-backed keys
 * when available. Ciphertext is opaque bytes; plaintext never leaves the secure pipeline
 * without an explicit unlock.
 */
interface DocumentCipher {

    /**
     * Encrypts [plain] with AES-256-GCM.
     *
     * @return IV + ciphertext payload suitable for storage
     */
    fun encrypt(plain: ByteArray): ByteArray

    /**
     * Decrypts a payload previously produced by [encrypt].
     *
     * @throws SecurityException when the payload is tampered or the key is unavailable
     */
    fun decrypt(payload: ByteArray): ByteArray

    /**
     * Whether a valid encryption key is present for this device / user.
     */
    fun isKeyAvailable(): Boolean
}

/**
 * PIN and biometric gate for accessing the encrypted documents vault.
 */
interface DocumentLockGate {

    /** True when a vault PIN has been configured. */
    suspend fun isPinConfigured(): Boolean

    /** Persists a new PIN (hashed). */
    suspend fun setPin(pin: String)

    /** Validates the given PIN. */
    suspend fun verifyPin(pin: String): Boolean

    /** Clears the configured PIN. */
    suspend fun clearPin()

    /** Whether biometric unlock is enabled for the vault. */
    suspend fun isBiometricEnabled(): Boolean

    /** Enables or disables biometric unlock. */
    suspend fun setBiometricEnabled(enabled: Boolean)
}
