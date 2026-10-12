package com.dandi.nyummy.settings.presentation.mealtime

import com.dandi.nyummy.common.entity.meal.Meal
import com.dandi.nyummy.common.entity.meal.MealTimeVO
import com.dandi.nyummy.common.entity.meal.MealTimesVO
import com.dandi.nyummy.settings.domain.GetMealTimesUseCase
import com.dandi.nyummy.settings.domain.SaveMealTimesUseCase
import com.dandi.nyummy.settings.presentation.FakeResourceHelper
import com.dandi.nyummy.settings.presentation.FakeSettingsRepository
import com.dandi.nyummy.settings.presentation.FakeTTIHelper
import com.dandi.nyummy.settings.presentation.RecordingMessageHelper
import com.dandi.nyummy.settings.presentation.RecordingNavigationHelper
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class MealTimeSettingViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val repository = FakeSettingsRepository()
    private val messageHelper = RecordingMessageHelper()
    private val navigationHelper = RecordingNavigationHelper()

    private val viewModel by lazy {
        MealTimeSettingViewModel(
            getMealTimes = GetMealTimesUseCase(repository, FakeResourceHelper, messageHelper, navigationHelper, FakeTTIHelper),
            saveMealTimes = SaveMealTimesUseCase(repository, FakeResourceHelper, messageHelper, navigationHelper, FakeTTIHelper),
            navigationHelper = navigationHelper,
        )
    }

    private val state get() = viewModel.uiState.value

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `들어오면 서버의 식사 시각을 읽어 보여준다`() = runTest(testDispatcher) {
        val mealTimes = MealTimesVO(breakfast = MealTimesVO.DefaultBreakfast.copy(isSkipped = true))
        repository.mealTimes = mealTimes

        viewModel.onIntent(MealTimeSettingIntent.Enter)
        advanceUntilIdle()

        assertTrue(state.isLoaded)
        assertEquals(mealTimes, state.mealTimes)
    }

    @Test
    fun `읽지 못하면 줄과 저장을 막고 다시 시도하면 다시 읽는다`() = runTest(testDispatcher) {
        repository.getMealTimesError = IOException("offline")
        viewModel.onIntent(MealTimeSettingIntent.Enter)
        advanceUntilIdle()
        assertFalse(state.isLoaded)

        viewModel.onIntent(MealTimeSettingIntent.ClickSave)
        advanceUntilIdle()
        assertTrue(repository.savedMealTimes.isEmpty())

        repository.getMealTimesError = null
        messageHelper.snackBars.single().onClickCTA?.invoke()
        advanceUntilIdle()

        assertTrue(state.isLoaded)
        assertEquals(2, repository.getMealTimesCount)
    }

    @Test
    fun `고른 시각을 서버에 저장하고 뒤로 간다`() = runTest(testDispatcher) {
        viewModel.onIntent(MealTimeSettingIntent.Enter)
        advanceUntilIdle()

        viewModel.onIntent(MealTimeSettingIntent.SelectMealTime(Meal.LUNCH, MealTimeVO(hour = 13)))
        viewModel.onIntent(MealTimeSettingIntent.ClickSave)
        advanceUntilIdle()

        assertEquals(listOf(MealTimesVO(lunch = MealTimeVO(hour = 13))), repository.savedMealTimes)
        assertEquals(1, navigationHelper.backCount)
    }

    @Test
    fun `저장이 실패하면 저장 버튼을 다시 열고 다시 시도하면 다시 저장한다`() = runTest(testDispatcher) {
        viewModel.onIntent(MealTimeSettingIntent.Enter)
        advanceUntilIdle()
        repository.saveMealTimesError = IOException("offline")

        viewModel.onIntent(MealTimeSettingIntent.ClickSave)
        advanceUntilIdle()

        assertFalse(state.isSaving)
        assertEquals(0, navigationHelper.backCount)

        repository.saveMealTimesError = null
        messageHelper.snackBars.single().onClickCTA?.invoke()
        advanceUntilIdle()

        assertEquals(listOf(MealTimesVO.default), repository.savedMealTimes)
        assertEquals(1, navigationHelper.backCount)
    }
}
