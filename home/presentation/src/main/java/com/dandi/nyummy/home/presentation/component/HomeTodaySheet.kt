package com.dandi.nyummy.home.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyBadgeTone
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyBottomSheet
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyButton
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyButtonStyle
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyMealRow
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyMealRowPlaceholder
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyNutrient
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyNutrientStat
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyPose
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyPoseImage
import com.dandi.nyummy.common.presentation.designsystem.component.NyummySheetTitle
import com.dandi.nyummy.common.presentation.designsystem.component.NyummySkeleton
import com.dandi.nyummy.common.presentation.designsystem.component.NyummySkeletonShape
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyText
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTextButton
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.history.entity.DailyMealHistoryVO
import com.dandi.nyummy.history.entity.MealHistoryVO
import com.dandi.nyummy.home.presentation.R
import com.dandi.nyummy.common.presentation.R as CommonR

/**
 * 오늘 식사 시트. "오늘 식사", 기록 횟수, 탄단지 합계, 오늘의 식사 목록, "식사 추가하기".
 *
 * 영양 압박을 주지 않도록 남은 kcal이나 목표 대비 %는 보여 주지 않고, 탄단지는 그램만 참고로 둔다.
 * 내용은 시트를 열 때마다 새로 읽고, 읽는 동안에는 자리만 잡아 두며, 실패하면 다시 시도할 수 있다.
 */
@Composable
internal fun HomeTodaySheet(
    recordedCount: Int,
    meals: DailyMealHistoryVO?,
    isLoading: Boolean,
    isFailed: Boolean,
    onRetry: () -> Unit,
    onAddMeal: () -> Unit,
    onDismiss: () -> Unit,
) {
    NyummyBottomSheet(onDismissRequest = onDismiss, modifier = Modifier.testTag(TodaySheetTag)) {
        Column {
            NyummySheetTitle(text = stringResource(R.string.home_sheet_title))
            Spacer(Modifier.height(NyummyTheme.spacing.s4))
            NyummyText(
                text = stringResource(R.string.home_sheet_recorded, recordedCount),
                style = NyummyTheme.typography.bodyM,
                color = NyummyTheme.colors.content.secondary,
            )
            Spacer(Modifier.height(NyummyTheme.spacing.s16))
            when {
                meals != null && !isLoading -> TodayMealsContent(meals)
                isFailed -> TodayMealsFailed(onRetry = onRetry)
                else -> TodayMealsLoading()
            }
            Spacer(Modifier.height(NyummyTheme.spacing.s20))
            NyummyButton(
                text = stringResource(R.string.home_sheet_add_meal),
                onClick = onAddMeal,
                style = NyummyButtonStyle.Secondary,
                icon = CommonR.drawable.nyummy_ic_camera,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun TodayMealsContent(meals: DailyMealHistoryVO) {
    Row(horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s8)) {
        NyummyNutrientStat(NyummyNutrient.Carb, meals.nutrition.carbohydrate.dailyGram, Modifier.weight(1f))
        NyummyNutrientStat(NyummyNutrient.Protein, meals.nutrition.protein.dailyGram, Modifier.weight(1f))
        NyummyNutrientStat(NyummyNutrient.Fat, meals.nutrition.fat.dailyGram, Modifier.weight(1f))
    }
    Spacer(Modifier.height(SectionGap))
    NyummyText(text = stringResource(R.string.home_sheet_section_title), style = NyummyTheme.typography.titleS)
    Spacer(Modifier.height(NyummyTheme.spacing.s8))
    Column(verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s8)) {
        meals.meals.forEach { meal -> TodayMealRow(meal) }
    }
}

/** 식사 한 줄. 분석 중이거나 실패한 기록은 이름 대신 상태 문구와 냐미를 보여 준다. */
@Composable
private fun TodayMealRow(meal: MealHistoryVO) {
    val time = mealTimeText(meal.recordedAt)
    when {
        meal.isAnalyzing -> NyummyMealRow(
            title = stringResource(R.string.home_meal_analyzing_title),
            subtitle = time,
            leading = { NyummyPoseImage(pose = NyummyPose.Ask, size = LeadingSize) },
            badge = stringResource(R.string.home_meal_analyzing_badge),
            badgeTone = NyummyBadgeTone.Info,
        )
        meal.isAnalysisFailed -> NyummyMealRow(
            title = stringResource(R.string.home_meal_failed_title),
            subtitle = time,
            leading = { NyummyPoseImage(pose = NyummyPose.Worry, size = LeadingSize) },
            badge = stringResource(R.string.home_meal_failed_badge),
            badgeTone = NyummyBadgeTone.Danger,
        )
        else -> NyummyMealRow(
            title = meal.name,
            subtitle = time,
            leading = {
                if (meal.photoUrl.isNotBlank()) {
                    AsyncImage(
                        model = meal.photoUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize(),
                    )
                } else {
                    NyummyMealRowPlaceholder()
                }
            },
        )
    }
}

@Composable
private fun TodayMealsLoading() {
    Column(verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s8)) {
        NyummySkeleton(shape = NyummySkeletonShape.Block, modifier = Modifier.fillMaxWidth().height(NutrientRowHeight))
        NyummySkeleton(shape = NyummySkeletonShape.Block, modifier = Modifier.fillMaxWidth().height(MealRowHeight))
    }
}

@Composable
private fun TodayMealsFailed(onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = NyummyTheme.spacing.s8),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        NyummyText(
            text = stringResource(R.string.home_sheet_load_failed),
            style = NyummyTheme.typography.bodyM,
            color = NyummyTheme.colors.content.secondary,
        )
        NyummyTextButton(text = stringResource(R.string.home_sheet_retry), onClick = onRetry)
    }
}

/** "08:10", "12:24" 같은 24시간 표기를 "오전 8:10", "오후 12:24"로 바꾼다. 읽지 못하면 그대로 둔다. */
@Composable
private fun mealTimeText(recordedAt: String): String {
    val parts = recordedAt.split(":")
    val hour = parts.getOrNull(0)?.toIntOrNull()
    val minute = parts.getOrNull(1)?.toIntOrNull()
    if (hour == null || minute == null || hour !in 0..23) return recordedAt
    return stringResource(
        R.string.home_meal_time,
        stringResource(if (hour >= 12) R.string.home_meal_pm else R.string.home_meal_am),
        (hour + 11) % 12 + 1,
        minute,
    )
}

internal const val TodaySheetTag = "home_today_sheet"
private val SectionGap = 28.dp
private val LeadingSize = 48.dp
private val NutrientRowHeight = 38.dp
private val MealRowHeight = 76.dp

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun TodayMealsContentPreview() {
    NyummyTheme {
        Column(Modifier.padding(NyummyTheme.spacing.gutter)) {
            TodayMealsContent(
                DailyMealHistoryVO(meals = listOf(MealHistoryVO(id = "1", name = "닭가슴살 샐러드", recordedAt = "12:24"))),
            )
        }
    }
}
