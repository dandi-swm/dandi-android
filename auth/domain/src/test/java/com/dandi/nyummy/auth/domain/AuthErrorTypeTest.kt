package com.dandi.nyummy.auth.domain

import com.dandi.nyummy.common.domain.error.HttpResponseException
import com.dandi.nyummy.common.domain.error.HttpResponseStatus
import com.dandi.nyummy.common.domain.error.handlingErrorOnUseCase
import com.dandi.nyummy.common.domain.error.isCommonErrorHandling
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthErrorTypeTest {

    private fun exceptionWithCode(rawCode: Int, code: String?): HttpResponseException =
        HttpResponseException(
            status = HttpResponseStatus.create(rawCode),
            rawCode = rawCode,
            errorRequestUrl = "http://localhost/api/v1/auth/login",
            msg = "test",
            cause = code?.let(::Throwable),
        )

    @Test
    fun `서버 에러 code가 등록된 타입이면 매칭된다`() {
        val errorType = exceptionWithCode(400, "api.common.missingParameter")
            .handlingErrorOnUseCase<AuthErrorType>()

        assertEquals(AuthErrorType.MISSING_PARAMETER, errorType)
    }

    @Test
    fun `등록되지 않은 code는 null을 반환한다`() {
        val errorType = exceptionWithCode(400, "api.auth.notRegistered")
            .handlingErrorOnUseCase<AuthErrorType>()

        assertNull(errorType)
    }

    @Test
    fun `cause가 없으면 null을 반환한다`() {
        val errorType = exceptionWithCode(400, null)
            .handlingErrorOnUseCase<AuthErrorType>()

        assertNull(errorType)
    }

    @Test
    fun `401은 공통 에러 처리 대상이다`() {
        assertTrue(exceptionWithCode(401, null).isCommonErrorHandling())
        assertFalse(exceptionWithCode(400, null).isCommonErrorHandling())
    }
}
