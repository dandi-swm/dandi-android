package com.swm.dandi.meal.presentation

import com.swm.dandi.common.presentation.mvi.UiState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class PreviousFoodUiState(
    val id: String,
    val name: String,
    val iconImageUrl: String,
    val recordCountLabel: String,
    val lastRecordedLabel: String,
)

data class PreviousMealUIState(
    val foods: ImmutableList<PreviousFoodUiState> = persistentListOf(),
    val isLoading: Boolean = true,
    val loadErrorMessage: String = "",
    val isSubmitting: Boolean = false,
    val selectedFoodId: String = "",
    val eatenHour: Int? = 12,
    val eatenMinute: Int? = 30,
    val selectedFoodError: String = "",
    val eatenTimeError: String = "",
    val submitResultMessage: String = "",
) : UiState {
    val hasEatenTime: Boolean
        get() = eatenHour != null && eatenMinute != null

    val eatenTimeLabel: String
        get() = if (eatenHour == null || eatenMinute == null) {
            ""
        } else {
            "${eatenHour.toTwoDigits()}:${eatenMinute.toTwoDigits()}"
        }

    val mealSessionLabel: String
        get() = when (eatenHour) {
            null -> "시간 선택 필요"
            in 5..10 -> "아침 식사"
            in 11..15 -> "점심 식사"
            in 16..20 -> "저녁 식사"
            else -> "간식"
        }

    val selectedFoodName: String
        get() = foods.firstOrNull { it.id == selectedFoodId }?.name.orEmpty()

    companion object {
        val empty: PreviousMealUIState = PreviousMealUIState()
    }
}

private fun Int.toTwoDigits(): String = toString().padStart(length = 2, padChar = '0')
