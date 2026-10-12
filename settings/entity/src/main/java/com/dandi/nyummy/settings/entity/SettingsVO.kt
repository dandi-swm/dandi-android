package com.dandi.nyummy.settings.entity

import com.dandi.nyummy.common.entity.meal.MealTimesVO

/**
 * 알림 수신 설정. 서버에 저장해 기기를 바꿔도 남는다. 기본값은 서버와 같다.
 *
 * @property isServiceEnabled 서비스 알림(식사 시간 알림, 분석 결과 알림). 기본은 받는다.
 * @property isMarketingEnabled 마케팅 알림(기록을 쉴 때 알림). 광고성 정보라 동의해야 받는다. 기본은 받지 않는다.
 */
data class NotificationSettingsVO(
    val isServiceEnabled: Boolean = true,
    val isMarketingEnabled: Boolean = false,
) {
    companion object {
        val default = NotificationSettingsVO()
    }
}

/**
 * 알림 수신 설정에서 바꿀 항목. null인 항목은 그대로 둔다.
 * 서버에도 바꾼 항목만 보낸다. 다른 기기에서 바꾼 값을 화면의 옛 값으로 덮지 않게 하려는 것이다.
 */
data class NotificationSettingsChange(
    val isServiceEnabled: Boolean? = null,
    val isMarketingEnabled: Boolean? = null,
) {
    /** [settings]에 이 변경을 적용한다. */
    fun applyTo(settings: NotificationSettingsVO): NotificationSettingsVO = settings.copy(
        isServiceEnabled = isServiceEnabled ?: settings.isServiceEnabled,
        isMarketingEnabled = isMarketingEnabled ?: settings.isMarketingEnabled,
    )

    /** 이 변경이 바꾸는 항목만 [saved]의 값으로 되돌린 변경. 저장에 실패했을 때 쓴다. */
    fun revertTo(saved: NotificationSettingsVO): NotificationSettingsChange = NotificationSettingsChange(
        isServiceEnabled = isServiceEnabled?.let { saved.isServiceEnabled },
        isMarketingEnabled = isMarketingEnabled?.let { saved.isMarketingEnabled },
    )
}

/**
 * 설정 화면이 한 번에 읽는 값.
 *
 * @property isBgmEnabled 앱 안의 배경음. 이 기기에만 저장한다.
 * @property mealTimes 평소 식사 시각. 서버에 저장한다(온보딩에서 처음 받는다). 읽지 못했으면 null.
 */
data class SettingsVO(
    val isBgmEnabled: Boolean = true,
    val notification: NotificationSettingsVO = NotificationSettingsVO.default,
    val mealTimes: MealTimesVO? = null,
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
