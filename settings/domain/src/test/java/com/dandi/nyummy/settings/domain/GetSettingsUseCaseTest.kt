package com.dandi.nyummy.settings.domain

import com.dandi.nyummy.common.entity.meal.MealTimeVO
import com.dandi.nyummy.common.entity.meal.MealTimesVO
import com.dandi.nyummy.settings.entity.NotificationSettingsVO
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class GetSettingsUseCaseTest {

    private val repository = FakeSettingsRepository()
    private val messageHelper = RecordingMessageHelper()
    private val navigationHelper = RecordingNavigationHelper()

    private val useCase = GetSettingsUseCase(
        repository = repository,
        resourceHelper = FakeResourceHelper,
        messageHelper = messageHelper,
        navigationHelper = navigationHelper,
        ttiHelper = FakeTTIHelper,
    )

    @Test
    fun `알림 설정과 서버의 식사 시각을 함께 돌려준다`() = runBlocking {
        val mealTimes = MealTimesVO(lunch = MealTimeVO(hour = 13))
        repository.mealTimes = mealTimes

        val settings = useCase(onRetry = {}).getOrThrow()

        assertEquals(NotificationSettingsVO(isMarketingEnabled = true), settings.notification)
        assertEquals(mealTimes, settings.mealTimes)
    }

    @Test
    fun `식사 시각을 읽지 못해도 다른 설정은 돌려주고 식사 시각만 비운다`() = runBlocking {
        repository.getMealTimesError = IOException("offline")

        val settings = useCase(onRetry = {}).getOrThrow()

        assertEquals(NotificationSettingsVO(isMarketingEnabled = true), settings.notification)
        assertNull(settings.mealTimes)
        assertTrue(messageHelper.dialogs.isEmpty())
        assertTrue(messageHelper.snackBars.isEmpty())
    }

    @Test
    fun `식사 시각 서버 오류는 화면에 돌아올 때마다 다시 읽으므로 안내하지 않는다`() = runBlocking {
        repository.getMealTimesError = httpException(500, "api.user.profileNotFound")

        val settings = useCase(onRetry = {}).getOrThrow()

        assertNull(settings.mealTimes)
        assertTrue(messageHelper.dialogs.isEmpty())
        assertTrue(messageHelper.snackBars.isEmpty())
    }

    @Test
    fun `로그인이 만료됐으면 세션 만료를 안내하고 식사 시각만 비운다`() = runBlocking {
        repository.getMealTimesError = httpException(401, "api.auth.tokenExpired")

        val settings = useCase(onRetry = {}).getOrThrow()

        assertNull(settings.mealTimes)
        messageHelper.dialogs.single().onClickButton?.invoke()
        assertEquals(1, navigationHelper.initialCount)
    }
}
