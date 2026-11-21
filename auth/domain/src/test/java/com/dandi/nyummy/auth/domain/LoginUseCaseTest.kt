package com.dandi.nyummy.auth.domain

import com.dandi.nyummy.common.domain.helper.StringResource
import com.dandi.nyummy.auth.entity.AuthTokenVO
import com.dandi.nyummy.home.domain.HomePreparePage
import com.dandi.nyummy.common.domain.session.SessionExpiryHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.lang.reflect.Proxy

class LoginUseCaseTest {

    private val repository = FakeAuthRepository()
    private val navigationHelper = RecordingNavigationHelper()
    private val messageHelper = RecordingMessageHelper()
    private val useCase = LoginUseCase(
        repository = repository,
        analyticsHelper = RecordingAnalyticsHelper(),
        widgetHelper = NoOpWidgetHelper,
        resourceHelper = FakeResourceHelper(),
        messageHelper = messageHelper,
        navigationHelper = navigationHelper,
        ttiHelper = FakeTTIHelper(),
    )

    @Test
    fun `redirectUrl 이 온보딩이면 그 앱 링크를 스택을 비우고 연다`() = runBlocking {
        repository.loginResult = AuthTokenVO("access", "refresh", redirectUrl = "https://link.nyummy.co.kr/onboarding")

        val result = useCase.login("test@dandi.app", "pw1234")

        assertTrue(result.isSuccess)
        assertEquals(listOf("https://link.nyummy.co.kr/onboarding" to true), navigationHelper.appLinks)
        assertTrue(navigationHelper.rootPages.isEmpty())
        assertTrue(repository.onboardingIncomplete)
    }

    @Test
    fun `redirectUrl 이 홈이면 냐미 방 준비 화면을 거쳐 홈으로 가도록 그 화면을 루트로 이동한다`() = runBlocking {
        repository.loginResult = AuthTokenVO("access", "refresh", redirectUrl = "/home")

        useCase.login("test@dandi.app", "pw1234")

        assertEquals(listOf<Any>(HomePreparePage), navigationHelper.rootPages)
        assertFalse(repository.onboardingIncomplete)
    }

    @Test
    fun `로그인에 실패하면 이동하지 않고 도메인 에러를 안내한다`() = runBlocking {
        repository.loginError = httpException(400, AuthErrorType.INVALID_CREDENTIALS.type)

        val result = useCase.login("test@dandi.app", "wrong")

        assertTrue(result.isFailure)
        assertTrue(navigationHelper.rootPages.isEmpty())
        assertEquals(AuthErrorType.INVALID_CREDENTIALS.errorMsg.name, messageHelper.dialogs.single().descText)
    }

    @Test
    fun `모르는 code 의 400 이면 로그인 실패 문구로 안내한다`() = runBlocking {
        repository.loginError = httpException(400, "api.common.invalidFormat")

        useCase.login("test@dandi.app", "pw1234")

        assertEquals(StringResource.AUTH_LOGIN_FAILED.name, messageHelper.dialogs.single().descText)
    }

    @Test
    fun `네트워크 오류면 연결 확인을 안내하고 실패를 돌려준다`() = runBlocking {
        repository.loginError = IOException("offline")

        val result = useCase.login("test@dandi.app", "pw1234")

        assertTrue(result.isFailure)
        assertEquals(StringResource.NETWORK_ERROR.name, messageHelper.dialogs.single().descText)
    }

    @Test
    fun `로그인 전 화면이라 code 없는 401 은 로그인 만료가 아니라 로그인 실패로 안내하고 로그인 화면으로 보내지 않는다`() = runBlocking {
        repository.loginError = httpException(401)

        val result = useCase.login("test@dandi.app", "pw1234")

        assertTrue(result.isFailure)
        assertEquals(StringResource.AUTH_LOGIN_FAILED.name, messageHelper.dialogs.single().descText)
        assertEquals(0, navigationHelper.initialCount)
    }

    @Test
    fun `응답을 해석하지 못한 예상 밖 예외도 로그인 실패로 안내한다`() = runBlocking {
        repository.loginError = IllegalStateException("broken body")

        val result = useCase.login("test@dandi.app", "pw1234")

        assertTrue(result.isFailure)
        assertEquals(StringResource.AUTH_LOGIN_FAILED.name, messageHelper.dialogs.single().descText)
    }

    @Test
    fun `세션 만료 뒤 다시 로그인하면 다음 세션 만료를 다시 처리할 수 있게 된다`() = runBlocking {
        val sessionExpiry = SessionExpiryHandler(
            resourceHelper = FakeResourceHelper(),
            messageHelper = RecordingMessageHelper(),
            navigationHelper = RecordingNavigationHelper(),
            sessionTokenHelper = noOp(),
            pushTokenHelper = noOp(),
            companionMemory = noOp(),
            tutorialHelper = noOp(),
            widgetHelper = noOp(),
            scope = CoroutineScope(Dispatchers.Unconfined),
        )
        sessionExpiry.onSessionExpired()
        assertTrue(sessionExpiry.isExpired)
        repository.loginResult = AuthTokenVO("access", "refresh", redirectUrl = "/home")

        LoginUseCase(
            repository = repository,
            analyticsHelper = RecordingAnalyticsHelper(),
            widgetHelper = NoOpWidgetHelper,
            resourceHelper = FakeResourceHelper(),
            messageHelper = messageHelper,
            navigationHelper = navigationHelper,
            ttiHelper = FakeTTIHelper(),
            sessionExpiryHandler = sessionExpiry,
        ).login("test@dandi.app", "pw1234")

        assertFalse(sessionExpiry.isExpired)
    }

    /** 부르면 아무것도 하지 않는 대역. suspend 함수도 바로 끝난다. */
    private inline fun <reified T : Any> noOp(): T =
        Proxy.newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java)) { _, _, _ -> Unit } as T
}
