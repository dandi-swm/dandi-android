package com.dandi.nyummy.history.presentation

import com.dandi.nyummy.common.presentation.mvi.MviIntent
import com.dandi.nyummy.history.entity.HistoryDateVO
import com.dandi.nyummy.history.presentation.model.HistoryMonth

/** 히스토리 화면에서 발생하는 사용자 입력입니다. */
sealed interface HistoryIntent : MviIntent {

    /** 캘린더를 좌우로 넘겨 [month]에서 멈췄습니다. */
    data class ChangeMonth(val month: HistoryMonth) : HistoryIntent

    /** 월 헤더의 `오늘` 버튼을 눌러 오늘 날짜로 이동했다. */
    data object ClickToday : HistoryIntent

    /** 캘린더에서 날짜 하나를 선택했습니다. */
    data class SelectDate(val date: HistoryDateVO) : HistoryIntent

    /** `하루 영양 현황` 카드의 접기/펼치기를 눌렀습니다. */
    data object ToggleNutritionSummary : HistoryIntent

    /** 식사 목록에서 기록 하나를 눌렀습니다. */
    data class ClickMeal(val mealId: String) : HistoryIntent

    /** 분석 실패 카드의 `다시 분석하기` 를 눌렀습니다. */
    data class ClickRetryAnalysis(val mealId: String) : HistoryIntent

    /** 분석 실패 카드의 `기록 삭제` 를 눌렀습니다. */
    data class ClickDeleteFailedMeal(val mealId: String) : HistoryIntent

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
