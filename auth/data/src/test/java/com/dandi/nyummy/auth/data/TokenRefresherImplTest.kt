package com.dandi.nyummy.auth.data

import com.dandi.nyummy.common.data.token.TokenProvider
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.IOException

/**
 * 토큰 재발급의 결과 계약. 세션이 끝났을 때만 토큰을 지우고 null(→ 로그인 만료)을 돌려주고,
 * 일시적인 실패는 IOException(→ 네트워크 오류)으로 던져 토큰을 남기는지 본다.
 */
class TokenRefresherImplTest {

    private lateinit var server: MockWebServer
    private lateinit var refresher: TokenRefresherImpl
    private val tokenProvider = InMemoryTokenProvider(access = "old-access", refresh = "old-refresh")

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val apiService = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(AuthApiService::class.java)
        refresher = TokenRefresherImpl(apiService, tokenProvider)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `재발급에 성공하면 새 토큰을 저장하고 새 액세스 토큰을 돌려준다`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"accessToken":"new-access","refreshToken":"new-refresh"}"""))

        val token = refresher.refresh("old-refresh")

        assertEquals("new-access", token)
        assertEquals("new-access", tokenProvider.accessToken)
        assertEquals("new-refresh", tokenProvider.refreshToken)
    }

    @Test
    fun `리프레시 토큰이 무효(401)면 토큰을 지우고 null 로 세션 만료를 알린다`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(401).setBody("""{"code":"api.auth.invalidRefreshToken"}"""))

        val token = refresher.refresh("old-refresh")

        assertNull(token)
        assertNull(tokenProvider.accessToken)
        assertNull(tokenProvider.refreshToken)
    }

    @Test
    fun `서버 5xx 는 세션 만료가 아니라 IOException 으로 던지고 토큰을 남긴다`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(503))

        assertThrowsIOException { refresher.refresh("old-refresh") }

        assertEquals("old-access", tokenProvider.accessToken)
        assertEquals("old-refresh", tokenProvider.refreshToken)
    }

    @Test
    fun `네트워크가 끊기면 IOException 을 던지고 토큰을 남긴다`() = runBlocking {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))

        assertThrowsIOException { refresher.refresh("old-refresh") }

        assertEquals("old-refresh", tokenProvider.refreshToken)
    }

    @Test
    fun `응답을 해석하지 못해도 Authenticator 밖으로 다른 예외를 내지 않고 IOException 으로 던진다`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody("not json"))

        assertThrowsIOException { refresher.refresh("old-refresh") }

        assertEquals("old-refresh", tokenProvider.refreshToken)
    }

    private suspend fun assertThrowsIOException(block: suspend () -> Unit) {
        try {
            block()
            fail("IOException 이 나야 한다")
        } catch (_: IOException) {
        }
    }

    private class InMemoryTokenProvider(access: String?, refresh: String?) : TokenProvider {
        override var accessToken: String? = access
            private set
        override var refreshToken: String? = refresh
            private set

        override suspend fun update(access: String, refresh: String) {
            accessToken = access
            refreshToken = refresh
        }

        override suspend fun clear() {
            accessToken = null
            refreshToken = null
        }
    }
}
