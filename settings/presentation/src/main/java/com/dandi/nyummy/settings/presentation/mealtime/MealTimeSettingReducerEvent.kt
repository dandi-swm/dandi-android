package com.dandi.nyummy.settings.presentation.mealtime

import com.dandi.nyummy.common.entity.meal.Meal
import com.dandi.nyummy.common.entity.meal.MealTimesVO
import com.dandi.nyummy.common.presentation.mvi.ReducerEvent

sealed interface MealTimeSettingReducerEvent : ReducerEvent {
    data class Loaded(val mealTimes: MealTimesVO) : MealTimeSettingReducerEvent
    data class EditingMealChanged(val meal: Meal?) : MealTimeSettingReducerEvent
    data class MealTimesChanged(val mealTimes: MealTimesVO) : MealTimeSettingReducerEvent
    data class SavingChanged(val saving: Boolean) : MealTimeSettingReducerEvent
}
