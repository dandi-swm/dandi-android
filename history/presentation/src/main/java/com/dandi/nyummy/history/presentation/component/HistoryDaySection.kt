package com.dandi.nyummy.history.presentation.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dandi.nyummy.common.presentation.R as CommonR
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyBadge
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyButton
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyButtonSize
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyButtonStyle
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyCircularProgress
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyDivider
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyFoodIcon
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyInlineNotice
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyInlineNoticeTone
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyNutrient
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyNutrientStat
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyPose
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyPoseImage
import com.dandi.nyummy.common.presentation.designsystem.component.NyummySkeleton
import com.dandi.nyummy.common.presentation.designsystem.component.NyummySkeletonShape
import com.dandi.nyummy.common.presentation.designsystem.component.NyummySpinner
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
import com.dandi.nyummy.history.presentation.model.dayShortTitleOf
import com.dandi.nyummy.history.presentation.model.dayTitleOf
import com.dandi.nyummy.history.presentation.model.mealOrderLabelOf
import com.dandi.nyummy.history.presentation.model.meridiemTimeOf
import com.dandi.nyummy.history.presentation.model.numberLabelOf
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf

/**
 * 고른 날의 기록. "10. 5 (월)" 제목과 "N번 기록했어요" 아래에 상황에 맞는 내용 하나를 보여 준다.
 *
 * - 불러오지 못함: 오류 안내 + 다시 불러오기
 * - 불러오는 중: 스피너 + "불러오는 중이에요"
 * - 기록 없음: 과거는 유도 없이 "이날은 쉬어 갔어요", 오늘은 "지금 기록하기"
 * - 기록 있음: 하루 영양 패널(참고, 접기/펼치기)과 식사 카드(완료, 분석 중, 분석 실패)
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
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s12),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s2)) {
            NyummyText(
                text = dayShortTitleOf(date),
                style = NyummyTheme.typography.titleM,
                modifier = Modifier.semantics { contentDescription = dayTitleOf(date) },
            )
            if (showSummary) {
                NyummyText(
                    text = stringResource(R.string.history_day_summary, completedMealCount),
                    style = NyummyTheme.typography.labelS,
                    color = NyummyTheme.colors.content.brand,
                )
            }
        }
        when {
            isLoadFailed -> DayState(
                icon = CommonR.drawable.nyummy_ic_circle_alert,
                title = stringResource(R.string.history_load_failed_title),
                body = stringResource(R.string.history_load_failed_body),
                action = stringResource(R.string.history_load_retry) to onRetryLoad,
                actionStyle = NyummyButtonStyle.Neutral,
            )
            isLoading -> DayLoading()
            meals.isEmpty() && isToday -> DayState(
                title = stringResource(R.string.history_empty_today_title),
                body = stringResource(R.string.history_empty_today_body),
                action = stringResource(R.string.history_empty_today_action) to onRecordMeal,
            )
            meals.isEmpty() -> DayState(
                title = stringResource(R.string.history_empty_past_title),
                body = stringResource(R.string.history_empty_past_body),
            )
            else -> {
                if (completedMealCount > 0) {
                    NutritionPanel(nutrition = nutrition, isExpanded = isNutritionExpanded, onToggle = onToggleNutrition)
                }
                meals.forEach { meal ->
                    when {
                        meal.isAnalyzing || meal.id in reanalyzingMealIds -> AnalyzingMealCard(meal)
                        meal.isAnalysisFailed -> FailedMealCard(
                            meal = meal,
                            onRetry = if (meal.canRetryAnalysis) ({ onRetryAnalysis(meal.id) }) else null,
                            onDelete = { onDeleteFailedMeal(meal.id) },
                        )
                        else -> CompletedMealCard(meal = meal, onClick = { onClickMeal(meal.id) })
                    }
                }
            }
        }
    }
}

/** 연초록 패널: "하루 영양 1,224 kcal  접기" 머리줄과 흰 탄단지 카드. 접으면 카드를 숨긴다. */
@Composable
private fun NutritionPanel(
    nutrition: DailyNutritionVO,
    isExpanded: Boolean,
    onToggle: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(NyummyTheme.colors.bg.brandSubtle, RoundedCornerShape(NyummyTheme.radius.m))
            .padding(NyummyTheme.spacing.s16),
        verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s12),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            NyummyText(text = stringResource(R.string.history_nutrition_title), style = NyummyTheme.typography.titleS)
            Spacer(Modifier.size(NyummyTheme.spacing.s6))
            NyummyText(
                text = stringResource(R.string.history_nutrition_kcal, numberLabelOf(nutrition.currentCalorieKcal)),
                style = NyummyTheme.typography.numberS,
                color = NyummyTheme.colors.content.brand,
                modifier = Modifier.weight(1f),
            )
            Row(
                modifier = Modifier.nyummyClickable(onClick = onToggle),
                horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s2),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NyummyText(
                    text = stringResource(if (isExpanded) R.string.history_nutrition_collapse else R.string.history_nutrition_expand),
                    style = NyummyTheme.typography.bodyS,
                    color = NyummyTheme.colors.content.secondary,
                )
                Icon(
                    painter = painterResource(
                        if (isExpanded) CommonR.drawable.nyummy_ic_chevron_up else CommonR.drawable.nyummy_ic_chevron_down,
                    ),
                    contentDescription = null,
                    tint = NyummyTheme.colors.content.secondary,
                    modifier = Modifier.size(NyummyTheme.size.iconXs),
                )
            }
        }
        if (isExpanded) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NyummyTheme.colors.bg.surface, RoundedCornerShape(NyummyTheme.radius.s))
                    .padding(horizontal = NyummyTheme.spacing.s20, vertical = NyummyTheme.spacing.s16),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NyummyNutrientStat(NyummyNutrient.Carb, nutrition.carbohydrate.dailyGram, Modifier.weight(1f))
                NyummyNutrientStat(NyummyNutrient.Protein, nutrition.protein.dailyGram, Modifier.weight(1f))
                NyummyNutrientStat(NyummyNutrient.Fat, nutrition.fat.dailyGram, Modifier.weight(1f))
            }
        }
    }
}

