package com.dandi.nyummy.settings.presentation.main

import com.dandi.nyummy.common.entity.meal.MealTimesVO
import com.dandi.nyummy.common.presentation.mvi.UiState
import com.dandi.nyummy.settings.entity.NotificationSettingsVO

/**
 * 설정 UI 상태.
 *
 * @property isLoaded 설정 값을 한 번 읽었다. 읽기 전에는 토글과 식사 시간 줄을 막아 저장하지 못하게 한다.
 * @property notification 서버에 저장된 알림 설정. 앱 알림이 켜져 있을 때 그대로 보여 준다.
 * @property isDeviceNotificationEnabled 기기에서 이 앱의 알림을 허용했는지("앱 알림" 토글). 꺼져 있으면 서비스, 마케팅 알림은
 *   서버 값과 상관없이 꺼진 것으로 보이고 바꿀 수 없다.
 * @property isDeviceNotificationRequested 앱 알림을 켜려고 했다. 화면이 시스템 알림 권한 팝업을 띄우고 결과를 알린다.
 * @property isDeviceSettingsDialogVisible 팝업으로 허용받지 못해 기기 설정에서 켜도록 안내하는 다이얼로그.
 * @property notificationDetail "자세히 보기" 시트로 보여 줄 알림 묶음. 닫혀 있으면 null.
 * @property isFontCreditVisible "글꼴 출처" 시트가 열려 있다.
 * @property mealTimes 평소 식사 시각. 서버에서 읽기 전이거나 한 번도 읽지 못했으면 null이고, 요약 줄을 비운다.
 * @property isLoggingOut 로그아웃 요청 중. 다이얼로그 버튼을 막는다.
 * @property isWithdrawing 탈퇴 요청 중. 다이얼로그 버튼을 막는다.
 */
data class SettingsUIState(
    val isLoaded: Boolean = false,
    val notification: NotificationSettingsVO = NotificationSettingsVO.default,
    val mealTimes: MealTimesVO? = null,
    val isDeviceNotificationEnabled: Boolean = true,
    val isDeviceNotificationRequested: Boolean = false,
    val isDeviceSettingsDialogVisible: Boolean = false,
    val notificationDetail: NotificationKind? = null,
    val isFontCreditVisible: Boolean = false,
    val isLogoutDialogVisible: Boolean = false,
    val isLoggingOut: Boolean = false,
    val isWithdrawDialogVisible: Boolean = false,
    val isWithdrawing: Boolean = false,
) : UiState {

    /** 서비스 알림 토글에 보이는 값. 앱 알림이 꺼져 있으면 서버 값과 상관없이 꺼져 있다. */
    val isServiceOn: Boolean get() = isDeviceNotificationEnabled && notification.isServiceEnabled

    /** 마케팅 알림 토글에 보이는 값. 앱 알림이 꺼져 있으면 서버 값과 상관없이 꺼져 있다. */
    val isMarketingOn: Boolean get() = isDeviceNotificationEnabled && notification.isMarketingEnabled

    companion object {
        val empty = SettingsUIState()
    }
}
