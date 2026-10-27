package com.dandi.nyummy.onboarding.domain

import com.dandi.nyummy.common.domain.analytics.AnalyticsEvent
import com.dandi.nyummy.common.entity.meal.MealTimesVO
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingAnalyticsTest {

    private val repository = FakeOnboardingRepository()
    private val analytics = RecordingAnalyticsHelper()
    private val useCase = FinishOnboardingUseCase(
        repository = repository,
        analyticsHelper = analytics,
        tutorialHelper = RecordingTutorialHelper(),
        resourceHelper = FakeResourceHelper(),
        messageHelper = RecordingMessageHelper(),
        navigationHelper = RecordingNavigationHelper(),
        ttiHelper = FakeTTIHelper(),
    )

    @Test
    fun `고양이를 만들고 온보딩을 마치면 onboarding_complete를 남긴다`() = runBlocking {
        useCase("냐미", MealTimesVO.default)

        assertEquals(listOf<AnalyticsEvent>(AnalyticsEvent.OnboardingComplete), analytics.events)
    }

    @Test
    fun `고양이를 만들지 못하면 아무것도 남기지 않는다`() = runBlocking {
        repository.error = httpException(400, OnboardingErrorType.INVALID_INPUT.type)

        useCase("냐미", MealTimesVO.default)

        assertTrue(analytics.events.isEmpty())
    }
}
