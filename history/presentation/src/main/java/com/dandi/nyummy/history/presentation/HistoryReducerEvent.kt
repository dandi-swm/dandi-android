package com.dandi.nyummy.history.presentation

import com.dandi.nyummy.common.presentation.mvi.ReducerEvent
import com.dandi.nyummy.history.entity.DailyMealHistoryVO
import com.dandi.nyummy.history.entity.HistoryCalendarVO
import com.dandi.nyummy.history.entity.HistoryDateVO
import com.dandi.nyummy.history.entity.MealHistoryVO

/** 히스토리 화면 Reducer 에 입력되는 내부 이벤트입니다. */
sealed interface HistoryReducerEvent : ReducerEvent {

    /** 월/일 데이터 조회를 시작했습니다(로딩 표시). */
    data object LoadStarted : HistoryReducerEvent

    /** 월/일 데이터 조회가 실패로 끝났습니다(로딩 종료). 에러 안내는 UseCase 의 스낵바가 담당합니다. */
    data object LoadFailed : HistoryReducerEvent

    /** 식사 수정/삭제 요청이 실패로 끝났습니다(진행 플래그 해제). 에러 안내는 UseCase 의 스낵바가 담당합니다. */
    data object MealActionFailed : HistoryReducerEvent

    /** 식사 수정/삭제 요청이 시작되었습니다(완료 전 중복 조작 차단). */
    data object MealActionStarted : HistoryReducerEvent

    /** 식사의 사진 URL 이 로드되었습니다. 현재 열린 상세가 [mealId]와 같을 때만 반영합니다. */
    data class MealDetailPhotoLoaded(
        val mealId: String,
        val photoUrl: String,
    ) : HistoryReducerEvent

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

    /** [mealId] 식사의 이름 수정이 서버에 저장되었습니다. */
    data class MealNameEditCommitted(
        val mealId: String,
        val newName: String,
    ) : HistoryReducerEvent

    /** 이름 수정이 취소되었습니다. */
    data object MealNameEditCanceled : HistoryReducerEvent

    /** 삭제 확인 다이얼로그가 열렸습니다. */
    data object MealDeleteRequested : HistoryReducerEvent

    /** 삭제가 취소되었습니다. */
    data object MealDeleteCanceled : HistoryReducerEvent

    /** [mealId] 식사 기록의 삭제가 서버에서 완료되었습니다. */
    data class MealDeleted(val mealId: String) : HistoryReducerEvent
}
