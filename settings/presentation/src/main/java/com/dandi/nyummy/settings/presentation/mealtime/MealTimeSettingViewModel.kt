package com.dandi.nyummy.settings.presentation.mealtime

import androidx.lifecycle.viewModelScope
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.message.IconType
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
    private val messageHelper: MessageHelper,
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
        viewModelScope.launch { dispatch(MealTimeSettingReducerEvent.Loaded(getMealTimes())) }
    }

    private fun save() {
        val state = currentState
        if (state.isSaving || !state.isLoaded) return
        dispatch(MealTimeSettingReducerEvent.SavingChanged(saving = true))
        viewModelScope.launch {
            saveMealTimes(state.mealTimes)
                .onSuccess { navigationHelper.navigateToBack() }
                .onFailure {
                    dispatch(MealTimeSettingReducerEvent.SavingChanged(saving = false))
                    messageHelper.showSnackBar(
                        iconType = IconType.ERROR,
                        messageText = SAVE_FAILED_MESSAGE,
                        callToActionText = RETRY_LABEL,
                        onClickCTA = { onIntent(MealTimeSettingIntent.ClickSave) },
                    )
                }
        }
    }

    private companion object {
        const val SAVE_FAILED_MESSAGE = "식사 시간을 저장하지 못했어요"
        const val RETRY_LABEL = "다시 시도"
    }
}
