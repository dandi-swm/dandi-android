package com.dandi.nyummy.home.presentation

import com.dandi.nyummy.common.presentation.mvi.UiState
import com.dandi.nyummy.history.entity.DailyMealHistoryVO
import com.dandi.nyummy.home.entity.HomeSummaryVO

/**
 * 홈 화면 상태.
 *
 * @property summary HUD와 오늘 바에 그릴 요약 정보
 * @property todayMeals 오늘 식사 시트의 탄단지 합계와 식사 목록. 아직 읽지 않았으면 null
 * @property isTodayMealsLoading 오늘 식사를 읽는 중
 * @property isTodayMealsFailed 오늘 식사를 읽지 못했다(시트에서 다시 시도할 수 있다)
 * @property isTodaySheetVisible 오늘 식사 시트 표시 여부
 * @property isRoomMenuExpanded 고양이방 메뉴 펼침 여부
 */
data class HomeUIState(
    val summary: HomeSummaryVO = HomeSummaryVO.empty,
    val todayMeals: DailyMealHistoryVO? = null,
    val isTodayMealsLoading: Boolean = false,
    val isTodayMealsFailed: Boolean = false,
    val isTodaySheetVisible: Boolean = false,
    val isRoomMenuExpanded: Boolean = false,
) : UiState {

    /** 오늘 한 끼라도 기록했는지. 오늘 바와 냐미 대사가 이 값으로 갈린다. */
    val hasRecordedToday: Boolean
        get() = summary.todayRecordedCount > 0

    /** 오늘 섭취 열량 / 목표 열량(0f..1f). 목표가 없으면 0. 오늘 바의 얇은 참고 막대에 쓴다. */
    val calorieProgress: Float
        get() = if (summary.goalCalorieKcal <= 0) {
            0f
        } else {
            (summary.todayCalorieKcal.toFloat() / summary.goalCalorieKcal).coerceIn(0f, 1f)
        }

    companion object {
        val empty = HomeUIState()
    }
}
