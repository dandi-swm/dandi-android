package com.dandi.nyummy.auth.domain

import com.dandi.nyummy.auth.entity.AuthTokenVO
import com.dandi.nyummy.auth.entity.SocialCredentialVO
import com.dandi.nyummy.auth.entity.SocialLoginType
import com.dandi.nyummy.auth.entity.SocialLoginVO
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/** 로그인·가입에 성공하면 앞 계정이 남긴 위젯 기록을 지우고, 화면 이동은 그 뒤에 한다. */
class AuthWidgetClearTest {

    private val repository = FakeAuthRepository()
    private val session = SocialSignUpSession()
    private val navigationHelper = RecordingNavigationHelper()

    /** 위젯 기록을 지운 순간까지 화면을 몇 번 옮겼는지. */
    private val movesAtClear = mutableListOf<Int>()
    private val widgetHelper = RecordingWidgetHelper(onCall = { movesAtClear += moves() })

    private val loginUseCase = LoginUseCase(
        repository = repository,
        analyticsHelper = RecordingAnalyticsHelper(),
        widgetHelper = widgetHelper,
        resourceHelper = FakeResourceHelper(),
        messageHelper = RecordingMessageHelper(),
        navigationHelper = navigationHelper,
        ttiHelper = FakeTTIHelper(),
    )
    private val socialLoginUseCase = SocialLoginUseCase(
        repository = repository,
        socialSignUpSession = session,
        analyticsHelper = RecordingAnalyticsHelper(),
        widgetHelper = widgetHelper,
        resourceHelper = FakeResourceHelper(),
        messageHelper = RecordingMessageHelper(),
        navigationHelper = navigationHelper,
        ttiHelper = FakeTTIHelper(),
    )
    private val signUpUseCase = SignUpUseCase(
        repository = repository,
        socialSignUpSession = session,
        analyticsHelper = RecordingAnalyticsHelper(),
        widgetHelper = widgetHelper,
        resourceHelper = FakeResourceHelper(),
        messageHelper = RecordingMessageHelper(),
        navigationHelper = navigationHelper,
        ttiHelper = FakeTTIHelper(),
    )

    private val kakao = SocialCredentialVO(SocialLoginType.KAKAO, token = "id-token", nonce = "nonce-1")

    @Test
    fun `이메일 로그인에 성공하면 위젯 기록을 지운 뒤 이동한다`() = runBlocking {
        repository.loginResult = AuthTokenVO("access", "refresh")

        loginUseCase.login("test@dandi.app", "pw1234")

        assertEquals(listOf<WidgetCall>(WidgetCall.Clear), widgetHelper.calls)
        assertEquals(listOf(0), movesAtClear)
        assertEquals(1, moves())
    }

    @Test
    fun `로그인에 실패하면 위젯 기록을 그대로 둔다`() = runBlocking {
        repository.loginError = httpException(400, AuthErrorType.INVALID_CREDENTIALS.type)
        loginUseCase.login("test@dandi.app", "wrong")
        repository.loginError = IOException("offline")
        loginUseCase.login("test@dandi.app", "pw1234")

        assertTrue(widgetHelper.calls.isEmpty())
    }

    @Test
    fun `기존 회원의 소셜 로그인은 위젯 기록을 지운 뒤 이동한다`() = runBlocking {
        repository.socialLoginResult = SocialLoginVO(token = AuthTokenVO("access", "refresh"))

        socialLoginUseCase.login(kakao)

        assertEquals(listOf<WidgetCall>(WidgetCall.Clear), widgetHelper.calls)
        assertEquals(listOf(0), movesAtClear)
    }

    @Test
    fun `소셜 신규 회원은 가입 전이라 위젯 기록을 지우지 않는다`() = runBlocking {
        repository.socialLoginResult = SocialLoginVO(verifiedToken = "social-verified")

        socialLoginUseCase.login(kakao)

        assertTrue(widgetHelper.calls.isEmpty())
    }

    @Test
    fun `이메일 가입과 소셜 가입에 성공하면 위젯 기록을 지운 뒤 온보딩으로 간다`() = runBlocking {
        signUpUseCase.signUp(emailVerifiedToken = "email-verified", password = "pw1234", confirmPassword = "pw1234", nickname = "단디")
        session.start("social-verified")
        signUpUseCase.signUpWithSocial(nickname = "단디")

        assertEquals(listOf<WidgetCall>(WidgetCall.Clear, WidgetCall.Clear), widgetHelper.calls)
        assertEquals(listOf(0, 1), movesAtClear)
        assertEquals(2, moves())
    }

    @Test
    fun `가입에 실패하면 위젯 기록을 그대로 둔다`() = runBlocking {
        repository.signUpError = httpException(409)

        signUpUseCase.signUp(emailVerifiedToken = "email-verified", password = "pw1234", confirmPassword = "pw1234", nickname = "단디")
        session.start("social-verified")
        signUpUseCase.signUpWithSocial(nickname = "단디")

        assertTrue(widgetHelper.calls.isEmpty())
    }

    private fun moves(): Int = navigationHelper.pages.size + navigationHelper.rootPages.size + navigationHelper.appLinks.size
}
