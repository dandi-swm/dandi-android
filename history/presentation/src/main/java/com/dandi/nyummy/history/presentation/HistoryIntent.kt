package com.dandi.nyummy.history.presentation

import com.dandi.nyummy.common.presentation.mvi.MviIntent
import com.dandi.nyummy.history.entity.AnalysisFeedbackReason
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

    /**
     * 알림의 식사 상세 링크로 화면이 열렸습니다. 그 식사의 날짜를 열고 상세를 엽니다.
     *
     * @param eventId 이 링크를 연 알림의 식별자. 같은 식사라도 알림이 다르면(실패 뒤 재분석 완료 등) 새로 엽니다.
     */
    data class OpenMealFromLink(val mealId: Long, val eventId: String) : HistoryIntent

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

    /** 평가 카드에서 별점을 골랐습니다. [halfSteps]는 반 칸 수(1~10)입니다. */
    data class ChangeFeedbackRating(val halfSteps: Int) : HistoryIntent

    /** 평가 카드에서 아쉬운 점을 골랐습니다. 이미 고른 것을 다시 누르면 선택을 풉니다. */
    data class SelectFeedbackReason(val reason: AnalysisFeedbackReason) : HistoryIntent

    data class ChangeFeedbackComment(val text: String) : HistoryIntent

    data object SubmitFeedback : HistoryIntent

    /** 평가 카드를 닫았습니다. 아무것도 보내지 않습니다. */
    data object DismissFeedback : HistoryIntent
}
