package com.dandi.nyummy.history.presentation.component

import androidx.annotation.StringRes
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
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
import com.dandi.nyummy.common.presentation.R as CommonR
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyBottomSheet
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyButton
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyButtonSize
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyButtonStyle
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyChip
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyCoachCard
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyDialog
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyDialogType
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyFoodIcon
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyIconButton
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyNutrient
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyNutrientStat
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyStarRating
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyText
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTextArea
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTextButton
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTextButtonTone
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTextField
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.history.domain.SubmitAnalysisFeedbackUseCase
import com.dandi.nyummy.history.entity.AnalysisFeedbackReason
import com.dandi.nyummy.history.entity.HistoryDateVO
import com.dandi.nyummy.history.entity.MealHistoryVO
import com.dandi.nyummy.history.presentation.AnalysisFeedbackUiState
import com.dandi.nyummy.history.presentation.HistoryIntent
import com.dandi.nyummy.history.presentation.HistoryMealDetailMode
import com.dandi.nyummy.history.presentation.HistoryMealDetailUiState
import com.dandi.nyummy.history.presentation.R
import com.dandi.nyummy.history.presentation.model.dayLabelOf
import com.dandi.nyummy.history.presentation.model.meridiemTimeOf
import com.dandi.nyummy.history.presentation.model.numberLabelOf

/**
 * 식사 하나의 상세와 이름 수정, 삭제 확인.
 *
 * - 보기: 하단 시트(날짜와 시각, 이름, 사진, 냐미 한마디, 이 식사 영양, 이름 수정 / 기록 삭제, 맨 아래 AI 분석 평가 카드(평가 대상일 때))
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
                feedback = detail.feedback,
                onIntent = onIntent,
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
            dismissEnabled = !detail.isActionInFlight,
            dismissible = !detail.isActionInFlight,
        )
    }
}

@Composable
private fun MealDetailContent(
    meal: MealHistoryVO,
    feedback: AnalysisFeedbackUiState?,
    onIntent: (HistoryIntent) -> Unit,
    selectedDate: HistoryDateVO,
    onClose: () -> Unit,
    onEditName: () -> Unit,
    onDelete: () -> Unit,
) {
    val name = meal.name.ifBlank { stringResource(R.string.history_analysis_unknown_meal) }
    val scrollState = rememberScrollState()
    Column(
        // 평가 카드의 입력칸을 쓸 때 키보드에 가리지 않도록 키보드 높이만큼 비우고 스크롤로 올린다.
        modifier = Modifier
            .imePadding()
            .verticalScroll(scrollState),
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
        // 평가 카드는 맨 아래에 있어서, 카드가 나타나거나 펼쳐지면 맨 아래까지 내려 보여 준다.
        AnalysisFeedbackSection(
            feedback = feedback,
            onIntent = onIntent,
            revealBottom = { scrollState.scrollToBottomIgnoringUser() },
        )
    }
}

/** AI 분석 평가 카드 자리. 카드는 애니메이션 없이 한 번에 나타나고 사라진다. */
@Composable
private fun AnalysisFeedbackSection(
    feedback: AnalysisFeedbackUiState?,
    onIntent: (HistoryIntent) -> Unit,
    revealBottom: suspend () -> Unit,
) {
    // 카드는 시트 맨 아래라 나타나도 화면 밖일 수 있다. 나타난 순간 카드까지 시트를 내린다.
    RevealWhenBecomesTrue(condition = feedback != null, reveal = revealBottom)
    if (feedback != null) {
        AnalysisFeedbackCard(feedback = feedback, onIntent = onIntent, revealBottom = revealBottom)
    }
}

/**
 * AI 분석 평가 카드. 별점(0.5 단위)을 고르면 이유 고르기와 보내기가 한 번에 펼쳐지고, 보내면 상세 시트가 닫히며 스낵바로 고마움을 알린다.
 * 아쉬운 점(하나만 고름)과 직접 쓰기는 모두 선택이다.
 */
