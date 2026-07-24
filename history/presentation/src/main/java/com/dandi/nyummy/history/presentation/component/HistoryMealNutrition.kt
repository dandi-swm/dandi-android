package com.dandi.nyummy.history.presentation.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
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
 * 하루 누적/이 식사 범례, 영양소 3종 이중 진행바([NyummyDualLinearProgress]),
 * 냐미 코치 말풍선과 캐릭터로 구성됩니다. 식사 상세 전용 조합이라 화면 로컬에 둡니다.
 */
@Composable
internal fun HistoryMealNutritionSection(
    nutrition: DailyNutritionVO,
    meal: MealHistoryVO,
    modifier: Modifier = Modifier,
) {
    val colors = DesignSystemThemeImpl.designSystemColor
    Box(modifier.size(SectionWidth, SectionHeight)) {
        DandiText(
            text = stringResource(R.string.history_nutrition_section_title),
            modifier = Modifier.size(TitleWidth, TitleHeight),
            color = colors.contentDefaultLevel0,
            style = DesignSystemThemeImpl.typeScale.textStrongM,
            overflow = TextOverflow.Clip,
        )
        LegendDot(
            color = colors.dataProgressDailyTotal,
            modifier = Modifier.offset(x = DailyLegendDotOffsetX, y = LegendDotOffsetY),
        )
        DandiText(
            text = stringResource(R.string.history_nutrition_legend_daily),
            modifier = Modifier.offset(x = DailyLegendTextOffsetX, y = LegendTextOffsetY),
            color = colors.contentNutritionLabel,
            style = DesignSystemThemeImpl.typeScale.labelStrongS,
        )
        LegendDot(
            color = colors.dataProgressMealContribution,
            modifier = Modifier.offset(x = MealLegendDotOffsetX, y = LegendDotOffsetY),
        )
        DandiText(
            text = stringResource(R.string.history_nutrition_legend_meal),
            modifier = Modifier.offset(x = MealLegendTextOffsetX, y = LegendTextOffsetY),
            color = colors.contentNutritionLabel,
            style = DesignSystemThemeImpl.typeScale.labelStrongS,
        )
        NutrientRow(
            label = stringResource(R.string.history_macro_carbohydrate),
            progress = nutrition.carbohydrate,
            mealGram = meal.carbohydrateGram,
            modifier = Modifier.offset(y = FirstNutrientOffsetY),
        )
        NutrientRow(
            label = stringResource(R.string.history_macro_protein),
            progress = nutrition.protein,
            mealGram = meal.proteinGram,
            modifier = Modifier.offset(y = SecondNutrientOffsetY),
        )
        NutrientRow(
            label = stringResource(R.string.history_macro_fat),
            progress = nutrition.fat,
            mealGram = meal.fatGram,
            modifier = Modifier.offset(y = ThirdNutrientOffsetY),
        )
        Box(
            modifier = Modifier
                .offset(x = CoachTailOffsetX, y = CoachTailOffsetY)
                .size(CoachTailSize)
                .rotate(CoachTailRotation)
                .background(colors.bgCoachBubble)
                .border(CoachBorderWidth, colors.borderCoachBubble),
        )
        Surface(
            modifier = Modifier
                .offset(x = CoachBubbleOffsetX, y = CoachBubbleOffsetY)
                .size(CoachBubbleWidth, CoachBubbleHeight),
            shape = RoundedCornerShape(DesignSystemThemeImpl.designSystemRadius.radius16),
            color = colors.bgCoachBubble,
            contentColor = colors.contentDefaultLevel0,
            border = BorderStroke(CoachBorderWidth, colors.borderCoachBubble),
        ) {
            Box(Modifier.fillMaxSize()) {
                DandiText(
                    text = stringResource(R.string.history_coach_title),
                    modifier = Modifier.offset(x = CoachTextInsetX, y = CoachTitleOffsetY),
                    color = colors.contentNutritionLabel,
                    style = DesignSystemThemeImpl.typeScale.labelStrongS,
                )
                DandiText(
                    text = stringResource(R.string.history_coach_copy),
                    modifier = Modifier
                        .offset(x = CoachTextInsetX, y = CoachCopyOffsetY)
                        .width(CoachCopyWidth),
                    color = colors.contentDefaultLevel0,
                    style = DesignSystemThemeImpl.typeScale.voiceRegularM,
                    maxLines = 2,
                    overflow = TextOverflow.Clip,
                )
            }
        }
        Box(
            modifier = Modifier
                .offset(x = CoachCharacterOffsetX, y = CoachCharacterOffsetY)
                .size(CoachCharacterWidth, CoachCharacterHeight),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.history_nutrition_coach),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun LegendDot(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(LegendDotSize)
            .background(color, DesignSystemThemeImpl.designSystemShape.pill),
    )
}

@Composable
private fun NutrientRow(
    label: String,
    progress: NutrientProgressVO,
    mealGram: Int,
    modifier: Modifier = Modifier,
) {
    val colors = DesignSystemThemeImpl.designSystemColor
    Box(modifier.size(SectionWidth, NutrientRowHeight)) {
        DandiText(
            text = label,
            modifier = Modifier.size(SectionWidth, NutrientLabelHeight),
            color = colors.contentNutritionLabel,
            style = DesignSystemThemeImpl.typeScale.labelStrongS,
        )
        DandiText(
            text = "하루 ${progress.dailyGram} / ${progress.goalGram}g",
            modifier = Modifier
                .offset(y = NutrientDailyLabelOffsetY)
                .size(SectionWidth, NutrientLabelHeight),
            color = colors.contentNutritionLabel,
            style = DesignSystemThemeImpl.typeScale.textRegularS,
        )
        NyummyDualLinearProgress(
            primaryProgress = progressOf(progress.dailyGram, progress.goalGram),
            secondaryProgress = progressOf(mealGram, progress.goalGram),
            modifier = Modifier
                .offset(y = NutrientTrackOffsetY)
                .fillMaxWidth(),
        )
    }
}

private val SectionWidth = 302.dp
private val SectionHeight = 326.dp
private val TitleWidth = 158.dp
private val TitleHeight = 22.dp
private val DailyLegendDotOffsetX = 168.dp
private val MealLegendDotOffsetX = 240.dp
private val LegendDotOffsetY = 9.dp
private val LegendDotSize = 6.dp
private val DailyLegendTextOffsetX = 178.dp
private val MealLegendTextOffsetX = 250.dp
private val LegendTextOffsetY = 3.dp
private val FirstNutrientOffsetY = 36.dp
private val SecondNutrientOffsetY = 104.dp
private val ThirdNutrientOffsetY = 172.dp
private val NutrientRowHeight = 50.dp
private val NutrientLabelHeight = 18.dp
private val NutrientDailyLabelOffsetY = 19.dp
private val NutrientTrackOffsetY = 42.dp
private val CoachTailOffsetX = 55.dp
private val CoachTailOffsetY = 271.dp
private val CoachTailSize = 14.dp
private const val CoachTailRotation = 45f
private val CoachBubbleOffsetX = 58.dp
private val CoachBubbleOffsetY = 244.dp
private val CoachBubbleWidth = 244.dp
private val CoachBubbleHeight = 78.dp
private val CoachBorderWidth = 1.dp
private val CoachTextInsetX = 18.dp
private val CoachTitleOffsetY = 11.dp
private val CoachCopyOffsetY = 31.dp
private val CoachCopyWidth = 208.dp
private val CoachCharacterOffsetX = 0.dp
private val CoachCharacterOffsetY = 258.dp
private val CoachCharacterWidth = 62.dp
private val CoachCharacterHeight = 66.dp

@Preview(showBackground = true)
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
        )
    }
}
