package com.dandi.nyummy.history.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyBottomSheet
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyButton
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyButtonStyle
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyCoachCard
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyDialog
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyDialogType
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyIconButton
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyNutrient
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyNutrientStat
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyText
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTextButton
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTextButtonTone
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTextField
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.history.entity.HistoryDateVO
import com.dandi.nyummy.history.entity.MealHistoryVO
import com.dandi.nyummy.history.presentation.HistoryIntent
import com.dandi.nyummy.history.presentation.HistoryMealDetailMode
import com.dandi.nyummy.history.presentation.HistoryMealDetailUiState
import com.dandi.nyummy.history.presentation.R
import com.dandi.nyummy.history.presentation.model.dayLabelOf
import com.dandi.nyummy.history.presentation.model.meridiemTimeOf
import com.dandi.nyummy.history.presentation.model.numberLabelOf
import com.dandi.nyummy.common.presentation.R as CommonR

/**
 * 식사 하나의 상세와 이름 수정, 삭제 확인.
 *
 * - 보기: 하단 시트(날짜와 시각, 이름, 사진, 냐미 한마디, 이 식사 영양, 이름 수정 / 기록 삭제)
 * - 이름 수정: 입력 다이얼로그. 이름을 비우면 안내하고 저장할 수 없다.
 * - 삭제 확인: 빨간 주 버튼 다이얼로그. 분석 실패 줄에서 바로 열리기도 한다.
 *
 * 수정이나 삭제를 서버에 보내는 동안에는 버튼과 닫기를 막아 결과가 엇갈리지 않게 한다.
 */
@Composable
internal fun HistoryMealDetail(
    detail: HistoryMealDetailUiState,
    selectedDate: HistoryDateVO,
    onIntent: (HistoryIntent) -> Unit,
) {
    when (detail.mode) {
        HistoryMealDetailMode.Viewing -> NyummyBottomSheet(onDismissRequest = { onIntent(HistoryIntent.DismissMealDetail) }) {
            MealDetailContent(
                meal = detail.meal,
                selectedDate = selectedDate,
                onClose = { onIntent(HistoryIntent.DismissMealDetail) },
                onEditName = { onIntent(HistoryIntent.ClickEditMealName) },
                onDelete = { onIntent(HistoryIntent.ClickDeleteMeal) },
            )
        }

        HistoryMealDetailMode.EditingName -> EditNameDialog(
            draft = detail.nameDraft,
            isSaving = detail.isActionInFlight,
            onDraftChange = { onIntent(HistoryIntent.ChangeMealNameDraft(it)) },
            onConfirm = { onIntent(HistoryIntent.ConfirmEditMealName) },
            onCancel = { onIntent(HistoryIntent.CancelEditMealName) },
        )

        HistoryMealDetailMode.ConfirmingDelete -> NyummyDialog(
            type = NyummyDialogType.Destructive,
            title = if (detail.meal.name.isBlank()) {
                stringResource(R.string.history_delete_dialog_title_unnamed)
            } else {
                stringResource(R.string.history_delete_dialog_title, detail.meal.name)
            },
            body = stringResource(R.string.history_delete_dialog_body),
            confirmText = stringResource(R.string.history_delete_dialog_confirm),
            onConfirm = { onIntent(HistoryIntent.ConfirmDeleteMeal) },
            onDismissRequest = { onIntent(HistoryIntent.CancelDeleteMeal) },
            confirmEnabled = !detail.isActionInFlight,
            dismissible = !detail.isActionInFlight,
        )
    }
}

@Composable
private fun MealDetailContent(
    meal: MealHistoryVO,
    selectedDate: HistoryDateVO,
    onClose: () -> Unit,
    onEditName: () -> Unit,
    onDelete: () -> Unit,
) {
    val name = meal.name.ifBlank { stringResource(R.string.history_analysis_unknown_meal) }
    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s16),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s12)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s2)) {
                    NyummyText(
                        text = stringResource(
                            R.string.history_meal_meta,
                            dayLabelOf(selectedDate),
                            meridiemTimeOf(meal.recordedAt),
                        ),
                        style = NyummyTheme.typography.labelS,
                        color = NyummyTheme.colors.content.brand,
                    )
                    NyummyText(text = name, style = NyummyTheme.typography.titleL)
                }
                NyummyIconButton(
                    icon = CommonR.drawable.nyummy_ic_x,
                    contentDescription = stringResource(R.string.history_detail_close),
                    onClick = onClose,
                )
            }
            MealPhoto(meal = meal)
        }
        NyummyCoachCard(text = meal.catComment.ifBlank { stringResource(R.string.history_detail_cat_comment_empty) })
        MealNutrition(meal = meal)
        Row(horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s8)) {
            NyummyButton(
                text = stringResource(R.string.history_detail_edit_name),
                onClick = onEditName,
                style = NyummyButtonStyle.Secondary,
                icon = CommonR.drawable.nyummy_ic_pencil,
                modifier = Modifier.weight(1f),
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(DeleteSlotHeight),
                contentAlignment = Alignment.Center,
            ) {
                NyummyTextButton(
                    text = stringResource(R.string.history_detail_delete),
                    onClick = onDelete,
                    tone = NyummyTextButtonTone.Danger,
                    leadingIcon = CommonR.drawable.nyummy_ic_trash_2,
                )
            }
        }
    }
}

