package com.familyos.core.domain.billing

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.max

/**
 * Signed FamilyOS Premium keys issued by the Windows Key Issuer.
 *
 * Format: `FOS-{NONCE}-{DAYS}-{HMAC}`
 * Example: `FOS-A1B2C3D4-365-9F3E2A1B`
 *
 * [days] `0` means lifetime. The HMAC is SHA-256 over `{NONCE}:{DAYS}` with [SECRET].
 * New keys are created only in `tools/key_issuer` — the Android app only verifies them.
 */
object LicenseKey {
    /** Must match `tools/key_issuer/issuer.py` SECRET. */
    internal const val SECRET = "FamilyOS.SEGAL.COMMUNICATIONS.license.v1.2026"

    data class Grant(
        val days: Int,
    ) {
        val isLifetime: Boolean get() = days <= 0
    }

    fun verify(raw: String): Grant? {
        val code = raw.trim().uppercase().replace(" ", "")
        val parts = code.split('-')
        if (parts.size != 4 || parts[0] != "FOS") return null
        val nonce = parts[1]
        val days = parts[2].toIntOrNull() ?: return null
        val sig = parts[3]
        if (nonce.length < 6 || sig.length < 8) return null
        if (days < 0) return null
        val expected = signature(nonce, days)
        if (!sig.equals(expected, ignoreCase = true)) return null
        return Grant(days)
    }

    internal fun signature(nonce: String, days: Int): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(SECRET.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        val digest = mac.doFinal("${nonce.uppercase()}:$days".toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { b -> "%02X".format(b.toInt() and 0xFF) }.take(8)
    }

    fun expiresAtMillis(grant: Grant, now: Long = System.currentTimeMillis()): Long? {
        if (grant.isLifetime) return null
        val dayMs = 24L * 60L * 60L * 1000L
        return now + max(1, grant.days) * dayMs
    }
}
