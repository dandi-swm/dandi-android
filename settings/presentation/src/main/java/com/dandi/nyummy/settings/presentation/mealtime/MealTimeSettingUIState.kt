package com.dandi.nyummy.settings.presentation.mealtime

import com.dandi.nyummy.common.entity.meal.Meal
import com.dandi.nyummy.common.entity.meal.MealTimesVO
import com.dandi.nyummy.common.presentation.mvi.UiState

/**
 * 식사 시간 UI 상태. 저장을 눌러야 기기에 저장한다.
 *
 * @property editingMeal 시간 시트를 연 끼니. 닫혀 있으면 null.
 * @property isLoaded 저장된 값을 읽었다. 읽기 전에는 줄을 그리지 않는다.
 * @property isSaving 저장 중. 저장 버튼을 막는다.
 */
data class MealTimeSettingUIState(
    val mealTimes: MealTimesVO = MealTimesVO.default,
    val editingMeal: Meal? = null,
    val isLoaded: Boolean = false,
    val isSaving: Boolean = false,
) : UiState {

    companion object {
        val empty = MealTimeSettingUIState()
    }
}
