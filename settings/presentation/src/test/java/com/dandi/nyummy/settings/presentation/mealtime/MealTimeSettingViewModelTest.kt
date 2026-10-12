package com.dandi.nyummy.settings.presentation.mealtime

import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.common.domain.helper.StringResource
import com.dandi.nyummy.common.domain.message.IconType
import com.dandi.nyummy.common.domain.message.MessageEffect
import com.dandi.nyummy.common.domain.navigation.NavRoute
import com.dandi.nyummy.common.domain.navigation.NavSignal
import com.dandi.nyummy.common.domain.navigation.Page
import com.dandi.nyummy.common.entity.meal.Meal
import com.dandi.nyummy.common.entity.meal.MealTimeVO
import com.dandi.nyummy.common.entity.meal.MealTimesVO
import com.dandi.nyummy.settings.domain.GetMealTimesUseCase
import com.dandi.nyummy.settings.domain.SaveMealTimesUseCase
import com.dandi.nyummy.settings.domain.SettingsRepository
import com.dandi.nyummy.settings.entity.NotificationSettingsVO
import com.dandi.nyummy.tti.TTIHelper
import com.dandi.nyummy.tti.TTIMetaData
import com.dandi.nyummy.tti.TTIPage
import com.dandi.nyummy.tti.TimelineCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
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

    private class FakeSettingsRepository : SettingsRepository {
        var mealTimes: MealTimesVO = MealTimesVO.default
        var getMealTimesError: Exception? = null
        var getMealTimesCount = 0
        var saveMealTimesError: Exception? = null
        val savedMealTimes = mutableListOf<MealTimesVO>()

        override suspend fun isBgmEnabled(): Boolean = true
        override suspend fun setBgmEnabled(enabled: Boolean) = Unit
        override suspend fun getNotificationSettings(): NotificationSettingsVO = NotificationSettingsVO.default
        override suspend fun updateNotificationSettings(settings: NotificationSettingsVO): NotificationSettingsVO = settings

        override suspend fun getMealTimes(): MealTimesVO {
            getMealTimesCount++
            getMealTimesError?.let { throw it }
            return mealTimes
        }

        override suspend fun saveMealTimes(mealTimes: MealTimesVO) {
            saveMealTimesError?.let { throw it }
            savedMealTimes += mealTimes
        }
    }

    private class RecordingMessageHelper : MessageHelper {
        data class SnackBarCall(val messageText: String, val onClickCTA: (() -> Unit)?)

        val snackBars = mutableListOf<SnackBarCall>()

        override val effect: Flow<MessageEffect> = emptyFlow()
        override fun showToast(toastMsg: String) = Unit
        override fun showSnackBar(
            iconType: IconType,
            messageText: String,
            callToActionText: String?,
            onClickCTA: (() -> Unit)?,
        ) {
            snackBars += SnackBarCall(messageText, onClickCTA)
        }

        override fun showSnackBar(
            iconType: IconType,
            messageRes: Int,
            callToActionText: String?,
            onClickCTA: (() -> Unit)?,
        ) = Unit

        override fun showOneButtonDialog(
            titleText: String?,
            descText: String,
            cantIgnore: Boolean,
            buttonText: String,
            onClickButton: (() -> Unit)?,
        ) = Unit

        override fun showTwoButtonDialog(
            titleText: String?,
            descText: String,
            cantIgnore: Boolean,
            leftButtonText: String,
            onClickLeftButton: (() -> Unit)?,
            rightButtonText: String,
            onClickRightButton: (() -> Unit)?,
        ) = Unit
    }

    private class RecordingNavigationHelper : NavigationHelper {
        var backCount = 0

        override val navigationFlow: Flow<NavSignal> = emptyFlow()
        override fun navigateByRoute(route: NavRoute) = Unit
        override fun navigateTo(page: Page) = Unit
        override fun navigateDeepLink(route: NavRoute) = Unit
        override fun navigateToBack() {
            backCount++
        }

        override fun navigateToAsRoot(page: Page) = Unit
        override fun navigateToInitial() = Unit
        override fun navigateToExternalLink(url: String) = Unit
    }

    private object FakeResourceHelper : ResourceHelper {
        override fun getString(resource: StringResource): String = ""
    }

    private object FakeTTIHelper : TTIHelper {
        override fun startTTITracking(page: TTIPage) = Unit
        override fun startTTITimeline(category: TimelineCategory) = Unit
        override fun endTTITimeline(category: TimelineCategory) = Unit
        override fun endTTITracking() = Unit
        override fun shotTTILogging() = Unit
        override fun addTTIMetaData(metadata: TTIMetaData, value: Any?) = Unit
    }
}
