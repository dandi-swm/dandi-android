package com.dandi.nyummy.settings.presentation.main

import com.dandi.nyummy.common.presentation.mvi.ReducerEvent
import com.dandi.nyummy.settings.entity.NotificationSettingsVO
import com.dandi.nyummy.settings.entity.SettingsVO

sealed interface SettingsReducerEvent : ReducerEvent {
    /**
     * 저장된 설정을 읽었다.
     *
     * @property keepNotification 읽는 사이 사용자가 알림을 바꿨다. 늦게 온 값으로 덮지 않고 화면 값을 둔다.
     */
    data class SettingsLoaded(
        val settings: SettingsVO,
        val keepNotification: Boolean = false,
    ) : SettingsReducerEvent
    data class DeviceNotificationChecked(val enabled: Boolean) : SettingsReducerEvent
    data class NotificationChanged(val notification: NotificationSettingsVO) : SettingsReducerEvent
    data class DeviceNotificationRequestChanged(val requested: Boolean) : SettingsReducerEvent
    data class DeviceSettingsDialogVisibilityChanged(val visible: Boolean) : SettingsReducerEvent

    data class NotificationDetailChanged(val kind: NotificationKind?) : SettingsReducerEvent
    data class LogoutDialogVisibilityChanged(val visible: Boolean) : SettingsReducerEvent
    data object LoggingOut : SettingsReducerEvent
    data class WithdrawDialogVisibilityChanged(val visible: Boolean) : SettingsReducerEvent
    data object Withdrawing : SettingsReducerEvent

    /** 탈퇴하지 못했다. 오류 스낵바가 다이얼로그 창에 가려지지 않게 다이얼로그도 닫는다. */
    data object WithdrawFailed : SettingsReducerEvent
}
