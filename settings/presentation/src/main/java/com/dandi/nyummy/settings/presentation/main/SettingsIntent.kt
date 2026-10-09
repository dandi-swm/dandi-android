package com.dandi.nyummy.settings.presentation.main

import com.dandi.nyummy.common.presentation.mvi.MviIntent
import com.dandi.nyummy.settings.entity.LegalDocument

/** 설정 화면에서 발생하는 사용자 의도. */
sealed interface SettingsIntent : MviIntent {

    /**
     * 화면이 보이게 됐다(첫 진입, 식사 시간이나 기기 설정에서 복귀). 값을 다시 읽는다.
     *
     * @param isDeviceNotificationEnabled 기기에서 이 앱의 알림을 허용했는지. 화면이 기기에서 읽어 넘긴다.
     */
    data class ScreenResumed(val isDeviceNotificationEnabled: Boolean) : SettingsIntent

    data object ClickBack : SettingsIntent

    data class ToggleBgm(val enabled: Boolean) : SettingsIntent

    data class ToggleAllNotification(val enabled: Boolean) : SettingsIntent

    data class ToggleMealReminder(val enabled: Boolean) : SettingsIntent

    data class ToggleNotice(val enabled: Boolean) : SettingsIntent

    data object ClickMealTime : SettingsIntent

    /** 약관 행을 눌렀다. 화면이 브라우저로 연다. */
    data class ClickLegal(val document: LegalDocument) : SettingsIntent

    /** 화면이 약관 링크를 열어 봤다. 못 열었으면 "다시 시도" 스낵바를 띄운다. */
    data class LegalLinkHandled(val document: LegalDocument, val opened: Boolean) : SettingsIntent

    data object ClickLogout : SettingsIntent

    data object DismissLogoutDialog : SettingsIntent

    data object ConfirmLogout : SettingsIntent
}
