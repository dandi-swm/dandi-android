package com.dandi.nyummy.settings.presentation.main

import com.dandi.nyummy.common.entity.meal.MealTimesVO
import com.dandi.nyummy.common.presentation.mvi.UiState
import com.dandi.nyummy.settings.entity.LegalDocument
import com.dandi.nyummy.settings.entity.NotificationSettingsVO

/**
 * 설정 UI 상태.
 *
 * @property isDeviceNotificationEnabled 기기에서 이 앱의 알림을 허용했는지. 꺼져 있으면 알림 토글을 모두 막고 안내를 띄운다.
 * @property pendingLegalDocument 화면이 브라우저로 열어야 할 약관. 열어 본 뒤 비운다.
 * @property mealTimes 평소 식사 시각. 서버에서 읽기 전이거나 한 번도 읽지 못했으면 null이고, 요약 줄을 비운다.
 * @property isLoggingOut 로그아웃 요청 중. 다이얼로그 버튼을 막는다.
 */
data class SettingsUIState(
    val isBgmEnabled: Boolean = true,
    val notification: NotificationSettingsVO = NotificationSettingsVO.default,
    val mealTimes: MealTimesVO? = null,
    val isDeviceNotificationEnabled: Boolean = true,
    val pendingLegalDocument: LegalDocument? = null,
    val isLogoutDialogVisible: Boolean = false,
    val isLoggingOut: Boolean = false,
) : UiState {

    companion object {
        val empty = SettingsUIState()
    }
}
