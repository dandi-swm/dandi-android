package com.dandi.nyummy.history.presentation.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyButton
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyButtonSize
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyButtonStyle
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyCircularProgress
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyDivider
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyInlineNotice
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyInlineNoticeTone
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyNutrient
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyNutrientStat
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyPose
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyPoseImage
import com.dandi.nyummy.common.presentation.designsystem.component.NyummySkeleton
import com.dandi.nyummy.common.presentation.designsystem.component.NyummySkeletonShape
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyText
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTextButton
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTextButtonSize
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTextButtonTone
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyVerticalDivider
import com.dandi.nyummy.common.presentation.designsystem.foundation.nyummyClickable
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.history.entity.DailyNutritionVO
import com.dandi.nyummy.history.entity.HistoryDateVO
import com.dandi.nyummy.history.entity.MealAnalysisStatus
import com.dandi.nyummy.history.entity.MealHistoryVO
import com.dandi.nyummy.history.entity.NutrientProgressVO
import com.dandi.nyummy.history.presentation.R
import com.dandi.nyummy.history.presentation.model.dayTitleOf
import com.dandi.nyummy.history.presentation.model.mealOrderLabelOf
import com.dandi.nyummy.history.presentation.model.meridiemTimeOf
import com.dandi.nyummy.history.presentation.model.numberLabelOf
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import com.dandi.nyummy.common.presentation.R as CommonR

/**
 * 고른 날의 기록. 날짜 제목 아래에 상황에 맞는 내용 하나를 보여 준다.
 *
 * - 불러오지 못함: 오류 안내 + 다시 불러오기
 * - 불러오는 중: 스켈레톤
 * - 기록 없음: 과거는 유도 없이 "이날은 쉬어 갔어요", 오늘은 "지금 기록하기"
 * - 기록 있음: 하루 영양(참고, 접기/펼치기)과 식사 목록(완료, 분석 중, 분석 실패)
 *
 * 하루 영양과 "N번 기록했어요"는 분석이 끝난 식사만 센다.
 */
@Composable
internal fun HistoryDaySection(
    date: HistoryDateVO,
    isToday: Boolean,
    meals: ImmutableList<MealHistoryVO>,
    completedMealCount: Int,
    nutrition: DailyNutritionVO,
    isNutritionExpanded: Boolean,
    isLoading: Boolean,
    isLoadFailed: Boolean,
    reanalyzingMealIds: ImmutableSet<String>,
    onToggleNutrition: () -> Unit,
    onRetryLoad: () -> Unit,
    onRecordMeal: () -> Unit,
    onClickMeal: (String) -> Unit,
    onRetryAnalysis: (String) -> Unit,
    onDeleteFailedMeal: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val showSummary = !isLoading && !isLoadFailed && completedMealCount > 0
    Column(modifier = modifier.fillMaxWidth()) {
        NyummyText(text = dayTitleOf(date), style = NyummyTheme.typography.titleL)
        if (showSummary) {
            Spacer(Modifier.height(NyummyTheme.spacing.s2))
            NyummyText(
                text = stringResource(R.string.history_day_summary, completedMealCount),
                style = NyummyTheme.typography.bodyM,
                color = NyummyTheme.colors.content.tertiary,
            )
        }
        when {
            isLoadFailed -> {
                Spacer(Modifier.height(NyummyTheme.spacing.s12))
                LoadFailed(onRetry = onRetryLoad)
            }
            isLoading -> {
                Spacer(Modifier.height(NyummyTheme.spacing.s12))
                DayLoading()
            }
            meals.isEmpty() -> {
                Spacer(Modifier.height(NyummyTheme.spacing.s16))
                EmptyDay(isToday = isToday, onRecordMeal = onRecordMeal)
            }
            else -> {
                if (completedMealCount > 0) {
                    Spacer(Modifier.height(NyummyTheme.spacing.s12))
                    DayNutrition(nutrition = nutrition, isExpanded = isNutritionExpanded, onToggle = onToggleNutrition)
                }
                Spacer(Modifier.height(NyummyTheme.spacing.s12))
                meals.forEach { meal ->
                    when {
                        meal.isAnalyzing || meal.id in reanalyzingMealIds -> AnalyzingMealRow(meal)
                        meal.isAnalysisFailed -> FailedMealRow(
                            meal = meal,
                            onRetry = { onRetryAnalysis(meal.id) },
                            onDelete = { onDeleteFailedMeal(meal.id) },
                        )
                        else -> CompletedMealRow(meal = meal, onClick = { onClickMeal(meal.id) })
                    }
                }
            }
        }
    }
}

