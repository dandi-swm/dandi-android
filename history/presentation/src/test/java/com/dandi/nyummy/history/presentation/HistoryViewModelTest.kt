package com.dandi.nyummy.history.presentation

import com.dandi.nyummy.history.entity.HistoryDateVO
import com.dandi.nyummy.history.presentation.util.previousMonthOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryViewModelTest {

    @Test
    fun `인접 월 날짜를 선택하면 그 달로 이동하면서 해당 날짜가 선택된다`() {
        val viewModel = HistoryViewModel()
        val initial = viewModel.uiState.value
        val (prevYear, prevMonth) = previousMonthOf(initial.displayedYear, initial.displayedMonth)
        val target = HistoryDateVO(prevYear, prevMonth, 15)

        viewModel.onIntent(HistoryIntent.SelectDate(target))

        val state = viewModel.uiState.value
        assertEquals(prevYear, state.displayedYear)
        assertEquals(prevMonth, state.displayedMonth)
        assertEquals(target, state.selectedDate)
        assertTrue(state.calendarDays.any { it.inCurrentMonth && it.date == target })
    }

    @Test
    fun `이름 수정을 확정하면 목록과 상세에 새 이름이 반영되고 보기 모드로 돌아간다`() {
        val viewModel = HistoryViewModel()
        val dayWithMeals = viewModel.uiState.value.calendarDays
            .first { it.inCurrentMonth && it.foodIconIds.isNotEmpty() }
        viewModel.onIntent(HistoryIntent.SelectDate(dayWithMeals.date))
        val meal = viewModel.uiState.value.selectedDayMeals.first()

        viewModel.onIntent(HistoryIntent.ClickMeal(meal.id))
        viewModel.onIntent(HistoryIntent.ClickEditMealName)
        viewModel.onIntent(HistoryIntent.ChangeMealNameDraft(" 연어 포케 "))
        viewModel.onIntent(HistoryIntent.ConfirmEditMealName)

        val state = viewModel.uiState.value
        assertEquals("연어 포케", state.selectedDayMeals.first { it.id == meal.id }.name)
        assertEquals("연어 포케", state.mealDetail?.meal?.name)
        assertEquals(HistoryMealDetailMode.Viewing, state.mealDetail?.mode)
    }

    @Test
    fun `표시 중인 달의 날짜를 선택하면 달은 그대로 유지된다`() {
        val viewModel = HistoryViewModel()
        val initial = viewModel.uiState.value
        val target = HistoryDateVO(initial.displayedYear, initial.displayedMonth, 1)

        viewModel.onIntent(HistoryIntent.SelectDate(target))

        val state = viewModel.uiState.value
        assertEquals(initial.displayedYear, state.displayedYear)
        assertEquals(initial.displayedMonth, state.displayedMonth)
        assertEquals(target, state.selectedDate)
    }
}
