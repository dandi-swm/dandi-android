package com.dandi.nyummy.settings.presentation.mealtime

import androidx.lifecycle.viewModelScope
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.presentation.mvi.MviViewModel
import com.dandi.nyummy.settings.domain.GetMealTimesUseCase
import com.dandi.nyummy.settings.domain.SaveMealTimesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MealTimeSettingViewModel @Inject constructor(
    private val getMealTimes: GetMealTimesUseCase,
    private val saveMealTimes: SaveMealTimesUseCase,
    private val navigationHelper: NavigationHelper,
) : MviViewModel<MealTimeSettingIntent, MealTimeSettingUIState, MealTimeSettingReducerEvent>(
    MealTimeSettingUIState.empty,
) {

    override fun onIntent(intent: MealTimeSettingIntent) {
        when (intent) {
            MealTimeSettingIntent.Enter -> load()
            MealTimeSettingIntent.ClickBack -> navigationHelper.navigateToBack()
            is MealTimeSettingIntent.ClickMeal -> dispatch(MealTimeSettingReducerEvent.EditingMealChanged(intent.meal))
            is MealTimeSettingIntent.SelectMealTime -> {
                dispatch(MealTimeSettingReducerEvent.MealTimesChanged(currentState.mealTimes.with(intent.meal, intent.time)))
                dispatch(MealTimeSettingReducerEvent.EditingMealChanged(null))
            }
            MealTimeSettingIntent.DismissMealTimeSheet -> dispatch(MealTimeSettingReducerEvent.EditingMealChanged(null))
            MealTimeSettingIntent.ClickSave -> save()
        }
    }

    override fun reduce(
        state: MealTimeSettingUIState,
        event: MealTimeSettingReducerEvent,
    ): MealTimeSettingUIState = when (event) {
        is MealTimeSettingReducerEvent.Loaded -> state.copy(mealTimes = event.mealTimes, isLoaded = true)
        is MealTimeSettingReducerEvent.EditingMealChanged -> state.copy(editingMeal = event.meal)
        is MealTimeSettingReducerEvent.MealTimesChanged -> state.copy(mealTimes = event.mealTimes)
        is MealTimeSettingReducerEvent.SavingChanged -> state.copy(isSaving = event.saving)
    }

    private fun load() {
        if (currentState.isLoaded) return
        // 실패 안내와 "다시 시도"는 UseCase가 띄운다. 다시 시도하면 읽기부터 다시 한다.
        viewModelScope.launch {
            getMealTimes(onRetry = { onIntent(MealTimeSettingIntent.Enter) })
                .onSuccess { dispatch(MealTimeSettingReducerEvent.Loaded(it)) }
        }
    }

    private fun save() {
        val state = currentState
        if (state.isSaving || !state.isLoaded) return
        dispatch(MealTimeSettingReducerEvent.SavingChanged(saving = true))
        viewModelScope.launch {
            // 실패 안내와 "다시 시도"는 UseCase가 띄운다. 여기서는 저장 버튼만 다시 연다.
            saveMealTimes(state.mealTimes, onRetry = { onIntent(MealTimeSettingIntent.ClickSave) })
                .onSuccess { navigationHelper.navigateToBack() }
                .onFailure { dispatch(MealTimeSettingReducerEvent.SavingChanged(saving = false)) }
        }
    }
}