/** "하루 영양 1,224 kcal  접기" 머리줄과 탄단지 카드. 접으면 카드를 숨긴다. */
@Composable
private fun DayNutrition(
    nutrition: DailyNutritionVO,
    isExpanded: Boolean,
    onToggle: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        NyummyText(text = stringResource(R.string.history_nutrition_title), style = NyummyTheme.typography.titleS)
        Spacer(Modifier.size(NyummyTheme.spacing.s8))
        NyummyText(
            text = stringResource(R.string.history_nutrition_kcal, numberLabelOf(nutrition.currentCalorieKcal)),
            style = NyummyTheme.typography.numberS,
            color = NyummyTheme.colors.content.tertiary,
            modifier = Modifier.weight(1f),
        )
        NyummyTextButton(
            text = stringResource(if (isExpanded) R.string.history_nutrition_collapse else R.string.history_nutrition_expand),
            onClick = onToggle,
            tone = NyummyTextButtonTone.Neutral,
            size = NyummyTextButtonSize.S,
            trailingIcon = if (isExpanded) CommonR.drawable.nyummy_ic_chevron_up else CommonR.drawable.nyummy_ic_chevron_down,
        )
    }
    if (isExpanded) {
        Spacer(Modifier.height(NutritionCardTopGap))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(NyummyTheme.radius.m))
                .background(NyummyTheme.colors.bg.surfaceSunken)
                .padding(horizontal = NyummyTheme.spacing.s12, vertical = NutritionCardVerticalPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NutrientCell(NyummyNutrient.Carb, nutrition.carbohydrate.dailyGram)
            NyummyVerticalDivider(height = NutrientDividerHeight)
            NutrientCell(NyummyNutrient.Protein, nutrition.protein.dailyGram)
            NyummyVerticalDivider(height = NutrientDividerHeight)
            NutrientCell(NyummyNutrient.Fat, nutrition.fat.dailyGram)
        }
    }
}

@Composable
private fun RowScope.NutrientCell(nutrient: NyummyNutrient, grams: Int) {
    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
        NyummyNutrientStat(nutrient = nutrient, grams = grams)
    }
}

/** 분석이 끝난 식사. 누르면 상세가 열린다. */
@Composable
private fun CompletedMealRow(meal: MealHistoryVO, onClick: () -> Unit) {
    MealRowFrame(
        leading = {
            Box(Modifier.size(FoodIconSize), contentAlignment = Alignment.Center) {
                HistoryFoodIcon(foodIconId = meal.foodIconId, sizeFraction = 1f)
            }
        },
        meta = stringResource(
            R.string.history_meal_meta,
            mealOrderLabelOf(meal.orderIndex),
            meridiemTimeOf(meal.recordedAt),
        ),
        title = meal.name.ifBlank { stringResource(R.string.history_analysis_unknown_meal) },
        sub = stringResource(R.string.history_nutrition_kcal, numberLabelOf(meal.calorieKcal)),
        modifier = Modifier.nyummyClickable(onClick = onClick, pressedScale = 1f),
        trailing = {
            Image(
                painter = painterResource(CommonR.drawable.nyummy_ic_chevron_right),
                contentDescription = null,
                colorFilter = ColorFilter.tint(NyummyTheme.colors.content.tertiary),
                modifier = Modifier.size(ChevronSize),
            )
        },
    )
}

/** 냐미가 사진을 맛보는(분석하는) 중인 식사. */
@Composable
private fun AnalyzingMealRow(meal: MealHistoryVO) {
    MealRowFrame(
        leading = { NyummyPoseImage(pose = NyummyPose.Taste, size = LeadingSize) },
        meta = meridiemTimeOf(meal.recordedAt),
        title = stringResource(R.string.history_analysis_pending_title),
        sub = stringResource(R.string.history_analysis_pending_body),
        trailing = { NyummyCircularProgress() },
    )
}

