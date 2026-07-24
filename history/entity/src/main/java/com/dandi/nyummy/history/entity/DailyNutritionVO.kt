package com.dandi.nyummy.history.entity

import kotlinx.serialization.Serializable

/**
 * 영양소 1종의 하루 진행 상황입니다.
 *
 * 퍼센트 등 파생 수치는 이중 진실을 막기 위해 저장하지 않고 presentation 에서 계산합니다.
 *
 * @property dailyGram 하루 누적 섭취량(g)
 * @property goalGram 하루 목표량(g)
 */
@Serializable
data class NutrientProgressVO(
    val dailyGram: Int = 0,
    val goalGram: Int = 0,
)

/**
 * 선택한 날짜의 하루 영양 요약입니다.
 *
 * @property currentCalorieKcal 하루 누적 열량(kcal)
 * @property targetCalorieKcal 하루 목표 열량(kcal)
 */
@Serializable
data class DailyNutritionVO(
    val currentCalorieKcal: Int = 0,
    val targetCalorieKcal: Int = 0,
    val carbohydrate: NutrientProgressVO = NutrientProgressVO(),
    val protein: NutrientProgressVO = NutrientProgressVO(),
    val fat: NutrientProgressVO = NutrientProgressVO(),
) {

    companion object {
        val empty = DailyNutritionVO()
    }
}