@Composable
private fun AnalysisFeedbackCard(
    feedback: AnalysisFeedbackUiState,
    onIntent: (HistoryIntent) -> Unit,
    revealBottom: suspend () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(NyummyTheme.colors.bg.surfaceSunken, RoundedCornerShape(NyummyTheme.radius.m))
            .padding(NyummyTheme.spacing.s16),
        verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s12),
    ) {
        Row {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s2),
            ) {
                NyummyText(text = stringResource(R.string.history_feedback_title), style = NyummyTheme.typography.titleS)
                NyummyText(
                    text = stringResource(R.string.history_feedback_subtitle),
                    style = NyummyTheme.typography.bodyS,
                    color = NyummyTheme.colors.content.tertiary,
                )
            }
            NyummyIconButton(
                icon = CommonR.drawable.nyummy_ic_x,
                contentDescription = stringResource(R.string.history_feedback_dismiss),
                onClick = { onIntent(HistoryIntent.DismissFeedback) },
                enabled = !feedback.isSubmitting,
            )
        }
        NyummyStarRating(
            halfSteps = feedback.ratingHalfSteps,
            onHalfStepsChange = { onIntent(HistoryIntent.ChangeFeedbackRating(it)) },
            contentDescription = stringResource(R.string.history_feedback_rating_description),
            enabled = !feedback.isSubmitting,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        // 별을 누르면 아래로 생기는 부분이 화면 밖이라 모르고 지나치기 쉽다. 처음 별점을 고른 순간에만 시트를 맨 아래까지 내린다.
        RevealWhenBecomesTrue(condition = feedback.isRated, reveal = revealBottom)
        // 별점을 고르면 애니메이션 없이 한 번에 펼친다.
        if (feedback.isRated) {
            Column(verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s12)) {
                NyummyText(
                    text = stringResource(R.string.history_feedback_reasons_label),
                    style = NyummyTheme.typography.labelM,
                    color = NyummyTheme.colors.content.secondary,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s8),
                    verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s8),
                ) {
                    AnalysisFeedbackReason.entries.forEach { reason ->
                        NyummyChip(
                            text = stringResource(reason.labelRes),
                            selected = reason == feedback.selectedReason,
                            onClick = { onIntent(HistoryIntent.SelectFeedbackReason(reason)) },
                            enabled = !feedback.isSubmitting,
                        )
                    }
                }
                NyummyTextArea(
                    value = feedback.comment,
                    onValueChange = { onIntent(HistoryIntent.ChangeFeedbackComment(it)) },
                    label = stringResource(R.string.history_feedback_comment_label),
                    placeholder = stringResource(R.string.history_feedback_comment_placeholder),
                    enabled = !feedback.isSubmitting,
                    maxLength = SubmitAnalysisFeedbackUseCase.REASON_MAX_LENGTH,
                    modifier = Modifier.fillMaxWidth(),
                )
                NyummyButton(
                    text = stringResource(R.string.history_feedback_submit),
                    onClick = { onIntent(HistoryIntent.SubmitFeedback) },
                    size = NyummyButtonSize.M,
                    enabled = feedback.canSubmit,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@get:StringRes
private val AnalysisFeedbackReason.labelRes: Int
    get() = when (this) {
        AnalysisFeedbackReason.WRONG_FOOD -> R.string.history_feedback_reason_wrong_food
        AnalysisFeedbackReason.WRONG_PORTION -> R.string.history_feedback_reason_wrong_portion
        AnalysisFeedbackReason.WRONG_NUTRITION -> R.string.history_feedback_reason_wrong_nutrition
        AnalysisFeedbackReason.MISSING_FOOD -> R.string.history_feedback_reason_missing_food
    }

/**
 * [condition]이 false에서 true로 바뀐 순간에만 [reveal]을 한 번 부른다. 새로 생긴 부분의 크기가 잡히도록 한 프레임 뒤에 부른다.
 * 처음부터 true인 채로 그려질 때(이름 수정 다이얼로그를 닫고 시트로 돌아올 때 등)는 부르지 않는다.
 */
@Composable
private fun RevealWhenBecomesTrue(condition: Boolean, reveal: suspend () -> Unit) {
    val currentReveal by rememberUpdatedState(reveal)
    var wasTrue by remember { mutableStateOf(condition) }
    LaunchedEffect(condition) {
        if (condition && !wasTrue) {
            withFrameNanos { }
            currentReveal()
        }
        wasTrue = condition
    }
}

/**
 * 맨 아래까지 부드럽게 스크롤한다. 사용자가 끌고 있어도 그 입력은 무시한다([MutatePriority.PreventUserInput]):
 * 스크롤이 끝날 때까지 사용자 스크롤을 막아, 자동 스크롤과 손가락이 서로 당겨 시트가 버티는 느낌이 나지 않게 한다.
 */
private suspend fun ScrollState.scrollToBottomIgnoringUser() {
    scroll(MutatePriority.PreventUserInput) {
        var scrolled = 0f
        animate(0f, (maxValue - value).toFloat(), animationSpec = tween(REVEAL_SCROLL_MILLIS)) { target, _ ->
            scrolled += scrollBy(target - scrolled)
        }
        // 그사이 시트가 자라 끝이 더 멀어졌으면 남은 만큼 맞춘다.
        scrollBy((maxValue - value).toFloat())
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
                NyummyFoodIcon(foodIconId = meal.foodIconId)
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
        dismissEnabled = !isSaving,
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

/** 평가 카드가 나타나거나 펼쳐진 뒤 맨 아래까지 내려가는 시간. */
private const val REVEAL_SCROLL_MILLIS = 400

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
                feedback = null,
                onIntent = {},
                selectedDate = HistoryDateVO(2026, 10, 5),
                onClose = {},
                onEditName = {},
                onDelete = {},
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 1100)
@Composable
private fun MealDetailContentFeedbackPreview() {
    NyummyTheme {
        Box(Modifier.padding(20.dp)) {
            MealDetailContent(
                meal = previewMeal,
                feedback = AnalysisFeedbackUiState(
                    ratingHalfSteps = 7,
                    selectedReason = AnalysisFeedbackReason.WRONG_PORTION,
                ),
                onIntent = {},
                selectedDate = HistoryDateVO(2026, 10, 5),
                onClose = {},
                onEditName = {},
                onDelete = {},
            )
        }
    }
}