/** 사진에서 음식을 찾지 못한 식사. 다시 분석하거나 기록을 지울 수 있다. */
@Composable
private fun FailedMealRow(meal: MealHistoryVO, onRetry: () -> Unit, onDelete: () -> Unit) {
    val failedDescription = stringResource(R.string.history_analysis_failed_description)
    MealRowFrame(
        leading = { NyummyPoseImage(pose = NyummyPose.Worry, size = LeadingSize) },
        meta = meridiemTimeOf(meal.recordedAt),
        metaColor = NyummyTheme.colors.content.danger,
        title = stringResource(R.string.history_analysis_failed_title),
        sub = stringResource(R.string.history_analysis_failed_body),
        alignTop = true,
        actions = {
            Row(
                modifier = Modifier.padding(top = NyummyTheme.spacing.s2),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s8),
            ) {
                NyummyTextButton(
                    text = stringResource(R.string.history_analysis_retry),
                    onClick = onRetry,
                    size = NyummyTextButtonSize.S,
                    leadingIcon = CommonR.drawable.nyummy_ic_refresh_cw,
                )
                NyummyVerticalDivider(height = ActionDividerHeight)
                NyummyTextButton(
                    text = stringResource(R.string.history_analysis_delete),
                    onClick = onDelete,
                    tone = NyummyTextButtonTone.Danger,
                    size = NyummyTextButtonSize.S,
                )
            }
        },
        trailing = {
            Image(
                painter = painterResource(CommonR.drawable.nyummy_ic_circle_alert),
                contentDescription = failedDescription,
                colorFilter = ColorFilter.tint(NyummyTheme.colors.content.danger),
                modifier = Modifier.size(NyummyTheme.size.iconL),
            )
        },
    )
}

/**
 * 식사 한 줄의 틀. 왼쪽 52 자리, 시각(label/s) + 제목(title/s) + 보조 문구(body/s), 오른쪽 끝 표시, 아래 구분선.
 * 분석 실패처럼 행동 버튼이 붙는 줄은 [alignTop]으로 위에 맞춘다.
 */
@Composable
private fun MealRowFrame(
    leading: @Composable () -> Unit,
    meta: String,
    title: String,
    sub: String,
    trailing: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    metaColor: Color = NyummyTheme.colors.content.brand,
    alignTop: Boolean = false,
    actions: (@Composable () -> Unit)? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(vertical = MealRowVerticalPadding),
            verticalAlignment = if (alignTop) Alignment.Top else Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s12),
        ) {
            Box(
                modifier = Modifier
                    .size(LeadingSize)
                    .clip(RoundedCornerShape(NyummyTheme.radius.s)),
                contentAlignment = Alignment.Center,
            ) { leading() }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .semantics(mergeDescendants = true) {},
                verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s2),
            ) {
                NyummyText(text = meta, style = NyummyTheme.typography.labelS, color = metaColor)
                NyummyText(text = title, style = NyummyTheme.typography.titleS, maxLines = 1)
                NyummyText(
                    text = sub,
                    style = NyummyTheme.typography.bodyS,
                    color = NyummyTheme.colors.content.tertiary,
                )
                actions?.invoke()
            }
            trailing()
        }
        NyummyDivider()
    }
}

/** 고른 날 기록이 없을 때. 과거는 쉬어 간 날로 두고, 오늘은 기록하러 가는 길을 준다. */
@Composable
private fun EmptyDay(isToday: Boolean, onRecordMeal: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = NyummyTheme.spacing.s12),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(EmptyGap),
    ) {
        if (isToday) {
            NyummyPoseImage(pose = NyummyPose.Gasp, size = NyummyTheme.size.characterM)
            NyummyText(text = stringResource(R.string.history_empty_today_title), style = NyummyTheme.typography.titleS)
            NyummyText(
                text = stringResource(R.string.history_empty_today_body),
                style = NyummyTheme.typography.bodyS,
                color = NyummyTheme.colors.content.tertiary,
            )
            NyummyTextButton(
                text = stringResource(R.string.history_empty_today_action),
                onClick = onRecordMeal,
                trailingIcon = CommonR.drawable.nyummy_ic_chevron_right,
                modifier = Modifier.padding(top = NyummyTheme.spacing.s2),
            )
        } else {
            NyummyPoseImage(pose = NyummyPose.Sleep, size = EmptyPastPoseSize)
            NyummyText(text = stringResource(R.string.history_empty_past_title), style = NyummyTheme.typography.titleS)
            NyummyText(
                text = stringResource(R.string.history_empty_past_body),
                style = NyummyTheme.typography.bodyS,
                color = NyummyTheme.colors.content.tertiary,
            )
        }
    }
}

