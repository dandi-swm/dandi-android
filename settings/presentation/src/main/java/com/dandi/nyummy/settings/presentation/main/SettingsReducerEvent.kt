package com.dandi.nyummy.settings.presentation.main

import com.dandi.nyummy.common.presentation.mvi.ReducerEvent
import com.dandi.nyummy.settings.entity.LegalDocument
import com.dandi.nyummy.settings.entity.NotificationSettingsVO
import com.dandi.nyummy.settings.entity.SettingsVO

sealed interface SettingsReducerEvent : ReducerEvent {
    data class SettingsLoaded(val settings: SettingsVO) : SettingsReducerEvent
    data class DeviceNotificationChecked(val enabled: Boolean) : SettingsReducerEvent
    data class BgmChanged(val enabled: Boolean) : SettingsReducerEvent
    data class NotificationChanged(val notification: NotificationSettingsVO) : SettingsReducerEvent
    data class LegalLinkRequested(val document: LegalDocument?) : SettingsReducerEvent
    data class LogoutDialogVisibilityChanged(val visible: Boolean) : SettingsReducerEvent
    data object LoggingOut : SettingsReducerEvent
}
