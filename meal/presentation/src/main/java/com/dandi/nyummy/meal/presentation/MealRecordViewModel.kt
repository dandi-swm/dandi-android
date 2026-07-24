package com.dandi.nyummy.meal.presentation

import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.presentation.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MealRecordViewModel @Inject constructor(
    private val navigationHelper: NavigationHelper,
) : MviViewModel<MealRecordIntent, MealRecordUIState, MealRecordReducerEvent>(MealRecordUIState.empty) {

    override fun onIntent(intent: MealRecordIntent) {
        when (intent) {
            is MealRecordIntent.ChangeDescription ->
                dispatch(MealRecordReducerEvent.DescriptionChanged(intent.text))

            is MealRecordIntent.SelectFoodIcon ->
                dispatch(MealRecordReducerEvent.FoodIconSelected(intent.iconId))

            MealRecordIntent.ClickChangePhoto -> Unit // TODO: 사진 촬영/선택 플로우 연결 (백엔드·카메라 미구현)

            MealRecordIntent.ClickViewAllIcons -> Unit // TODO: 음식 아이콘 전체보기 화면 연결

            MealRecordIntent.ClickSave -> Unit // TODO: 식사 기록 저장 API 연동 (백엔드 미구현)

            MealRecordIntent.ClickBack -> navigationHelper.navigateToBack()
        }
    }

    override fun reduce(state: MealRecordUIState, event: MealRecordReducerEvent): MealRecordUIState =
        when (event) {
            is MealRecordReducerEvent.DescriptionChanged ->
                state.copy(record = state.record.copy(description = event.text))

            is MealRecordReducerEvent.FoodIconSelected ->
                state.copy(record = state.record.copy(foodIconId = event.iconId))
        }
}
