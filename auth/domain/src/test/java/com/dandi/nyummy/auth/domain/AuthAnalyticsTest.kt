package com.dandi.nyummy.auth.domain

import com.dandi.nyummy.auth.entity.AuthTokenVO
import com.dandi.nyummy.auth.entity.SocialCredentialVO
import com.dandi.nyummy.auth.entity.SocialLoginType
import com.dandi.nyummy.auth.entity.SocialLoginVO
import com.dandi.nyummy.common.domain.analytics.AnalyticsEvent
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthAnalyticsTest {

    private val repository = FakeAuthRepository()
    private val session = SocialSignUpSession()
    private val analytics = RecordingAnalyticsHelper()

    private val loginUseCase = LoginUseCase(
        repository = repository,
        analyticsHelper = analytics,
        resourceHelper = FakeResourceHelper(),
        messageHelper = RecordingMessageHelper(),
        navigationHelper = RecordingNavigationHelper(),
        ttiHelper = FakeTTIHelper(),
    )
    private val socialLoginUseCase = SocialLoginUseCase(
        repository = repository,
        socialSignUpSession = session,
        analyticsHelper = analytics,
        resourceHelper = FakeResourceHelper(),
        messageHelper = RecordingMessageHelper(),
        navigationHelper = RecordingNavigationHelper(),
        ttiHelper = FakeTTIHelper(),
    )
    private val signUpUseCase = SignUpUseCase(
        repository = repository,
        socialSignUpSession = session,
        analyticsHelper = analytics,
        resourceHelper = FakeResourceHelper(),
        messageHelper = RecordingMessageHelper(),
        navigationHelper = RecordingNavigationHelper(),
        ttiHelper = FakeTTIHelper(),
    )

    private val kakao = SocialCredentialVO(SocialLoginType.KAKAO, token = "id-token", nonce = "nonce-1")

    @Test
    fun `이메일 로그인에 성공하면 login을 email로 남긴다`() = runBlocking {
        repository.loginResult = AuthTokenVO("access", "refresh")

        loginUseCase.login("test@dandi.app", "pw1234")

        assertEquals(listOf("login" to mapOf("method" to "email")), recorded())
    }

    @Test
    fun `로그인에 실패하면 아무것도 남기지 않는다`() = runBlocking {
        repository.loginError = httpException(400, AuthErrorType.INVALID_CREDENTIALS.type)

        loginUseCase.login("test@dandi.app", "wrong")

        assertTrue(analytics.events.isEmpty())
    }

    @Test
    fun `기존 회원의 소셜 로그인은 login을 제공자 이름으로 남긴다`() = runBlocking {
        repository.socialLoginResult = SocialLoginVO(token = AuthTokenVO("access", "refresh"))

        socialLoginUseCase.login(kakao)

        assertEquals(listOf("login" to mapOf("method" to "kakao")), recorded())
    }

    @Test
    fun `소셜 신규 회원은 가입 화면으로 갈 뿐 login을 남기지 않는다`() = runBlocking {
        repository.socialLoginResult = SocialLoginVO(verifiedToken = "social-verified")

        socialLoginUseCase.login(kakao)

        assertTrue(analytics.events.isEmpty())
    }

    @Test
    fun `가입에 성공하면 sign_up을 가입 수단으로 남긴다`() = runBlocking {
        signUpUseCase.signUp(emailVerifiedToken = "email-verified", password = "pw1234", confirmPassword = "pw1234", nickname = "단디")
        session.start("social-verified")
        signUpUseCase.signUpWithSocial(nickname = "단디")

        assertEquals(
            listOf("sign_up" to mapOf("method" to "email"), "sign_up" to mapOf("method" to "social")),
            recorded(),
        )
    }

    @Test
    fun `가입에 실패하면 sign_up을 남기지 않는다`() = runBlocking {
        repository.signUpError = httpException(409)

        signUpUseCase.signUp(emailVerifiedToken = "email-verified", password = "pw1234", confirmPassword = "pw1234", nickname = "단디")

        assertTrue(analytics.events.isEmpty())
    }

    private fun recorded(): List<Pair<String, Map<String, String>>> =
        analytics.events.map { event: AnalyticsEvent -> event.name to event.params }
}