/** 하루 영양 카드 하나와 식사 줄 둘 크기의 자리. */
@Composable
private fun DayLoading() {
    Column(verticalArrangement = Arrangement.spacedBy(SkeletonGap)) {
        listOf(SkeletonNutritionHeight, SkeletonRowHeight, SkeletonRowHeight).forEach { height ->
            NyummySkeleton(
                shape = NyummySkeletonShape.Block,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height),
            )
        }
    }
}

@Composable
private fun LoadFailed(onRetry: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(SkeletonGap)) {
        NyummyInlineNotice(
            title = stringResource(R.string.history_load_failed_title),
            body = stringResource(R.string.history_load_failed_body),
            tone = NyummyInlineNoticeTone.Danger,
            modifier = Modifier.fillMaxWidth(),
        )
        NyummyButton(
            text = stringResource(R.string.history_load_retry),
            onClick = onRetry,
            style = NyummyButtonStyle.Secondary,
            size = NyummyButtonSize.M,
            icon = CommonR.drawable.nyummy_ic_refresh_cw,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private val NutritionCardTopGap = 10.dp
private val NutritionCardVerticalPadding = 14.dp
private val NutrientDividerHeight = 28.dp
private val MealRowVerticalPadding = 14.dp
private val LeadingSize = 52.dp
private val FoodIconSize = 44.dp
private val ChevronSize = 20.dp
private val ActionDividerHeight = 12.dp
private val EmptyGap = 6.dp
private val EmptyPastPoseSize = 112.dp
private val SkeletonGap = 10.dp
private val SkeletonNutritionHeight = 132.dp
private val SkeletonRowHeight = 76.dp

private val previewMeals = persistentListOf(
    MealHistoryVO(id = "1", name = "비빔밥", recordedAt = "08:10", calorieKcal = 612, orderIndex = 1),
    MealHistoryVO(id = "2", name = "클럽 샌드위치", recordedAt = "12:40", calorieKcal = 612, orderIndex = 2),
    MealHistoryVO(id = "3", recordedAt = "19:05", status = MealAnalysisStatus.ANALYZING),
    MealHistoryVO(id = "4", recordedAt = "21:20", status = MealAnalysisStatus.FAILED),
)

@Composable
private fun PreviewSection(
    meals: ImmutableList<MealHistoryVO> = previewMeals,
    isToday: Boolean = true,
    isLoading: Boolean = false,
    isLoadFailed: Boolean = false,
) {
    NyummyTheme {
        HistoryDaySection(
            date = HistoryDateVO(2026, 10, 5),
            isToday = isToday,
            meals = meals,
            completedMealCount = meals.count { it.isAnalysisCompleted },
            nutrition = DailyNutritionVO(
                currentCalorieKcal = 1_224,
                carbohydrate = NutrientProgressVO(dailyGram = 152),
                protein = NutrientProgressVO(dailyGram = 48),
                fat = NutrientProgressVO(dailyGram = 39),
            ),
            isNutritionExpanded = true,
            isLoading = isLoading,
            isLoadFailed = isLoadFailed,
            reanalyzingMealIds = persistentSetOf(),
            onToggleNutrition = {},
            onRetryLoad = {},
            onRecordMeal = {},
            onClickMeal = {},
            onRetryAnalysis = {},
            onDeleteFailedMeal = {},
            modifier = Modifier.padding(horizontal = PreviewGutter),
        )
    }
}

private val PreviewGutter: Dp = 20.dp

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun HistoryDaySectionPreview() = PreviewSection()

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun HistoryDaySectionEmptyTodayPreview() = PreviewSection(meals = persistentListOf())

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun HistoryDaySectionEmptyPastPreview() = PreviewSection(meals = persistentListOf(), isToday = false)

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun HistoryDaySectionLoadingPreview() = PreviewSection(isLoading = true)

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun HistoryDaySectionErrorPreview() = PreviewSection(isLoadFailed = true)