/** 분석이 끝난 식사. 흰 카드에 끼니 태그, 시각, 이름, kcal. 누르면 상세 페이지로 간다. */
@Composable
private fun CompletedMealCard(meal: MealHistoryVO, onClick: () -> Unit) {
    MealCardFrame(
        container = NyummyTheme.colors.bg.surface,
        bordered = true,
        modifier = Modifier.nyummyClickable(onClick = onClick, pressedScale = 1f),
        leading = { NyummyFoodIcon(foodIconId = meal.foodIconId) },
        trailing = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s4),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NyummyText(
                    text = stringResource(R.string.history_nutrition_kcal, numberLabelOf(meal.calorieKcal)),
                    style = NyummyTheme.typography.numberXs,
                    color = NyummyTheme.colors.content.brand,
                )
                Icon(
                    painter = painterResource(CommonR.drawable.nyummy_ic_chevron_right),
                    contentDescription = null,
                    tint = NyummyTheme.colors.content.tertiary,
                    modifier = Modifier.size(ChevronSize),
                )
            }
        },
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s6),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NyummyBadge(text = mealOrderLabelOf(meal.orderIndex))
            NyummyText(
                text = meridiemTimeOf(meal.recordedAt),
                style = NyummyTheme.typography.bodyS,
                color = NyummyTheme.colors.content.tertiary,
            )
        }
        NyummyText(
            text = meal.name.ifBlank { stringResource(R.string.history_analysis_unknown_meal) },
            style = NyummyTheme.typography.bodyL,
            maxLines = 1,
        )
    }
}

/** 냐미가 사진을 맛보는(분석하는) 중인 식사. 연초록 카드. */
@Composable
private fun AnalyzingMealCard(meal: MealHistoryVO) {
    MealCardFrame(
        container = NyummyTheme.colors.bg.brandSubtle,
        leading = { NyummyPoseImage(pose = NyummyPose.Taste, size = ThumbnailSize) },
        trailing = { NyummyCircularProgress() },
    ) {
        StatusTexts(
            time = meridiemTimeOf(meal.recordedAt),
            title = stringResource(R.string.history_analysis_pending_title),
            body = stringResource(R.string.history_analysis_pending_body),
        )
    }
}

