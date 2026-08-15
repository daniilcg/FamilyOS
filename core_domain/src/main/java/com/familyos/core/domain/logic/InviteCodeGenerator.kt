package com.familyos.core.domain.logic

import com.familyos.core.domain.util.Constants
import java.security.SecureRandom

/**
 * Generates human-friendly family invite codes.
 */
object InviteCodeGenerator {

    private val random = SecureRandom()

    /**
     * Creates a new invite code of [length] characters using an unambiguous alphabet.
     */
    fun generate(length: Int = Constants.INVITE_CODE_LENGTH): String {
        require(length in 4..32) { "Invite code length must be between 4 and 32" }
        val alphabet = Constants.INVITE_CODE_ALPHABET
        val builder = StringBuilder(length)
        repeat(length) {
            val index = random.nextInt(alphabet.length)
            builder.append(alphabet[index])
        }
        return builder.toString()
    }

    /**
     * Normalizes user-entered invite codes for lookup (trim + uppercase).
     */
    fun normalize(raw: String): String = raw.trim().uppercase()

    /**
     * Returns true when [code] matches the expected alphabet and length.
     */
    fun isValid(code: String, length: Int = Constants.INVITE_CODE_LENGTH): Boolean {
        val normalized = normalize(code)
        if (normalized.length != length) return false
        return normalized.all { it in Constants.INVITE_CODE_ALPHABET }
    }
}
