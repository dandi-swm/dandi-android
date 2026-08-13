package com.dandi.nyummy.auth.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EmailLoginValidatorTest {

    @Test
    fun `올바른 이메일은 통과한다`() {
        listOf(
            "jinu@example.com",
            "a.b+tag@sub.domain.co",
            "user_name-1@my-domain.co.kr",
            "UPPER.CASE@EXAMPLE.COM",
            "x!#\$%&'*+/=?^_`{|}~-y@example.io",
        ).forEach { email ->
            assertNull(email, EmailLoginValidator.validateEmail(email))
        }
    }

    @Test
    fun `형식이 틀린 이메일은 EMAIL_FORMAT 을 반환한다`() {
        listOf(
            "",
            "jinu",
            "jinu@",
            "@example.com",
            "jinu@example",
            "jinu example.com",
            "jinu@@example.com",
            "ji nu@example.com",
        ).forEach { email ->
            assertEquals(email, EmailLoginFieldError.EMAIL_FORMAT, EmailLoginValidator.validateEmail(email))
        }
    }

    @Test
    fun `빈 도메인 라벨과 잘못된 라벨 형태는 거부한다`() {
        listOf(
            "jinu@.d.d",          // 선행 빈 라벨
            "jinu@d..d",          // 연속 점 (빈 라벨)
            "jinu@example.com.",  // 후행 빈 라벨
            "jinu@-domain.com",   // 하이픈으로 시작하는 라벨
            "jinu@domain-.com",   // 하이픈으로 끝나는 라벨
            "jinu@example.c",     // TLD 1자
            "jinu@example.123",   // 숫자 TLD
        ).forEach { email ->
            assertEquals(email, EmailLoginFieldError.EMAIL_FORMAT, EmailLoginValidator.validateEmail(email))
        }
    }

    @Test
    fun `로컬 파트의 점 위치 오류는 거부한다`() {
        listOf(
            ".jinu@example.com",  // 선행 점
            "jinu.@example.com",  // 후행 점
            "ji..nu@example.com", // 연속 점
        ).forEach { email ->
            assertEquals(email, EmailLoginFieldError.EMAIL_FORMAT, EmailLoginValidator.validateEmail(email))
        }
    }

    @Test
    fun `길이 제한을 넘는 이메일은 거부한다`() {
        val longLocalPart = "a".repeat(65) + "@example.com"
        assertEquals(EmailLoginFieldError.EMAIL_FORMAT, EmailLoginValidator.validateEmail(longLocalPart))

        val tooLongEmail = "a".repeat(64) + "@" + "b".repeat(63) + ".${"c".repeat(63)}.${"d".repeat(63)}.com"
        assertEquals(EmailLoginFieldError.EMAIL_FORMAT, EmailLoginValidator.validateEmail(tooLongEmail))

        val maxLocalPart = "a".repeat(64) + "@example.com"
        assertNull(EmailLoginValidator.validateEmail(maxLocalPart))
    }
}
