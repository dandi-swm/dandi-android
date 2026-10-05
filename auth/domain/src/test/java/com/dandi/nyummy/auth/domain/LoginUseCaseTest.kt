package com.dandi.nyummy.auth.domain

import com.dandi.nyummy.auth.entity.AuthTokenVO
import com.dandi.nyummy.home.domain.HomePage
import com.dandi.nyummy.onboarding.domain.OnboardingPage
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LoginUseCaseTest {

    private val repository = FakeAuthRepository()
    private val navigationHelper = RecordingNavigationHelper()
    private val messageHelper = RecordingMessageHelper()
    private val useCase = LoginUseCase(
        repository = repository,
        resourceHelper = FakeResourceHelper(),
        messageHelper = messageHelper,
        navigationHelper = navigationHelper,
        ttiHelper = FakeTTIHelper(),
    )

    @Test
    fun `redirectUrl 이 온보딩이면 온보딩을 루트로 이동한다`() = runBlocking {
        repository.loginResult = AuthTokenVO("access", "refresh", redirectUrl = "/onboarding")

        val result = useCase.login("test@dandi.app", "pw1234")

        assertTrue(result.isSuccess)
        assertEquals(listOf<Any>(OnboardingPage), navigationHelper.rootPages)
        assertTrue(repository.onboardingIncomplete)
    }

    @Test
    fun `redirectUrl 이 홈이면 홈을 루트로 이동한다`() = runBlocking {
        repository.loginResult = AuthTokenVO("access", "refresh", redirectUrl = "/home")

        useCase.login("test@dandi.app", "pw1234")

        assertEquals(listOf<Any>(HomePage), navigationHelper.rootPages)
        assertFalse(repository.onboardingIncomplete)
    }

    @Test
    fun `로그인에 실패하면 이동하지 않고 도메인 에러를 안내한다`() = runBlocking {
        repository.loginError = httpException(400, AuthErrorType.INVALID_CREDENTIALS.type)

        val result = useCase.login("test@dandi.app", "wrong")

        assertTrue(result.isFailure)
        assertTrue(navigationHelper.rootPages.isEmpty())
        assertEquals(AuthErrorType.INVALID_CREDENTIALS.errorMsg, messageHelper.dialogs.single().descText)
    }
}