/** 식사 사진. 사진이 없거나 받지 못하면 음식 아이콘을 가운데 둔다. */
@Composable
private fun MealPhoto(meal: MealHistoryVO) {
    var loadFailed by remember(meal.photoUrl) { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(PhotoHeight)
            .clip(RoundedCornerShape(NyummyTheme.radius.m))
            .background(NyummyTheme.colors.bg.surfaceSunken),
        contentAlignment = Alignment.Center,
    ) {
        if (meal.photoUrl.isBlank() || loadFailed) {
            Box(Modifier.size(PhotoFallbackIconSize)) {
                HistoryFoodIcon(foodIconId = meal.foodIconId, sizeFraction = 1f)
            }
        } else {
            AsyncImage(
                model = meal.photoUrl,
                // 바로 위 식사 이름이 같은 내용을 읽어 주므로 장식으로 둔다.
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                onState = { state -> if (state is AsyncImagePainter.State.Error) loadFailed = true },
            )
        }
    }
}

/** "이 식사 영양 612 kcal"과 탄단지. 목표 대비 비율 없이 참고 숫자만 보여 준다. */
@Composable
private fun MealNutrition(meal: MealHistoryVO) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(NyummyTheme.radius.m))
            .background(NyummyTheme.colors.bg.surfaceSunken)
            .padding(horizontal = NyummyTheme.spacing.s16, vertical = NutritionVerticalPadding),
        verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s8),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            NyummyText(
                text = stringResource(R.string.history_detail_nutrition_title),
                style = NyummyTheme.typography.bodyM,
                color = NyummyTheme.colors.content.secondary,
                modifier = Modifier.weight(1f),
            )
            NyummyText(text = numberLabelOf(meal.calorieKcal), style = NyummyTheme.typography.numberM)
            Spacer(Modifier.size(KcalUnitGap))
            NyummyText(
                text = stringResource(R.string.history_kcal_unit),
                style = NyummyTheme.typography.labelS,
                color = NyummyTheme.colors.content.tertiary,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s8)) {
            NyummyNutrientStat(NyummyNutrient.Carb, meal.carbohydrateGram, Modifier.weight(1f))
            NyummyNutrientStat(NyummyNutrient.Protein, meal.proteinGram, Modifier.weight(1f))
            NyummyNutrientStat(NyummyNutrient.Fat, meal.fatGram, Modifier.weight(1f))
        }
    }
}

@Composable
private fun EditNameDialog(
    draft: String,
    isSaving: Boolean,
    onDraftChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    val isBlank = draft.isBlank()
    NyummyDialog(
        title = stringResource(R.string.history_edit_dialog_title),
        confirmText = stringResource(R.string.history_edit_dialog_confirm),
        onConfirm = onConfirm,
        onDismissRequest = onCancel,
        confirmEnabled = !isBlank && !isSaving,
        dismissible = !isSaving,
    ) {
        NyummyTextField(
            value = draft,
            onValueChange = onDraftChange,
            label = stringResource(R.string.history_edit_dialog_field_label),
            errorMessage = if (isBlank) stringResource(R.string.history_edit_dialog_empty) else null,
            enabled = !isSaving,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { if (!isBlank) onConfirm() }),
        )
    }
}

private val PhotoHeight = 190.dp
private val PhotoFallbackIconSize = 72.dp
private val NutritionVerticalPadding = 14.dp
private val KcalUnitGap = 3.dp

// 왼쪽 "이름 수정" 버튼(L, 56)과 높이를 맞춘다.
private val DeleteSlotHeight = 56.dp

private val previewMeal = MealHistoryVO(
    id = "1",
    name = "비빔밥",
    recordedAt = "08:10",
    calorieKcal = 612,
    carbohydrateGram = 82,
    proteinGram = 24,
    fatGram = 18,
    orderIndex = 1,
    catComment = "채소 가득한 비빔밥이네! 고추장은 조금만 넣었지? 계란 반숙까지 완벽해. 오늘 첫 끼 최고였어.",
)

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun MealDetailContentPreview() {
    NyummyTheme {
        Box(Modifier.padding(20.dp)) {
            MealDetailContent(
                meal = previewMeal,
                selectedDate = HistoryDateVO(2026, 10, 5),
                onClose = {},
                onEditName = {},
                onDelete = {},
            )
        }
    }
}
