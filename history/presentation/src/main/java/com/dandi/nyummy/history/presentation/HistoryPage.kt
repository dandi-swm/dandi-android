package com.dandi.nyummy.history.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dandi.nyummy.common.presentation.component.DandiText
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemTheme
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemThemeImpl
import com.dandi.nyummy.history.entity.HistoryDateVO
import com.dandi.nyummy.history.presentation.component.HistoryCalendarCard
import com.dandi.nyummy.history.presentation.component.HistoryDailySection
import com.dandi.nyummy.history.presentation.component.HistoryMealDetailOverlay
import com.dandi.nyummy.history.presentation.mock.HistoryMockData
import com.dandi.nyummy.history.presentation.model.buildCalendarDayUiModels
import com.dandi.nyummy.history.presentation.model.dayLabelOf
import com.dandi.nyummy.history.presentation.model.mealCountLabelOf
import kotlinx.collections.immutable.toImmutableList

/**
 * 히스토리(캘린더/식사 기록 조회) 화면입니다.
 *
 * 월간 캘린더에서 날짜를 고르면 그날의 식사 목록과 하루 영양 현황이 바뀌고,
 * 식사를 누르면 상세 오버레이가 열립니다. 상태 수집과 [HistoryIntent] 전달만 담당합니다.
 */
@Composable
fun HistoryPage(
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HistoryScreen(
        uiState = uiState,
        onIntent = viewModel::onIntent,
        modifier = modifier,
    )
}

@Composable
private fun HistoryScreen(
    uiState: HistoryUIState,
    onIntent: (HistoryIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DesignSystemThemeImpl.designSystemColor.bgSurfaceIvory),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = PageTopGap, bottom = PageBottomGap),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PageTitleInset),
            ) {
                DandiText(
                    text = stringResource(R.string.history_title),
                    color = DesignSystemThemeImpl.designSystemColor.contentDefaultLevel0,
                    style = DesignSystemThemeImpl.typeScale.displayRegularXL,
                )
                Spacer(Modifier.height(PageSubtitleTopGap))
                DandiText(
                    text = stringResource(R.string.history_subtitle),
                    color = DesignSystemThemeImpl.designSystemColor.contentDefaultLevel1,
                    style = DesignSystemThemeImpl.typeScale.textRegularS,
                )
            }
            Spacer(Modifier.height(CalendarTopGap))
            HistoryCalendarCard(
                monthLabel = uiState.monthLabel,
                days = uiState.calendarDays,
                selectedDate = uiState.selectedDate,
                onClickPreviousMonth = { onIntent(HistoryIntent.ClickPreviousMonth) },
                onClickNextMonth = { onIntent(HistoryIntent.ClickNextMonth) },
                onSelectDate = { onIntent(HistoryIntent.SelectDate(it)) },
            )
            Spacer(Modifier.height(DailySectionTopGap))
            HistoryDailySection(
                dayTitle = "${dayLabelOf(uiState.selectedDate)} 식사",
                mealCountLabel = mealCountLabelOf(uiState.selectedDayMeals.size),
                nutrition = uiState.dailyNutrition,
                isNutritionExpanded = uiState.isNutritionExpanded,
                isLoading = uiState.isLoading,
                meals = uiState.selectedDayMeals,
                onToggleNutrition = { onIntent(HistoryIntent.ToggleNutritionSummary) },
                onClickMeal = { onIntent(HistoryIntent.ClickMeal(it)) },
            )
        }
        uiState.mealDetail?.let { detail ->
            HistoryMealDetailOverlay(
                detail = detail,
                selectedDate = uiState.selectedDate,
                mealCount = uiState.selectedDayMeals.size,
                dailyNutrition = uiState.dailyNutrition,
                onIntent = onIntent,
            )
        }
    }
}

private val PageTopGap = 12.dp
private val PageBottomGap = 120.dp
private val PageTitleInset = 20.dp
private val PageSubtitleTopGap = 2.dp
private val CalendarTopGap = 20.dp
private val DailySectionTopGap = 30.dp

private fun previewUiState(): HistoryUIState {
    val today = HistoryDateVO(2026, 7, 24)
    val selectedDate = HistoryDateVO(2026, 7, 18)
    val calendar = HistoryMockData.monthOf(year = 2026, month = 7, today = today)
    val day = HistoryMockData.dayOf(date = selectedDate, today = today)
    return HistoryUIState(
        displayedYear = 2026,
        displayedMonth = 7,
        today = today,
        selectedDate = selectedDate,
        calendarDays = buildCalendarDayUiModels(2026, 7, calendar.days.associateBy { it.date }),
        selectedDayMeals = day.meals.toImmutableList(),
        dailyNutrition = day.nutrition,
    )
}

@Preview(showBackground = true, widthDp = 390, heightDp = 1400)
@Composable
private fun HistoryScreenPreview() {
    DesignSystemTheme {
        HistoryScreen(uiState = previewUiState(), onIntent = {})
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 1000)
@Composable
private fun HistoryScreenEmptyPreview() {
    DesignSystemTheme {
        HistoryScreen(
            uiState = previewUiState().copy(
                selectedDayMeals = kotlinx.collections.immutable.persistentListOf(),
                dailyNutrition = com.dandi.nyummy.history.entity.DailyNutritionVO(
                    targetCalorieKcal = 2_000,
                ),
            ),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 1000)
@Composable
private fun HistoryScreenLoadingPreview() {
    DesignSystemTheme {
        HistoryScreen(
            uiState = previewUiState().copy(isLoading = true),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 900)
@Composable
private fun HistoryScreenDetailPreview() {
    DesignSystemTheme {
        val base = previewUiState()
        HistoryScreen(
            uiState = base.copy(
                mealDetail = HistoryMealDetailUiState(
                    meal = base.selectedDayMeals.firstOrNull()
                        ?: com.dandi.nyummy.history.entity.MealHistoryVO.empty,
                ),
            ),
            onIntent = {},
        )
    }
}
