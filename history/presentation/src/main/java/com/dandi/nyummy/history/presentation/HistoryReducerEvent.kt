package com.dandi.nyummy.history.presentation

import com.dandi.nyummy.common.presentation.mvi.ReducerEvent
import com.dandi.nyummy.history.entity.DailyMealHistoryVO
import com.dandi.nyummy.history.entity.HistoryCalendarVO
import com.dandi.nyummy.history.entity.HistoryDateVO
import com.dandi.nyummy.history.entity.MealHistoryVO

/** 히스토리 화면 Reducer 에 입력되는 내부 이벤트입니다. */
sealed interface HistoryReducerEvent : ReducerEvent {

    /** 표시할 달과 선택 날짜의 데이터가 준비되었습니다. */
    data class MonthLoaded(
        val today: HistoryDateVO,
        val calendar: HistoryCalendarVO,
        val selectedDate: HistoryDateVO,
        val dailyDetail: DailyMealHistoryVO,
    ) : HistoryReducerEvent

    /** 표시 중인 달 안에서 선택 날짜가 바뀌었습니다. */
    data class DaySelected(
        val date: HistoryDateVO,
        val dailyDetail: DailyMealHistoryVO,
    ) : HistoryReducerEvent

    /** `하루 영양 현황` 카드의 접힘 상태가 토글되었습니다. */
    data object NutritionSummaryToggled : HistoryReducerEvent

    /** 식사 상세 오버레이가 열렸습니다. */
    data class MealDetailOpened(val meal: MealHistoryVO) : HistoryReducerEvent

    /** 식사 상세 오버레이가 닫혔습니다. */
    data object MealDetailDismissed : HistoryReducerEvent

    /** 이름 수정 다이얼로그가 열렸습니다. */
    data object MealNameEditStarted : HistoryReducerEvent

    /** 이름 수정 입력값이 바뀌었습니다. */
    data class MealNameDraftChanged(val text: String) : HistoryReducerEvent

    /** 이름 수정이 확정되었습니다. */
    data object MealNameEditCommitted : HistoryReducerEvent

    /** 이름 수정이 취소되었습니다. */
    data object MealNameEditCanceled : HistoryReducerEvent

    /** 삭제 확인 다이얼로그가 열렸습니다. */
    data object MealDeleteRequested : HistoryReducerEvent

    /** 삭제가 취소되었습니다. */
    data object MealDeleteCanceled : HistoryReducerEvent

    /** 상세 오버레이에 열린 식사 기록의 삭제가 확정되었습니다. */
    data object MealDeleted : HistoryReducerEvent
}
