package com.dandi.nyummy.history.presentation.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dandi.nyummy.common.presentation.component.DandiText
import com.dandi.nyummy.common.presentation.component.NyummyBadge
import com.dandi.nyummy.common.presentation.component.NyummyBadgeTone
import com.dandi.nyummy.common.presentation.component.NyummyLinearProgress
import com.dandi.nyummy.common.presentation.component.NyummyMascot
import com.dandi.nyummy.common.presentation.component.NyummyMascotPose
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemTheme
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemThemeImpl
import com.dandi.nyummy.common.presentation.ui.theme.designSystemDropShadow
import com.dandi.nyummy.history.entity.DailyNutritionVO
import com.dandi.nyummy.history.entity.MealAnalysisStatus
import com.dandi.nyummy.history.entity.MealHistoryVO
import com.dandi.nyummy.history.entity.NutrientProgressVO
import com.dandi.nyummy.history.presentation.R
import com.dandi.nyummy.common.presentation.R as CommonR
import com.dandi.nyummy.history.presentation.model.mealOrderLabelOf
import com.dandi.nyummy.history.presentation.model.meridiemTimeOf
import com.dandi.nyummy.history.presentation.model.numberLabelOf
import com.dandi.nyummy.history.presentation.model.percentOf
import com.dandi.nyummy.history.presentation.model.progressOf
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList

/**
 * 선택한 날짜의 식사 섹션입니다.
 * 날짜 헤더, 접을 수 있는 `하루 영양 현황` 카드, 식사 목록(없으면 안내 문구)으로 구성됩니다.
 *
 * 분석이 끝나지 않았거나 실패한 기록은 이름·열량이 비어 있어 일반 식사 행 대신
 * 상태 카드([HistoryMealAnalyzingCard] / [HistoryMealFailedCard])로 그립니다.
 *
 * @param reanalyzingMealIds 재분석을 요청해 결과를 기다리는 중인 식사 식별자들
 */
@Composable
internal fun HistoryDailySection(
    dayTitle: String,
    mealCountLabel: String,
    nutrition: DailyNutritionVO,
    isNutritionExpanded: Boolean,
    isLoading: Boolean,
    meals: ImmutableList<MealHistoryVO>,
    reanalyzingMealIds: ImmutableSet<String>,
    onToggleNutrition: () -> Unit,
    onClickMeal: (String) -> Unit,
    onRetryAnalysis: (String) -> Unit,
    onDeleteFailedMeal: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = DesignSystemThemeImpl.designSystemLayout.mobileGutter),
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
                style = DesignSystemThemeImpl.typeScale.titleStrongXL,
            )
            NyummyBadge(
                label = mealCountLabel,
                tone = NyummyBadgeTone.Positive,
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = EmptyMessageVerticalGap),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                NyummyMascot(
                    pose = NyummyMascotPose.Sleeping,
                    contentDescription = null,
                    modifier = Modifier.size(EmptyMascotSize),
                )
                Spacer(Modifier.height(DesignSystemThemeImpl.designSystemSpacing.space12))
                DandiText(
                    text = stringResource(R.string.history_empty_meals),
                    color = DesignSystemThemeImpl.designSystemColor.contentDefaultLevel2,
                    style = DesignSystemThemeImpl.typeScale.textRegularM,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(MealRowGap),
            ) {
                meals.forEach { meal ->
                    when {
                        meal.isAnalyzing || meal.id in reanalyzingMealIds ->
                            HistoryMealAnalyzingCard(meal = meal)

                        meal.isAnalysisFailed -> HistoryMealFailedCard(
                            meal = meal,
                            onRetry = { onRetryAnalysis(meal.id) },
                            onDelete = { onDeleteFailedMeal(meal.id) },
                        )

                        else -> HistoryMealRow(
                            meal = meal,
                            mealCount = meals.count { it.isAnalysisCompleted },
                            onClick = { onClickMeal(meal.id) },
                        )
                    }
                }
            }
        }
    }
}

