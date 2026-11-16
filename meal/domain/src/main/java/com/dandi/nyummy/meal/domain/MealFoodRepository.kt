package com.dandi.nyummy.meal.domain

import com.dandi.nyummy.meal.entity.FoundFoodVO
import com.dandi.nyummy.meal.entity.MealFoodConfirmVO

/** 기록한 식사에서 찾은 음식을 확인하는 흐름(조회, 고른 목록 보내기, 기록 취소). */
interface MealFoodRepository {

    /** 식사에서 찾은 음식 목록과, 아직 사용자의 확인을 기다리는지 여부를 조회한다. */
    suspend fun getFoundFoods(mealId: Long): MealFoodConfirmVO

    /**
     * 사용자가 고른 음식 목록 전체를 보낸다. 서버는 받은 목록으로 바로 영양 계산을 시작하고, 목록에 없는 기존 줄은 지운다.
     * 맨 위 줄이 대표 음식이다.
     */
    suspend fun submitFoods(mealId: Long, foods: List<FoundFoodVO>)

    /** 기록 직후의 식사를 취소한다. 하루 기록 횟수와 보상 상자는 되돌아가지 않는다. */
    suspend fun cancelMeal(mealId: Long)
}
