package com.dandi.nyummy.onboarding.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CatNameValidatorTest {

    @Test
    fun `공백만 있으면 비어 있는 이름이다`() {
        assertEquals(CatNameError.EMPTY, CatNameValidator.validate(""))
        assertEquals(CatNameError.EMPTY, CatNameValidator.validate("   "))
    }

    @Test
    fun `앞뒤 공백을 빼고 10자까지 허용한다`() {
        assertNull(CatNameValidator.validate("냐"))
        assertNull(CatNameValidator.validate("  가나다라마바사아자차  "))
        assertEquals(CatNameError.TOO_LONG, CatNameValidator.validate("가나다라마바사아자차카"))
    }

    @Test
    fun `이모지는 한 글자로 센다`() {
        assertNull(CatNameValidator.validate("냐미🐱🐱🐱🐱🐱🐱🐱🐱"))
    }
}