/**
 * 식사 한 건 카드. 상단에 "첫 끼 · 오후 12:30" 라벨, 가운데에 아이콘·이름·이동 셰브론,
 * 하단에 열량을 쌓는다.
 */
@Composable
private fun HistoryMealRow(
    meal: MealHistoryVO,
    mealCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = DesignSystemThemeImpl.designSystemColor
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(DesignSystemThemeImpl.designSystemRadius.radius16),
        color = colors.bgDefaultLevel1,
        contentColor = colors.contentDefaultLevel0,
        border = BorderStroke(MealRowBorderWidth, colors.borderCardSubtle),
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = MealRowInset,
                vertical = MealRowVerticalInset,
            ),
        ) {
            DandiText(
                text = "${mealOrderLabelOf(meal.orderIndex, mealCount)} · ${meridiemTimeOf(meal.recordedAt)}",
                color = colors.contentAccentSage,
                style = DesignSystemThemeImpl.typeScale.labelStrongS,
            )
            Spacer(Modifier.height(MealRowLabelGap))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(MealRowIconSize),
                    contentAlignment = Alignment.Center,
                ) {
                    HistoryFoodIcon(meal.foodIconId, sizeFraction = MealRowIconFraction)
                }
                Spacer(Modifier.width(MealRowIconGap))
                DandiText(
                    text = meal.name,
                    modifier = Modifier.weight(1f),
                    color = colors.contentDefaultLevel0,
                    style = DesignSystemThemeImpl.typeScale.textStrongL,
                )
                Icon(
                    painter = painterResource(CommonR.drawable.nyummy_icon_chevron_right),
                    contentDescription = null,
                    modifier = Modifier.size(MealRowChevronSize),
                    tint = colors.contentIconLevel1,
                )
            }
            Spacer(Modifier.height(MealRowCalorieGap))
            DandiText(
                text = "${numberLabelOf(meal.calorieKcal)} kcal",
                color = colors.contentDefaultLevel2,
                style = DesignSystemThemeImpl.typeScale.textRegularS,
            )
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
            Spacer(Modifier.height(NutritionCalorieTopGap))
            Row(verticalAlignment = Alignment.Bottom) {
                DandiText(
                    text = if (isLoading) "—" else numberLabelOf(nutrition.currentCalorieKcal),
                    color = colors.contentAccentSage,
                    style = DesignSystemThemeImpl.typeScale.numberStrongL,
                )
                Spacer(Modifier.width(NutritionCalorieUnitGap))
                DandiText(
                    text = "/ ${numberLabelOf(nutrition.targetCalorieKcal)} kcal",
                    modifier = Modifier.padding(bottom = NutritionCalorieUnitBaselineLift),
                    color = colors.contentDefaultLevel2,
                    style = DesignSystemThemeImpl.typeScale.textRegularL,
                )
            }
            Spacer(Modifier.height(NutritionProgressTopGap))
            Row(verticalAlignment = Alignment.CenterVertically) {
                NyummyLinearProgress(
                    progress = if (isLoading) {
                        0f
                    } else {
                        progressOf(nutrition.currentCalorieKcal, nutrition.targetCalorieKcal)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(NutritionProgressHeight),
                )
                Spacer(Modifier.width(NutritionProgressPercentGap))
                DandiText(
                    text = if (isLoading) {
                        "—"
                    } else {
                        "${percentOf(nutrition.currentCalorieKcal, nutrition.targetCalorieKcal)}%"
                    },
                    color = colors.contentAccentSage,
                    style = DesignSystemThemeImpl.typeScale.numberStrongM,
                )
            }
            if (expanded) {
                Spacer(Modifier.height(NutritionMacroTopGap))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(NutritionDividerHeight)
                        .background(colors.borderDefaultLevel1),
                )
                Spacer(Modifier.height(NutritionMacroTopGap))
                Row(modifier = Modifier.fillMaxWidth()) {
                    NutritionMacro(
                        label = stringResource(R.string.history_macro_carbohydrate),
                        progress = nutrition.carbohydrate,
                        color = colors.dataNutrientCarbohydrate,
                        isLoading = isLoading,
                        modifier = Modifier.weight(1f),
                    )
                    NutritionMacro(
                        label = stringResource(R.string.history_macro_protein),
                        progress = nutrition.protein,
                        color = colors.dataNutrientProtein,
                        isLoading = isLoading,
                        modifier = Modifier.weight(1f),
                    )
                    NutritionMacro(
                        label = stringResource(R.string.history_macro_fat),
                        progress = nutrition.fat,
                        color = colors.dataNutrientFat,
                        isLoading = isLoading,
                        modifier = Modifier.weight(1f),
                    )
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

/** 색 도트 + 영양소 이름 위에 섭취 그램을 쌓아 보여주는 열입니다. */
@Composable
private fun NutritionMacro(
    label: String,
    progress: NutrientProgressVO,
    color: Color,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = DesignSystemThemeImpl.designSystemColor
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(NutritionMacroDotSize)
                    .background(color, DesignSystemThemeImpl.designSystemShape.pill),
            )
            Spacer(Modifier.width(NutritionMacroDotGap))
            DandiText(
                text = label,
                color = colors.contentDefaultLevel1,
                style = DesignSystemThemeImpl.typeScale.textRegularS,
            )
        }
        Spacer(Modifier.height(NutritionMacroValueGap))
        DandiText(
            text = if (isLoading) "—" else "${progress.dailyGram}g",
            color = colors.contentDefaultLevel0,
            style = DesignSystemThemeImpl.typeScale.textStrongL,
        )
    }
}

