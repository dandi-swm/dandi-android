package com.dandi.nyummy.meal.presentation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

/**
 * 식사 기록 화면에서 고를 수 있는 음식 픽셀 아이콘 카탈로그입니다.
 *
 * [id] 는 [com.dandi.nyummy.meal.entity.MealRecordVO.foodIconId] 로 저장되는 식별자입니다.
 * 아이콘 목록이 서버에서 내려오게 되면 이 enum 은 데이터 기반 모델로 대체됩니다.
 */
enum class MealFoodIcon(
    val id: String,
    @DrawableRes val iconRes: Int,
    @StringRes val labelRes: Int,
) {
    Salad("salad", R.drawable.ic_meal_food_salad, R.string.meal_record_icon_salad),
    Rice("rice", R.drawable.ic_meal_food_rice, R.string.meal_record_icon_rice),
    Sandwich("sandwich", R.drawable.ic_meal_food_sandwich, R.string.meal_record_icon_sandwich),
    Sushi("sushi", R.drawable.ic_meal_food_sushi, R.string.meal_record_icon_sushi),
}
