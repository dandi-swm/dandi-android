package com.dandi.nyummy.settings.domain

import com.dandi.nyummy.common.entity.meal.MealTimesVO
import javax.inject.Inject

/** 평소 식사 시각. 정한 적이 없으면 기본값. */
class GetMealTimesUseCase @Inject constructor(
    private val repository: SettingsRepository,
) {
    suspend operator fun invoke(): MealTimesVO = repository.getMealTimes()
}
