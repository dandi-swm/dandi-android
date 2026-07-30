package com.dandi.nyummy.home.presentation.component

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dandi.nyummy.common.presentation.component.DandiText
import com.dandi.nyummy.common.presentation.component.NyummyButton
import com.dandi.nyummy.common.presentation.component.NyummyButtonSize
import com.dandi.nyummy.common.presentation.component.NyummyButtonStyle
import com.dandi.nyummy.common.presentation.component.NyummyMealRow
import com.dandi.nyummy.common.presentation.component.NyummyMealRowData
import com.dandi.nyummy.common.presentation.component.NyummySheetMacroSummary
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemTheme
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemThemeImpl
import com.dandi.nyummy.home.presentation.R
import com.dandi.nyummy.home.presentation.mock.HomeMacroMock
import com.dandi.nyummy.home.presentation.mock.HomeMockData
import java.util.Locale

/**
 * 오늘 식사 요약 모달 바텀시트. 화면 전체(하단 네비 포함)를 덮고,
 * 아래로 스와이프하거나 스크림을 탭하면 닫힌다.
 *
 * 캐노니컬 `NyummyMealSummaryBottomSheet`(Figma 956:626)의 토큰을 따르되,
 * 좌우 풀폭 모달로 쓰기 위해 반응형 레이아웃으로 그린다.
 * 열량·기록 수는 홈 요약 상태를 그대로 쓰고, 탄·단·지와 식사 목록은
 * API 연동 전까지 [HomeMockData]의 목업 값을 보여준다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeTodaySummarySheet(
    todayRecordedCount: Int,
    todayCalorieKcal: Int,
    goalCalorieKcal: Int,
    calorieProgress: Float,
    calorieProgressPercent: Int,
    remainingCalorieKcal: Int,
    onDismiss: () -> Unit,
    onAddMeal: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = DesignSystemThemeImpl.designSystemColor

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier.testTag(SheetTestTag),
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(
            topStart = DesignSystemThemeImpl.designSystemRadius.radius24,
            topEnd = DesignSystemThemeImpl.designSystemRadius.radius24,
        ),
        containerColor = colors.bgDefaultLevel1,
        contentColor = colors.contentDefaultLevel0,
        scrimColor = colors.bgScrimDefault,
        dragHandle = { SheetDragHandle() },
    ) {
        HomeTodaySummarySheetContent(
            todayRecordedCount = todayRecordedCount,
            todayCalorieKcal = todayCalorieKcal,
            goalCalorieKcal = goalCalorieKcal,
            calorieProgress = calorieProgress,
            calorieProgressPercent = calorieProgressPercent,
            remainingCalorieKcal = remainingCalorieKcal,
            onAddMeal = onAddMeal,
        )
    }
}

@Composable
private fun HomeTodaySummarySheetContent(
    todayRecordedCount: Int,
    todayCalorieKcal: Int,
    goalCalorieKcal: Int,
    calorieProgress: Float,
    calorieProgressPercent: Int,
    remainingCalorieKcal: Int,
    onAddMeal: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = DesignSystemThemeImpl.designSystemColor
    val spacing = DesignSystemThemeImpl.designSystemSpacing

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = SheetInset)
            .navigationBarsPadding(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            DandiText(
                text = stringResource(R.string.home_sheet_title),
                modifier = Modifier
                    .weight(1f)
                    .semantics { heading() },
                color = colors.contentDefaultLevel0,
                style = DesignSystemThemeImpl.typeScale.displayRegularL,
            )
            DandiText(
                text = stringResource(R.string.home_progress_percent, calorieProgressPercent),
                color = colors.contentNutritionLabel,
                style = DesignSystemThemeImpl.typeScale.textStrongXL,
            )
        }
        Spacer(Modifier.height(spacing.space4))
        DandiText(
            text = stringResource(
                R.string.home_sheet_summary,
                todayRecordedCount,
                formatCount(todayCalorieKcal),
                formatCount(goalCalorieKcal),
            ),
            color = colors.contentDefaultLevel1,
            style = DesignSystemThemeImpl.typeScale.textRegularS,
        )
        Spacer(Modifier.height(spacing.space12))
        CalorieProgressBar(progress = calorieProgress)
        Spacer(Modifier.height(spacing.space16))
        Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(spacing.space12)) {
            MacroSummaryCard(
                summary = macroSummaryOf(
                    labelRes = R.string.home_sheet_macro_carbohydrate,
                    macro = HomeMockData.sheetCarbohydrate,
                ),
                modifier = Modifier.weight(1f),
            )
            MacroSummaryCard(
                summary = macroSummaryOf(
                    labelRes = R.string.home_sheet_macro_protein,
                    macro = HomeMockData.sheetProtein,
                ),
                modifier = Modifier.weight(1f),
            )
            MacroSummaryCard(
                summary = macroSummaryOf(
                    labelRes = R.string.home_sheet_macro_fat,
                    macro = HomeMockData.sheetFat,
                ),
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(spacing.space20))
        DandiText(
            text = stringResource(R.string.home_sheet_section_title),
            modifier = Modifier.semantics { heading() },
            color = colors.contentNutritionLabel,
            style = DesignSystemThemeImpl.typeScale.textStrongL,
        )
        Spacer(Modifier.height(spacing.space8))
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(spacing.space8),
        ) {
            HomeMockData.todayMeals.forEachIndexed { index, meal ->
                NyummyMealRow(
                    data = NyummyMealRowData(
                        orderLabel = stringResource(R.string.home_sheet_meal_order, index + 1),
                        name = meal.name,
                        recordedMeta = meal.recordedAt,
                        calories = stringResource(
                            R.string.home_sheet_meal_calorie,
                            formatCount(meal.calorieKcal),
                        ),
                    ),
                    // TODO: 식사 상세 화면이 추가되면 해당 기록 상세로 연결한다.
                    onClick = {},
                )
            }
        }
        Spacer(Modifier.height(spacing.space12))
        RemainingCalorieBanner(remainingCalorieKcal = remainingCalorieKcal)
        Spacer(Modifier.height(spacing.space16))
        NyummyButton(
            label = stringResource(R.string.home_sheet_add_meal),
            modifier = Modifier.fillMaxWidth(),
            style = NyummyButtonStyle.Secondary,
            size = NyummyButtonSize.Large,
            onClick = onAddMeal,
        )
        Spacer(Modifier.height(spacing.space16))
    }
}

@Composable
private fun SheetDragHandle(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.padding(
            top = DesignSystemThemeImpl.designSystemSpacing.space12,
            bottom = DesignSystemThemeImpl.designSystemSpacing.space8,
        ),
    ) {
        Box(
            modifier = Modifier
                .size(HandleWidth, HandleHeight)
                .clip(DesignSystemThemeImpl.designSystemShape.handle)
                .background(DesignSystemThemeImpl.designSystemColor.bgSheetHandle),
        )
    }
}

@Composable
private fun CalorieProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    val colors = DesignSystemThemeImpl.designSystemColor
    val normalizedProgress = progress.coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(ProgressBarHeight)
            .clip(DesignSystemThemeImpl.designSystemShape.progress)
            .background(colors.bgProgressTrack)
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(normalizedProgress, 0f..1f)
            },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(normalizedProgress)
                .height(ProgressBarHeight)
                .background(colors.dataProgressDefault),
        )
    }
}

@Composable
private fun MacroSummaryCard(
    summary: NyummySheetMacroSummary,
    modifier: Modifier = Modifier,
) {
    val colors = DesignSystemThemeImpl.designSystemColor

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(MacroCardRadius),
        color = colors.bgSurfaceCardSubtle,
    ) {
        Column(modifier = Modifier.padding(MacroCardInset)) {
            DandiText(
                text = summary.label,
                color = colors.contentDefaultLevel1,
                style = DesignSystemThemeImpl.typeScale.labelRegularXS,
            )
            Spacer(Modifier.height(DesignSystemThemeImpl.designSystemSpacing.space4))
            DandiText(
                text = summary.amount,
                color = colors.contentNutritionLabel,
                style = DesignSystemThemeImpl.typeScale.labelStrongXS,
            )
            Spacer(Modifier.height(DesignSystemThemeImpl.designSystemSpacing.space4))
            DandiText(
                text = summary.percent,
                color = colors.contentNutritionLabel,
                style = DesignSystemThemeImpl.typeScale.labelStrongS,
            )
        }
    }
}

@Composable
private fun RemainingCalorieBanner(
    remainingCalorieKcal: Int,
    modifier: Modifier = Modifier,
) {
    val colors = DesignSystemThemeImpl.designSystemColor

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(RemainingBannerHeight),
        shape = RoundedCornerShape(MacroCardRadius),
        color = colors.bgSurfaceCardSubtle,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.width(RemainingBannerLeadingSpace))
            Box(
                modifier = Modifier
                    .size(RemainingDotSize)
                    .clip(DesignSystemThemeImpl.designSystemShape.pill)
                    .background(colors.dataProgressDefault),
            )
            Spacer(Modifier.width(DesignSystemThemeImpl.designSystemSpacing.space8))
            DandiText(
                text = stringResource(
                    R.string.home_sheet_remaining,
                    formatCount(remainingCalorieKcal),
                ),
                color = colors.contentDefaultLevel1,
                style = DesignSystemThemeImpl.typeScale.textStrongM,
            )
        }
    }
}

@Composable
private fun macroSummaryOf(
    @StringRes labelRes: Int,
    macro: HomeMacroMock,
): NyummySheetMacroSummary =
    NyummySheetMacroSummary(
        label = stringResource(labelRes),
        amount = stringResource(R.string.home_sheet_macro_amount, macro.currentGram, macro.goalGram),
        percent = stringResource(R.string.home_progress_percent, macro.percent),
    )

private fun formatCount(value: Int): String =
    String.format(Locale.getDefault(), "%,d", value)

private const val SheetTestTag = "home_today_summary_sheet"
private val SheetInset = 20.dp
private val HandleWidth = 50.dp
private val HandleHeight = 4.dp
private val ProgressBarHeight = 10.dp
private val MacroCardRadius = 14.dp
private val MacroCardInset = 10.dp
private val RemainingBannerHeight = 48.dp
private val RemainingBannerLeadingSpace = 14.dp
private val RemainingDotSize = 8.dp

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun HomeTodaySummarySheetContentPreview() {
    DesignSystemTheme {
        HomeTodaySummarySheetContent(
            todayRecordedCount = 1,
            todayCalorieKcal = 1350,
            goalCalorieKcal = 1800,
            calorieProgress = 0.75f,
            calorieProgressPercent = 75,
            remainingCalorieKcal = 450,
            onAddMeal = {},
        )
    }
}
