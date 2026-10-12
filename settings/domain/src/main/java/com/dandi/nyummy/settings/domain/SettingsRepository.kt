package com.dandi.nyummy.settings.domain

import com.dandi.nyummy.common.entity.meal.MealTimesVO
import com.dandi.nyummy.settings.entity.NotificationSettingsChange
import com.dandi.nyummy.settings.entity.NotificationSettingsVO

interface SettingsRepository {

    /** 앱 안의 배경음을 켤지. 이 기기에만 저장한다. */
    suspend fun isBgmEnabled(): Boolean

    suspend fun setBgmEnabled(enabled: Boolean)

    /** 서버에 저장된 알림 수신 설정(서비스 알림, 마케팅 알림). */
    suspend fun getNotificationSettings(): NotificationSettingsVO

    /** 알림 수신 설정에서 [change]가 바꾸는 항목만 서버에 저장한다. */
    suspend fun updateNotificationSettings(change: NotificationSettingsChange)

    /** 서버에 저장된 평소 식사 시각. 서버가 비워 둔(알림을 받지 않는) 끼니는 건너뛴 것으로 준다. */
    suspend fun getMealTimes(): MealTimesVO

    /** 평소 식사 시각을 서버에 저장한다. 서버는 이 시각으로 식사 알림을 보낸다. */
    suspend fun saveMealTimes(mealTimes: MealTimesVO)
}
