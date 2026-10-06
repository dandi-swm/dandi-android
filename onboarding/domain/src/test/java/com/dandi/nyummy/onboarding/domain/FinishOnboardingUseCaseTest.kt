package com.dandi.nyummy.onboarding.domain

import com.dandi.nyummy.common.entity.meal.MealTimeVO
import com.dandi.nyummy.common.entity.meal.MealTimesVO
import com.dandi.nyummy.home.domain.HomePage
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class FinishOnboardingUseCaseTest {

    private val repository = FakeOnboardingRepository()
    private val navigationHelper = RecordingNavigationHelper()
    private val useCase = FinishOnboardingUseCase(
        repository = repository,
        resourceHelper = FakeResourceHelper(),
        messageHelper = RecordingMessageHelper(),
        navigationHelper = navigationHelper,
        ttiHelper = FakeTTIHelper(),
    )

    @Test
    fun `고른 식사 시각을 저장하고 온보딩 완료를 기록한 뒤 홈을 루트로 이동한다`() = runBlocking {
        val mealTimes = MealTimesVO(lunch = MealTimeVO(hour = 13, minute = 0))

        useCase(mealTimes)

        assertEquals(listOf(mealTimes), repository.savedMealTimes)
        assertEquals(1, repository.onboardingCompleteCount)
        assertEquals(listOf(HomePage), navigationHelper.rootPages)
    }

    @Test
    fun `저장에 실패해도 온보딩 완료를 기록하고 홈으로 간다`() = runBlocking {
        repository.saveMealTimesError = IOException("disk full")

        useCase(MealTimesVO.default)

        assertTrue(repository.savedMealTimes.isEmpty())
        assertEquals(1, repository.onboardingCompleteCount)
        assertEquals(listOf(HomePage), navigationHelper.rootPages)
    }
}
