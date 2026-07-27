package com.dandi.nyummy.history.presentation

import com.dandi.nyummy.common.presentation.mvi.MviIntent
import com.dandi.nyummy.history.entity.HistoryDateVO

/** 히스토리 화면에서 발생하는 사용자 입력입니다. */
sealed interface HistoryIntent : MviIntent {

    /** 캘린더 헤더의 이전 달 버튼을 눌렀습니다. */
    data object ClickPreviousMonth : HistoryIntent

    /** 캘린더 헤더의 다음 달 버튼을 눌렀습니다. */
    data object ClickNextMonth : HistoryIntent

    /** 캘린더에서 날짜 하나를 선택했습니다. */
    data class SelectDate(val date: HistoryDateVO) : HistoryIntent

    /** `하루 영양 현황` 카드의 접기/펼치기를 눌렀습니다. */
    data object ToggleNutritionSummary : HistoryIntent

    /** 식사 목록에서 기록 하나를 눌렀습니다. */
    data class ClickMeal(val mealId: String) : HistoryIntent

    /** 식사 상세 오버레이의 닫기(또는 바깥 영역)를 눌렀습니다. */
    data object DismissMealDetail : HistoryIntent

    /** 식사 상세의 `이름 수정` 버튼을 눌렀습니다. */
    data object ClickEditMealName : HistoryIntent

    /** 이름 수정 다이얼로그의 입력값이 바뀌었습니다. */
    data class ChangeMealNameDraft(val text: String) : HistoryIntent

    /** 이름 수정 다이얼로그에서 저장을 눌렀습니다. */
    data object ConfirmEditMealName : HistoryIntent

    /** 이름 수정 다이얼로그에서 취소를 눌렀습니다. */
    data object CancelEditMealName : HistoryIntent

    /** 식사 상세의 `기록 삭제` 버튼을 눌렀습니다. */
    data object ClickDeleteMeal : HistoryIntent

    /** 삭제 확인 다이얼로그에서 삭제를 확정했습니다. */
    data object ConfirmDeleteMeal : HistoryIntent

    /** 삭제 확인 다이얼로그에서 취소를 눌렀습니다. */
    data object CancelDeleteMeal : HistoryIntent
}
