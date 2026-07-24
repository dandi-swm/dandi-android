package com.dandi.nyummy.history.presentation.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dandi.nyummy.common.presentation.component.DandiText
import com.dandi.nyummy.common.presentation.component.NyummyBadge
import com.dandi.nyummy.common.presentation.component.NyummyBadgeTone
import com.dandi.nyummy.common.presentation.component.NyummyLinearProgress
import com.dandi.nyummy.common.presentation.component.NyummyMealRow
import com.dandi.nyummy.common.presentation.component.NyummyMealRowData
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemTheme
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemThemeImpl
import com.dandi.nyummy.common.presentation.ui.theme.designSystemDropShadow
import com.dandi.nyummy.history.entity.DailyNutritionVO
import com.dandi.nyummy.history.entity.MealHistoryVO
import com.dandi.nyummy.history.entity.NutrientProgressVO
import com.dandi.nyummy.history.presentation.R
import com.dandi.nyummy.history.presentation.model.mealOrderLabelOf
import com.dandi.nyummy.history.presentation.model.mealRowMetaOf
import com.dandi.nyummy.history.presentation.model.numberLabelOf
import com.dandi.nyummy.history.presentation.model.percentOf
import com.dandi.nyummy.history.presentation.model.progressOf
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

/**
 * 선택한 날짜의 식사 섹션입니다.
 * 날짜 헤더, 접을 수 있는 `하루 영양 현황` 카드, 식사 목록(없으면 안내 문구)으로 구성됩니다.
 */
@Composable
internal fun HistoryDailySection(
    dayTitle: String,
    mealCountLabel: String,
    nutrition: DailyNutritionVO,
    isNutritionExpanded: Boolean,
    isLoading: Boolean,
    meals: ImmutableList<MealHistoryVO>,
    onToggleNutrition: () -> Unit,
    onClickMeal: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.width(DailySectionWidth),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DandiText(
                text = dayTitle,
                modifier = Modifier.weight(1f),
                color = DesignSystemThemeImpl.designSystemColor.contentDefaultLevel0,
                style = DesignSystemThemeImpl.typeScale.titleStrongL,
            )
            DandiText(
                text = mealCountLabel,
                color = DesignSystemThemeImpl.designSystemColor.contentNutritionLabel,
                style = DesignSystemThemeImpl.typeScale.textRegularS,
                textAlign = TextAlign.End,
            )
        }
        Spacer(Modifier.height(DailyHeaderBottomGap))
        HistoryDailyNutritionCard(
            nutrition = nutrition,
            expanded = isNutritionExpanded,
            isLoading = isLoading,
            onToggle = onToggleNutrition,
        )
        Spacer(Modifier.height(DailyNutritionBottomGap))
        if (meals.isEmpty()) {
            DandiText(
                text = stringResource(R.string.history_empty_meals),
                modifier = Modifier.padding(vertical = EmptyMessageVerticalGap),
                color = DesignSystemThemeImpl.designSystemColor.contentDefaultLevel2,
                style = DesignSystemThemeImpl.typeScale.textRegularM,
                textAlign = TextAlign.Center,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(MealRowGap)) {
                meals.forEach { meal ->
                    NyummyMealRow(
                        data = NyummyMealRowData(
                            orderLabel = mealOrderLabelOf(meal.orderIndex, meals.size),
                            name = meal.name,
                            recordedMeta = mealRowMetaOf(meal),
                            calories = "${meal.calorieKcal} kcal",
                        ),
                        onClick = { onClickMeal(meal.id) },
                        foodIcon = { HistoryFoodIcon(meal.foodIconId) },
                    )
                }
            }
        }
    }
}

/**
 * 접기/펼치기가 되는 `하루 영양 현황` 카드입니다.
 *
 * 공용 카드와 달리 우상단이 상태 문구 대신 접기 토글이라 별도로 구현합니다.
 * 접힘 상태에서는 열량 줄까지만 보여줍니다.
 */
@Composable
private fun HistoryDailyNutritionCard(
    nutrition: DailyNutritionVO,
    expanded: Boolean,
    isLoading: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = DesignSystemThemeImpl.designSystemColor
    val shape = RoundedCornerShape(DesignSystemThemeImpl.designSystemRadius.radius20)
    Surface(
        modifier = modifier
            .designSystemDropShadow(
                shape = shape,
                shadow = DesignSystemThemeImpl.designSystemElevation.surfaceLow,
            )
            .fillMaxWidth(),
        shape = shape,
        color = colors.bgSurfaceCardSubtle,
        contentColor = colors.contentDefaultLevel0,
        border = BorderStroke(NutritionCardBorderWidth, colors.borderNutrition),
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = NutritionCardInset,
                vertical = NutritionCardVerticalInset,
            ),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                DandiText(
                    text = stringResource(R.string.history_nutrition_title),
                    modifier = Modifier.weight(1f),
                    color = colors.contentDefaultLevel0,
                    style = DesignSystemThemeImpl.typeScale.textStrongL,
                )
                Row(
                    modifier = Modifier.clickable(role = Role.Button, onClick = onToggle),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    DandiText(
                        text = stringResource(
                            if (expanded) R.string.history_nutrition_collapse else R.string.history_nutrition_expand,
                        ),
                        color = colors.contentActionSecondary,
                        style = DesignSystemThemeImpl.typeScale.labelStrongS,
                    )
                    Spacer(Modifier.width(NutritionToggleChevronGap))
                    NutritionToggleChevron(pointsUp = expanded)
                }
            }
            Spacer(Modifier.height(NutritionTitleBottomGap))
            Row(verticalAlignment = Alignment.CenterVertically) {
                DandiText(
                    text = if (isLoading) {
                        "— / ${numberLabelOf(nutrition.targetCalorieKcal)} kcal"
                    } else {
                        "${numberLabelOf(nutrition.currentCalorieKcal)} / " +
                            "${numberLabelOf(nutrition.targetCalorieKcal)} kcal"
                    },
                    modifier = Modifier.weight(1f),
                    color = if (isLoading) colors.contentDefaultLevel1 else colors.contentDefaultLevel0,
                    style = DesignSystemThemeImpl.typeScale.textStrongM,
                )
                NyummyBadge(
                    label = if (isLoading) {
                        "—"
                    } else {
                        "${percentOf(nutrition.currentCalorieKcal, nutrition.targetCalorieKcal)}%"
                    },
                    tone = NyummyBadgeTone.Warning,
                )
            }
            if (expanded) {
                Spacer(Modifier.height(NutritionMacroTopGap))
                Row(horizontalArrangement = Arrangement.spacedBy(NutritionMacroGap)) {
                    NutritionMacro("탄수", nutrition.carbohydrate, colors.dataNutrientCarbohydrate, isLoading)
                    NutritionMacro("단백질", nutrition.protein, colors.dataNutrientProtein, isLoading)
                    NutritionMacro("지방", nutrition.fat, colors.dataNutrientFat, isLoading)
                }
            }
        }
    }
}

