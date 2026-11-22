package com.dandi.nyummy.common.presentation.image

import coil3.network.HttpException
import coil3.network.NetworkResponse
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.SocketTimeoutException

class CdnImageRetryTest {

    @Test
    fun `첫 시도가 성공하면 한 번만 부른다`() {
        var calls = 0

        val result = runBlocking { retryOnce(attempt = { ++calls; "ok" }, shouldRetry = { it != "ok" }) }

        assertEquals("ok", result)
        assertEquals(1, calls)
    }

    @Test
    fun `다시 받을 실패면 한 번 더 부르고 그 결과를 돌려준다`() {
        val results = ArrayDeque(listOf("timeout", "ok"))
        var calls = 0

        val result = runBlocking { retryOnce(attempt = { calls++; results.removeFirst() }, shouldRetry = { it == "timeout" }) }

        assertEquals("ok", result)
        assertEquals(2, calls)
    }

    @Test
    fun `두 번째도 실패하면 더 부르지 않고 두 번째 실패를 돌려준다`() {
        var calls = 0

        val result = runBlocking { retryOnce(attempt = { ++calls; "timeout$calls" }, shouldRetry = { it.startsWith("timeout") }) }

        assertEquals("timeout2", result)
        assertEquals(2, calls)
    }

    @Test
    fun `시간 초과와 연결 실패는 다시 받는다`() {
        assertTrue(isRetryableImageFailure(SocketTimeoutException("timeout")))
        assertTrue(isRetryableImageFailure(IOException("connection reset")))
    }

    @Test
    fun `일시적인 서버 오류는 다시 받는다`() {
        assertTrue(isRetryableImageFailure(http(500)))
        assertTrue(isRetryableImageFailure(http(502)))
        assertTrue(isRetryableImageFailure(http(503)))
    }

    @Test
    fun `파일 없음, 오프라인 캐시 실패, 디코딩 실패는 다시 받지 않는다`() {
        assertFalse(isRetryableImageFailure(http(403)))
        assertFalse(isRetryableImageFailure(http(404)))
        assertFalse(isRetryableImageFailure(http(504))) // 오프라인에서 캐시만 찾다 실패
        assertFalse(isRetryableImageFailure(IllegalStateException("decode failed")))
    }

    @Test
    fun `CDN 주소만 다시 받기 대상이다`() {
        assertTrue(NyummyCdn.isCdnUrl(NyummyCdn.itemUrl(1L)))
        assertTrue(NyummyCdn.isCdnUrl("https://cdn.nyummy.co.kr/items/1.png"))
        assertFalse(NyummyCdn.isCdnUrl("https://cdn.nyummy.co.kr.evil.com/items/1.webp"))
        assertFalse(NyummyCdn.isCdnUrl("https://bucket.s3.amazonaws.com/meal.jpg?X-Amz-Signature=abc"))
    }

    private fun http(code: Int) = HttpException(NetworkResponse(code = code))
}
