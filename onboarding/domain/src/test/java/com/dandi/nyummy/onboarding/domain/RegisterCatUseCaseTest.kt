package com.dandi.nyummy.onboarding.domain

import com.dandi.nyummy.home.domain.HomePage
import com.dandi.nyummy.onboarding.entity.CatVO
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class RegisterCatUseCaseTest {

    private val repository = FakeOnboardingRepository()
    private val navigationHelper = RecordingNavigationHelper()
    private val messageHelper = RecordingMessageHelper()
    private val useCase = RegisterCatUseCase(
        repository = repository,
        resourceHelper = FakeResourceHelper(),
        messageHelper = messageHelper,
        navigationHelper = navigationHelper,
        ttiHelper = FakeTTIHelper(),
    )

    @Test
    fun `앞뒤 공백을 뺀 이름으로 등록하고 화면 이동은 하지 않는다`() = runBlocking {
        val result = useCase("  냐미 ")

        assertEquals(CatVO(id = 1L, name = "냐미"), result.getOrNull())
        assertEquals(listOf("냐미"), repository.registeredNames)
        assertTrue(navigationHelper.rootPages.isEmpty())
        assertTrue(messageHelper.dialogs.isEmpty())
    }

    @Test
    fun `이미 고양이가 있으면 온보딩을 끝낸 것으로 보고 홈으로 보낸다`() = runBlocking {
        repository.error = httpException(409, OnboardingErrorType.CAT_ALREADY_EXISTS.type)

        val result = useCase("냐미")

        assertTrue(result.isFailure)
        assertEquals(listOf<Any>(HomePage), navigationHelper.rootPages)
        assertTrue(messageHelper.dialogs.isEmpty())
    }

    @Test
    fun `이름 형식 오류는 안내만 하고 머문다`() = runBlocking {
        repository.error = httpException(400, OnboardingErrorType.CAT_NAME_INVALID.type)

        val result = useCase("냐미")

        assertTrue(result.isFailure)
        assertTrue(navigationHelper.rootPages.isEmpty())
        assertEquals(listOf(OnboardingErrorType.CAT_NAME_INVALID.errorMsg), messageHelper.dialogs)
    }

    @Test
    fun `401 은 공통 에러 처리(세션 만료 안내)로 넘기고 홈으로 보내지 않는다`() = runBlocking {
        repository.error = httpException(401)

        useCase("냐미")

        assertEquals(1, messageHelper.dialogs.size)
        assertTrue(navigationHelper.rootPages.isEmpty())
    }

    @Test
    fun `네트워크 오류는 일시적 오류로 안내한다`() = runBlocking {
        repository.error = IOException("offline")

        val result = useCase("냐미")

        assertTrue(result.isFailure)
        assertEquals(1, messageHelper.dialogs.size)
    }
}
