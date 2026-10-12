package com.dandi.nyummy.settings.data.dto

import com.dandi.nyummy.common.entity.meal.MealTimeVO
import com.dandi.nyummy.common.entity.meal.MealTimesVO
import org.junit.Assert.assertEquals
import org.junit.Test

class MealTimeDTOTest {

    @Test
    fun `서버의 시각을 그대로 옮긴다`() {
        val mealTimes = UserProfileDTO(breakfastHour = 7, lunchHour = 13, dinnerHour = 19).toMealTimesVO()

        assertEquals(
            MealTimesVO(breakfast = MealTimeVO(hour = 7), lunch = MealTimeVO(hour = 13), dinner = MealTimeVO(hour = 19)),
            mealTimes,
        )
    }

    @Test
    fun `서버가 비워 둔 끼니는 건너뛴 것으로 두고 시각은 기본값으로 채운다`() {
        val mealTimes = UserProfileDTO(breakfastHour = null, lunchHour = 12, dinnerHour = null).toMealTimesVO()

        assertEquals(MealTimesVO.DefaultBreakfast.copy(isSkipped = true), mealTimes.breakfast)
        assertEquals(MealTimeVO(hour = 12), mealTimes.lunch)
        assertEquals(MealTimesVO.DefaultDinner.copy(isSkipped = true), mealTimes.dinner)
    }

    @Test
    fun `범위를 벗어난 시각은 하루 안으로 맞춘다`() {
        val mealTimes = UserProfileDTO(breakfastHour = -1, lunchHour = 12, dinnerHour = 24).toMealTimesVO()

        assertEquals(MealTimeVO(hour = 0), mealTimes.breakfast)
        assertEquals(MealTimeVO(hour = 23), mealTimes.dinner)
    }

    @Test
    fun `저장 요청은 건너뛴 끼니를 null 로 둔다`() {
        val request = MealTimeRequestDTO.from(
            MealTimesVO(breakfast = MealTimeVO(hour = 9, isSkipped = true), lunch = MealTimeVO(hour = 12)),
        )

        assertEquals(MealTimeRequestDTO(breakfastHour = null, lunchHour = 12, dinnerHour = 18), request)
    }
}
