package com.dandi.nyummy.settings.presentation.main

import com.dandi.nyummy.auth.domain.LogoutUseCase
import com.dandi.nyummy.common.domain.helper.StringResource
import com.dandi.nyummy.common.entity.meal.MealTimeVO
import com.dandi.nyummy.common.entity.meal.MealTimesVO
import com.dandi.nyummy.common.entity.time.DeviceTime
import com.dandi.nyummy.common.presentation.helper.StringProvider
import com.dandi.nyummy.legal.domain.LegalPage
import com.dandi.nyummy.legal.entity.LegalDocument
import com.dandi.nyummy.settings.domain.GetSettingsUseCase
import com.dandi.nyummy.settings.domain.MealTimeSettingPage
import com.dandi.nyummy.settings.domain.MyProfilePage
import com.dandi.nyummy.settings.domain.UpdateNotificationSettingsUseCase
import com.dandi.nyummy.settings.domain.WithdrawUseCase
import com.dandi.nyummy.settings.entity.MyProfileVO
import com.dandi.nyummy.settings.entity.NotificationSettingsChange
import com.dandi.nyummy.settings.entity.NotificationSettingsVO
import com.dandi.nyummy.settings.presentation.FakeAuthRepository
import com.dandi.nyummy.settings.presentation.FakeCompanionMemoryHelper
import com.dandi.nyummy.settings.presentation.FakePushTokenHelper
import com.dandi.nyummy.settings.presentation.FakeResourceHelper
import com.dandi.nyummy.settings.presentation.FakeSettingsRepository
import com.dandi.nyummy.settings.presentation.FakeTTIHelper
import com.dandi.nyummy.settings.presentation.FakeTutorialHelper
import com.dandi.nyummy.settings.presentation.NoOpWidgetHelper
import com.dandi.nyummy.settings.presentation.R
import com.dandi.nyummy.settings.presentation.RecordingMessageHelper
import com.dandi.nyummy.settings.presentation.RecordingNavigationHelper
import com.dandi.nyummy.settings.presentation.resText
import java.io.IOException
import java.time.LocalDate
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
            logout = LogoutUseCase(FakeAuthRepository, FakePushTokenHelper, FakeCompanionMemoryHelper, FakeTutorialHelper, NoOpWidgetHelper, navigationHelper),
            withdraw = WithdrawUseCase(
                repository,
                FakePushTokenHelper,
                FakeCompanionMemoryHelper,
                FakeTutorialHelper,
                NoOpWidgetHelper,
                FakeResourceHelper,
                messageHelper,
                navigationHelper,
                FakeTTIHelper,
            ),
            navigationHelper = navigationHelper,
            messageHelper = messageHelper,
            strings = TestStrings,
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
    fun `집사 이름을 읽어 맨 위 줄에 보여 주고 누르면 마이페이지로 간다`() = runTest(testDispatcher) {
        repository.profile = MyProfileVO(nickname = "진우")

        resume()
        advanceUntilIdle()
        viewModel.onIntent(SettingsIntent.ClickMyProfile)

        assertEquals("진우", state.nickname)
        assertEquals(listOf(MyProfilePage), navigationHelper.pages)
    }

    @Test
    fun `다시 읽다가 프로필을 읽지 못하면 마지막으로 읽은 집사 이름을 둔다`() = runTest(testDispatcher) {
        repository.profile = MyProfileVO(nickname = "진우")
        resume()
        advanceUntilIdle()

        repository.getMealTimesError = IOException("offline")
        resume()
        advanceUntilIdle()

        assertEquals("진우", state.nickname)
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
        // 처리 사이에 자정이 지나도 실패하지 않도록 전후 날짜를 모두 허용한다.
        val before = dateLabel(DeviceTime.today())

        viewModel.onIntent(SettingsIntent.ToggleMarketingPush(true))
        advanceUntilIdle()
        viewModel.onIntent(SettingsIntent.ToggleMarketingPush(false))
        advanceUntilIdle()
        val after = dateLabel(DeviceTime.today())

        val messages = messageHelper.snackBars.map { it.messageText }
        assertEquals(2, messages.size)
        assertTrue(messages[0] in setOf(before, after).map { "$it 마케팅 알림 수신에 동의했어요" })
        assertTrue(messages[1] in setOf(before, after).map { "$it 마케팅 알림 수신 동의를 철회했어요" })
    }

    @Test
    fun `같은 토글은 앞 저장이 끝난 뒤 마지막 값만 보낸다`() = runTest(testDispatcher) {
        // 앞 요청을 취소하면 서버가 이미 처리한 옛 값(동의)이 새 값(철회)보다 늦게 반영될 수 있다.
        val firstSave = CompletableDeferred<Unit>()
        repository.notificationUpdateResponses += { firstSave.await() }
        resume()
        advanceUntilIdle()

        viewModel.onIntent(SettingsIntent.ToggleMarketingPush(true))
        advanceUntilIdle()
        viewModel.onIntent(SettingsIntent.ToggleMarketingPush(false))
        advanceUntilIdle()

        // 앞 저장이 끝나기 전에는 다음 요청을 보내지 않는다. 화면은 마지막 값을 보여 준다.
        assertEquals(listOf(NotificationSettingsChange(isMarketingEnabled = true)), repository.requestedNotifications)
        assertFalse(state.notification.isMarketingEnabled)

        firstSave.complete(Unit)
        advanceUntilIdle()

        assertEquals(
            listOf(NotificationSettingsChange(isMarketingEnabled = true), NotificationSettingsChange(isMarketingEnabled = false)),
            repository.savedNotifications,
        )
        assertFalse(state.notification.isMarketingEnabled)
    }

    @Test
    fun `저장 중에 바꿨다가 서버 값으로 돌아오면 더 보내지 않는다`() = runTest(testDispatcher) {
        val firstSave = CompletableDeferred<Unit>()
        repository.notificationUpdateResponses += { firstSave.await() }
        resume()
        advanceUntilIdle()

        viewModel.onIntent(SettingsIntent.ToggleMarketingPush(true))
        advanceUntilIdle()
        viewModel.onIntent(SettingsIntent.ToggleMarketingPush(false))
        viewModel.onIntent(SettingsIntent.ToggleMarketingPush(true))
        advanceUntilIdle()
        firstSave.complete(Unit)
        advanceUntilIdle()

        assertEquals(listOf(NotificationSettingsChange(isMarketingEnabled = true)), repository.requestedNotifications)
        assertTrue(state.notification.isMarketingEnabled)
    }

    private fun dateLabel(date: LocalDate) = "${date.year}년 ${date.monthValue}월 ${date.dayOfMonth}일"

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
        val retry = messageHelper.snackBars.single { it.messageText == resText(R.string.settings_save_failed) }
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
        messageHelper.snackBars.single { it.messageText == StringResource.SETTINGS_LOAD_FAILED.name }.onClickCTA?.invoke()
        advanceUntilIdle()

        assertTrue(state.isLoaded)
    }

    @Test
    fun `회원 탈퇴를 누르면 확인 다이얼로그를 띄우고 닫기로 닫는다`() = runTest(testDispatcher) {
        viewModel.onIntent(SettingsIntent.ClickWithdraw)
        assertTrue(state.isWithdrawDialogVisible)

        viewModel.onIntent(SettingsIntent.DismissWithdrawDialog)
        assertFalse(state.isWithdrawDialogVisible)
        assertEquals(0, repository.withdrawCount)
    }

    @Test
    fun `탈퇴하는 동안은 다이얼로그를 닫지 못하고 다시 눌러도 한 번만 보낸다`() = runTest(testDispatcher) {
        val gate = CompletableDeferred<Unit>()
        repository.withdrawResponse = { gate.await() }
        viewModel.onIntent(SettingsIntent.ClickWithdraw)

        viewModel.onIntent(SettingsIntent.ConfirmWithdraw)
        viewModel.onIntent(SettingsIntent.ConfirmWithdraw)
        viewModel.onIntent(SettingsIntent.DismissWithdrawDialog)
        advanceUntilIdle()

        assertTrue(state.isWithdrawing)
        assertTrue(state.isWithdrawDialogVisible)
        assertEquals(1, repository.withdrawCount)
        gate.complete(Unit)
        advanceUntilIdle()
    }

    @Test
    fun `탈퇴하지 못하면 다이얼로그를 닫아 오류 스낵바가 보이게 한다`() = runTest(testDispatcher) {
        repository.withdrawResponse = { throw IOException("offline") }
        viewModel.onIntent(SettingsIntent.ClickWithdraw)

        viewModel.onIntent(SettingsIntent.ConfirmWithdraw)
        advanceUntilIdle()

        assertFalse(state.isWithdrawing)
        assertFalse(state.isWithdrawDialogVisible)
        assertEquals(StringResource.SETTINGS_WITHDRAW_FAILED.name, messageHelper.snackBars.single().messageText)
    }

    @Test
    fun `앱 알림이 꺼져 있으면 서버 값과 상관없이 서비스와 마케팅 알림이 꺼져 보인다`() = runTest(testDispatcher) {
        repository.notificationSettings = NotificationSettingsVO(isServiceEnabled = true, isMarketingEnabled = true)
        viewModel.onIntent(SettingsIntent.ScreenResumed(isDeviceNotificationEnabled = false))
        advanceUntilIdle()

        assertFalse(state.isServiceOn)
        assertFalse(state.isMarketingOn)
        // 서버 값은 그대로 둔다. 앱 알림을 켜면 이 값이 다시 보인다.
        assertEquals(NotificationSettingsVO(isServiceEnabled = true, isMarketingEnabled = true), state.notification)

        viewModel.onIntent(SettingsIntent.ScreenResumed(isDeviceNotificationEnabled = true))
        advanceUntilIdle()
        assertTrue(state.isServiceOn)
        assertTrue(state.isMarketingOn)
    }

    @Test
    fun `앱 알림을 켜면 권한 팝업을 요청하고 허용되면 서버 값은 건드리지 않는다`() = runTest(testDispatcher) {
        repository.notificationSettings = NotificationSettingsVO(isServiceEnabled = false, isMarketingEnabled = true)
        viewModel.onIntent(SettingsIntent.ScreenResumed(isDeviceNotificationEnabled = false))
        advanceUntilIdle()

        viewModel.onIntent(SettingsIntent.EnableAppNotification)
        assertTrue(state.isDeviceNotificationRequested)

        viewModel.onIntent(SettingsIntent.DeviceNotificationRequestFinished(isDeviceNotificationEnabled = true))
        advanceUntilIdle()

        assertFalse(state.isDeviceNotificationRequested)
        assertTrue(state.isDeviceNotificationEnabled)
        assertFalse(state.isDeviceSettingsDialogVisible)
        assertFalse(state.isServiceOn)
        assertTrue(state.isMarketingOn)
        assertTrue(repository.requestedNotifications.isEmpty())
    }

    @Test
    fun `이미 앱 알림이 켜져 있으면 권한 팝업을 요청하지 않는다`() = runTest(testDispatcher) {
        resume()
        advanceUntilIdle()

        viewModel.onIntent(SettingsIntent.EnableAppNotification)

        assertFalse(state.isDeviceNotificationRequested)
    }

    @Test
    fun `권한 팝업으로 허용받지 못하면 기기 설정 안내를 띄우고 닫을 수 있다`() = runTest(testDispatcher) {
        viewModel.onIntent(SettingsIntent.ScreenResumed(isDeviceNotificationEnabled = false))
        advanceUntilIdle()

        viewModel.onIntent(SettingsIntent.EnableAppNotification)
        viewModel.onIntent(SettingsIntent.DeviceNotificationRequestFinished(isDeviceNotificationEnabled = false))

        assertFalse(state.isDeviceNotificationRequested)
        assertFalse(state.isDeviceNotificationEnabled)
        assertTrue(state.isDeviceSettingsDialogVisible)

        viewModel.onIntent(SettingsIntent.ConfirmDeviceSettings)
        assertFalse(state.isDeviceSettingsDialogVisible)

        viewModel.onIntent(SettingsIntent.EnableAppNotification)
        viewModel.onIntent(SettingsIntent.DeviceNotificationRequestFinished(isDeviceNotificationEnabled = false))
        viewModel.onIntent(SettingsIntent.DismissDeviceSettingsDialog)
        assertFalse(state.isDeviceSettingsDialogVisible)
    }

    @Test
    fun `요청하지 않았는데 온 권한 결과는 무시한다`() = runTest(testDispatcher) {
        viewModel.onIntent(SettingsIntent.ScreenResumed(isDeviceNotificationEnabled = false))
        advanceUntilIdle()

        viewModel.onIntent(SettingsIntent.DeviceNotificationRequestFinished(isDeviceNotificationEnabled = false))

        assertFalse(state.isDeviceSettingsDialogVisible)
    }

    @Test
    fun `약관 행을 누르면 앱 안의 약관 화면으로 간다`() = runTest(testDispatcher) {
        viewModel.onIntent(SettingsIntent.ClickLegal(LegalDocument.TERMS))
        viewModel.onIntent(SettingsIntent.ClickLegal(LegalDocument.PRIVACY))

        assertEquals(listOf(LegalPage(LegalDocument.TERMS), LegalPage(LegalDocument.PRIVACY)), navigationHelper.pages)
    }
}

/** 날짜 형식과 마케팅 동의 문구는 한국어 원문으로 돌려줘 문장 조립까지 확인한다. */
private val TestStrings = StringProvider { id, args ->
    when (id) {
        R.string.settings_consent_date_pattern -> "yyyy년 M월 d일"
        R.string.settings_marketing_agreed -> "%1${'$'}s 마케팅 알림 수신에 동의했어요".format(*args)
        R.string.settings_marketing_withdrawn -> "%1${'$'}s 마케팅 알림 수신 동의를 철회했어요".format(*args)
        else -> "string:$id"
    }
}