/** 접힘 상태를 위/아래 방향으로 보여주는 셰브론입니다. 전환 시 회전 애니메이션으로 뒤집힙니다. */
@Composable
private fun NutritionToggleChevron(
    pointsUp: Boolean,
    modifier: Modifier = Modifier,
) {
    val rotation by animateFloatAsState(
        targetValue = if (pointsUp) 0f else 180f,
        label = "nutritionToggleChevron",
    )
    Icon(
        imageVector = Icons.Filled.KeyboardArrowUp,
        contentDescription = null,
        modifier = modifier
            .size(NutritionToggleChevronSize)
            .graphicsLayer { rotationZ = rotation },
        tint = DesignSystemThemeImpl.designSystemColor.contentActionSecondary,
    )
}

@Composable
private fun NutritionMacro(
    label: String,
    progress: NutrientProgressVO,
    color: Color,
    isLoading: Boolean,
) {
    val colors = DesignSystemThemeImpl.designSystemColor
    val targetFraction = if (isLoading) {
        0f
    } else {
        progressOf(progress.dailyGram, progress.goalGram)
    }
    // 첫 표시·펼침 시 0에서 목표 값으로 차오르도록 한 프레임 뒤에 목표를 밀어 넣는다.
    // (값 사이 이동 애니메이션은 NyummyLinearProgress 가 내장)
    var displayedFraction by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(targetFraction) { displayedFraction = targetFraction }
    Column(modifier = Modifier.width(NutritionMacroWidth)) {
        DandiText(
            text = if (isLoading) {
                "$label —"
            } else {
                "$label ${percentOf(progress.dailyGram, progress.goalGram)}%"
            },
            color = colors.contentNutritionLabel,
            style = DesignSystemThemeImpl.typeScale.labelStrongS,
        )
        Spacer(Modifier.height(NutritionMacroTrackGap))
        NyummyLinearProgress(
            progress = displayedFraction,
            modifier = Modifier
                .fillMaxWidth()
                .height(NutritionMacroTrackHeight),
            color = color,
        )
    }
}

private val DailySectionWidth = 350.dp
private val DailyHeaderBottomGap = 14.dp
private val DailyNutritionBottomGap = 14.dp
private val EmptyMessageVerticalGap = 28.dp
private val MealRowGap = 8.dp

private val NutritionCardBorderWidth = 1.dp
private val NutritionToggleChevronGap = 2.dp
private val NutritionToggleChevronSize = 16.dp
private val NutritionCardInset = 16.dp
private val NutritionCardVerticalInset = 10.dp
private val NutritionTitleBottomGap = 2.dp
private val NutritionMacroTopGap = 7.dp
private val NutritionMacroGap = 10.dp
private val NutritionMacroWidth = 96.dp
private val NutritionMacroTrackGap = 7.dp
private val NutritionMacroTrackHeight = 6.dp

private val previewNutrition = DailyNutritionVO(
    currentCalorieKcal = 2_129,
    targetCalorieKcal = 2_000,
    carbohydrate = NutrientProgressVO(dailyGram = 265, goalGram = 300),
    protein = NutrientProgressVO(dailyGram = 124, goalGram = 120),
    fat = NutrientProgressVO(dailyGram = 68, goalGram = 70),
)

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun HistoryDailySectionPreview() {
    DesignSystemTheme {
        HistoryDailySection(
            dayTitle = "7월 18일 식사",
            mealCountLabel = "1회 기록",
            nutrition = previewNutrition,
            isNutritionExpanded = true,
            isLoading = false,
            meals = persistentListOf(
                MealHistoryVO(
                    id = "preview-1",
                    name = "치킨 샐러드",
                    foodIconId = "salad",
                    recordedAt = "08:10",
                    calorieKcal = 412,
                    orderIndex = 1,
                ),
            ).toImmutableList(),
            onToggleNutrition = {},
            onClickMeal = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun HistoryDailySectionCollapsedEmptyPreview() {
    DesignSystemTheme {
        HistoryDailySection(
            dayTitle = "7월 18일 식사",
            mealCountLabel = "0회 기록",
            nutrition = previewNutrition.copy(currentCalorieKcal = 0),
            isNutritionExpanded = false,
            isLoading = false,
            meals = persistentListOf(),
            onToggleNutrition = {},
            onClickMeal = {},
        )
    }
}
