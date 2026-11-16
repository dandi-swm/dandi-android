package com.dandi.nyummy.meal.presentation.foodpick

import com.dandi.nyummy.common.presentation.mvi.MviIntent

sealed interface MealFoodPickIntent : MviIntent {

    /** 화면에 들어왔습니다. */
    data class Enter(val mealId: Long) : MealFoodPickIntent

    /** 불러오지 못한 뒤 `다시 불러오기`를 눌렀습니다. */
    data object ClickRetry : MealFoodPickIntent

    /** 닫기나 뒤로 가기를 눌렀습니다. 기록을 지우지 않고 돌아갑니다. */
    data object ClickClose : MealFoodPickIntent

    /** 음식 줄을 눌러 체크를 바꿨습니다. */
    data class ToggleFood(val key: Int) : MealFoodPickIntent

    /** `음식 추가`를 눌렀습니다. */
    data object OpenNameInput : MealFoodPickIntent

    /** 추가할 음식 이름을 고쳤습니다. */
    data class ChangeNameInput(val text: String) : MealFoodPickIntent

    /** 이름 입력 줄의 `추가`를 눌렀습니다. */
    data object ConfirmNameInput : MealFoodPickIntent

    /** `N개 가져가기`를 눌렀습니다. */
    data object ClickSubmit : MealFoodPickIntent
}
