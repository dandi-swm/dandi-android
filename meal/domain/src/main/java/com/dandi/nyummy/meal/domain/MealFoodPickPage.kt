package com.dandi.nyummy.meal.domain

import com.dandi.nyummy.common.domain.navigation.NavRoute
import com.dandi.nyummy.common.domain.navigation.Page

/**
 * 이미 기록한 식사에서 찾은 음식을 고르는 화면. 기록 화면의 확인 대기 식사를 누르거나, 그런 식사의 링크로 들어온다.
 * 뒤로 가면 기록 화면이다. 기록 직후의 고르기는 식사 기록 화면([MealRecordPage]) 안의 한 단계라 이 화면을 쓰지 않는다.
 */
data class MealFoodPickPage(val mealId: Long) : Page {

    override fun toRoute(): NavRoute = NavRoute(PATH, mapOf(ARG_MEAL_ID to mealId.toString()))

    companion object {
        const val ARG_MEAL_ID = "mealId"
        const val PATH = "/meal/foods/{$ARG_MEAL_ID}"
    }
}
