package com.dandi.nyummy.common.domain.error

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ErrorParserTest {

    @Test
    fun `code 가 없는 401 은 로그인 만료다`() {
        val e = httpException(401)

        assertTrue(e.isSessionExpired())
        assertTrue(e.isCommonErrorHandling())
    }

    @Test
    fun `세션 code 가 붙은 401 은 로그인 만료다`() {
        val e = httpException(401, SESSION_UNAUTHORIZED_CODE)

        assertTrue(e.isSessionExpired())
        assertTrue(e.isCommonErrorHandling())
    }

    @Test
    fun `다른 code 가 붙은 401 은 로그인 만료도 공통 오류도 아니다`() {
        val e = httpException(401, "api.auth.invalidVerifiedToken")

        assertFalse(e.isSessionExpired())
        assertFalse(e.isCommonErrorHandling())
    }

    @Test
    fun `404 와 5xx 는 code 와 상관없이 공통 오류이고 400 은 아니다`() {
        assertTrue(httpException(404, "api.meal.notFound").isCommonErrorHandling())
        assertTrue(httpException(500).isCommonErrorHandling())
        assertTrue(httpException(503, "api.email.sendFailed").isCommonErrorHandling())
        assertFalse(httpException(400).isCommonErrorHandling())
    }

    @Test
    fun `빈 code 는 code 가 없는 것으로 본다`() {
        val e = httpException(401, "")

        assertNull(e.serverErrorCode)
        assertTrue(e.isSessionExpired())
    }

    @Test
    fun `domain 에서 처리하지 않는 code 는 등록돼 있어도 handlingErrorOnUseCase 가 돌려주지 않는다`() {
        val e = httpException(409, TestErrorType.PRESENTATION_ONLY.type)

        assertEquals(TestErrorType.PRESENTATION_ONLY, e.registeredErrorType<TestErrorType>())
        assertNull(e.handlingErrorOnUseCase<TestErrorType>())
    }

    @Test
    fun `domain 에서 처리하는 code 는 둘 다 돌려준다`() {
        val e = httpException(401, TestErrorType.DOMAIN.type)

        assertEquals(TestErrorType.DOMAIN, e.registeredErrorType<TestErrorType>())
        assertEquals(TestErrorType.DOMAIN, e.handlingErrorOnUseCase<TestErrorType>())
    }
}

internal enum class TestErrorType(
    override val type: String,
    override val errorMsg: String,
    override val isHandledOnDomain: Boolean = true,
) : HttpErrorType {
    DOMAIN(type = "api.test.domain", errorMsg = "도메인에서 안내"),
    PRESENTATION_ONLY(type = "api.test.presentationOnly", errorMsg = "화면에서 안내", isHandledOnDomain = false),
}

/** 서버 에러 바디의 `code`만 cause로 담는 실제 변환(BaseRemoteDataSource)과 같은 형태로 만든다. */
internal fun httpException(code: Int, errorCode: String? = null): HttpResponseException =
    HttpResponseException(
        status = HttpResponseStatus.create(code),
        rawCode = code,
        errorRequestUrl = "https://test/api",
        msg = "Http Request Failed ($code)",
        cause = errorCode?.let(::Throwable),
    )
