package com.swm.dandi.meal.presentation

import androidx.lifecycle.viewModelScope
import com.swm.dandi.common.domain.helper.NavigationHelper
import com.swm.dandi.common.presentation.mvi.MviViewModel
import com.swm.dandi.meal.domain.MealUseCase
import com.swm.dandi.meal.domain.NewFoodPage
import com.swm.dandi.meal.entity.CreateMealRequestVO
import com.swm.dandi.meal.entity.MealInputModeVO
import com.swm.dandi.meal.entity.PreviousFoodVO
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class PreviousMealViewModel @Inject constructor(
    private val mealUseCase: MealUseCase,
    private val navigationHelper: NavigationHelper,
) : MviViewModel<PreviousMealIntent, PreviousMealUIState, PreviousMealReducerEvent>(
    PreviousMealUIState.empty,
) {

    init {
        loadPreviousMealPage()
    }

    override fun onIntent(intent: PreviousMealIntent) {
        when (intent) {
            PreviousMealIntent.ClickBack -> navigationHelper.navigateToBack()
            PreviousMealIntent.ClickNewFood -> navigationHelper.navigateTo(NewFoodPage)
            is PreviousMealIntent.SelectFood -> {
                dispatch(PreviousMealReducerEvent.FoodSelected(intent.foodId))
            }

            is PreviousMealIntent.QuickRecord -> {
                dispatch(PreviousMealReducerEvent.FoodSelected(intent.foodId))
                submit(foodId = intent.foodId)
            }

            is PreviousMealIntent.SelectEatenTime -> {
                dispatch(PreviousMealReducerEvent.EatenTimeSelected(intent.hour, intent.minute))
            }

            PreviousMealIntent.SelectCurrentTime -> selectCurrentTime()
            PreviousMealIntent.Submit -> submit(foodId = currentState.selectedFoodId)
            PreviousMealIntent.ClearSubmitResult -> dispatch(PreviousMealReducerEvent.SubmitResultCleared)
        }
    }

    override fun reduce(
        state: PreviousMealUIState,
        event: PreviousMealReducerEvent,
    ): PreviousMealUIState = when (event) {
        PreviousMealReducerEvent.LoadStarted -> state.copy(
            isLoading = true,
            loadErrorMessage = "",
            submitResultMessage = "",
        )

        is PreviousMealReducerEvent.PreviousMealPageLoaded -> state.copy(
            foods = event.foods,
            isLoading = false,
            loadErrorMessage = "",
            selectedFoodId = state.selectedFoodId.takeIf { selectedId ->
                event.foods.any { it.id == selectedId }
            }.orEmpty(),
        )

        is PreviousMealReducerEvent.LoadFailed -> state.copy(
            isLoading = false,
            loadErrorMessage = event.message,
        )

        is PreviousMealReducerEvent.FoodSelected -> state.copy(
            selectedFoodId = event.foodId,
            selectedFoodError = "",
            submitResultMessage = "",
        )

        is PreviousMealReducerEvent.EatenTimeSelected -> state.copy(
            eatenHour = event.hour,
            eatenMinute = event.minute,
            eatenTimeError = "",
            submitResultMessage = "",
        )

        is PreviousMealReducerEvent.ValidationFailed -> state.copy(
            selectedFoodError = event.selectedFoodError,
            eatenTimeError = event.eatenTimeError,
            submitResultMessage = "",
            isSubmitting = false,
        )

        PreviousMealReducerEvent.SubmitStarted -> state.copy(
            selectedFoodError = "",
            eatenTimeError = "",
            submitResultMessage = "식사 기록을 저장하는 중입니다.",
            isSubmitting = true,
        )

        is PreviousMealReducerEvent.SubmitSucceeded -> state.copy(
            selectedFoodError = "",
            eatenTimeError = "",
            submitResultMessage = "${event.foodName} 기록을 저장했습니다.",
            isSubmitting = false,
        )

        is PreviousMealReducerEvent.SubmitFailed -> state.copy(
            submitResultMessage = event.message,
            isSubmitting = false,
        )

        PreviousMealReducerEvent.SubmitResultCleared -> state.copy(submitResultMessage = "")
    }

    private fun loadPreviousMealPage() {
        dispatch(PreviousMealReducerEvent.LoadStarted)
        viewModelScope.launch {
            mealUseCase.getPreviousMealPage()
                .onSuccess { page ->
                    dispatch(
                        PreviousMealReducerEvent.PreviousMealPageLoaded(
                            foods = page.previousFoods.map { it.toUiState() }.toPersistentList(),
                        )
                    )
                }
                .onFailure {
                    dispatch(PreviousMealReducerEvent.LoadFailed("이전 음식 목록을 불러오지 못했습니다."))
                }
        }
    }

    private fun selectCurrentTime() {
        val calendar = Calendar.getInstance()
        dispatch(
            PreviousMealReducerEvent.EatenTimeSelected(
                hour = calendar.get(Calendar.HOUR_OF_DAY),
                minute = calendar.get(Calendar.MINUTE),
            )
        )
    }

    private fun submit(foodId: String) {
        val selectedFoodError = if (foodId.isEmpty()) {
            "이전 음식을 선택해 주세요."
        } else {
            ""
        }
        val eatenTimeError = if (!currentState.hasEatenTime) {
            "먹은 시간을 선택해 주세요."
        } else {
            ""
        }
        if (selectedFoodError.isNotEmpty() || eatenTimeError.isNotEmpty()) {
            dispatch(
                PreviousMealReducerEvent.ValidationFailed(
                    selectedFoodError = selectedFoodError,
                    eatenTimeError = eatenTimeError,
                )
            )
            return
        }

        val foodName = currentState.foods.firstOrNull { it.id == foodId }?.name.orEmpty()
        val eatenAt = formatEatenAt(
            hour = currentState.eatenHour ?: return,
            minute = currentState.eatenMinute ?: return,
        )
        dispatch(PreviousMealReducerEvent.SubmitStarted)
        viewModelScope.launch {
            val request = CreateMealRequestVO(
                inputMode = MealInputModeVO.PREVIOUS_FOOD,
                foodHistoryId = foodId,
                eatenAt = eatenAt,
            )
            mealUseCase.createMeal(request)
                .onSuccess { meal ->
                    dispatch(PreviousMealReducerEvent.SubmitSucceeded(meal.displayName.ifEmpty { foodName }))
                }
                .onFailure {
                    dispatch(PreviousMealReducerEvent.SubmitFailed("식사 기록을 저장하지 못했습니다."))
                }
        }
    }

    private fun formatEatenAt(hour: Int, minute: Int): String {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return SimpleDateFormat(EATEN_AT_PATTERN, Locale.US).format(calendar.time)
    }

    private fun PreviousFoodVO.toUiState(): PreviousFoodUiState =
        PreviousFoodUiState(
            id = foodHistoryId,
            name = displayName,
            iconImageUrl = iconImageUrl.ifEmpty { foodHistoryId.toDefaultIconImageUrl() },
            recordCountLabel = "${recordCount}회 기록",
            lastRecordedLabel = lastRecordedAt.toLastRecordedLabel(),
        )

    private fun String.toLastRecordedLabel(): String =
        if (isBlank()) {
            "마지막 기록 없음"
        } else {
            "마지막: ${take(DATE_LENGTH).replace("-", ".")}"
        }

    private fun String.toDefaultIconImageUrl(): String =
        when (this) {
            "food_rice" -> "dandi://drawable/meal_ic_food_rice_pixel"
            "food_sandwich" -> "dandi://drawable/meal_ic_food_sandwich_pixel"
            "food_salad" -> "dandi://drawable/meal_ic_food_salad_pixel"
            "food_sushi" -> "dandi://drawable/meal_ic_food_sushi_pixel"
            else -> ""
        }

    private companion object {
        const val EATEN_AT_PATTERN = "yyyy-MM-dd'T'HH:mm:ssXXX"
        const val DATE_LENGTH = 10
    }
}
