package com.dandi.nyummy.settings.presentation.main

import com.dandi.nyummy.auth.domain.LogoutUseCase
import com.dandi.nyummy.common.entity.meal.MealTimeVO
import com.dandi.nyummy.common.entity.meal.MealTimesVO
import com.dandi.nyummy.settings.domain.GetSettingsUseCase
import com.dandi.nyummy.settings.domain.MealTimeSettingPage
import com.dandi.nyummy.settings.domain.SetBgmEnabledUseCase
import com.dandi.nyummy.settings.domain.UpdateNotificationSettingsUseCase
import com.dandi.nyummy.settings.presentation.FakeAuthRepository
import com.dandi.nyummy.settings.presentation.FakePushTokenHelper
import com.dandi.nyummy.settings.presentation.FakeResourceHelper
import com.dandi.nyummy.settings.presentation.FakeSettingsRepository
import com.dandi.nyummy.settings.presentation.FakeTTIHelper
import com.dandi.nyummy.settings.presentation.RecordingMessageHelper
import com.dandi.nyummy.settings.presentation.RecordingNavigationHelper
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val repository = FakeSettingsRepository()
    private val messageHelper = RecordingMessageHelper()
    private val navigationHelper = RecordingNavigationHelper()

    private val viewModel by lazy {
        SettingsViewModel(
            getSettings = GetSettingsUseCase(repository, FakeResourceHelper, messageHelper, navigationHelper, FakeTTIHelper),
            setBgmEnabled = SetBgmEnabledUseCase(repository),
            updateNotificationSettings = UpdateNotificationSettingsUseCase(
                repository,
                FakeResourceHelper,
                messageHelper,
                navigationHelper,
                FakeTTIHelper,
            ),
            logout = LogoutUseCase(FakeAuthRepository, FakePushTokenHelper, navigationHelper),
            navigationHelper = navigationHelper,
            messageHelper = messageHelper,
        )
    }

    private val state get() = viewModel.uiState.value

    private fun resume() = viewModel.onIntent(SettingsIntent.ScreenResumed(isDeviceNotificationEnabled = true))

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `설정을 읽기 전에는 토글을 바꾸거나 식사 시간 화면으로 가지 않는다`() = runTest(testDispatcher) {
        val gate = CompletableDeferred<MealTimesVO>()
        repository.mealTimesResponses += { gate.await() }
        resume()
        advanceUntilIdle()
        assertFalse(state.isLoaded)

        viewModel.onIntent(SettingsIntent.ToggleBgm(false))
        viewModel.onIntent(SettingsIntent.ToggleMarketingPush(true))
        viewModel.onIntent(SettingsIntent.ClickMealTime)
        advanceUntilIdle()

        assertTrue(repository.savedBgm.isEmpty())
        assertTrue(repository.savedNotifications.isEmpty())
        assertTrue(navigationHelper.pages.isEmpty())
        assertTrue(state.isBgmEnabled)

        gate.complete(MealTimesVO.default)
        advanceUntilIdle()
        assertTrue(state.isLoaded)

        viewModel.onIntent(SettingsIntent.ToggleBgm(false))
        viewModel.onIntent(SettingsIntent.ClickMealTime)
        advanceUntilIdle()

        assertEquals(listOf(false), repository.savedBgm)
        assertEquals(listOf(MealTimeSettingPage), navigationHelper.pages)
    }

    @Test
    fun `다시 읽으면 앞선 읽기는 버리고 마지막 읽기 결과만 쓴다`() = runTest(testDispatcher) {
        // 식사 시간을 저장하기 전에 시작한 읽기가, 저장하고 돌아와 시작한 읽기보다 늦게 끝나는 경우다.
        val stale = CompletableDeferred<MealTimesVO>()
        val saved = MealTimesVO(lunch = MealTimeVO(hour = 13))
        repository.mealTimesResponses += { stale.await() }
        repository.mealTimesResponses += { saved }

        resume()
        advanceUntilIdle()
        resume()
        advanceUntilIdle()
        assertEquals(saved, state.mealTimes)

        stale.complete(MealTimesVO.default)
        advanceUntilIdle()

        assertEquals(saved, state.mealTimes)
    }

    @Test
    fun `식사 시각만 읽지 못하면 요약은 비우고 다른 설정은 쓸 수 있다`() = runTest(testDispatcher) {
        repository.getMealTimesError = IOException("offline")

        resume()
        advanceUntilIdle()

        assertTrue(state.isLoaded)
        assertNull(state.mealTimes)
        viewModel.onIntent(SettingsIntent.ToggleBgm(false))
        advanceUntilIdle()
        assertEquals(listOf(false), repository.savedBgm)
    }

    @Test
    fun `다시 읽다가 식사 시각을 읽지 못하면 마지막으로 읽은 값을 둔다`() = runTest(testDispatcher) {
        val mealTimes = MealTimesVO(dinner = MealTimeVO(hour = 19))
        repository.mealTimes = mealTimes
        resume()
        advanceUntilIdle()

        repository.getMealTimesError = IOException("offline")
        resume()
        advanceUntilIdle()

        assertEquals(mealTimes, state.mealTimes)
    }
}
