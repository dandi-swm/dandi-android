package com.dandi.nyummy.history.presentation.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dandi.nyummy.common.presentation.component.DandiText
import com.dandi.nyummy.common.presentation.component.NyummyButton
import com.dandi.nyummy.common.presentation.component.NyummyButtonStyle
import com.dandi.nyummy.common.presentation.component.NyummyDestructiveDialog
import com.dandi.nyummy.common.presentation.component.NyummyEditDialog
import com.dandi.nyummy.common.presentation.component.NyummyModalScrim
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemTheme
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemThemeImpl
import com.dandi.nyummy.common.presentation.ui.theme.designSystemDropShadow
import com.dandi.nyummy.history.entity.DailyNutritionVO
import com.dandi.nyummy.history.entity.HistoryDateVO
import com.dandi.nyummy.history.entity.MealHistoryVO
import com.dandi.nyummy.history.entity.NutrientProgressVO
import com.dandi.nyummy.history.presentation.HistoryIntent
import com.dandi.nyummy.history.presentation.HistoryMealDetailMode
import com.dandi.nyummy.history.presentation.HistoryMealDetailUiState
import com.dandi.nyummy.history.presentation.R
import com.dandi.nyummy.history.presentation.model.dayLabelOf
import com.dandi.nyummy.history.presentation.model.mealOrderLabelOf
import com.dandi.nyummy.history.presentation.model.numberLabelOf
import com.dandi.nyummy.history.presentation.model.percentOf

/**
 * 히스토리 위에 뜨는 식사 상세 오버레이입니다.
 *
 * 보기 모드에서는 상세 카드를, 이름 수정/삭제 확인 모드에서는 해당 다이얼로그를 보여줍니다.
 */
@Composable
internal fun HistoryMealDetailOverlay(
    detail: HistoryMealDetailUiState,
    selectedDate: HistoryDateVO,
    mealCount: Int,
    dailyNutrition: DailyNutritionVO,
    onIntent: (HistoryIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        NyummyModalScrim(
            onDismissRequest = when (detail.mode) {
                HistoryMealDetailMode.Viewing -> {
                    { onIntent(HistoryIntent.DismissMealDetail) }
                }

                HistoryMealDetailMode.EditingName,
                HistoryMealDetailMode.ConfirmingDelete,
                -> null
            },
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(vertical = OverlayVerticalInset),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
        ) {
            when (detail.mode) {
                HistoryMealDetailMode.Viewing -> HistoryMealDetailCard(
                    meal = detail.meal,
                    selectedDate = selectedDate,
                    mealCount = mealCount,
                    dailyNutrition = dailyNutrition,
                    onIntent = onIntent,
                )

                HistoryMealDetailMode.EditingName -> NyummyEditDialog(
                    title = stringResource(R.string.history_edit_dialog_title),
                    fieldLabel = stringResource(R.string.history_edit_dialog_field_label),
                    fieldValue = detail.nameDraft,
                    onFieldValueChange = { onIntent(HistoryIntent.ChangeMealNameDraft(it)) },
                    timeLabel = stringResource(
                        R.string.history_edit_dialog_time_label,
                        detail.meal.recordedAt,
                    ),
                    cancelLabel = stringResource(R.string.history_edit_dialog_cancel),
                    confirmLabel = stringResource(R.string.history_edit_dialog_confirm),
                    onCancel = { onIntent(HistoryIntent.CancelEditMealName) },
                    onConfirm = { onIntent(HistoryIntent.ConfirmEditMealName) },
                )

                HistoryMealDetailMode.ConfirmingDelete -> NyummyDestructiveDialog(
                    title = stringResource(R.string.history_delete_dialog_title),
                    body = stringResource(R.string.history_delete_dialog_body),
                    targetLabel = "${detail.meal.name} · ${detail.meal.recordedAt}",
                    helper = stringResource(R.string.history_delete_dialog_helper),
                    cancelLabel = stringResource(R.string.history_delete_dialog_cancel),
                    confirmLabel = stringResource(R.string.history_delete_dialog_confirm),
                    onCancel = { onIntent(HistoryIntent.CancelDeleteMeal) },
                    onConfirm = { onIntent(HistoryIntent.ConfirmDeleteMeal) },
                )
            }
        }
    }
}

