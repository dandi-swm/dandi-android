package com.dandi.nyummy.meal.presentation

import com.dandi.nyummy.common.presentation.component.NyummyPhotoPickerState
import com.dandi.nyummy.common.presentation.mvi.UiState
import com.dandi.nyummy.meal.entity.MealRecordVO
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

/**
 * 식사 기록 화면의 UI 상태입니다.
 *
 * 초기 상태는 [MealRecordVO.empty] 그대로의 빈 기록이며, 필수 입력(사진·설명·아이콘)이
 * 모두 채워졌을 때만 [isSaveEnabled] 가 참이 됩니다.
 */
data class MealRecordUIState(
    val record: MealRecordVO = MealRecordVO.empty,
    val foodIcons: ImmutableList<MealFoodIcon> = MealFoodIcon.entries.toImmutableList(),
) : UiState {

    val photoState: NyummyPhotoPickerState
        get() = if (record.hasPhoto) NyummyPhotoPickerState.Selected else NyummyPhotoPickerState.Empty

    val isSaveEnabled: Boolean
        get() = record.hasPhoto && record.description.isNotBlank() && record.foodIconId.isNotEmpty()

    companion object {
        val empty = MealRecordUIState()
    }
}
