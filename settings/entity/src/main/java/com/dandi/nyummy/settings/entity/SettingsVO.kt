package com.dandi.nyummy.settings.entity

import com.dandi.nyummy.common.entity.meal.MealTimesVO

/**
 * 알림 설정. 서버에 저장해 기기를 바꿔도 남는다.
 *
 * @property isAllEnabled 전체 알림. 끄면 아래 알림도 오지 않는다(각 값은 그대로 남긴다).
 * @property isMealReminderEnabled 평소 식사 시간에 아직 기록이 없으면 알려 준다.
 * @property isNoticeEnabled 운영과 보상 소식.
 */
data class NotificationSettingsVO(
    val isAllEnabled: Boolean = true,
    val isMealReminderEnabled: Boolean = true,
    val isNoticeEnabled: Boolean = true,
) {
    companion object {
        val default = NotificationSettingsVO()
    }
}

/**
 * 설정 화면이 한 번에 읽는 값.
 *
 * @property isBgmEnabled 앱 안의 배경음. 이 기기에만 저장한다.
 * @property mealTimes 평소 식사 시각. 이 기기에만 저장한다(온보딩에서 처음 받는다).
 */
data class SettingsVO(
    val isBgmEnabled: Boolean = true,
    val notification: NotificationSettingsVO = NotificationSettingsVO.default,
    val mealTimes: MealTimesVO = MealTimesVO.default,
) {
    companion object {
        val default = SettingsVO()
    }
}

/** 설정에서 여는 약관 문서. */
enum class LegalDocument {
    TERMS,
    PRIVACY,
}
