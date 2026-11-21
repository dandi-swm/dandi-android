package com.dandi.nyummy.common.domain.error

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException

class DomainFailureTest {

    private val errorTypes = TestErrorType.entries

    @Test
    fun `등록된 서버 code 는 404 나 401 이어도 공통 오류보다 먼저 비즈니스 에러다`() {
        listOf(404, 401, 500).forEach { status ->
            val failure = httpException(status, TestErrorType.DOMAIN.type).toDomainFailure(errorTypes)

            assertEquals(DomainFailure.Business(TestErrorType.DOMAIN, failure.cause as HttpResponseException), failure)
        }
    }

    @Test
    fun `ALL 정책은 로그인 만료 401 과 404 와 5xx 를 공통 오류로 본다`() {
        listOf(httpException(401), httpException(401, SESSION_TOKEN_EXPIRED_CODE), httpException(404), httpException(503)).forEach {
            assertTrue(it.toDomainFailure(errorTypes) is DomainFailure.Common)
        }
    }

    @Test
    fun `등록하지 않은 code 의 4xx 와 다른 code 의 401 은 응답 오류로 화면 안내에 맡긴다`() {
        assertTrue(httpException(400, "api.unknown").toDomainFailure(errorTypes) is DomainFailure.UnknownHttp)
        assertTrue(httpException(401, "api.auth.invalidVerifiedToken").toDomainFailure(errorTypes) is DomainFailure.UnknownHttp)
    }

    @Test
    fun `SIGNED_OUT 정책은 401 을 로그인 만료로 보지 않고 404 와 5xx 만 공통 오류로 본다`() {
        val policy = CommonErrorPolicy.SIGNED_OUT

        assertTrue(httpException(401).toDomainFailure(errorTypes, policy) is DomainFailure.UnknownHttp)
        assertTrue(httpException(404).toDomainFailure(errorTypes, policy) is DomainFailure.Common)
        assertTrue(httpException(500).toDomainFailure(errorTypes, policy) is DomainFailure.Common)
    }

    @Test
    fun `SESSION_ONLY 정책은 로그인 만료만 공통 오류로 본다`() {
        val policy = CommonErrorPolicy.SESSION_ONLY

        assertTrue(httpException(401).toDomainFailure(errorTypes, policy) is DomainFailure.Common)
        assertTrue(httpException(404).toDomainFailure(errorTypes, policy) is DomainFailure.UnknownHttp)
        assertTrue(httpException(500).toDomainFailure(errorTypes, policy) is DomainFailure.UnknownHttp)
    }

    @Test
    fun `NONE 정책은 공통 오류를 두지 않는다`() {
        listOf(httpException(401), httpException(404), httpException(500)).forEach {
            assertTrue(it.toDomainFailure(errorTypes, CommonErrorPolicy.NONE) is DomainFailure.UnknownHttp)
        }
    }

    @Test
    fun `IOException 은 네트워크, 그 밖의 예외는 예상 못 한 예외다`() {
        val io = IOException("offline")
        val unexpected = IllegalStateException("empty body")

        assertEquals(DomainFailure.Network(io), io.toDomainFailure(errorTypes))
        assertEquals(DomainFailure.Unexpected(unexpected), unexpected.toDomainFailure(errorTypes))
    }

    @Test
    fun `코루틴 취소는 나누지 않고 그대로 다시 던진다`() {
        val cancel = CancellationException("screen closed")
        try {
            cancel.toDomainFailure(errorTypes)
            fail("CancellationException 을 삼켰다")
        } catch (e: CancellationException) {
            assertSame(cancel, e)
        }
    }

    @Test
    fun `runSuspendCatching 은 실패를 Result 로 바꾸지만 취소는 다시 던진다`() = runBlocking {
        val failure = IllegalStateException("disk full")
        assertSame(failure, runSuspendCatching { throw failure }.exceptionOrNull())
        assertEquals(3, runSuspendCatching { 3 }.getOrNull())

        val cancel = CancellationException("screen closed")
        try {
            runSuspendCatching { throw cancel }
            fail("CancellationException 을 삼켰다")
        } catch (e: CancellationException) {
            assertSame(cancel, e)
        }
    }

    @Test
    fun `isCommonErrorNotified 는 UseCase 와 같은 순서로 공통 안내 여부를 본다`() {
        // 서버 code 가 붙은 404 는 비즈니스 에러라 공통 안내가 아니다(화면이 안내해야 한다).
        assertFalse(httpException(404, TestErrorType.DOMAIN.type).isCommonErrorNotified(errorTypes))
        assertTrue(httpException(404).isCommonErrorNotified(errorTypes))
        assertFalse(httpException(500).isCommonErrorNotified(errorTypes, CommonErrorPolicy.SESSION_ONLY))
        assertFalse(IOException("offline").isCommonErrorNotified(errorTypes))
        assertFalse(CancellationException().isCommonErrorNotified(errorTypes))
    }
}
