package com.dandi.nyummy.home.presentation

import com.dandi.nyummy.common.presentation.mvi.UiState
import com.dandi.nyummy.home.entity.HomeSummaryVO
import kotlin.math.roundToInt

/**
 * 홈 화면 상태.
 *
 * @property summary HUD·마이룸 카드에 그릴 요약 정보
 * @property isTodaySummarySheetVisible 오늘 식사 요약 바텀시트 표시 여부
 * @property isRoomActionMenuExpanded 마이룸 카드 플로팅 액션 메뉴 펼침 여부
 */
data class HomeUIState(
    val summary: HomeSummaryVO = HomeSummaryVO.empty,
    val isTodaySummarySheetVisible: Boolean = false,
    val isRoomActionMenuExpanded: Boolean = false,
) : UiState {

    /** 오늘 목표까지 남은 열량 (kcal). 목표를 넘겼으면 0. */
    val remainingCalorieKcal: Int
        get() = (summary.goalCalorieKcal - summary.todayCalorieKcal).coerceAtLeast(0)

    /** 오늘 섭취 열량 / 목표 열량 진행률 (0f..1f). 목표가 없으면 0. */
    val calorieProgress: Float
        get() = if (summary.goalCalorieKcal <= 0) {
            0f
        } else {
            (summary.todayCalorieKcal.toFloat() / summary.goalCalorieKcal).coerceIn(0f, 1f)
        }

    /** 진행률 라벨용 정수 퍼센트 (예: 75). */
    val calorieProgressPercent: Int
        get() = (calorieProgress * 100).roundToInt()

    companion object {
        val empty = HomeUIState()
    }
}
