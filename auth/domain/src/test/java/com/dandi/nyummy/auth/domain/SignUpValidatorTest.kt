package com.dandi.nyummy.auth.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SignUpValidatorTest {

    @Test
    fun `올바른 이메일은 통과한다`() {
        assertNull(SignUpValidator.validateEmail("test@dandi.app"))
    }

    @Test
    fun `형식이 틀린 이메일은 EMAIL_FORMAT 오류를 반환한다`() {
        assertEquals(SignUpFieldError.EMAIL_FORMAT, SignUpValidator.validateEmail("test@dandi"))
        assertEquals(SignUpFieldError.EMAIL_FORMAT, SignUpValidator.validateEmail("test.dandi.app"))
    }

    @Test
    fun `영문과 숫자를 포함한 8자 이상 비밀번호는 통과한다`() {
        assertNull(SignUpValidator.validatePassword("abcd1234"))
    }

    @Test
    fun `짧거나 영문·숫자 조합이 아닌 비밀번호는 PASSWORD_POLICY 오류를 반환한다`() {
        assertEquals(SignUpFieldError.PASSWORD_POLICY, SignUpValidator.validatePassword("ab12"))
        assertEquals(SignUpFieldError.PASSWORD_POLICY, SignUpValidator.validatePassword("abcdefgh"))
        assertEquals(SignUpFieldError.PASSWORD_POLICY, SignUpValidator.validatePassword("12345678"))
    }

    @Test
    fun `비밀번호 확인이 일치하면 통과하고 다르면 MISMATCH 오류를 반환한다`() {
        assertNull(SignUpValidator.validatePasswordConfirm("abcd1234", "abcd1234"))
        assertEquals(
            SignUpFieldError.PASSWORD_CONFIRM_MISMATCH,
            SignUpValidator.validatePasswordConfirm("abcd1234", "abcd12345"),
        )
    }

    @Test
    fun `공백 닉네임은 NICKNAME_EMPTY 오류를 반환한다`() {
        assertNull(SignUpValidator.validateNickname("진우 집사"))
        assertEquals(SignUpFieldError.NICKNAME_EMPTY, SignUpValidator.validateNickname("  "))
    }

    @Test
    fun `집사 이름은 앞뒤 공백을 뺀 8자까지이고 이모지는 2자 이상으로 센다`() {
        assertNull(SignUpValidator.validateNickname("가나다라마바사아"))
        assertNull(SignUpValidator.validateNickname("  가나다라마바사아  "))
        assertEquals(SignUpFieldError.NICKNAME_TOO_LONG, SignUpValidator.validateNickname("가나다라마바사아자"))
        assertEquals(SignUpFieldError.NICKNAME_TOO_LONG, SignUpValidator.validateNickname("가나다라마바사😺"))
    }
}
