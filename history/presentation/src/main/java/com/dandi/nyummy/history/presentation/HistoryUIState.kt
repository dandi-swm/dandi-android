package com.dandi.nyummy.history.presentation

import com.dandi.nyummy.common.presentation.mvi.UiState
import com.dandi.nyummy.history.entity.DailyNutritionVO
import com.dandi.nyummy.history.entity.HistoryDateVO
import com.dandi.nyummy.history.entity.MealAnalysisStatus
import com.dandi.nyummy.history.entity.MealHistoryVO
import com.dandi.nyummy.history.presentation.model.HistoryCalendarDayUiModel
import com.dandi.nyummy.history.presentation.model.buildCalendarDayUiModels
import com.dandi.nyummy.history.presentation.model.HistoryMonth
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.collections.immutable.toImmutableSet

/**
 * 히스토리 화면의 UI 상태입니다.
 *
 * 캘린더는 달마다 한 장씩 좌우로 넘기며, 받아 둔 달의 칸은 [calendarMonths]에 남겨 둡니다.
 * 날짜를 고르면 [selectedDayMeals]와 [dailyNutrition]이 함께 바뀝니다.
 * [mealDetail]이 null 이 아니면 식사 상세 오버레이가 열립니다.
 *
 * @property displayedYear 지금 보고 있는 달의 연도. 달을 넘기면 데이터를 받기 전에 바로 바뀝니다.
 * @property calendarMonths 받아 둔 달의 캘린더 칸. 다시 넘겨 왔을 때 바로 보여 주고 뒤에서 새로 받습니다.
 * @property isLoadFailed 고른 날의 기록을 불러오지 못했다. 화면 안에서 안내하고 다시 불러오게 합니다.
 */
data class HistoryUIState(
    val displayedYear: Int = 0,
    val displayedMonth: Int = 0,
    val today: HistoryDateVO = HistoryDateVO.empty,
    val selectedDate: HistoryDateVO = HistoryDateVO.empty,
    val calendarMonths: ImmutableMap<HistoryMonth, ImmutableList<HistoryCalendarDayUiModel>> = persistentMapOf(),
    val selectedDayMeals: ImmutableList<MealHistoryVO> = persistentListOf(),
    val dailyNutrition: DailyNutritionVO = DailyNutritionVO.empty,
    val isNutritionExpanded: Boolean = true,
    val isLoading: Boolean = false,
    val isLoadFailed: Boolean = false,
    val reanalyzingMealIds: ImmutableSet<String> = persistentSetOf(),
    val mealDetail: HistoryMealDetailUiState? = null,
) : UiState {

    /** 지금 보고 있는 달. */
    val displayedHistoryMonth: HistoryMonth
        get() = HistoryMonth(year = displayedYear, month = displayedMonth)

    /** 오늘이 속한 달. 캘린더는 이 달까지만 넘어갑니다. */
    val currentMonth: HistoryMonth
        get() = HistoryMonth.of(today)

    /** 지금 보고 있는 달의 캘린더 칸. */
    val calendarDays: ImmutableList<HistoryCalendarDayUiModel>
        get() = calendarDaysOf(displayedHistoryMonth)

    /** [month]의 캘린더 칸. 아직 받지 않은 달은 기록 없이 날짜만 채웁니다. */
    fun calendarDaysOf(month: HistoryMonth): ImmutableList<HistoryCalendarDayUiModel> =
        calendarMonths[month] ?: buildCalendarDayUiModels(month.year, month.month, records = emptyMap())

    /** [month]의 캘린더 칸을 [days]로 바꿔 넣습니다. */
    fun withCalendarMonth(
        month: HistoryMonth,
        days: ImmutableList<HistoryCalendarDayUiModel>,
    ): HistoryUIState = copy(calendarMonths = calendarMonths.toPersistentMap().put(month, days))

    val hasNoMeals: Boolean
        get() = !isLoading && selectedDayMeals.isEmpty()

    /** 고른 날이 오늘인지. 기록이 없을 때 오늘이면 기록하러 가는 길을 보여 줍니다. */
    val isTodaySelected: Boolean
        get() = selectedDate == today

    /** 영양 합계에 실제로 반영되는(분석이 끝난) 식사 수. 실패/분석 중 기록은 세지 않습니다. */
    val completedMealCount: Int
        get() = selectedDayMeals.count { it.isAnalysisCompleted }

    /** [mealId] 식사의 재분석 요청이 진행 중인지 여부. */
    fun isReanalyzing(mealId: String): Boolean = mealId in reanalyzingMealIds

    /** [mealId] 식사의 분석 상태를 [status] 로 바꾸고 재분석 진행 표시를 해제합니다. */
    fun withMealStatus(mealId: String, status: MealAnalysisStatus): HistoryUIState = copy(
        selectedDayMeals = selectedDayMeals
            .map { meal -> if (meal.id == mealId) meal.copy(status = status) else meal }
            .withCompletedMealOrder(),
        reanalyzingMealIds = (reanalyzingMealIds - mealId).toImmutableSet(),
    )

    /** [mealId] 식사의 재분석 진행 표시만 바꿉니다. */
    fun withReanalyzing(mealId: String, inFlight: Boolean): HistoryUIState = copy(
        reanalyzingMealIds = if (inFlight) {
            (reanalyzingMealIds + mealId).toImmutableSet()
        } else {
            (reanalyzingMealIds - mealId).toImmutableSet()
        },
    )

    /** 상세 오버레이가 열려 있을 때만 [transform]을 적용합니다. */
    fun withMealDetail(
        transform: (HistoryMealDetailUiState) -> HistoryMealDetailUiState,
    ): HistoryUIState = mealDetail?.let { copy(mealDetail = transform(it)) } ?: this

    /**
     * [mealId] 식사의 이름 수정 저장 결과를 반영합니다.
     * 목록에서는 해당 식사만 rename 하고, 상세 오버레이는 같은 식사가 열려 있을 때만 갱신합니다 —
     * 응답 도착 시점에 다른 식사가 열려 있어도 그 식사의 draft 를 건드리지 않습니다.
     */
    fun commitMealNameEdit(mealId: String, newName: String): HistoryUIState {
        if (newName.isEmpty()) return this
        val detail = mealDetail
        return copy(
            selectedDayMeals = selectedDayMeals
                .map { meal -> if (meal.id == mealId) meal.copy(name = newName) else meal }
                .toImmutableList(),
            mealDetail = if (detail != null && detail.meal.id == mealId) {
                detail.copy(
                    meal = detail.meal.copy(name = newName),
                    mode = HistoryMealDetailMode.Viewing,
                    nameDraft = "",
                    isActionInFlight = false,
                )
            } else {
                detail
            },
        )
    }

    /**
     * [mealId] 식사의 삭제 완료를 반영합니다.
     * 남은 식사로 하루 영양 합계를 다시 계산하고, 해당 날짜의 캘린더 셀 표시도 갱신합니다.
     * 상세 오버레이는 같은 식사가 열려 있을 때만 닫습니다.
     */
    fun deleteDetailMeal(mealId: String): HistoryUIState {
        val closedDetail = if (mealDetail?.meal?.id == mealId) null else mealDetail
        if (selectedDayMeals.none { it.id == mealId }) return copy(mealDetail = closedDetail)
        val remaining = selectedDayMeals.filterNot { it.id == mealId }.withCompletedMealOrder()
        val totalCalorie = remaining.sumOf { it.calorieKcal }
        val cellIcons = remaining.take(2).map { it.foodIconId }.toImmutableList()
        val selectedMonth = HistoryMonth.of(selectedDate)
        val updatedDays = calendarDaysOf(selectedMonth).map { cell ->
            if (cell.inCurrentMonth && cell.date == selectedDate) {
                cell.copy(hasRecord = remaining.isNotEmpty(), foodIconIds = cellIcons)
            } else {
                cell
            }
        }.toImmutableList()
        return copy(
            selectedDayMeals = remaining,
            reanalyzingMealIds = (reanalyzingMealIds - mealId).toImmutableSet(),
            dailyNutrition = dailyNutrition.copy(
                currentCalorieKcal = totalCalorie,
                carbohydrate = dailyNutrition.carbohydrate.copy(
                    dailyGram = remaining.sumOf { it.carbohydrateGram },
                ),
                protein = dailyNutrition.protein.copy(dailyGram = remaining.sumOf { it.proteinGram }),
                fat = dailyNutrition.fat.copy(dailyGram = remaining.sumOf { it.fatGram }),
            ),
            mealDetail = closedDetail,
        ).withCalendarMonth(selectedMonth, updatedDays)
    }

    companion object {
        val empty = HistoryUIState()

        /**
         * 데이터 로드 전에도 캘린더와 월 이동이 유효한 연/월로 동작하도록
         * 오늘 날짜 기준으로 초기화한 상태를 만듭니다. 캘린더 칸은 받기 전까지 날짜만 그립니다.
         */
        fun initial(today: HistoryDateVO): HistoryUIState = HistoryUIState(
            displayedYear = today.year,
            displayedMonth = today.month,
            today = today,
            selectedDate = today,
            isLoading = true,
        )
    }
}

