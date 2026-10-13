package com.dandi.nyummy.settings.data

import com.dandi.nyummy.common.entity.meal.MealTimesVO
import com.dandi.nyummy.settings.data.dto.MealTimeRequestDTO
import com.dandi.nyummy.settings.data.dto.PushSettingDTO
import com.dandi.nyummy.settings.domain.SettingsRepository
import com.dandi.nyummy.settings.entity.NotificationSettingsChange
import com.dandi.nyummy.settings.entity.NotificationSettingsVO

class SettingsRepositoryImpl(
    private val remoteDataSource: SettingsRemoteDataSource,
) : SettingsRepository {

    override suspend fun getNotificationSettings(): NotificationSettingsVO = remoteDataSource.getPushSetting().toVO()

    override suspend fun updateNotificationSettings(change: NotificationSettingsChange) {
        remoteDataSource.updatePushSetting(PushSettingDTO.from(change))
    }

    override suspend fun getMealTimes(): MealTimesVO = remoteDataSource.getMyProfile().toMealTimesVO()

    override suspend fun saveMealTimes(mealTimes: MealTimesVO) {
        remoteDataSource.updateMealTime(MealTimeRequestDTO.from(mealTimes))
    }
}
