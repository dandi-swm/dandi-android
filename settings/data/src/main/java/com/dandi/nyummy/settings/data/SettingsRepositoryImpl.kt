package com.dandi.nyummy.settings.data

import com.dandi.nyummy.common.data.token.TokenProvider
import com.dandi.nyummy.common.entity.meal.MealTimesVO
import com.dandi.nyummy.settings.data.dto.MealTimeRequestDTO
import com.dandi.nyummy.settings.data.dto.PushSettingDTO
import com.dandi.nyummy.settings.domain.SettingsRepository
import com.dandi.nyummy.settings.entity.NotificationSettingsChange
import com.dandi.nyummy.settings.entity.NotificationSettingsVO
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

class SettingsRepositoryImpl(
    private val remoteDataSource: SettingsRemoteDataSource,
    private val tokenProvider: TokenProvider,
) : SettingsRepository {

    override suspend fun getNotificationSettings(): NotificationSettingsVO = remoteDataSource.getPushSetting().toVO()

    override suspend fun updateNotificationSettings(change: NotificationSettingsChange) {
        remoteDataSource.updatePushSetting(PushSettingDTO.from(change))
    }

    override suspend fun getMealTimes(): MealTimesVO = remoteDataSource.getMyProfile().toMealTimesVO()

    override suspend fun saveMealTimes(mealTimes: MealTimesVO) {
        remoteDataSource.updateMealTime(MealTimeRequestDTO.from(mealTimes))
    }

    override suspend fun withdraw() {
        remoteDataSource.withdraw()
        // 서버가 탈퇴 전에 발급한 토큰을 모두 무효로 만든다. 남겨 두면 다음 실행 때 없는 계정으로 로그인된 채 시작한다.
        // 탈퇴가 끝난 뒤 화면을 나가 취소돼도 지운다. 취소된 코루틴에서는 suspend 저장도 바로 취소되기 때문이다.
        withContext(NonCancellable) { tokenProvider.clear() }
    }
}
