package com.familyos.core.data.auth

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PasswordHasherTest {

    @Test
    fun hashAndVerify_roundTrip() {
        val salt = PasswordHasher.newSalt()
        val password = "FamilyOS-Secret-42"
        val hash = PasswordHasher.hash(password, salt)

        assertThat(PasswordHasher.verify(password, salt, hash)).isTrue()
        assertThat(PasswordHasher.verify("wrong-password", salt, hash)).isFalse()
    }

    @Test
    fun differentSalts_produceDifferentHashes() {
        val password = "same-password"
        val salt1 = PasswordHasher.newSalt()
        val salt2 = PasswordHasher.newSalt()
        assertThat(salt1).isNotEqualTo(salt2)
        assertThat(PasswordHasher.hash(password, salt1))
            .isNotEqualTo(PasswordHasher.hash(password, salt2))
    }
}
