package com.dandi.nyummy.settings.presentation.main

import com.dandi.nyummy.common.presentation.mvi.MviIntent
import com.dandi.nyummy.legal.entity.LegalDocument

/** 설정 화면에서 발생하는 사용자 의도. */
sealed interface SettingsIntent : MviIntent {

    /**
     * 화면이 보이게 됐다(첫 진입, 식사 시간이나 기기 설정에서 복귀). 값을 다시 읽는다.
     *
     * @param isDeviceNotificationEnabled 기기에서 이 앱의 알림을 허용했는지. 화면이 기기에서 읽어 넘긴다.
     */
    data class ScreenResumed(val isDeviceNotificationEnabled: Boolean) : SettingsIntent

    data object ClickBack : SettingsIntent

    /**
     * "앱 알림"을 켰다. 기기 알림이 꺼져 있으면 시스템 알림 권한 팝업을 띄운다.
     * 끄는 것은 앱이 할 수 없어 화면이 기기 알림 설정을 바로 연다.
     */
    data object EnableAppNotification : SettingsIntent

    /**
     * 시스템 알림 권한 팝업이 끝났다.
     *
     * @param isDeviceNotificationEnabled 팝업이 끝난 뒤 기기에서 이 앱의 알림이 허용돼 있는지. 화면이 기기에서 읽어 넘긴다.
     */
    data class DeviceNotificationRequestFinished(val isDeviceNotificationEnabled: Boolean) : SettingsIntent

    /** 기기 설정 안내 다이얼로그에서 "기기 설정 열기"를 눌렀다. 화면이 기기 설정을 열고, 돌아오면 [ScreenResumed]가 다시 확인한다. */
    data object ConfirmDeviceSettings : SettingsIntent

    data object DismissDeviceSettingsDialog : SettingsIntent

    /** 서비스 알림(식사 시간 알림, 분석 결과 알림)을 켜거나 껐다. */
    data class ToggleServicePush(val enabled: Boolean) : SettingsIntent

    /** 마케팅 알림(기록을 쉴 때 알림) 수신에 동의하거나 철회했다. */
    data class ToggleMarketingPush(val enabled: Boolean) : SettingsIntent

    /** 알림 묶음의 "자세히 보기"를 눌렀다. 어떤 알림이 들어 있는지 시트로 보여 준다. */
    data class ClickNotificationDetail(val kind: NotificationKind) : SettingsIntent

    data object DismissNotificationDetail : SettingsIntent

    data object ClickMealTime : SettingsIntent

    /** 약관 행을 눌렀다. 앱 안의 약관 화면으로 간다. */
    data class ClickLegal(val document: LegalDocument) : SettingsIntent

    data object ClickLogout : SettingsIntent

    data object DismissLogoutDialog : SettingsIntent

    data object ConfirmLogout : SettingsIntent

    /** "회원 탈퇴"를 눌렀다. 되돌릴 수 없다는 확인 다이얼로그를 띄운다. */
    data object ClickWithdraw : SettingsIntent

    data object DismissWithdrawDialog : SettingsIntent

    data object ConfirmWithdraw : SettingsIntent
}
