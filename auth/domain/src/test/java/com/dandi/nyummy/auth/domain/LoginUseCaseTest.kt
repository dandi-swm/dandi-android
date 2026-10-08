package com.dandi.nyummy.auth.domain

import com.dandi.nyummy.auth.entity.AuthTokenVO
import com.dandi.nyummy.home.domain.HomePage
import com.dandi.nyummy.onboarding.domain.OnboardingPage
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

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

    @Test
    fun `모르는 code 의 400 이면 로그인 실패 문구로 안내한다`() = runBlocking {
        repository.loginError = httpException(400, "api.common.invalidFormat")

        useCase.login("test@dandi.app", "pw1234")

        assertEquals("로그인하지 못했어요. 잠시 후 다시 시도해 주세요.", messageHelper.dialogs.single().descText)
    }

    @Test
    fun `네트워크 오류면 연결 확인을 안내하고 실패를 돌려준다`() = runBlocking {
        repository.loginError = IOException("offline")

        val result = useCase.login("test@dandi.app", "pw1234")

        assertTrue(result.isFailure)
        assertEquals("네트워크 연결을 확인한 뒤 다시 시도해주세요.", messageHelper.dialogs.single().descText)
    }
}
