package com.dandi.nyummy.meal.presentation

import com.dandi.nyummy.common.presentation.mvi.MviIntent

/** 식사 기록 화면에서 발생하는 사용자 입력입니다. */
sealed interface MealRecordIntent : MviIntent {

    /** 음식 설명 입력값이 바뀌었습니다. */
    data class ChangeDescription(val text: String) : MealRecordIntent

    /** 음식 아이콘 하나를 선택했습니다. */
    data class SelectFoodIcon(val iconId: String) : MealRecordIntent

    /** `사진 바꾸기`/`사진 선택` 버튼을 눌렀습니다. */
    data object ClickChangePhoto : MealRecordIntent

    /** 음식 아이콘 `전체보기` 버튼을 눌렀습니다. */
    data object ClickViewAllIcons : MealRecordIntent

    /** `식사 기록하기` 버튼을 눌렀습니다. */
    data object ClickSave : MealRecordIntent

    /** 뒤로 가기 버튼을 눌렀습니다. */
    data object ClickBack : MealRecordIntent
}
