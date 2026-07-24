package com.dandi.nyummy.meal.presentation

import com.dandi.nyummy.common.presentation.mvi.ReducerEvent

/** 식사 기록 화면의 상태를 변이시키는 내부 이벤트입니다. */
sealed interface MealRecordReducerEvent : ReducerEvent {

    data class DescriptionChanged(val text: String) : MealRecordReducerEvent

    data class FoodIconSelected(val iconId: String) : MealRecordReducerEvent
}
