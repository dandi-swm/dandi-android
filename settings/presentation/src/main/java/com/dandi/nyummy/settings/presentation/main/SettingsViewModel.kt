package com.dandi.nyummy.settings.presentation.main

import androidx.lifecycle.viewModelScope
import com.dandi.nyummy.auth.domain.LogoutUseCase
import com.dandi.nyummy.common.domain.error.HttpResponseException
import com.dandi.nyummy.common.domain.error.isSessionExpired
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.message.IconType
import com.dandi.nyummy.common.entity.time.KstTime
import com.dandi.nyummy.common.presentation.mvi.MviViewModel
import com.dandi.nyummy.settings.domain.GetSettingsUseCase
import com.dandi.nyummy.settings.domain.MealTimeSettingPage
import com.dandi.nyummy.settings.domain.SetBgmEnabledUseCase
import com.dandi.nyummy.settings.domain.UpdateNotificationSettingsUseCase
import com.dandi.nyummy.settings.entity.LegalDocument
import com.dandi.nyummy.settings.entity.NotificationSettingsChange
import com.dandi.nyummy.settings.entity.NotificationSettingsVO
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val getSettings: GetSettingsUseCase,
    private val setBgmEnabled: SetBgmEnabledUseCase,
    private val updateNotificationSettings: UpdateNotificationSettingsUseCase,
    private val logout: LogoutUseCase,
    private val navigationHelper: NavigationHelper,
    private val messageHelper: MessageHelper,
) : MviViewModel<SettingsIntent, SettingsUIState, SettingsReducerEvent>(SettingsUIState.empty) {

    // 알림 묶음별 저장 요청. 같은 토글을 빠르게 여러 번 누르면 마지막 값만 반영하고, 다른 토글의 저장은 끊지 않는다.
    private val notificationJobs = mutableMapOf<NotificationKind, Job>()
    private var loadJob: Job? = null

    // 서버에 저장된 것으로 확인한 알림 설정. 저장이 실패하면 이 값으로 되돌린다.
    private var savedNotification: NotificationSettingsVO? = null

    // 사용자가 바꾼 횟수. 읽기를 시작한 뒤 늘었으면 늦게 도착한 읽기 결과로 그 값을 덮지 않는다.
    private var bgmEditCount = 0
    private var notificationEditCount = 0

    override fun onIntent(intent: SettingsIntent) {
        when (intent) {
            is SettingsIntent.ScreenResumed -> {
                dispatch(SettingsReducerEvent.DeviceNotificationChecked(intent.isDeviceNotificationEnabled))
                load()
            }
            SettingsIntent.ClickBack -> navigationHelper.navigateToBack()
            is SettingsIntent.ToggleBgm -> whenLoaded { changeBgm(intent.enabled) }
            is SettingsIntent.ToggleServicePush -> whenLoaded {
                changeNotification(NotificationKind.SERVICE, NotificationSettingsChange(isServiceEnabled = intent.enabled))
            }
            is SettingsIntent.ToggleMarketingPush -> whenLoaded {
                changeNotification(NotificationKind.MARKETING, NotificationSettingsChange(isMarketingEnabled = intent.enabled))
            }
            is SettingsIntent.ClickNotificationDetail -> dispatch(SettingsReducerEvent.NotificationDetailChanged(intent.kind))
            SettingsIntent.DismissNotificationDetail -> dispatch(SettingsReducerEvent.NotificationDetailChanged(null))
            SettingsIntent.ClickMealTime -> whenLoaded { navigationHelper.navigateTo(MealTimeSettingPage) }
            is SettingsIntent.ClickLegal -> dispatch(SettingsReducerEvent.LegalLinkRequested(intent.document))
            is SettingsIntent.LegalLinkHandled -> onLegalLinkHandled(intent.document, intent.opened)
            SettingsIntent.ClickLogout -> dispatch(SettingsReducerEvent.LogoutDialogVisibilityChanged(visible = true))
            SettingsIntent.DismissLogoutDialog ->
                if (!currentState.isLoggingOut) dispatch(SettingsReducerEvent.LogoutDialogVisibilityChanged(visible = false))
            SettingsIntent.ConfirmLogout -> confirmLogout()
        }
    }

    override fun reduce(state: SettingsUIState, event: SettingsReducerEvent): SettingsUIState = when (event) {
        is SettingsReducerEvent.SettingsLoaded -> state.copy(
            isLoaded = true,
            isBgmEnabled = if (event.keepBgm) state.isBgmEnabled else event.settings.isBgmEnabled,
            notification = if (event.keepNotification) state.notification else event.settings.notification,
            // 다시 읽다가 실패하면 마지막으로 읽은 값을 둔다.
            mealTimes = event.settings.mealTimes ?: state.mealTimes,
        )
        is SettingsReducerEvent.DeviceNotificationChecked -> state.copy(isDeviceNotificationEnabled = event.enabled)
        is SettingsReducerEvent.BgmChanged -> state.copy(isBgmEnabled = event.enabled)
        is SettingsReducerEvent.NotificationChanged -> state.copy(notification = event.notification)
        is SettingsReducerEvent.NotificationDetailChanged -> state.copy(notificationDetail = event.kind)
        is SettingsReducerEvent.LegalLinkRequested -> state.copy(pendingLegalDocument = event.document)
        is SettingsReducerEvent.LogoutDialogVisibilityChanged -> state.copy(isLogoutDialogVisible = event.visible)
        SettingsReducerEvent.LoggingOut -> state.copy(isLoggingOut = true)
    }

    /**
     * 설정 값을 다시 읽는다. 화면에 돌아올 때마다 부르므로, 앞서 시작한 읽기는 취소하고 마지막 읽기 결과만 쓴다.
     * 그러지 않으면 식사 시간을 저장하고 돌아왔을 때 늦게 끝난 이전 읽기가 새 값을 덮을 수 있다.
     */
    private fun load() {
        val bgmEditsAtStart = bgmEditCount
        val notificationEditsAtStart = notificationEditCount
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            // 읽지 못하면 UseCase가 "다시 시도"를 띄운다. 다시 시도하면 처음부터 다시 읽는다.
            getSettings(onRetry = ::load).onSuccess { loaded ->
                val notificationEdited = notificationEditCount != notificationEditsAtStart
                // 읽는 사이 알림을 바꿨으면 그 저장 결과가 되돌릴 기준이 된다. 기준이 아직 없을 때만 읽은 값을 쓴다.
                if (!notificationEdited || savedNotification == null) savedNotification = loaded.notification
                dispatch(
                    SettingsReducerEvent.SettingsLoaded(
                        settings = loaded,
                        keepBgm = bgmEditCount != bgmEditsAtStart,
                        keepNotification = notificationEdited,
                    ),
                )
            }
        }
    }

    /** 설정 값을 처음 읽기 전에는 바꾸거나 식사 시간 화면으로 가지 않는다. 읽기 전 값으로 저장하지 않게 하려는 것이다. */
    private inline fun whenLoaded(action: () -> Unit) {
        if (currentState.isLoaded) action()
    }

    private fun changeBgm(enabled: Boolean) {
        bgmEditCount++
        dispatch(SettingsReducerEvent.BgmChanged(enabled))
        viewModelScope.launch {
            setBgmEnabled(enabled).onFailure {
                dispatch(SettingsReducerEvent.BgmChanged(!enabled))
                showSaveFailed { changeBgm(enabled) }
            }
        }
    }

    /**
     * 바로 반영하고 바꾼 항목만 저장한다. 저장이 실패하면 그 항목만 되돌리고 "다시 시도" 스낵바를 띄운다.
     * 다시 시도는 실패한 토글 하나([change])만 다시 저장한다. 스낵바가 떠 있는 동안 바꾼 다른 토글을 덮지 않기 위해서다.
     * 마케팅 알림은 저장되면 동의하거나 철회한 날짜를 알린다(광고성 정보 수신 동의 결과 안내).
     */
    private fun changeNotification(kind: NotificationKind, change: NotificationSettingsChange) {
        notificationEditCount++
        dispatch(SettingsReducerEvent.NotificationChanged(change.applyTo(currentState.notification)))
        notificationJobs[kind]?.cancel()
        notificationJobs[kind] = viewModelScope.launch {
            updateNotificationSettings(change).onSuccess {
                savedNotification = change.applyTo(savedNotification ?: currentState.notification)
                change.isMarketingEnabled?.let(::showMarketingConsentResult)
            }.onFailure { error ->
                savedNotification?.let { saved ->
                    dispatch(SettingsReducerEvent.NotificationChanged(change.revertTo(saved).applyTo(currentState.notification)))
                }
                // 로그인 만료는 UseCase가 이미 다이얼로그로 알렸다.
                if ((error as? HttpResponseException)?.isSessionExpired() != true) {
                    showSaveFailed { changeNotification(kind, change) }
                }
            }
        }
    }

    /** 마케팅 알림 수신 동의나 철회가 처리된 날짜를 알린다. 광고성 정보 수신 동의 결과는 처리 날짜와 함께 알려야 한다. */
    private fun showMarketingConsentResult(agreed: Boolean) {
        val today = KstTime.now()
        val date = "${today.year}년 ${today.month}월 ${today.day}일"
        messageHelper.showSnackBar(
            iconType = IconType.SUCCESS,
            messageText = if (agreed) "$date $MARKETING_AGREED_MESSAGE" else "$date $MARKETING_WITHDRAWN_MESSAGE",
        )
    }

    private fun showSaveFailed(retry: () -> Unit) {
        messageHelper.showSnackBar(
            iconType = IconType.ERROR,
            messageText = SAVE_FAILED_MESSAGE,
            callToActionText = RETRY_LABEL,
            onClickCTA = retry,
        )
    }

    private fun onLegalLinkHandled(document: LegalDocument, opened: Boolean) {
        dispatch(SettingsReducerEvent.LegalLinkRequested(null))
        if (opened) return
        messageHelper.showSnackBar(
            iconType = IconType.ERROR,
            messageText = when (document) {
                LegalDocument.TERMS -> TERMS_OPEN_FAILED_MESSAGE
                LegalDocument.PRIVACY -> PRIVACY_OPEN_FAILED_MESSAGE
            },
            callToActionText = RETRY_LABEL,
            onClickCTA = { onIntent(SettingsIntent.ClickLegal(document)) },
        )
    }

    private fun confirmLogout() {
        if (currentState.isLoggingOut) return
        dispatch(SettingsReducerEvent.LoggingOut)
        viewModelScope.launch { logout() }
    }

    private companion object {
        const val SAVE_FAILED_MESSAGE = "설정을 저장하지 못했어요"
        const val TERMS_OPEN_FAILED_MESSAGE = "이용약관을 열지 못했어요"
        const val PRIVACY_OPEN_FAILED_MESSAGE = "개인정보처리방침을 열지 못했어요"
        const val RETRY_LABEL = "다시 시도"
        const val MARKETING_AGREED_MESSAGE = "마케팅 알림 수신에 동의했어요"
        const val MARKETING_WITHDRAWN_MESSAGE = "마케팅 알림 수신 동의를 철회했어요"
    }
}
