package com.dandi.nyummy.common.data.token

import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * 재발급 경합과 실패 갈래. 동시에 401이 여러 개 와도 재발급은 한 번이고,
 * 재발급이 일시적으로 실패하면 원래 요청이 401(→ 로그인 만료)이 아니라 IOException(→ 네트워크 오류)으로 끝나는지 본다.
 */
class TokenAuthenticatorTest {

    private lateinit var server: MockWebServer
    private val tokenProvider = MemoryTokenProvider(access = "expired", refresh = "refresh")

    @Before
    fun setUp() {
        server = MockWebServer()
        // 새 토큰을 단 요청만 성공하고, 그 밖은 401이다.
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse =
                if (request.getHeader("Authorization") == "Bearer fresh") MockResponse().setBody("ok") else MockResponse().setResponseCode(401)
        }
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `동시에 401 이 여러 개 와도 재발급은 한 번만 하고 모두 새 토큰으로 성공한다`() {
        val refreshCount = AtomicInteger()
        val client = client(
            refresher = { _ ->
                refreshCount.incrementAndGet()
                delay(REFRESH_DELAY_MILLIS) // 다른 요청이 그 사이 401을 받게 한다.
                tokenProvider.update(access = "fresh", refresh = "refresh-2")
                "fresh"
            },
        )
        val pool = Executors.newFixedThreadPool(CONCURRENT_REQUESTS)

        val codes = pool.invokeAll(List(CONCURRENT_REQUESTS) { Callable { client.newCall(request()).execute().use { it.code } } })
            .map { it.get(5, TimeUnit.SECONDS) }
        pool.shutdown()

        assertEquals(List(CONCURRENT_REQUESTS) { 200 }, codes)
        assertEquals(1, refreshCount.get())
    }

    @Test
    fun `재발급이 일시적으로 실패하면 원래 요청은 IOException 으로 끝나고 토큰은 남는다`() {
        val client = client(refresher = { _ -> throw IOException("refresh offline") })

        try {
            client.newCall(request()).execute().close()
            fail("401 응답이 아니라 IOException 이어야 한다")
        } catch (_: IOException) {
        }
        assertEquals("refresh", tokenProvider.refreshToken)
    }

    @Test
    fun `세션이 끝나 재발급할 수 없으면 원래 401 을 그대로 돌려준다`() {
        val client = client(refresher = { _ -> null })

        val code = client.newCall(request()).execute().use { it.code }

        assertEquals(401, code)
    }

    @Test
    fun `리프레시 토큰이 없으면 재발급을 시도하지 않는다`() {
        tokenProvider.clearNow()
        var called = false
        val client = client(refresher = { _ -> called = true; "fresh" })

        val code = client.newCall(request()).execute().use { it.code }

        assertEquals(401, code)
        assertTrue(!called)
    }

    private fun client(refresher: suspend (String) -> String?): OkHttpClient {
        val tokenRefresher = object : TokenRefresher {
            override suspend fun refresh(refreshToken: String): String? = refresher(refreshToken)
        }
        return OkHttpClient.Builder()
            .authenticator(TokenAuthenticator(tokenProvider, tokenRefresher))
            .build()
    }

    private fun request(): Request = Request.Builder()
        .url(server.url("/api"))
        .header(TokenAuthenticator.HEADER_AUTHORIZATION, "${TokenAuthenticator.BEARER_PREFIX}${tokenProvider.accessToken}")
        .build()

    private class MemoryTokenProvider(access: String?, refresh: String?) : TokenProvider {
        @Volatile override var accessToken: String? = access
            private set

        @Volatile override var refreshToken: String? = refresh
            private set

        override suspend fun update(access: String, refresh: String) {
            accessToken = access
            refreshToken = refresh
        }

        override suspend fun clear() = clearNow()

        fun clearNow() {
            accessToken = null
            refreshToken = null
        }
    }

    private companion object {
        const val CONCURRENT_REQUESTS = 8
        const val REFRESH_DELAY_MILLIS = 200L
    }
}
