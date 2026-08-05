package com.dandi.nyummy.auth.data.dto

import org.junit.Assert.assertEquals
import org.junit.Test

class AuthTokenDTOTest {

    @Test
    fun `모든 필드가 null이면 빈 문자열 VO로 변환된다`() {
        val vo = AuthTokenDTO().toVO()

        assertEquals("", vo.accessToken)
        assertEquals("", vo.refreshToken)
        assertEquals("", vo.redirectUrl)
    }

    @Test
    fun `채워진 필드는 그대로 VO에 매핑된다`() {
        val vo = AuthTokenDTO(
            accessToken = "access",
            refreshToken = "refresh",
            redirectUrl = "www.dandi.com/home",
        ).toVO()

        assertEquals("access", vo.accessToken)
        assertEquals("refresh", vo.refreshToken)
        assertEquals("www.dandi.com/home", vo.redirectUrl)
    }

    @Test
    fun `redirectUrl이 없는 회원가입 응답도 안전하게 변환된다`() {
        val vo = AuthTokenDTO(accessToken = "access", refreshToken = "refresh").toVO()

        assertEquals("access", vo.accessToken)
        assertEquals("", vo.redirectUrl)
    }
}
