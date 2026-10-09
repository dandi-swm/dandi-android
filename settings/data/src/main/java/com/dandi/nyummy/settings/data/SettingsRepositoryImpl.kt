package com.dandi.nyummy.settings.data

import com.dandi.nyummy.common.data.preference.AppPreferenceProvider
import com.dandi.nyummy.common.entity.meal.MealTimesVO
import com.dandi.nyummy.settings.data.dto.NotificationSettingsDTO
import com.dandi.nyummy.settings.domain.SettingsRepository
import com.dandi.nyummy.settings.entity.NotificationSettingsVO

class SettingsRepositoryImpl(
    private val notificationDataSource: NotificationSettingsDataSource,
    private val localDataSource: SettingsLocalDataSource,
    private val appPreferenceProvider: AppPreferenceProvider,
) : SettingsRepository {

    override suspend fun isBgmEnabled(): Boolean = localDataSource.isBgmEnabled()

    override suspend fun setBgmEnabled(enabled: Boolean) {
        localDataSource.setBgmEnabled(enabled)
    }

    override suspend fun getNotificationSettings(): NotificationSettingsVO =
        notificationDataSource.getNotificationSettings().toVO()

    override suspend fun updateNotificationSettings(settings: NotificationSettingsVO): NotificationSettingsVO =
        notificationDataSource.updateNotificationSettings(NotificationSettingsDTO.from(settings)).toVO()

    override suspend fun getMealTimes(): MealTimesVO = appPreferenceProvider.getMealTimes() ?: MealTimesVO.default

    override suspend fun saveMealTimes(mealTimes: MealTimesVO) {
        appPreferenceProvider.setMealTimes(mealTimes)
    }
}
