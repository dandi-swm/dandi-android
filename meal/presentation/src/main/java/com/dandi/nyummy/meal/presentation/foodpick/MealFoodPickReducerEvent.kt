package com.dandi.nyummy.meal.presentation.foodpick

import com.dandi.nyummy.common.presentation.mvi.ReducerEvent
import com.dandi.nyummy.meal.entity.FoundFoodVO

sealed interface MealFoodPickReducerEvent : ReducerEvent {

    /** 찾은 음식을 불러오기 시작했습니다. */
    data object LoadStarted : MealFoodPickReducerEvent

    /** 찾은 음식을 불러왔습니다. */
    data class Loaded(val foods: List<FoundFoodVO>) : MealFoodPickReducerEvent

    /** 찾은 음식을 불러오지 못했습니다. */
    data object LoadFailed : MealFoodPickReducerEvent

    /** 음식 줄의 체크가 바뀌었습니다. */
    data class FoodToggled(val key: Int) : MealFoodPickReducerEvent

    /** 이름 입력 줄이 열렸습니다. */
    data object NameInputOpened : MealFoodPickReducerEvent

    /** 추가할 음식 이름이 바뀌었습니다. */
    data class NameInputChanged(val text: String) : MealFoodPickReducerEvent

    /** 입력한 이름이 목록에 추가됐습니다. */
    data object NameInputConfirmed : MealFoodPickReducerEvent

    /** 고른 목록을 보내기 시작했습니다. */
    data object SubmitStarted : MealFoodPickReducerEvent

    /** 고른 목록을 보내지 못했습니다. 화면에 머뭅니다. */
    data object SubmitFailed : MealFoodPickReducerEvent
}