@Composable
private fun HistoryMealDetailCard(
    meal: MealHistoryVO,
    selectedDate: HistoryDateVO,
    mealCount: Int,
    dailyNutrition: DailyNutritionVO,
    onIntent: (HistoryIntent) -> Unit,
) {
    val colors = DesignSystemThemeImpl.designSystemColor
    val shape = DesignSystemThemeImpl.designSystemShape.dialogDefault
    Surface(
        modifier = Modifier
            .width(DetailCardWidth)
            .designSystemDropShadow(
                shape = shape,
                shadow = DesignSystemThemeImpl.designSystemElevation.dialogStandard,
            ),
        shape = shape,
        color = colors.bgDefaultLevel1,
        contentColor = colors.contentDefaultLevel0,
    ) {
        Column(modifier = Modifier.padding(DetailCardInset)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    DandiText(
                        text = "${dayLabelOf(selectedDate)} · ${mealOrderLabelOf(meal.orderIndex, mealCount)}",
                        color = colors.contentNutritionLabel,
                        style = DesignSystemThemeImpl.typeScale.labelStrongS,
                    )
                    Spacer(Modifier.height(DetailTitleTopGap))
                    DandiText(
                        text = meal.name,
                        color = colors.contentDefaultLevel0,
                        style = DesignSystemThemeImpl.typeScale.displayRegularL,
                    )
                }
                NyummyButton(
                    label = stringResource(R.string.history_detail_close),
                    style = NyummyButtonStyle.Secondary,
                    onClick = { onIntent(HistoryIntent.DismissMealDetail) },
                )
            }
            Spacer(Modifier.height(DetailHeaderBottomGap))
            Row {
                Surface(
                    modifier = Modifier.size(DetailPhotoSize),
                    shape = RoundedCornerShape(DesignSystemThemeImpl.designSystemRadius.radius16),
                    color = colors.bgMealPhoto,
                    border = BorderStroke(DetailBorderWidth, colors.borderMealPhoto),
                ) {
                    Box(
                        modifier = Modifier.padding(DetailPhotoPadding),
                        contentAlignment = Alignment.Center,
                    ) {
                        // TODO: 사진 URL 로딩 연동 (백엔드 미구현) — 지금은 음식 아이콘으로 대체
                        HistoryFoodIcon(meal.foodIconId)
                    }
                }
                Spacer(Modifier.width(DetailPhotoRightGap))
                Column(modifier = Modifier.weight(1f)) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(DetailTimeChipHeight),
                        shape = RoundedCornerShape(DesignSystemThemeImpl.designSystemRadius.radius16),
                        color = colors.bgWarningSoft,
                        contentColor = colors.contentWarning,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = DetailTimeChipInset),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            DandiText(
                                text = stringResource(R.string.history_detail_captured_title),
                                modifier = Modifier.weight(1f),
                                color = colors.contentNutritionLabel,
                                style = DesignSystemThemeImpl.typeScale.labelStrongS,
                            )
                            DandiText(
                                text = meal.recordedAt,
                                color = colors.contentWarning,
                                style = DesignSystemThemeImpl.typeScale.textStrongXL,
                            )
                        }
                    }
                    Spacer(Modifier.height(DetailTimeChipBottomGap))
                    Row {
                        NyummyButton(
                            label = stringResource(R.string.history_detail_edit_name),
                            style = NyummyButtonStyle.Secondary,
                            onClick = { onIntent(HistoryIntent.ClickEditMealName) },
                        )
                        Spacer(Modifier.width(DetailActionGap))
                        NyummyButton(
                            label = stringResource(R.string.history_detail_delete),
                            style = NyummyButtonStyle.Danger,
                            onClick = { onIntent(HistoryIntent.ClickDeleteMeal) },
                        )
                    }
                }
            }
            Spacer(Modifier.height(DetailPhotoBottomGap))
            Row(verticalAlignment = Alignment.Bottom) {
                Column(modifier = Modifier.weight(1f)) {
                    DandiText(
                        text = stringResource(R.string.history_detail_calorie_label),
                        color = colors.contentNutritionLabel,
                        style = DesignSystemThemeImpl.typeScale.labelStrongS,
                    )
                    DandiText(
                        text = "${meal.calorieKcal} kcal",
                        color = colors.contentDefaultLevel0,
                        style = DesignSystemThemeImpl.typeScale.numberStrongL,
                    )
                }
                Surface(
                    modifier = Modifier.size(DetailGoalChipWidth, DetailGoalChipHeight),
                    shape = RoundedCornerShape(DesignSystemThemeImpl.designSystemRadius.radius16),
                    color = colors.bgSuccessSoft,
                    contentColor = colors.contentDefaultLevel0,
                ) {
                    Column(
                        modifier = Modifier.padding(
                            horizontal = DetailGoalChipInset,
                            vertical = DetailGoalChipVerticalInset,
                        ),
                    ) {
                        DandiText(
                            text = "하루 ${numberLabelOf(dailyNutrition.currentCalorieKcal)} / " +
                                numberLabelOf(dailyNutrition.targetCalorieKcal),
                            color = colors.contentNutritionLabel,
                            style = DesignSystemThemeImpl.typeScale.labelStrongXS,
                        )
                        DandiText(
                            text = "목표의 ${
                                percentOf(dailyNutrition.currentCalorieKcal, dailyNutrition.targetCalorieKcal)
                            }%",
                            color = colors.contentDefaultLevel0,
                            style = DesignSystemThemeImpl.typeScale.textStrongM,
                        )
                    }
                }
            }
            Spacer(Modifier.height(DetailCalorieBottomGap))
            HistoryMealNutritionSection(
                nutrition = dailyNutrition,
                meal = meal,
            )
        }
    }
}