/** 사진에서 음식을 찾지 못한 식사. 연빨강 카드. 기록을 지울 수 있고, 서버가 다시 분석할 수 있는 상태면 재분석도 할 수 있다. */
@Composable
private fun FailedMealCard(meal: MealHistoryVO, onRetry: (() -> Unit)?, onDelete: () -> Unit) {
    MealCardFrame(
        container = NyummyTheme.colors.bg.dangerSubtle,
        alignTop = true,
        leading = { NyummyPoseImage(pose = NyummyPose.Worry, size = ThumbnailSize) },
        trailing = {
            Icon(
                painter = painterResource(CommonR.drawable.nyummy_ic_circle_alert),
                contentDescription = stringResource(R.string.history_analysis_failed_description),
                tint = NyummyTheme.colors.content.danger,
                modifier = Modifier.size(StatusIconSize),
            )
        },
    ) {
        StatusTexts(
            time = meridiemTimeOf(meal.recordedAt),
            title = stringResource(R.string.history_analysis_failed_title),
            body = stringResource(R.string.history_analysis_failed_body),
        )
        Row(
            modifier = Modifier.padding(top = NyummyTheme.spacing.s8),
            horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s8),
        ) {
            if (onRetry != null) {
                NyummyButton(
                    text = stringResource(R.string.history_analysis_retry),
                    onClick = onRetry,
                    size = NyummyButtonSize.S,
                )
            }
            NyummyText(
                text = stringResource(R.string.history_analysis_delete),
                style = NyummyTheme.typography.labelM,
                color = NyummyTheme.colors.content.danger,
                modifier = Modifier
                    .height(DeleteButtonHeight)
                    .nyummyClickable(onClick = onDelete)
                    .background(NyummyTheme.colors.bg.surface, RoundedCornerShape(NyummyTheme.radius.xs))
                    .wrapContentHeight(Alignment.CenterVertically)
                    .padding(horizontal = NyummyTheme.spacing.s12),
            )
        }
    }
}

@Composable
private fun StatusTexts(time: String, title: String, body: String) {
    NyummyText(text = time, style = NyummyTheme.typography.bodyS, color = NyummyTheme.colors.content.tertiary)
    NyummyText(text = title, style = NyummyTheme.typography.titleS, maxLines = 1)
    NyummyText(text = body, style = NyummyTheme.typography.bodyS, color = NyummyTheme.colors.content.secondary)
}

/**
 * 식사 카드의 틀. 모서리 16, 안쪽 16, 왼쪽 40 원형 썸네일, 가운데 글자, 오른쪽 표시.
 * 분석 실패처럼 행동 버튼이 붙는 카드는 [alignTop]으로 위에 맞춘다.
 */
@Composable
private fun MealCardFrame(
    container: Color,
    leading: @Composable () -> Unit,
    trailing: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    bordered: Boolean = false,
    alignTop: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(NyummyTheme.radius.m)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(container, shape)
            .then(
                if (bordered) Modifier.border(NyummyTheme.borderWidth.hairline, NyummyTheme.colors.border.default, shape) else Modifier,
            )
            .padding(NyummyTheme.spacing.s16),
        verticalAlignment = if (alignTop) Alignment.Top else Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s12),
    ) {
        Box(
            modifier = Modifier
                .size(ThumbnailSize)
                .clip(CircleShape),
            contentAlignment = Alignment.Center,
        ) { leading() }
        Column(
            modifier = Modifier
                .weight(1f)
                .semantics(mergeDescendants = true) {},
            verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s4),
            content = content,
        )
        trailing()
    }
}

/** 기록이 없거나 불러오지 못했을 때의 안내. 가운데에 (아이콘,) 제목, 설명, (작은 버튼). */
@Composable
private fun DayState(
    title: String,
    body: String,
    icon: Int? = null,
    action: Pair<String, () -> Unit>? = null,
    actionStyle: NyummyButtonStyle = NyummyButtonStyle.Tonal,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = NyummyTheme.spacing.s32),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s8),
    ) {
        if (icon != null) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = NyummyTheme.colors.content.tertiary,
                modifier = Modifier.size(StateIconSize),
            )
        }
        NyummyText(text = title, style = NyummyTheme.typography.titleS)
        NyummyText(text = body, style = NyummyTheme.typography.bodyM, color = NyummyTheme.colors.content.secondary)
        if (action != null) {
            NyummyButton(
                text = action.first,
                onClick = action.second,
                style = actionStyle,
                size = NyummyButtonSize.S,
                modifier = Modifier.padding(top = NyummyTheme.spacing.s8),
            )
        }
    }
}

@Composable
private fun DayLoading() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = NyummyTheme.spacing.s32),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s8),
    ) {
        NyummySpinner(contentDescription = null, modifier = Modifier.size(LoadingSize))
        NyummyText(
            text = stringResource(R.string.history_loading),
            style = NyummyTheme.typography.bodyM,
            color = NyummyTheme.colors.content.secondary,
        )
    }
}

private val ThumbnailSize = 40.dp
private val ChevronSize = 16.dp
private val StatusIconSize = 22.dp
private val StateIconSize = 40.dp
private val LoadingSize = 32.dp

// 옆의 "다시 분석하기"(S 버튼, 32)와 높이를 맞춘다.
private val DeleteButtonHeight = 32.dp

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