private val DailyHeaderBottomGap = 14.dp
private val DailyNutritionBottomGap = 14.dp
private val EmptyMessageVerticalGap = 28.dp
private val EmptyMascotSize = 160.dp
private val MealRowGap = 8.dp

private val NutritionCardBorderWidth = 1.dp
private val NutritionToggleChevronGap = 2.dp
private val NutritionToggleChevronSize = 16.dp
private val NutritionCardInset = 16.dp
private val NutritionCardVerticalInset = 14.dp
private val NutritionCalorieTopGap = 8.dp
private val NutritionCalorieUnitGap = 6.dp
private val NutritionCalorieUnitBaselineLift = 3.dp
private val NutritionProgressTopGap = 10.dp
private val NutritionProgressHeight = 8.dp
private val NutritionProgressPercentGap = 12.dp
private val NutritionMacroTopGap = 12.dp
private val NutritionDividerHeight = 1.dp
private val NutritionMacroDotSize = 8.dp
private val NutritionMacroDotGap = 6.dp
private val NutritionMacroValueGap = 4.dp
private val MealRowBorderWidth = 1.dp
private val MealRowInset = 16.dp
private val MealRowVerticalInset = 12.dp
private val MealRowLabelGap = 6.dp
private val MealRowIconSize = 40.dp
private const val MealRowIconFraction = 0.75f
private val MealRowIconGap = 12.dp
private val MealRowChevronSize = 20.dp
private val MealRowCalorieGap = 4.dp

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
                MealHistoryVO(
                    id = "preview-2",
                    recordedAt = "12:30",
                    orderIndex = 2,
                    status = MealAnalysisStatus.FAILED,
                ),
            ).toImmutableList(),
            reanalyzingMealIds = persistentSetOf(),
            onToggleNutrition = {},
            onClickMeal = {},
            onRetryAnalysis = {},
            onDeleteFailedMeal = {},
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
            reanalyzingMealIds = persistentSetOf(),
            onToggleNutrition = {},
            onClickMeal = {},
            onRetryAnalysis = {},
            onDeleteFailedMeal = {},
        )
    }
}