private val OverlayVerticalInset = 24.dp
private val DetailCardWidth = 342.dp
private val DetailCardInset = 20.dp
private val DetailBorderWidth = 1.dp
private val DetailTitleTopGap = 6.dp
private val DetailHeaderBottomGap = 12.dp
private val DetailPhotoSize = 86.dp
private val DetailPhotoPadding = 14.dp
private val DetailPhotoRightGap = 12.dp
private val DetailTimeChipHeight = 60.dp
private val DetailTimeChipInset = 12.dp
private val DetailTimeChipBottomGap = 3.dp
private val DetailActionGap = 8.dp
private val DetailPhotoBottomGap = 12.dp
private val DetailGoalChipWidth = 132.dp
private val DetailGoalChipHeight = 56.dp
private val DetailGoalChipInset = 12.dp
private val DetailGoalChipVerticalInset = 8.dp
private val DetailCalorieBottomGap = 18.dp

@Preview(showBackground = true, widthDp = 390, heightDp = 780)
@Composable
private fun HistoryMealDetailOverlayPreview() {
    DesignSystemTheme {
        HistoryMealDetailOverlay(
            detail = HistoryMealDetailUiState(
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
            ),
            selectedDate = HistoryDateVO(2026, 7, 18),
            mealCount = 5,
            dailyNutrition = DailyNutritionVO(
                currentCalorieKcal = 2_129,
                targetCalorieKcal = 2_000,
                carbohydrate = NutrientProgressVO(dailyGram = 265, goalGram = 300),
                protein = NutrientProgressVO(dailyGram = 124, goalGram = 120),
                fat = NutrientProgressVO(dailyGram = 68, goalGram = 70),
            ),
            onIntent = {},
        )
    }
}
