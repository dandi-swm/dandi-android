package com.dandi.nyummy.auth.data

import com.dandi.nyummy.auth.data.dto.EmailVerificationConfirmRequestDTO
import com.dandi.nyummy.auth.data.dto.EmailVerificationRequestDTO
import com.dandi.nyummy.auth.data.dto.LoginRequestDTO
import com.dandi.nyummy.auth.data.dto.SignUpRequestDTO
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * AuthApiService의 요청 경로·메서드·바디 직렬화와 응답 역직렬화를 검증합니다.
 */
class AuthApiServiceTest {

    private lateinit var server: MockWebServer
    private lateinit var apiService: AuthApiService

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
    }

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        apiService = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(AuthApiService::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `로그인 요청이 올바른 경로로 전송되고 토큰 응답을 파싱한다`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"redirectUrl":"https://dandi.app/home","accessToken":"access-123","refreshToken":"refresh-456"}"""
            )
        )

        val response = apiService.login(LoginRequestDTO(email = "test@dandi.app", password = "pw1234"))

        assertTrue(response.isSuccessful)
        assertEquals("access-123", response.body()?.accessToken)
        assertEquals("refresh-456", response.body()?.refreshToken)
        assertEquals("https://dandi.app/home", response.body()?.redirectUrl)

        val recorded = server.takeRequest()
        assertEquals("POST", recorded.method)
        assertEquals("/api/v1/auth/login", recorded.path)
        val sentBody = json.decodeFromString<LoginRequestDTO>(recorded.body.readUtf8())
        assertEquals("test@dandi.app", sentBody.email)
        assertEquals("pw1234", sentBody.password)
    }

    @Test
    fun `회원가입 요청이 인증 완료 토큰과 신체 정보를 포함해 전송되고 토큰 응답을 파싱한다`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"accessToken":"access-123","refreshToken":"refresh-456"}"""
            )
        )

        val response = apiService.signUp(
            SignUpRequestDTO(
                emailVerifiedToken = "verified-token",
                password = "pw1234",
                confirmPassword = "pw1234",
                nickname = "단디",
                gender = "MALE",
                birth = "2000-01-15",
                height = 175,
                weight = 70,
            )
        )

        assertTrue(response.isSuccessful)
        assertEquals("access-123", response.body()?.accessToken)

        val recorded = server.takeRequest()
        assertEquals("POST", recorded.method)
        assertEquals("/api/v1/auth/signup", recorded.path)
        val rawBody = recorded.body.readUtf8()
        assertFalse(rawBody.contains("\"email\""))
        val sentBody = json.decodeFromString<SignUpRequestDTO>(rawBody)
        assertEquals("verified-token", sentBody.emailVerifiedToken)
        assertEquals("pw1234", sentBody.confirmPassword)
        assertEquals("단디", sentBody.nickname)
        assertEquals("2000-01-15", sentBody.birth)
        assertEquals(175, sentBody.height)
    }

    @Test
    fun `회원가입 요청에서 선택 프로필 필드는 null이면 직렬화에서 생략된다`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"accessToken":"access-123","refreshToken":"refresh-456"}"""
            )
        )

        apiService.signUp(
            SignUpRequestDTO(
                emailVerifiedToken = "verified-token",
                password = "pw1234",
                confirmPassword = "pw1234",
                nickname = "단디",
            )
        )

        val rawBody = server.takeRequest().body.readUtf8()
        assertFalse(rawBody.contains("gender"))
        assertFalse(rawBody.contains("birth"))
        assertFalse(rawBody.contains("height"))
        assertFalse(rawBody.contains("weight"))
    }

    @Test
    fun `이메일 인증 코드 발송은 챌린지 토큰 응답을 파싱한다`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"emailChallengeToken":"challenge-token"}"""
            )
        )

        val response = apiService.requestEmailVerification(EmailVerificationRequestDTO(email = "test@dandi.app"))

        assertTrue(response.isSuccessful)
        assertEquals("challenge-token", response.body()?.emailChallengeToken)

        val recorded = server.takeRequest()
        assertEquals("POST", recorded.method)
        assertEquals("/api/v1/auth/email-verification", recorded.path)
    }

    @Test
    fun `이메일 인증 코드 확인 요청에 인증 코드와 챌린지 토큰이 포함되고 인증 완료 토큰 응답을 파싱한다`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"emailVerifiedToken":"verified-token"}"""
            )
        )

        val response = apiService.confirmEmailVerification(
            EmailVerificationConfirmRequestDTO(authCode = "123456", emailChallengeToken = "challenge-token")
        )

        assertTrue(response.isSuccessful)
        assertEquals("verified-token", response.body()?.emailVerifiedToken)

        val recorded = server.takeRequest()
        assertEquals("POST", recorded.method)
        assertEquals("/api/v1/auth/email-verification/confirm", recorded.path)
        val sentBody = json.decodeFromString<EmailVerificationConfirmRequestDTO>(recorded.body.readUtf8())
        assertEquals("123456", sentBody.authCode)
        assertEquals("challenge-token", sentBody.emailChallengeToken)
    }

    @Test
    fun `응답에 알 수 없는 필드가 있어도 파싱에 성공한다`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"accessToken":"access-123","refreshToken":"refresh-456","newServerField":"whatever"}"""
            )
        )

        val response = apiService.login(LoginRequestDTO(email = "test@dandi.app", password = "pw1234"))

        assertTrue(response.isSuccessful)
        assertEquals("access-123", response.body()?.accessToken)
    }
}
