package com.dandi.nyummy.auth.data

import com.dandi.nyummy.auth.entity.EmailVerificationPurpose
import com.dandi.nyummy.auth.entity.Gender
import com.dandi.nyummy.auth.entity.SocialCredentialVO
import com.dandi.nyummy.auth.entity.SocialLoginType
import com.dandi.nyummy.common.data.token.TokenProvider
import com.dandi.nyummy.common.data.preference.AppPreferenceProvider
import com.dandi.nyummy.common.domain.error.HttpResponseException
import com.dandi.nyummy.common.entity.meal.MealTimesVO
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
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
    private lateinit var appPreferenceProvider: FakeAppPreferenceProvider
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
        appPreferenceProvider = FakeAppPreferenceProvider()
        repository = AuthRepositoryImpl(AuthDataSource(apiService), tokenProvider, appPreferenceProvider)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `회원가입 성공 시 발급 토큰을 저장한다`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"accessToken":"access-123","refreshToken":"refresh-456"}"""
            )
        )

        repository.signUp(
            verifiedToken = "verified-token",
            password = "pw1234",
            confirmPassword = "pw1234",
            nickname = "단디",
            gender = Gender.MALE,
            birth = "2000-01-15",
            height = 175,
            weight = 70,
        )

        assertEquals("access-123", tokenProvider.accessToken)
        assertEquals("refresh-456", tokenProvider.refreshToken)
    }

    @Test
    fun `온보딩 진행 상태를 환경설정에 저장한다`() = runBlocking {
        repository.setOnboardingIncomplete(incomplete = true)

        assertTrue(appPreferenceProvider.onboardingIncomplete)
    }

    @Test
    fun `회원가입 실패 시 예외를 던지고 토큰을 저장하지 않는다`() {
        server.enqueue(
            MockResponse().setResponseCode(400).setBody(
                """{"code":"api.common.missingParameter","message":"입력값이 누락되었습니다."}"""
            )
        )

        assertThrows(HttpResponseException::class.java) {
            runBlocking {
                repository.signUp(
                    verifiedToken = "verified-token",
                    password = "pw1234",
                    confirmPassword = "pw1234",
                    nickname = "단디",
                    gender = Gender.MALE,
                    birth = "2000-01-15",
                    height = 175,
                    weight = 70,
                )
            }
        }

        assertNull(tokenProvider.accessToken)
        assertNull(tokenProvider.refreshToken)
    }

    @Test
    fun `이메일 인증 코드 발송·확인은 각 토큰을 반환하고 인증 토큰을 저장하지 않는다`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody("""{"emailChallengeToken":"challenge-token"}""")
        )
        server.enqueue(
            MockResponse().setResponseCode(200).setBody("""{"verifiedToken":"verified-token"}""")
        )

        val challenge = repository.requestEmailVerification(
            email = "test@dandi.app",
            purpose = EmailVerificationPurpose.SIGNUP,
        )
        val verified = repository.confirmEmailVerification(
            authCode = "123456",
            emailChallengeToken = challenge.emailChallengeToken,
        )

        assertEquals("challenge-token", challenge.emailChallengeToken)
        assertEquals("verified-token", verified.emailVerifiedToken)
        assertNull(tokenProvider.accessToken)
        assertNull(tokenProvider.refreshToken)
    }

    @Test
    fun `소셜 로그인 기존 회원이면 발급 토큰을 저장한다`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"redirectUrl":"/home","accessToken":"access-123","refreshToken":"refresh-456"}"""
            )
        )

        val result = repository.socialLogin(kakaoCredential)

        assertTrue(result.isLoggedIn)
        assertEquals("access-123", tokenProvider.accessToken)
        assertEquals("refresh-456", tokenProvider.refreshToken)
        val sentBody = server.takeRequest().body.readUtf8()
        assertTrue(sentBody.contains("\"provider\":\"KAKAO\""))
        assertTrue(sentBody.contains("\"nonce\":\"nonce-1\""))
    }

    @Test
    fun `소셜 로그인 신규 회원이면 토큰을 저장하지 않고 검증 완료 토큰을 돌려준다`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody("""{"verifiedToken":"social-verified"}""")
        )

        val result = repository.socialLogin(kakaoCredential)

        assertTrue(result.isSignUpRequired)
        assertEquals("social-verified", result.verifiedToken)
        assertNull(tokenProvider.accessToken)
        assertNull(tokenProvider.refreshToken)
    }

    @Test
    fun `소셜 로그인 토큰 검증 실패면 예외를 던지고 토큰을 저장하지 않는다`() {
        server.enqueue(
            MockResponse().setResponseCode(401).setBody(
                """{"code":"api.auth.invalidSocialToken","message":"유효하지 않은 소셜 로그인 토큰입니다."}"""
            )
        )

        val error = assertThrows(HttpResponseException::class.java) {
            runBlocking { repository.socialLogin(kakaoCredential) }
        }

        assertEquals(401, error.rawCode)
        assertNull(tokenProvider.accessToken)
    }

    @Test
    fun `소셜 회원가입은 비밀번호 없이 요청하고 발급 토큰을 저장한다`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"accessToken":"access-123","refreshToken":"refresh-456"}"""
            )
        )

        repository.signUp(
            verifiedToken = "social-verified",
            password = null,
            confirmPassword = null,
            nickname = "단디",
            gender = Gender.FEMALE,
            birth = "2000-01-15",
            height = 160,
            weight = 50,
        )

        val sentBody = server.takeRequest().body.readUtf8()
        assertFalse(sentBody.contains("password"))
        assertTrue(sentBody.contains("\"verifiedToken\":\"social-verified\""))
        assertEquals("access-123", tokenProvider.accessToken)
    }

    private val kakaoCredential = SocialCredentialVO(
        type = SocialLoginType.KAKAO,
        token = "id-token",
        nonce = "nonce-1",
    )

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

    private class FakeAppPreferenceProvider : AppPreferenceProvider {
        var onboardingIncomplete = false

        override suspend fun hasShownPermissionNotice(): Boolean = false
        override suspend fun markPermissionNoticeShown() = Unit
        override suspend fun isOnboardingIncomplete(): Boolean = onboardingIncomplete
        override suspend fun setOnboardingIncomplete(incomplete: Boolean) {
            onboardingIncomplete = incomplete
        }

        var mealTimes: MealTimesVO? = null

        override suspend fun getMealTimes(): MealTimesVO? = mealTimes
        override suspend fun setMealTimes(mealTimes: MealTimesVO) {
            this.mealTimes = mealTimes
        }
    }
}
