package com.dandi.nyummy.auth.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EmailLoginValidatorTest {

    @Test
    fun `올바른 이메일은 통과한다`() {
        assertNull(EmailLoginValidator.validateEmail("jinu@example.com"))
        assertNull(EmailLoginValidator.validateEmail("a.b+tag@sub.domain.co"))
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

}
