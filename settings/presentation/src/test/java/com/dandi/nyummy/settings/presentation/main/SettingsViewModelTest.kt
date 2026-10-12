package com.dandi.nyummy.settings.presentation.main

import com.dandi.nyummy.auth.domain.LogoutUseCase
import com.dandi.nyummy.common.entity.time.KstTime
import com.dandi.nyummy.common.entity.meal.MealTimeVO
import com.dandi.nyummy.common.entity.meal.MealTimesVO
import com.dandi.nyummy.settings.domain.GetSettingsUseCase
import com.dandi.nyummy.settings.domain.MealTimeSettingPage
import com.dandi.nyummy.settings.domain.UpdateNotificationSettingsUseCase
import com.dandi.nyummy.settings.entity.NotificationSettingsChange
import com.dandi.nyummy.settings.entity.NotificationSettingsVO
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

        viewModel.onIntent(SettingsIntent.ToggleServicePush(false))
        viewModel.onIntent(SettingsIntent.ToggleMarketingPush(true))
        viewModel.onIntent(SettingsIntent.ClickMealTime)
        advanceUntilIdle()

        assertTrue(repository.savedNotifications.isEmpty())
        assertTrue(navigationHelper.pages.isEmpty())
        assertEquals(NotificationSettingsVO.default, state.notification)

        gate.complete(MealTimesVO.default)
        advanceUntilIdle()
        assertTrue(state.isLoaded)

        viewModel.onIntent(SettingsIntent.ToggleServicePush(false))
        viewModel.onIntent(SettingsIntent.ClickMealTime)
        advanceUntilIdle()

        assertEquals(listOf(NotificationSettingsChange(isServiceEnabled = false)), repository.savedNotifications)
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
        viewModel.onIntent(SettingsIntent.ToggleServicePush(false))
        advanceUntilIdle()
        assertEquals(listOf(NotificationSettingsChange(isServiceEnabled = false)), repository.savedNotifications)
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

    @Test
    fun `서비스 알림을 끄면 서비스 항목만 저장한다`() = runTest(testDispatcher) {
        resume()
        advanceUntilIdle()

        viewModel.onIntent(SettingsIntent.ToggleServicePush(false))
        advanceUntilIdle()

        assertEquals(listOf(NotificationSettingsChange(isServiceEnabled = false)), repository.savedNotifications)
        assertEquals(NotificationSettingsVO(isServiceEnabled = false, isMarketingEnabled = false), state.notification)
        assertTrue(messageHelper.snackBars.isEmpty())
    }

    @Test
    fun `마케팅 알림에 동의하거나 철회하면 처리한 날짜를 알린다`() = runTest(testDispatcher) {
        resume()
        advanceUntilIdle()
        val today = KstTime.now()
        val date = "${today.year}년 ${today.month}월 ${today.day}일"

        viewModel.onIntent(SettingsIntent.ToggleMarketingPush(true))
        advanceUntilIdle()
        viewModel.onIntent(SettingsIntent.ToggleMarketingPush(false))
        advanceUntilIdle()

        assertEquals(
            listOf("$date 마케팅 알림 수신에 동의했어요", "$date 마케팅 알림 수신 동의를 철회했어요"),
            messageHelper.snackBars.map { it.messageText },
        )
    }

    @Test
    fun `저장에 실패하면 그 항목만 되돌리고 아직 저장 중인 다른 항목은 둔다`() = runTest(testDispatcher) {
        val serviceSave = CompletableDeferred<Unit>()
        val marketingSave = CompletableDeferred<Unit>()
        repository.notificationUpdateResponses += { serviceSave.await() }
        repository.notificationUpdateResponses += { marketingSave.await() }
        resume()
        advanceUntilIdle()

        viewModel.onIntent(SettingsIntent.ToggleServicePush(false))
        advanceUntilIdle()
        viewModel.onIntent(SettingsIntent.ToggleMarketingPush(true))
        advanceUntilIdle()
        // 마케팅 저장이 끝나기 전에 서비스 저장이 실패한다. 서비스만 되돌리고 마케팅 변경은 남아야 한다.
        serviceSave.completeExceptionally(IOException("offline"))
        advanceUntilIdle()

        assertEquals(NotificationSettingsVO(isServiceEnabled = true, isMarketingEnabled = true), state.notification)
        marketingSave.complete(Unit)
        advanceUntilIdle()
        assertEquals(NotificationSettingsVO(isServiceEnabled = true, isMarketingEnabled = true), state.notification)
        val retry = messageHelper.snackBars.single { it.messageText == "설정을 저장하지 못했어요" }
        retry.onClickCTA?.invoke()
        advanceUntilIdle()
        assertEquals(NotificationSettingsVO(isServiceEnabled = false, isMarketingEnabled = true), state.notification)
        assertEquals(NotificationSettingsChange(isServiceEnabled = false), repository.savedNotifications.last())
    }

    @Test
    fun `자세히 보기를 누르면 그 알림 묶음 시트를 열고 닫는다`() = runTest(testDispatcher) {
        viewModel.onIntent(SettingsIntent.ClickNotificationDetail(NotificationKind.MARKETING))
        assertEquals(NotificationKind.MARKETING, state.notificationDetail)

        viewModel.onIntent(SettingsIntent.DismissNotificationDetail)
        assertNull(state.notificationDetail)
    }

    @Test
    fun `설정을 읽지 못하면 다시 시도로 처음부터 다시 읽는다`() = runTest(testDispatcher) {
        repository.getNotificationSettingsError = IOException("offline")
        resume()
        advanceUntilIdle()
        assertFalse(state.isLoaded)

        repository.getNotificationSettingsError = null
        messageHelper.snackBars.single { it.messageText == "설정을 불러오지 못했어요" }.onClickCTA?.invoke()
        advanceUntilIdle()

        assertTrue(state.isLoaded)
    }
}
