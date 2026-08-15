package com.familyos.core.domain.logic

import com.familyos.core.domain.util.Constants
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class InviteCodeGeneratorTest {

    @Test
    fun generate_usesExpectedLengthAndAlphabet() {
        val code = InviteCodeGenerator.generate()

        assertThat(code).hasLength(Constants.INVITE_CODE_LENGTH)
        assertThat(code.all { it in Constants.INVITE_CODE_ALPHABET }).isTrue()
    }

    @Test
    fun generate_customLength() {
        val code = InviteCodeGenerator.generate(length = 12)

        assertThat(code).hasLength(12)
    }

    @Test
    fun normalize_trimsAndUppercases() {
        assertThat(InviteCodeGenerator.normalize("  abcd1234  ")).isEqualTo("ABCD1234")
    }

    @Test
    fun isValid_acceptsGeneratedCodes() {
        val code = InviteCodeGenerator.generate()
        assertThat(InviteCodeGenerator.isValid(code)).isTrue()
    }

    @Test
    fun isValid_rejectsWrongLengthOrAlphabet() {
        assertThat(InviteCodeGenerator.isValid("SHORT")).isFalse()
        assertThat(InviteCodeGenerator.isValid("ABCD1OIL")).isFalse() // O,I,L excluded
        assertThat(InviteCodeGenerator.isValid("abcd2345")).isTrue() // normalized
    }
}
