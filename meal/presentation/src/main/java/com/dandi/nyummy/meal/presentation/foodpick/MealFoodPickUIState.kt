package com.dandi.nyummy.meal.presentation.foodpick

import com.dandi.nyummy.common.presentation.mvi.UiState
import com.dandi.nyummy.meal.presentation.model.FoodPickDraft

/**
 * 기록 탭에서 들어온 찾은 음식 고르기 화면의 상태.
 *
 * @property isLoading 찾은 음식을 불러오는 중인지 여부
 * @property isLoadFailed 불러오지 못해 다시 시도를 보여 주는지 여부
 * @property draft 사용자가 고치는 음식 목록
 * @property isSubmitting 고른 목록을 보내는 중인지 여부. 그동안 고치기와 가져가기를 막는다.
 */
data class MealFoodPickUIState(
    val isLoading: Boolean = true,
    val isLoadFailed: Boolean = false,
    val draft: FoodPickDraft = FoodPickDraft(),
    val isSubmitting: Boolean = false,
) : UiState
