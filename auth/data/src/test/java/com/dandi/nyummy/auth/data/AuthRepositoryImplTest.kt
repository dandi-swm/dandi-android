package com.dandi.nyummy.auth.data

import com.dandi.nyummy.common.data.token.TokenProvider
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * AuthRepositoryImpl의 토큰 저장 부수효과와 요청 조립을 검증합니다.
 */
class AuthRepositoryImplTest {

    private lateinit var server: MockWebServer
    private lateinit var tokenProvider: FakeTokenProvider
    private lateinit var repository: AuthRepositoryImpl

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
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
        tokenProvider = FakeTokenProvider()
        repository = AuthRepositoryImpl(AuthDataSource(apiService), tokenProvider)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `이메일 인증 코드 발송·확인은 토큰을 저장하지 않는다`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200))
        server.enqueue(MockResponse().setResponseCode(200))

        repository.requestEmailVerification(email = "test@dandi.app")
        repository.confirmEmailVerification(email = "test@dandi.app", verificationCode = "123456")

        assertNull(tokenProvider.accessToken)
        assertNull(tokenProvider.refreshToken)
    }

    private class FakeTokenProvider : TokenProvider {
        override var accessToken: String? = null
            private set
        override var refreshToken: String? = null
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
