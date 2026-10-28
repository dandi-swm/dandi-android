package com.dandi.nyummy.history.presentation

import com.dandi.nyummy.common.presentation.mvi.MviIntent
import com.dandi.nyummy.history.entity.HistoryDateVO
import com.dandi.nyummy.history.presentation.model.HistoryMonth

/** 히스토리 화면에서 발생하는 사용자 입력입니다. */
sealed interface HistoryIntent : MviIntent {

    /**
     * 히스토리가 화면에 보이게 됐습니다(첫 진입, 탭 전환·식사 기록 후 복귀).
     * 탭을 오가도 화면 상태가 남아 있어, 그 사이 바뀐 기록을 다시 읽습니다.
     */
    data object ScreenResumed : HistoryIntent

    /** 캘린더를 좌우로 넘겨 [month]에서 멈췄습니다. */
    data class ChangeMonth(val month: HistoryMonth) : HistoryIntent

    /** 월 헤더의 `오늘` 버튼을 눌러 오늘 날짜로 이동했다. */
    data object ClickToday : HistoryIntent

    /** 불러오지 못한 기록을 다시 불러오기를 눌렀습니다. */
    data object RetryLoad : HistoryIntent

    /** 오늘 기록이 없을 때 `지금 기록하기`를 눌렀습니다. */
    data object ClickRecordMeal : HistoryIntent

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

    /** 삭제 확인 다이얼로그에서 삭제를 확정했습니다. */
    data object ConfirmDeleteMeal : HistoryIntent

    /** 삭제 확인 다이얼로그에서 취소를 눌렀습니다. */
    data object CancelDeleteMeal : HistoryIntent
}
