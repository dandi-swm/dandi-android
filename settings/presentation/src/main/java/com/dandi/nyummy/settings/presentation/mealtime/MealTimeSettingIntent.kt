package com.dandi.nyummy.settings.presentation.mealtime

import com.dandi.nyummy.common.entity.meal.Meal
import com.dandi.nyummy.common.entity.meal.MealTimeVO
import com.dandi.nyummy.common.presentation.mvi.MviIntent

/** 식사 시간 화면에서 발생하는 사용자 의도. */
sealed interface MealTimeSettingIntent : MviIntent {

    /** 화면에 들어왔다. 저장된 식사 시각을 읽는다. */
    data object Enter : MealTimeSettingIntent

    data object ClickBack : MealTimeSettingIntent

    /** 끼니 줄을 눌렀다. 그 끼니의 시간 시트를 연다. */
    data class ClickMeal(val meal: Meal) : MealTimeSettingIntent

    /** 시간 시트에서 시각을 고르거나 "안 먹어요"를 골랐다. */
    data class SelectMealTime(val meal: Meal, val time: MealTimeVO) : MealTimeSettingIntent

    data object DismissMealTimeSheet : MealTimeSettingIntent

    data object ClickSave : MealTimeSettingIntent
}
