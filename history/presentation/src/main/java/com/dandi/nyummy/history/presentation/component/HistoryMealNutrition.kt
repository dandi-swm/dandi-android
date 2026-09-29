package com.dandi.nyummy.history.presentation.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dandi.nyummy.common.presentation.component.DandiText
import com.dandi.nyummy.common.presentation.component.NyummyDualLinearProgress
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemTheme
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemThemeImpl
import com.dandi.nyummy.history.entity.DailyNutritionVO
import com.dandi.nyummy.history.entity.MealHistoryVO
import com.dandi.nyummy.history.entity.NutrientProgressVO
import com.dandi.nyummy.history.presentation.R
import com.dandi.nyummy.history.presentation.model.progressOf

/**
 * 식사 상세의 `영양 섭취 현황` 섹션입니다.
 *
 * 막대그래프 아이콘 + 제목 + 범례 한 줄 아래에 영양소 3종(이모지 아이콘 · 이름 · 이 식사 기여량 · 하루 누적)과
 * 이중 진행바([NyummyDualLinearProgress])를 쌓습니다. 카드 껍데기는 호출부가 감쌉니다.
 */
@Composable
internal fun HistoryMealNutritionSection(
    nutrition: DailyNutritionVO,
    meal: MealHistoryVO,
    modifier: Modifier = Modifier,
) {
    val colors = DesignSystemThemeImpl.designSystemColor
    Column(modifier = modifier.fillMaxWidth()) {
        // 큰 글꼴 배율에서 제목과 범례가 한 줄에 안 들어가면 범례를 다음 줄로 내려 말줄임을 피한다.
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalArrangement = Arrangement.spacedBy(WrapRowGap),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                HistoryBarChartIcon(modifier = Modifier.size(TitleIconSize))
                Spacer(Modifier.width(TitleIconGap))
                DandiText(
                    text = stringResource(R.string.history_nutrition_section_title),
                    color = colors.contentDefaultLevel0,
                    style = DesignSystemThemeImpl.typeScale.textStrongL,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Legend(
                    color = colors.dataProgressDailyTotal,
                    label = stringResource(R.string.history_nutrition_legend_daily),
                )
                Spacer(Modifier.width(LegendGap))
                Legend(
                    color = colors.dataProgressMealContribution,
                    label = stringResource(R.string.history_nutrition_legend_meal),
                )
            }
        }
        Spacer(Modifier.height(TitleBottomGap))
        Column(verticalArrangement = Arrangement.spacedBy(NutrientRowGap)) {
            NutrientRow(
                emoji = CarbohydrateEmoji,
                label = stringResource(R.string.history_macro_carbohydrate),
                progress = nutrition.carbohydrate,
                mealGram = meal.carbohydrateGram,
            )
            NutrientRow(
                emoji = ProteinEmoji,
                label = stringResource(R.string.history_macro_protein),
                progress = nutrition.protein,
                mealGram = meal.proteinGram,
            )
            NutrientRow(
                emoji = FatEmoji,
                label = stringResource(R.string.history_macro_fat),
                progress = nutrition.fat,
                mealGram = meal.fatGram,
            )
        }
    }
}

/** 세이지 톤 막대그래프 아이콘. 열량 요약 칩과 영양 섹션 제목이 함께 씁니다. */
@Composable
internal fun HistoryBarChartIcon(
    modifier: Modifier = Modifier,
) {
    val color = DesignSystemThemeImpl.designSystemColor.contentSuccess
    Canvas(modifier = modifier) {
        val barWidth = size.width * BarChartBarWidthFraction
        val gap = (size.width - barWidth * BarChartBarCount) / (BarChartBarCount - 1)
        val radius = CornerRadius(barWidth / 2f)
        BarChartHeights.forEachIndexed { index, fraction ->
            val barHeight = size.height * fraction
            drawRoundRect(
                color = color,
                topLeft = Offset(x = index * (barWidth + gap), y = size.height - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = radius,
            )
        }
    }
}

@Composable
private fun Legend(
    color: Color,
    label: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(LegendDotSize)
                .background(color, DesignSystemThemeImpl.designSystemShape.pill),
        )
        Spacer(Modifier.width(LegendDotGap))
        DandiText(
            text = label,
            color = DesignSystemThemeImpl.designSystemColor.contentNutritionLabel,
            style = DesignSystemThemeImpl.typeScale.labelRegularXS,
        )
    }
}

