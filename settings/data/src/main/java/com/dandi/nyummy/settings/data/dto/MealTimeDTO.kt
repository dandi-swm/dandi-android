package com.dandi.nyummy.settings.data.dto

import com.dandi.nyummy.common.entity.meal.MealTimeVO
import com.dandi.nyummy.common.entity.meal.MealTimesVO
import kotlinx.serialization.Serializable

/**
 * 내 프로필(`GET /api/v1/users/me`) 중 설정이 쓰는 평소 식사 시각입니다.
 *
 * @property breakfastHour 평소 아침 시각(0~23). 안 먹는 끼니는 null
 * @property lunchHour 평소 점심 시각(0~23). 안 먹으면 null
 * @property dinnerHour 평소 저녁 시각(0~23). 안 먹으면 null
 */
@Serializable
data class UserProfileDTO(
    val breakfastHour: Int? = null,
    val lunchHour: Int? = null,
    val dinnerHour: Int? = null,
) {
    /**
     * 서버는 안 먹는 끼니를 null로 준다. 그 끼니는 건너뛴 것으로 두고,
     * 다시 켤 때 보일 시각은 서버에 남지 않으므로 기본값으로 채운다.
     */
    fun toMealTimesVO(): MealTimesVO = MealTimesVO(
        breakfast = breakfastHour.toMealTime(MealTimesVO.DefaultBreakfast),
        lunch = lunchHour.toMealTime(MealTimesVO.DefaultLunch),
        dinner = dinnerHour.toMealTime(MealTimesVO.DefaultDinner),
    )

    private fun Int?.toMealTime(default: MealTimeVO): MealTimeVO =
        this?.let { MealTimeVO(hour = it.coerceIn(0, LAST_HOUR)) } ?: default.copy(isSkipped = true)
}

// @Serializable 클래스의 companion은 직렬화기를 찾는 데 쓰여 private으로 두면 안 된다. 그래서 상수는 밖에 둔다.
private const val LAST_HOUR = 23

/**
 * 평소 식사 시각 변경 요청(`PUT /api/v1/users/me/meal-time`)입니다. 세 끼를 한 번에 바꾼다.
 *
 * @property breakfastHour 평소 아침 시각(0~23). 안 먹는 끼니는 null로 두어 그 끼니 알림을 받지 않는다.
 * @property lunchHour 평소 점심 시각(0~23). 안 먹으면 null
 * @property dinnerHour 평소 저녁 시각(0~23). 안 먹으면 null
 */
@Serializable
data class MealTimeRequestDTO(
    val breakfastHour: Int? = null,
    val lunchHour: Int? = null,
    val dinnerHour: Int? = null,
) {
    companion object {
        fun from(mealTimes: MealTimesVO) = MealTimeRequestDTO(
            breakfastHour = mealTimes.breakfast.hourOrNull(),
            lunchHour = mealTimes.lunch.hourOrNull(),
            dinnerHour = mealTimes.dinner.hourOrNull(),
        )

        private fun MealTimeVO.hourOrNull(): Int? = if (isSkipped) null else hour
    }
}
