package com.dandi.nyummy.onboarding.domain

import com.dandi.nyummy.common.entity.meal.Meal
import com.dandi.nyummy.common.entity.meal.MealTimeVO
import com.dandi.nyummy.common.entity.meal.MealTimesVO
import com.dandi.nyummy.home.domain.HomePreparePage
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class FinishOnboardingUseCaseTest {

    private val repository = FakeOnboardingRepository()
    private val navigationHelper = RecordingNavigationHelper()
    private val messageHelper = RecordingMessageHelper()
    private val useCase = FinishOnboardingUseCase(
        repository = repository,
        analyticsHelper = RecordingAnalyticsHelper(),
        resourceHelper = FakeResourceHelper(),
        messageHelper = messageHelper,
        navigationHelper = navigationHelper,
        ttiHelper = FakeTTIHelper(),
    )

    @Test
    fun `앞뒤 공백을 뺀 이름과 고른 식사 시각으로 고양이를 만들고 완료를 기록한 뒤 냐미 방 준비 화면을 루트로 이동한다`() = runBlocking {
        val mealTimes = MealTimesVO(lunch = MealTimeVO(hour = 13))

        val result = useCase("  냐미 ", mealTimes)

        assertTrue(result.isSuccess)
        assertEquals(listOf("냐미" to mealTimes), repository.createdCats)
        assertEquals(1, repository.onboardingCompleteCount)
        assertEquals(listOf(HomePreparePage), navigationHelper.rootPages)
        assertTrue(messageHelper.dialogs.isEmpty())
    }

    @Test
    fun `이미 고양이가 있으면 만들어진 것으로 보고 온보딩을 마친다`() = runBlocking {
        repository.error = httpException(409, "api.cat.alreadyExists")

        val result = useCase("냐미", MealTimesVO.default)

        assertTrue(result.isSuccess)
        assertEquals(1, repository.onboardingCompleteCount)
        assertEquals(listOf(HomePreparePage), navigationHelper.rootPages)
        assertTrue(messageHelper.dialogs.isEmpty())
    }

    @Test
    fun `먹는 끼니끼리 시각이 겹치면 서버를 부르지 않고 안내한 뒤 머문다`() = runBlocking {
        val overlapped = MealTimesVO.default.with(Meal.DINNER, MealTimeVO(hour = 12))

        val result = useCase("냐미", overlapped)

        assertTrue(result.isFailure)
        assertTrue(repository.createdCats.isEmpty())
        assertEquals(1, messageHelper.snackBars.size)
        assertEquals(0, repository.onboardingCompleteCount)
        assertTrue(navigationHelper.rootPages.isEmpty())
    }

    @Test
    fun `서버가 입력값을 거절하면 확인 안내를 하고 머문다`() = runBlocking {
        repository.error = httpException(400, OnboardingErrorType.INVALID_INPUT.type)

        val result = useCase("냐미", MealTimesVO.default)

        assertTrue(result.isFailure)
        assertEquals(listOf(OnboardingErrorType.INVALID_INPUT.errorMsg), messageHelper.dialogs)
        assertEquals(0, repository.onboardingCompleteCount)
        assertTrue(navigationHelper.rootPages.isEmpty())
    }

    @Test
    fun `401 은 공통 에러 처리(세션 만료 안내)로 넘기고 홈으로 보내지 않는다`() = runBlocking {
        repository.error = httpException(401)

        val result = useCase("냐미", MealTimesVO.default)

        assertTrue(result.isFailure)
        assertEquals(1, messageHelper.dialogs.size)
        assertEquals(0, repository.onboardingCompleteCount)
        assertTrue(navigationHelper.rootPages.isEmpty())
    }

    @Test
    fun `네트워크 오류는 일시적 오류로 안내하고 머문다`() = runBlocking {
        repository.error = IOException("offline")

        val result = useCase("냐미", MealTimesVO.default)

        assertTrue(result.isFailure)
        assertEquals(1, messageHelper.dialogs.size)
        assertEquals(0, repository.onboardingCompleteCount)
        assertTrue(navigationHelper.rootPages.isEmpty())
    }
}