/** 이모지 아이콘 · 영양소 이름 · 이 식사 기여량 · 하루 누적/목표를 한 줄에 두고, 아래에 이중 진행바를 그립니다. */
@Composable
private fun NutrientRow(
    emoji: String,
    label: String,
    progress: NutrientProgressVO,
    mealGram: Int,
) {
    val colors = DesignSystemThemeImpl.designSystemColor
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(NutrientIconSlot),
            contentAlignment = Alignment.Center,
        ) {
            DandiText(
                text = emoji,
                style = DesignSystemThemeImpl.typeScale.textStrongXL,
            )
        }
        Spacer(Modifier.width(NutrientIconGap))
        Column(modifier = Modifier.weight(1f)) {
            // 큰 글꼴 배율에서 이름과 값이 한 줄에 안 들어가면 값을 다음 줄로 내려 이름이 잘리지 않게 한다.
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(WrapRowGap),
                itemVerticalAlignment = Alignment.Bottom,
            ) {
                DandiText(
                    text = label,
                    color = colors.contentDefaultLevel0,
                    style = DesignSystemThemeImpl.typeScale.textStrongM,
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    DandiText(
                        text = "이 식사 +${mealGram}g",
                        color = colors.contentSuccess,
                        style = DesignSystemThemeImpl.typeScale.labelStrongS,
                    )
                    Spacer(Modifier.width(NutrientValueGap))
                    DandiText(
                        text = "하루 ${progress.dailyGram} / ${progress.goalGram}g",
                        color = colors.contentNutritionLabel,
                        style = DesignSystemThemeImpl.typeScale.textRegularS,
                    )
                }
            }
            Spacer(Modifier.height(NutrientTrackGap))
            NyummyDualLinearProgress(
                primaryProgress = progressOf(progress.dailyGram, progress.goalGram),
                secondaryProgress = progressOf(mealGram, progress.goalGram),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private const val CarbohydrateEmoji = "🌾"
private const val ProteinEmoji = "🥩"
private const val FatEmoji = "💧"
private const val BarChartBarCount = 3
private const val BarChartBarWidthFraction = 0.24f
private val BarChartHeights = listOf(0.55f, 1f, 0.75f)
private val WrapRowGap = 4.dp
private val TitleIconSize = 18.dp
private val TitleIconGap = 8.dp
private val LegendDotSize = 8.dp
private val LegendDotGap = 6.dp
private val LegendGap = 12.dp
private val TitleBottomGap = 16.dp
private val NutrientRowGap = 16.dp
private val NutrientIconSlot = 32.dp
private val NutrientIconGap = 10.dp
private val NutrientValueGap = 10.dp
private val NutrientTrackGap = 8.dp

@Preview(showBackground = true, widthDp = 342)
@Composable
private fun HistoryMealNutritionSectionPreview() {
    DesignSystemTheme {
        HistoryMealNutritionSection(
            nutrition = DailyNutritionVO(
                currentCalorieKcal = 2_129,
                targetCalorieKcal = 2_000,
                carbohydrate = NutrientProgressVO(dailyGram = 265, goalGram = 300),
                protein = NutrientProgressVO(dailyGram = 124, goalGram = 120),
                fat = NutrientProgressVO(dailyGram = 68, goalGram = 70),
            ),
            meal = MealHistoryVO(
                id = "preview-1",
                name = "치킨 샐러드",
                foodIconId = "salad",
                recordedAt = "08:10",
                calorieKcal = 412,
                carbohydrateGram = 18,
                proteinGram = 42,
                fatGram = 21,
                orderIndex = 1,
            ),
            modifier = Modifier.padding(20.dp),
        )
    }
}
