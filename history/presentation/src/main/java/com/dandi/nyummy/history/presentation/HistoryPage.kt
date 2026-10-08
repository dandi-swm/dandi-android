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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyText
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemTheme
import com.dandi.nyummy.history.entity.DailyNutritionStatus
import com.dandi.nyummy.history.entity.DailyNutritionVO
import com.dandi.nyummy.history.entity.HistoryCalendarDayVO
import com.dandi.nyummy.history.entity.HistoryDateVO
import com.dandi.nyummy.history.entity.MealHistoryVO
import com.dandi.nyummy.history.entity.NutrientProgressVO
import com.dandi.nyummy.history.presentation.component.HistoryCalendar
import com.dandi.nyummy.history.presentation.component.HistoryDailySection
import com.dandi.nyummy.history.presentation.component.HistoryMealDetailOverlay
import com.dandi.nyummy.history.presentation.model.HistoryMonth
import com.dandi.nyummy.history.presentation.model.buildCalendarDayUiModels
import com.dandi.nyummy.history.presentation.model.dayTitleOf
import com.dandi.nyummy.history.presentation.model.mealCountLabelOf
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf

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
            .background(NyummyTheme.colors.bg.canvas),
    ) {
        // 넓은 화면에서는 콘텐츠를 가운데 480 폭으로 모은다.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .wrapContentWidth(Alignment.CenterHorizontally)
                .widthIn(max = ContentMaxWidth)
                .padding(top = PageTopGap, bottom = PageBottomGap),
        ) {
            Column(modifier = Modifier.padding(horizontal = NyummyTheme.spacing.gutter)) {
                NyummyText(
                    text = stringResource(R.string.history_title),
                    style = NyummyTheme.typography.displayM,
                )
                Spacer(Modifier.height(NyummyTheme.spacing.s4))
                NyummyText(
                    text = stringResource(R.string.history_subtitle),
                    style = NyummyTheme.typography.bodyM,
                    color = NyummyTheme.colors.content.tertiary,
                )
                Spacer(Modifier.height(CalendarTopGap))
                HistoryCalendar(
                    displayedMonth = uiState.displayedHistoryMonth,
                    today = uiState.today,
                    selectedDate = uiState.selectedDate,
                    calendarMonths = uiState.calendarMonths,
                    onChangeMonth = { onIntent(HistoryIntent.ChangeMonth(it)) },
                    onClickToday = { onIntent(HistoryIntent.ClickToday) },
                    onSelectDate = { onIntent(HistoryIntent.SelectDate(it)) },
                )
            }
            Spacer(Modifier.height(DailySectionTopGap))
            HistoryDailySection(
                dayTitle = dayTitleOf(uiState.selectedDate),
                mealCountLabel = mealCountLabelOf(uiState.completedMealCount),
                nutrition = uiState.dailyNutrition,
                isNutritionExpanded = uiState.isNutritionExpanded,
                isLoading = uiState.isLoading,
                meals = uiState.selectedDayMeals,
                reanalyzingMealIds = uiState.reanalyzingMealIds,
                onToggleNutrition = { onIntent(HistoryIntent.ToggleNutritionSummary) },
                onClickMeal = { onIntent(HistoryIntent.ClickMeal(it)) },
                onRetryAnalysis = { onIntent(HistoryIntent.ClickRetryAnalysis(it)) },
                onDeleteFailedMeal = { onIntent(HistoryIntent.ClickDeleteFailedMeal(it)) },
            )
        }
        uiState.mealDetail?.let { detail ->
            HistoryMealDetailOverlay(
                detail = detail,
                selectedDate = uiState.selectedDate,
                mealCount = uiState.completedMealCount,
                dailyNutrition = uiState.dailyNutrition,
                onIntent = onIntent,
            )
        }
    }
}

private val PageTopGap = 12.dp
private val PageBottomGap = 120.dp
private val ContentMaxWidth = 480.dp

// "오늘" 버튼 터치 영역(44)이 월 이름(30)보다 높아 위 간격을 그만큼 줄여 Figma 위치(24)에 맞춘다.
private val CalendarTopGap = 17.dp
private val DailySectionTopGap = 28.dp

private fun previewUiState(): HistoryUIState {
    val today = HistoryDateVO(2026, 7, 24)
    val selectedDate = HistoryDateVO(2026, 7, 18)
    val meals = persistentListOf(
        MealHistoryVO(
            id = "1",
            name = "치킨 샐러드",
            foodIconId = "salad",
            recordedAt = "08:10",
            calorieKcal = 412,
            carbohydrateGram = 18,
            proteinGram = 42,
            fatGram = 21,
            orderIndex = 1,
        ),
        MealHistoryVO(
            id = "2",
            name = "연어 덮밥",
            foodIconId = "rice",
            recordedAt = "12:30",
            calorieKcal = 545,
            carbohydrateGram = 78,
            proteinGram = 31,
            fatGram = 12,
            orderIndex = 2,
        ),
    )
    val records = mapOf(
        selectedDate to HistoryCalendarDayVO(
            date = selectedDate,
            status = DailyNutritionStatus.IN_RANGE,
            foodIconIds = listOf("salad", "rice"),
            mealCount = 2,
        ),
        HistoryDateVO(2026, 7, 10) to HistoryCalendarDayVO(
            date = HistoryDateVO(2026, 7, 10),
            status = DailyNutritionStatus.OUT_OF_RANGE,
            foodIconIds = listOf("pasta"),
            mealCount = 1,
        ),
    )
    return HistoryUIState(
        displayedYear = 2026,
        displayedMonth = 7,
        today = today,
        selectedDate = selectedDate,
        calendarMonths = persistentMapOf(HistoryMonth(2026, 7) to buildCalendarDayUiModels(2026, 7, records)),
        selectedDayMeals = meals,
        dailyNutrition = DailyNutritionVO(
            currentCalorieKcal = 957,
            targetCalorieKcal = 2_000,
            carbohydrate = NutrientProgressVO(dailyGram = 96, goalGram = 300),
            protein = NutrientProgressVO(dailyGram = 73, goalGram = 120),
            fat = NutrientProgressVO(dailyGram = 33, goalGram = 70),
        ),
    )
}

@Preview(showBackground = true, widthDp = 390, heightDp = 1400)
@Composable
private fun HistoryScreenPreview() {
    NyummyTheme {
        DesignSystemTheme {
            HistoryScreen(uiState = previewUiState(), onIntent = {})
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 1000)
@Composable
private fun HistoryScreenEmptyPreview() {
    NyummyTheme {
        DesignSystemTheme {
            HistoryScreen(
                uiState = previewUiState().copy(
                    selectedDayMeals = persistentListOf(),
                    dailyNutrition = DailyNutritionVO(targetCalorieKcal = 2_000),
                ),
                onIntent = {},
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 1000)
@Composable
private fun HistoryScreenLoadingPreview() {
    NyummyTheme {
        DesignSystemTheme {
            HistoryScreen(
                uiState = previewUiState().copy(isLoading = true),
                onIntent = {},
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 900)
@Composable
private fun HistoryScreenDetailPreview() {
    NyummyTheme {
        DesignSystemTheme {
            val base = previewUiState()
            HistoryScreen(
                uiState = base.copy(
                    mealDetail = HistoryMealDetailUiState(
                        meal = base.selectedDayMeals.firstOrNull() ?: MealHistoryVO.empty,
                    ),
                ),
                onIntent = {},
            )
        }
    }
}