/**
 * 식사 상세 오버레이의 상태입니다.
 *
 * @property nameDraft 이름 수정 다이얼로그의 입력값. [HistoryMealDetailMode.EditingName]에서만 의미가 있습니다.
 * @property isActionInFlight 수정/삭제 요청이 진행 중인지 여부. true 인 동안 확정/취소 조작을 무시합니다.
 */
data class HistoryMealDetailUiState(
    val meal: MealHistoryVO = MealHistoryVO.empty,
    val mode: HistoryMealDetailMode = HistoryMealDetailMode.Viewing,
    val nameDraft: String = "",
    val isActionInFlight: Boolean = false,
)

/** 식사 상세 오버레이가 보여주는 단계입니다. */
enum class HistoryMealDetailMode {
    /** 상세 카드 보기 */
    Viewing,

    /** 이름 수정 다이얼로그 */
    EditingName,

    /** 삭제 확인 다이얼로그 */
    ConfirmingDelete,
}

/**
 * "첫 끼 / 마지막 끼니" 라벨이 쓰는 하루 안의 순번을 **분석이 끝난 식사만** 세어 다시 매깁니다.
 *
 * 서버가 준 순번은 실패·분석 중 기록까지 포함한 값이라, 그대로 쓰면 실패 기록이 섞인 날의
 * 라벨이 어긋납니다. 상태 카드는 순번을 쓰지 않으므로 0 으로 둡니다.
 */
internal fun List<MealHistoryVO>.withCompletedMealOrder(): ImmutableList<MealHistoryVO> {
    var completedOrder = 0
    return map { meal ->
        if (meal.isAnalysisCompleted) {
            meal.copy(orderIndex = ++completedOrder)
        } else {
            meal.copy(orderIndex = 0)
        }
    }.toImmutableList()
}
