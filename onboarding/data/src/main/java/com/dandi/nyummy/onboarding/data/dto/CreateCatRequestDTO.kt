package com.dandi.nyummy.onboarding.data.dto

import com.dandi.nyummy.common.entity.meal.MealTimeVO
import com.dandi.nyummy.common.entity.meal.MealTimesVO
import kotlinx.serialization.Serializable

/**
 * 고양이 생성 요청입니다.
 *
 * @property name 사용자가 지어 준 고양이 이름 (앞뒤 공백 제거)
 * @property breakfastHour 평소 아침 시각(0~23). 안 먹는 끼니는 null로 두어 그 끼니 알림을 받지 않는다.
 * @property lunchHour 평소 점심 시각(0~23). 안 먹으면 null
 * @property dinnerHour 평소 저녁 시각(0~23). 안 먹으면 null
 */
@Serializable
data class CreateCatRequestDTO(
    val name: String,
    val breakfastHour: Int? = null,
    val lunchHour: Int? = null,
    val dinnerHour: Int? = null,
) {
    companion object {
        fun of(name: String, mealTimes: MealTimesVO) = CreateCatRequestDTO(
            name = name,
            breakfastHour = mealTimes.breakfast.hourOrNull(),
            lunchHour = mealTimes.lunch.hourOrNull(),
            dinnerHour = mealTimes.dinner.hourOrNull(),
        )

        private fun MealTimeVO.hourOrNull(): Int? = if (isSkipped) null else hour
    }
}
