package com.dandi.nyummy.history.entity

/**
 * 직접 적는 영양 값입니다.
 */
data class MealFoodNutritionVO(
    val calorieKcal: Int = 0,
    val carbohydrateGram: Int = 0,
    val proteinGram: Int = 0,
    val fatGram: Int = 0,
)

/**
 * 음식 한 줄에서 바꿀 항목입니다. null인 항목은 그대로 둡니다.
 *
 * @property name 새 이름. 바꾸면 (영양을 고정하지 않은 줄은) 서버가 영양을 다시 구합니다.
 * @property portion 새 인분
 * @property nutrition 직접 적은 영양. 보내면 그 줄의 영양이 이 값으로 고정됩니다.
 * @property iconId 새 음식 아이콘 번호
 */
data class MealFoodChangeVO(
    val name: String? = null,
    val portion: Double? = null,
    val nutrition: MealFoodNutritionVO? = null,
    val iconId: Long? = null,
) {
    /** 바꿀 항목이 하나도 없는지 여부. 서버는 빈 요청을 받지 않습니다. */
    val isEmpty: Boolean
        get() = name == null && portion == null && nutrition == null && iconId == null
}

/**
 * 음식 목록 전체를 보낼 때의 한 줄입니다. 순서를 바꾸거나 음식을 추가할 때 씁니다.
 *
 * @property mealFoodId 음식 줄 ID. 새로 추가하는 줄은 [NEW_ID]입니다.
 */
data class MealFoodLineVO(
    val mealFoodId: Long = NEW_ID,
    val name: String = "",
    val portion: Double = MealFoodVO.DEFAULT_PORTION,
) {
    companion object {
        /** 아직 서버에 없는 줄의 ID. 서버 ID는 1부터 시작한다. */
        const val NEW_ID = 0L
    }
}
