package com.dandi.nyummy.settings.data

import com.dandi.nyummy.common.entity.meal.MealTimesVO
import com.dandi.nyummy.settings.data.dto.MealTimeRequestDTO
import com.dandi.nyummy.settings.data.dto.NotificationSettingsDTO
import com.dandi.nyummy.settings.domain.SettingsRepository
import com.dandi.nyummy.settings.entity.NotificationSettingsVO

class SettingsRepositoryImpl(
    private val notificationDataSource: NotificationSettingsDataSource,
    private val localDataSource: SettingsLocalDataSource,
    private val remoteDataSource: SettingsRemoteDataSource,
) : SettingsRepository {

    override suspend fun isBgmEnabled(): Boolean = localDataSource.isBgmEnabled()

    override suspend fun setBgmEnabled(enabled: Boolean) {
        localDataSource.setBgmEnabled(enabled)
    }

    override suspend fun getNotificationSettings(): NotificationSettingsVO =
        notificationDataSource.getNotificationSettings().toVO()

    override suspend fun updateNotificationSettings(settings: NotificationSettingsVO): NotificationSettingsVO =
        notificationDataSource.updateNotificationSettings(NotificationSettingsDTO.from(settings)).toVO()

    override suspend fun getMealTimes(): MealTimesVO = remoteDataSource.getMyProfile().toMealTimesVO()

    override suspend fun saveMealTimes(mealTimes: MealTimesVO) {
        remoteDataSource.updateMealTime(MealTimeRequestDTO.from(mealTimes))
    }
}
