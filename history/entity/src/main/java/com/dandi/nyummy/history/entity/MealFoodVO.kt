package com.dandi.nyummy.history.entity

import kotlinx.serialization.Serializable

/**
 * 식사에 든 음식 한 줄입니다. 맨 위 줄이 대표 음식입니다.
 *
 * @property mealFoodId 음식 줄 ID
 * @property name 음식 이름
 * @property portion 인분 (0.5 ~ 5.0, 0.5 단위)
 * @property hasNutrition 영양이 계산됐는지 여부. 계산 전이면 아래 영양 값은 0입니다.
 * @property calorieKcal 열량(kcal). 인분을 반영한 값입니다.
 * @property carbohydrateGram 탄수화물(g)
 * @property proteinGram 단백질(g)
 * @property fatGram 지방(g)
 * @property isNutritionFixed 사용자가 영양을 직접 적어 고정한 줄인지 여부. 고정한 줄은 이름을 바꿔도 다시 계산되지 않습니다.
 * @property foodIconId 음식 픽셀 아이콘 식별자. 계산 전이면 빈 값입니다.
 */
@Serializable
data class MealFoodVO(
    val mealFoodId: Long = 0L,
    val name: String = "",
    val portion: Double = DEFAULT_PORTION,
    val hasNutrition: Boolean = false,
    val calorieKcal: Int = 0,
    val carbohydrateGram: Int = 0,
    val proteinGram: Int = 0,
    val fatGram: Int = 0,
    val isNutritionFixed: Boolean = false,
    val foodIconId: String = "",
) {
    companion object {
        const val DEFAULT_PORTION = 1.0
        const val MIN_PORTION = 0.5
        const val MAX_PORTION = 5.0
        const val PORTION_STEP = 0.5

        /** 음식 이름 최대 길이. */
        const val MAX_NAME_LENGTH = 30

        /** 식사 하나에 담을 수 있는 음식 줄 수. */
        const val MAX_FOODS = 10

        const val MAX_CALORIE = 9999
        const val MAX_NUTRIENT = 999
    }
}
