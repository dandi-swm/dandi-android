package com.dandi.nyummy.home.presentation

import com.dandi.nyummy.cat.entity.CatState
import com.dandi.nyummy.common.presentation.mvi.UiState
import com.dandi.nyummy.history.entity.DailyNutritionVO
import com.dandi.nyummy.history.entity.MealHistoryVO
import com.dandi.nyummy.home.entity.HomeSummaryVO
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * 홈 화면 상태.
 *
 * @property summary HUD와 오늘 바에 그릴 요약 정보
 * @property todayNutrition 오늘 식사 시트의 탄단지 합계. 아직 읽지 않았으면 null
 * @property todayMeals 오늘 식사 시트의 식사 목록(시간순)
 * @property isTodayMealsLoading 오늘 식사를 읽는 중
 * @property isTodayMealsFailed 오늘 식사를 읽지 못했다(시트에서 다시 시도할 수 있다)
 * @property isTodaySheetVisible 오늘 식사 시트 표시 여부
 * @property isSummaryLoaded 홈 요약을 한 번이라도 받았다. 요약이 그려졌는지 화면이 알리는 데(홈 TTI) 쓴다.
 * @property isRoomMenuExpanded 고양이방 메뉴 펼침 여부
 * @property isCatAnimationFailed 냐미 애니메이션을 받지 못했다(고양이가 없거나 서버, 네트워크 실패). 기본 냐미로 대신한다.
 * @property catState 지금 냐미 상태. 홈 요약을 읽기 전에는 null
 * @property catGroup 지금 재생할 동작 묶음 번호
 * @property catPlayId 동작을 새로 고를 때마다 바뀐다
 * @property catLine 말풍선 대사. 애니메이션이 없으면 null이고, 화면은 기본 대사를 쓴다.
 * @property catMotion 지금 재생할 냐미 동작. 애니메이션이나 상태가 아직 없으면 null이다.
 */
data class HomeUIState(
    val summary: HomeSummaryVO = HomeSummaryVO.empty,
    val isSummaryLoaded: Boolean = false,
    val todayNutrition: DailyNutritionVO? = null,
    val todayMeals: ImmutableList<MealHistoryVO> = persistentListOf(),
    val isTodayMealsLoading: Boolean = false,
    val isTodayMealsFailed: Boolean = false,
    val isTodaySheetVisible: Boolean = false,
    val isRoomMenuExpanded: Boolean = false,
    val isCatAnimationFailed: Boolean = false,
    val catState: CatState? = null,
    val catGroup: Int = 0,
    val catPlayId: Int = 0,
    val catLine: String? = null,
    val catMotion: HomeCatMotion? = null,
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
