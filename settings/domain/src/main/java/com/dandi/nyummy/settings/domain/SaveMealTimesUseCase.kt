package com.dandi.nyummy.settings.domain

import com.dandi.nyummy.common.entity.meal.MealTimesVO
import kotlin.coroutines.cancellation.CancellationException
import javax.inject.Inject

/** 평소 식사 시각 저장. 기기 저장소 쓰기가 실패하면 화면이 알린다. */
class SaveMealTimesUseCase @Inject constructor(
    private val repository: SettingsRepository,
) {
    suspend operator fun invoke(mealTimes: MealTimesVO): Result<Unit> = try {
        Result.success(repository.saveMealTimes(mealTimes))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }
}
