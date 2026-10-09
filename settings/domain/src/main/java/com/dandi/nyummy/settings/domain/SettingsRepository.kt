package com.dandi.nyummy.settings.domain

import com.dandi.nyummy.common.entity.meal.MealTimesVO
import com.dandi.nyummy.settings.entity.NotificationSettingsVO

interface SettingsRepository {

    /** 앱 안의 배경음을 켤지. 이 기기에만 저장한다. */
    suspend fun isBgmEnabled(): Boolean

    suspend fun setBgmEnabled(enabled: Boolean)

    suspend fun getNotificationSettings(): NotificationSettingsVO

    /** 알림 설정을 통째로 저장하고 서버가 저장한 값을 돌려준다. */
    suspend fun updateNotificationSettings(settings: NotificationSettingsVO): NotificationSettingsVO

    /** 평소 식사 시각. 정한 적이 없으면 기본값. */
    suspend fun getMealTimes(): MealTimesVO

    suspend fun saveMealTimes(mealTimes: MealTimesVO)
}
