package com.dandi.nyummy.settings.presentation.main

import com.dandi.nyummy.common.entity.meal.MealTimesVO
import com.dandi.nyummy.common.presentation.mvi.UiState
import com.dandi.nyummy.settings.entity.NotificationSettingsVO

/**
 * 설정 UI 상태.
 *
 * @property isLoaded 설정 값을 한 번 읽었다. 읽기 전에는 토글과 식사 시간 줄을 막아 저장하지 못하게 한다.
 * @property isDeviceNotificationEnabled 기기에서 이 앱의 알림을 허용했는지. 꺼져 있으면 기기 설정으로 가는 안내를 띄운다(토글은 막지 않는다).
 * @property notificationDetail "자세히 보기" 시트로 보여 줄 알림 묶음. 닫혀 있으면 null.
 * @property mealTimes 평소 식사 시각. 서버에서 읽기 전이거나 한 번도 읽지 못했으면 null이고, 요약 줄을 비운다.
 * @property isLoggingOut 로그아웃 요청 중. 다이얼로그 버튼을 막는다.
 * @property isWithdrawing 탈퇴 요청 중. 다이얼로그 버튼을 막는다.
 */
data class SettingsUIState(
    val isLoaded: Boolean = false,
    val notification: NotificationSettingsVO = NotificationSettingsVO.default,
    val mealTimes: MealTimesVO? = null,
    val isDeviceNotificationEnabled: Boolean = true,
    val notificationDetail: NotificationKind? = null,
    val isLogoutDialogVisible: Boolean = false,
    val isLoggingOut: Boolean = false,
    val isWithdrawDialogVisible: Boolean = false,
    val isWithdrawing: Boolean = false,
) : UiState {

    companion object {
        val empty = SettingsUIState()
    }
}
