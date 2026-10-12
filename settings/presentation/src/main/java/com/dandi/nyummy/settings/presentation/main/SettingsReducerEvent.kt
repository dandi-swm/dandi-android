package com.dandi.nyummy.settings.presentation.main

import com.dandi.nyummy.common.presentation.mvi.ReducerEvent
import com.dandi.nyummy.settings.entity.LegalDocument
import com.dandi.nyummy.settings.entity.NotificationSettingsVO
import com.dandi.nyummy.settings.entity.SettingsVO

sealed interface SettingsReducerEvent : ReducerEvent {
    /**
     * 저장된 설정을 읽었다.
     *
     * @property keepBgm 읽는 사이 사용자가 배경음을 바꿨다. 늦게 온 값으로 덮지 않고 화면 값을 둔다.
     * @property keepNotification 읽는 사이 사용자가 알림을 바꿨다. 늦게 온 값으로 덮지 않고 화면 값을 둔다.
     */
    data class SettingsLoaded(
        val settings: SettingsVO,
        val keepBgm: Boolean = false,
        val keepNotification: Boolean = false,
    ) : SettingsReducerEvent
    data class DeviceNotificationChecked(val enabled: Boolean) : SettingsReducerEvent
    data class BgmChanged(val enabled: Boolean) : SettingsReducerEvent
    data class NotificationChanged(val notification: NotificationSettingsVO) : SettingsReducerEvent
    data class LegalLinkRequested(val document: LegalDocument?) : SettingsReducerEvent
    data class LogoutDialogVisibilityChanged(val visible: Boolean) : SettingsReducerEvent
    data object LoggingOut : SettingsReducerEvent
}
