package com.dandi.nyummy.settings.presentation.main

import androidx.lifecycle.viewModelScope
import com.dandi.nyummy.auth.domain.LogoutUseCase
import com.dandi.nyummy.common.domain.error.HttpResponseException
import com.dandi.nyummy.common.domain.error.isSessionExpired
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.message.IconType
import com.dandi.nyummy.common.presentation.mvi.MviViewModel
import com.dandi.nyummy.settings.domain.GetSettingsUseCase
import com.dandi.nyummy.settings.domain.MealTimeSettingPage
import com.dandi.nyummy.settings.domain.SetBgmEnabledUseCase
import com.dandi.nyummy.settings.domain.UpdateNotificationSettingsUseCase
import com.dandi.nyummy.settings.entity.LegalDocument
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

    private var notificationJob: Job? = null

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
            is SettingsIntent.ToggleBgm -> changeBgm(intent.enabled)
            is SettingsIntent.ToggleAllNotification -> changeNotification { it.copy(isAllEnabled = intent.enabled) }
            is SettingsIntent.ToggleMealReminder -> changeNotification { it.copy(isMealReminderEnabled = intent.enabled) }
            is SettingsIntent.ToggleNotice -> changeNotification { it.copy(isNoticeEnabled = intent.enabled) }
            SettingsIntent.ClickMealTime -> navigationHelper.navigateTo(MealTimeSettingPage)
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
            isBgmEnabled = if (event.keepBgm) state.isBgmEnabled else event.settings.isBgmEnabled,
            notification = if (event.keepNotification) state.notification else event.settings.notification,
            // 다시 읽다가 실패하면 마지막으로 읽은 값을 둔다.
            mealTimes = event.settings.mealTimes ?: state.mealTimes,
        )
        is SettingsReducerEvent.DeviceNotificationChecked -> state.copy(isDeviceNotificationEnabled = event.enabled)
        is SettingsReducerEvent.BgmChanged -> state.copy(isBgmEnabled = event.enabled)
        is SettingsReducerEvent.NotificationChanged -> state.copy(notification = event.notification)
        is SettingsReducerEvent.LegalLinkRequested -> state.copy(pendingLegalDocument = event.document)
        is SettingsReducerEvent.LogoutDialogVisibilityChanged -> state.copy(isLogoutDialogVisible = event.visible)
        SettingsReducerEvent.LoggingOut -> state.copy(isLoggingOut = true)
    }

    private fun load() {
        val bgmEditsAtStart = bgmEditCount
        val notificationEditsAtStart = notificationEditCount
        viewModelScope.launch {
            getSettings().onSuccess { loaded ->
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
     * 바로 반영하고 저장한다. 저장이 실패하면 되돌리고 "다시 시도" 스낵바를 띄운다.
     * 다시 시도는 실패한 토글 하나([change])만 그때의 값에 다시 적용한다. 스낵바가 떠 있는 동안 바꾼 다른 토글을 덮지 않기 위해서다.
     */
    private fun changeNotification(change: (NotificationSettingsVO) -> NotificationSettingsVO) {
        notificationEditCount++
        val next = change(currentState.notification)
        dispatch(SettingsReducerEvent.NotificationChanged(next))
        // 빠르게 여러 번 누르면 마지막 값만 결과를 반영한다.
        notificationJob?.cancel()
        notificationJob = viewModelScope.launch {
            updateNotificationSettings(next).onSuccess {
                savedNotification = it
            }.onFailure { error ->
                savedNotification?.let { dispatch(SettingsReducerEvent.NotificationChanged(it)) }
                // 로그인 만료는 UseCase가 이미 다이얼로그로 알렸다.
                if ((error as? HttpResponseException)?.isSessionExpired() != true) {
                    showSaveFailed { changeNotification(change) }
                }
            }
        }
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
    }
}
