package com.familyos.core.domain.billing

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LicenseKeyTest {

    @Test
    fun verifiesPythonIssuerVector() {
        val grant = LicenseKey.verify("FOS-A1B2C3D4-365-68F3C9DE")
        assertThat(grant).isNotNull()
        assertThat(grant!!.days).isEqualTo(365)
        assertThat(grant.isLifetime).isFalse()
    }

    @Test
    fun lifetimeZeroDays() {
        val nonce = "DEADBEEF"
        val sig = LicenseKey.signature(nonce, 0)
        val grant = LicenseKey.verify("fos-$nonce-0-$sig")
        assertThat(grant).isNotNull()
        assertThat(grant!!.isLifetime).isTrue()
        assertThat(LicenseKey.expiresAtMillis(grant)).isNull()
    }

    @Test
    fun rejectsTamperedSignature() {
        val nonce = "A1B2C3D4"
        val sig = LicenseKey.signature(nonce, 365)
        assertThat(LicenseKey.verify("FOS-$nonce-365-00000000")).isNull()
        assertThat(LicenseKey.verify("FOS-$nonce-30-$sig")).isNull()
        assertThat(LicenseKey.verify("FAMILYOS-SEGAL-PREMIUM")).isNull()
    }
}
