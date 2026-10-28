package com.dandi.nyummy.history.presentation.detail

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyButton
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyButtonSize
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyButtonStyle
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyChip
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyDialog
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyFoodIcon
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyInlineNotice
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyInlineNoticeTone
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyNutrient
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyNutrientStat
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyPose
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyPoseImage
import com.dandi.nyummy.common.presentation.designsystem.component.NyummySpinner
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyStarRating
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyText
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTextArea
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTextField
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTopBar
import com.dandi.nyummy.common.presentation.designsystem.foundation.nyummyClickable
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.common.presentation.designsystem.theme.nyummyShadow
import com.dandi.nyummy.history.domain.SubmitAnalysisFeedbackUseCase
import com.dandi.nyummy.history.entity.AnalysisFeedbackReason
import com.dandi.nyummy.history.entity.HistoryDateVO
import com.dandi.nyummy.history.entity.MealHistoryVO
import com.dandi.nyummy.history.presentation.R
import com.dandi.nyummy.history.presentation.component.MealDeleteDialog
import com.dandi.nyummy.history.presentation.model.dayLabelOf
import com.dandi.nyummy.history.presentation.model.mealOrderLabelOf
import com.dandi.nyummy.history.presentation.model.meridiemTimeOf
import com.dandi.nyummy.history.presentation.model.numberLabelOf
import com.dandi.nyummy.common.presentation.R as CommonR

/**
 * 식사 하나의 상세 페이지(`/history/meals/{mealId}`). 뒤로 가면 기록 화면이다.
 *
 * - 맨 위에 화면 폭 정사각 사진을 상태 표시줄 뒤까지 깔고, 그 위에 흰 원 뒤로 버튼을 띄운다.
 *   사진이 위로 지나가면 흰 상단 바(뒤로 + 음식 이름)가 나타난다.
 * - 아래로 날짜와 시각, 이름, 냐미의 한마디(길면 펼치기), 이 식사 영양, AI 분석 평가 카드(평가 대상일 때), 이름 수정 / 기록 삭제.
 * - 이름 수정은 입력 다이얼로그, 삭제는 빨간 주 버튼 다이얼로그. 지우면 기록 화면으로 돌아간다.
 *
 * 수정이나 삭제를 서버에 보내는 동안에는 버튼과 닫기를 막아 결과가 엇갈리지 않게 한다.
 */
@Composable
fun MealDetailPage(
    mealId: Long,
    modifier: Modifier = Modifier,
    viewModel: MealDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(mealId) {
        viewModel.onIntent(MealDetailIntent.Enter(mealId))
    }
    MealDetailScreen(uiState = uiState, onIntent = viewModel::onIntent, modifier = modifier)
}

@Composable
internal fun MealDetailScreen(
    uiState: MealDetailUIState,
    onIntent: (MealDetailIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val meal = uiState.meal
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NyummyTheme.colors.bg.canvas),
    ) {
        if (meal != null) {
            MealDetailContent(meal = meal, feedback = uiState.feedback, onIntent = onIntent)
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
            ) {
                NyummyTopBar(title = "", onBackClick = { onIntent(MealDetailIntent.ClickBack) })
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = NyummyTheme.spacing.gutter),
                ) {
                    if (uiState.isLoadFailed) {
                        LoadFailed(
                            onRetry = { onIntent(MealDetailIntent.RetryLoad) },
                            modifier = Modifier.align(Alignment.Center),
                        )
                    } else {
                        NyummySpinner(contentDescription = null, modifier = Modifier.align(Alignment.Center))
                    }
                }
            }
        }
    }
    meal ?: return
    when (uiState.mode) {
        MealDetailMode.Viewing -> Unit

        MealDetailMode.EditingName -> EditNameDialog(
            draft = uiState.nameDraft,
            isSaving = uiState.isActionInFlight,
            onDraftChange = { onIntent(MealDetailIntent.ChangeNameDraft(it)) },
            onConfirm = { onIntent(MealDetailIntent.ConfirmEditName) },
            onCancel = { onIntent(MealDetailIntent.CancelEditName) },
        )

        MealDetailMode.ConfirmingDelete -> MealDeleteDialog(
            mealName = meal.name,
            isDeleting = uiState.isActionInFlight,
            onConfirm = { onIntent(MealDetailIntent.ConfirmDelete) },
            onCancel = { onIntent(MealDetailIntent.CancelDelete) },
        )
    }
}

@Composable
private fun MealDetailContent(
    meal: MealHistoryVO,
    feedback: AnalysisFeedbackUiState?,
    onIntent: (MealDetailIntent) -> Unit,
) {
    val name = meal.name.ifBlank { stringResource(R.string.history_analysis_unknown_meal) }
    val scrollState = rememberScrollState()
    var photoHeightPx by remember { mutableIntStateOf(0) }
    var topBarHeightPx by remember { mutableIntStateOf(0) }
    // 사진이 상단 바 아래로 다 지나가면 흰 상단 바로 바꾼다.
    val showTopBar by remember {
        derivedStateOf { photoHeightPx > 0 && scrollState.value >= photoHeightPx - topBarHeightPx }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            // 평가 카드의 입력칸을 쓸 때 키보드에 가리지 않도록 키보드 높이만큼 비우고 스크롤로 올린다.
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(scrollState),
        ) {
            MealPhoto(meal = meal, modifier = Modifier.onSizeChanged { photoHeightPx = it.height })
            Column(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(
                        start = NyummyTheme.spacing.gutter,
                        end = NyummyTheme.spacing.gutter,
                        top = NyummyTheme.spacing.s20,
                        bottom = NyummyTheme.spacing.s40,
                    ),
                verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s20),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s4)) {
                    NyummyText(
                        text = mealMetaOf(meal),
                        style = NyummyTheme.typography.bodyS,
                        color = NyummyTheme.colors.content.tertiary,
                    )
                    NyummyText(text = name, style = NyummyTheme.typography.titleXl)
                }
                CatCommentCard(comment = meal.catComment.ifBlank { stringResource(R.string.history_detail_cat_comment_empty) })
                MealNutrition(meal = meal)
                // 평가 카드는 수정·삭제 위에 둔다. 나타나거나 펼쳐지면 카드 끝까지 내려 보여 준다.
                AnalysisFeedbackSection(
                    feedback = feedback,
                    onIntent = onIntent,
                    revealBottom = { scrollState.scrollToBottomIgnoringUser() },
                )
                MealActions(
                    onEditName = { onIntent(MealDetailIntent.ClickEditName) },
                    onDelete = { onIntent(MealDetailIntent.ClickDelete) },
                )
            }
        }
        FloatingBackButton(
            onClick = { onIntent(MealDetailIntent.ClickBack) },
            modifier = Modifier
                .statusBarsPadding()
                .padding(start = NyummyTheme.spacing.s16, top = NyummyTheme.spacing.s8),
        )
        AnimatedVisibility(visible = showTopBar, enter = fadeIn(), exit = fadeOut()) {
            NyummyTopBar(
                title = name,
                onBackClick = { onIntent(MealDetailIntent.ClickBack) },
                containerColor = NyummyTheme.colors.bg.surface,
                modifier = Modifier
                    .background(NyummyTheme.colors.bg.surface)
                    .statusBarsPadding()
                    .onSizeChanged { topBarHeightPx = it.height },
            )
        }
    }
}

/** "10월 5일 오전 8:10 · 첫 끼". 끼니 순번을 모르면 날짜와 시각만 쓴다. */
@Composable
private fun mealMetaOf(meal: MealHistoryVO): String {
    val dateTime = stringResource(R.string.history_meal_meta, dayLabelOf(meal.date), meridiemTimeOf(meal.recordedAt))
    return if (meal.orderIndex > 0) {
        stringResource(R.string.history_detail_meta_with_order, dateTime, mealOrderLabelOf(meal.orderIndex))
    } else {
        dateTime
    }
}

/** 사진 위에 뜨는 흰 원 뒤로 버튼. */
@Composable
private fun FloatingBackButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val description = stringResource(CommonR.string.nyummy_back_description)
    Box(
        modifier = modifier
            .size(FloatingBackSize)
            .nyummyShadow(CircleShape, NyummyTheme.elevation.soft)
            .background(NyummyTheme.colors.bg.surface, CircleShape)
            .nyummyClickable(onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(CommonR.drawable.nyummy_ic_chevron_left),
            contentDescription = null,
            tint = NyummyTheme.colors.content.primary,
            modifier = Modifier.size(FloatingBackIconSize),
        )
    }
}

/** 화면 폭 정사각 사진. 사진이 없거나 받지 못하면 음식 아이콘을 가운데 둔다. */
@Composable
private fun MealPhoto(meal: MealHistoryVO, modifier: Modifier = Modifier) {
    var loadFailed by remember(meal.photoUrl) { mutableStateOf(false) }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .background(NyummyTheme.colors.bg.surfaceSubtle),
        contentAlignment = Alignment.Center,
    ) {
        if (meal.photoUrl.isBlank() || loadFailed) {
            Box(Modifier.size(PhotoFallbackIconSize)) {
                NyummyFoodIcon(foodIconId = meal.foodIconId)
            }
        } else {
            AsyncImage(
                model = meal.photoUrl,
                // 바로 아래 식사 이름이 같은 내용을 읽어 주므로 장식으로 둔다.
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                onState = { state -> if (state is AsyncImagePainter.State.Error) loadFailed = true },
            )
        }
    }
}

/** 연초록 "냐미의 한마디" 카드. 세 줄을 넘으면 접어 두고 "펼치기"로 부드럽게 다 보여 준다. */
@Composable
private fun CatCommentCard(comment: String) {
    var expanded by rememberSaveable(comment) { mutableStateOf(false) }
    var overflows by remember(comment) { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(NyummyTheme.colors.bg.brandSubtle, RoundedCornerShape(NyummyTheme.radius.m))
            .animateContentSize()
            .padding(
                start = NyummyTheme.spacing.s16,
                end = NyummyTheme.spacing.s16,
                top = NyummyTheme.spacing.s16,
                bottom = NyummyTheme.spacing.s12,
            ),
        verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s10),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s8),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(AvatarSize)
                    .clip(CircleShape)
                    .background(NyummyTheme.colors.bg.surface),
                contentAlignment = Alignment.Center,
            ) {
                NyummyPoseImage(pose = NyummyPose.Hello, size = AvatarSize)
            }
            NyummyText(text = stringResource(R.string.history_detail_cat_comment_title), style = NyummyTheme.typography.labelM)
        }
        NyummyText(
            text = comment,
            style = NyummyTheme.typography.voiceS,
            color = NyummyTheme.colors.content.voice,
            maxLines = if (expanded) Int.MAX_VALUE else COMMENT_COLLAPSED_LINES,
            onTextLayout = { if (!expanded) overflows = it.hasVisualOverflow },
        )
        if (overflows || expanded) {
            Row(
                modifier = Modifier
                    .align(Alignment.End)
                    .nyummyClickable(onClick = { expanded = !expanded }),
                horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s2),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NyummyText(
                    text = stringResource(if (expanded) R.string.history_nutrition_collapse else R.string.history_nutrition_expand),
                    style = NyummyTheme.typography.bodyS,
                    color = NyummyTheme.colors.content.secondary,
                )
                Icon(
                    painter = painterResource(
                        if (expanded) CommonR.drawable.nyummy_ic_chevron_up else CommonR.drawable.nyummy_ic_chevron_down,
                    ),
                    contentDescription = null,
                    tint = NyummyTheme.colors.content.secondary,
                    modifier = Modifier.size(NyummyTheme.size.iconXs),
                )
            }
        }
    }
}

/** "이 식사 영양 612 kcal"과 탄단지. 기록 화면의 하루 영양처럼 연초록 패널 안 흰 카드에 둔다. */
@Composable
private fun MealNutrition(meal: MealHistoryVO) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(NyummyTheme.colors.bg.brandSubtle, RoundedCornerShape(NyummyTheme.radius.m))
            .padding(NyummyTheme.spacing.s16),
        verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s12),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            NyummyText(
                text = stringResource(R.string.history_detail_nutrition_title),
                style = NyummyTheme.typography.titleS,
                modifier = Modifier.weight(1f),
            )
            NyummyText(
                text = stringResource(R.string.history_nutrition_kcal, numberLabelOf(meal.calorieKcal)),
                style = NyummyTheme.typography.numberM,
                color = NyummyTheme.colors.content.brand,
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(NyummyTheme.colors.bg.surface, RoundedCornerShape(NyummyTheme.radius.s))
                .padding(horizontal = NyummyTheme.spacing.s20, vertical = NyummyTheme.spacing.s16),
        ) {
            NyummyNutrientStat(NyummyNutrient.Carb, meal.carbohydrateGram, Modifier.weight(1f))
            NyummyNutrientStat(NyummyNutrient.Protein, meal.proteinGram, Modifier.weight(1f))
            NyummyNutrientStat(NyummyNutrient.Fat, meal.fatGram, Modifier.weight(1f))
        }
    }
}

/** 이름 수정 / 기록 삭제. 둘 다 회색 큰 버튼이고 삭제만 빨간 글자다. 확정은 다이얼로그에서 한다. */
@Composable
private fun MealActions(onEditName: () -> Unit, onDelete: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s8)) {
        NyummyButton(
            text = stringResource(R.string.history_detail_edit_name),
            onClick = onEditName,
            style = NyummyButtonStyle.Neutral,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(ActionHeight)
                .nyummyClickable(onClick = onDelete)
                .background(NyummyTheme.colors.bg.actionNeutral, RoundedCornerShape(NyummyTheme.radius.m)),
            contentAlignment = Alignment.Center,
        ) {
            NyummyText(
                text = stringResource(R.string.history_detail_delete),
                style = NyummyTheme.typography.labelL,
                color = NyummyTheme.colors.content.danger,
            )
        }
    }
}

@Composable
private fun LoadFailed(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s12),
    ) {
        NyummyInlineNotice(
            title = stringResource(R.string.history_load_failed_title),
            body = stringResource(R.string.history_load_failed_body),
            tone = NyummyInlineNoticeTone.Danger,
            modifier = Modifier.fillMaxWidth(),
        )
        NyummyButton(
            text = stringResource(R.string.history_load_retry),
            onClick = onRetry,
            style = NyummyButtonStyle.Neutral,
            size = NyummyButtonSize.M,
            icon = CommonR.drawable.nyummy_ic_refresh_cw,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** AI 분석 평가 카드 자리. 카드는 애니메이션 없이 한 번에 나타나고 사라진다. */
@Composable
private fun AnalysisFeedbackSection(
    feedback: AnalysisFeedbackUiState?,
    onIntent: (MealDetailIntent) -> Unit,
    revealBottom: suspend () -> Unit,
) {
    // 카드는 아래쪽이라 나타나도 화면 밖일 수 있다. 나타난 순간 카드까지 내린다.
    RevealWhenBecomesTrue(condition = feedback != null, reveal = revealBottom)
    if (feedback != null) {
        AnalysisFeedbackCard(feedback = feedback, onIntent = onIntent, revealBottom = revealBottom)
    }
}

/**
 * AI 분석 평가 카드(흰 바탕, 테두리). 별점(0.5 단위)을 고르면 이유 고르기와 보내기가 한 번에 펼쳐지고,
 * 보내면 카드가 닫히며 스낵바로 고마움을 알린다. 아쉬운 점(하나만 고름)과 직접 쓰기는 모두 선택이다.
 */
@Composable
private fun AnalysisFeedbackCard(
    feedback: AnalysisFeedbackUiState,
    onIntent: (MealDetailIntent) -> Unit,
    revealBottom: suspend () -> Unit,
) {
    val shape = RoundedCornerShape(NyummyTheme.radius.m)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(NyummyTheme.colors.bg.surface, shape)
            .border(NyummyTheme.borderWidth.hairline, NyummyTheme.colors.border.default, shape)
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
                    color = NyummyTheme.colors.content.secondary,
                )
            }
            val dismissDescription = stringResource(R.string.history_feedback_dismiss)
            Icon(
                painter = painterResource(CommonR.drawable.nyummy_ic_x),
                contentDescription = null,
                tint = NyummyTheme.colors.content.tertiary,
                modifier = Modifier
                    .nyummyClickable(
                        onClick = { onIntent(MealDetailIntent.DismissFeedback) },
                        enabled = !feedback.isSubmitting,
                    )
                    .semantics { contentDescription = dismissDescription }
                    .size(DismissIconSize),
            )
        }
        NyummyStarRating(
            halfSteps = feedback.ratingHalfSteps,
            onHalfStepsChange = { onIntent(MealDetailIntent.ChangeFeedbackRating(it)) },
            contentDescription = stringResource(R.string.history_feedback_rating_description),
            enabled = !feedback.isSubmitting,
        )
        // 별을 누르면 아래로 생기는 부분이 화면 밖이라 모르고 지나치기 쉽다. 처음 별점을 고른 순간에만 카드 끝까지 내린다.
        RevealWhenBecomesTrue(condition = feedback.isRated, reveal = revealBottom)
        // 별점을 고르면 애니메이션 없이 한 번에 펼친다.
        if (feedback.isRated) {
            Column(verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s12)) {
                NyummyText(text = stringResource(R.string.history_feedback_reasons_label), style = NyummyTheme.typography.labelM)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s8),
                    verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s8),
                ) {
                    AnalysisFeedbackReason.entries.forEach { reason ->
                        NyummyChip(
                            text = stringResource(reason.labelRes),
                            selected = reason == feedback.selectedReason,
                            onClick = { onIntent(MealDetailIntent.SelectFeedbackReason(reason)) },
                            enabled = !feedback.isSubmitting,
                        )
                    }
                }
                NyummyTextArea(
                    value = feedback.comment,
                    onValueChange = { onIntent(MealDetailIntent.ChangeFeedbackComment(it)) },
                    label = stringResource(R.string.history_feedback_comment_label),
                    placeholder = stringResource(R.string.history_feedback_comment_placeholder),
                    enabled = !feedback.isSubmitting,
                    maxLength = SubmitAnalysisFeedbackUseCase.REASON_MAX_LENGTH,
                    modifier = Modifier.fillMaxWidth(),
                )
                NyummyButton(
                    text = stringResource(R.string.history_feedback_submit),
                    onClick = { onIntent(MealDetailIntent.SubmitFeedback) },
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
 * 처음부터 true인 채로 그려질 때(이름 수정 다이얼로그를 닫고 돌아올 때 등)는 부르지 않는다.
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
 * 스크롤이 끝날 때까지 사용자 스크롤을 막아, 자동 스크롤과 손가락이 서로 당겨 화면이 버티는 느낌이 나지 않게 한다.
 */
private suspend fun ScrollState.scrollToBottomIgnoringUser() {
    scroll(MutatePriority.PreventUserInput) {
        var scrolled = 0f
        animate(0f, (maxValue - value).toFloat(), animationSpec = tween(REVEAL_SCROLL_MILLIS)) { target, _ ->
            scrolled += scrollBy(target - scrolled)
        }
        // 그사이 내용이 자라 끝이 더 멀어졌으면 남은 만큼 맞춘다.
        scrollBy((maxValue - value).toFloat())
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

/** 냐미의 한마디를 접어 둘 때 보여 주는 줄 수. */
private const val COMMENT_COLLAPSED_LINES = 3

private val PhotoFallbackIconSize = 96.dp
private val FloatingBackSize = 40.dp
private val FloatingBackIconSize = 22.dp
private val AvatarSize = 36.dp
private val DismissIconSize = 18.dp

// 왼쪽 "이름 수정" 버튼(L, 56)과 높이를 맞춘다.
private val ActionHeight = 56.dp

private val previewMeal = MealHistoryVO(
    id = "1",
    name = "비빔밥",
    recordedAt = "08:10",
    calorieKcal = 612,
    carbohydrateGram = 82,
    proteinGram = 24,
    fatGram = 18,
    orderIndex = 1,
    date = HistoryDateVO(2026, 10, 5),
    catComment = "채소 가득한 비빔밥이네! 고추장은 조금만 넣었지? 계란 반숙까지 완벽해. 오늘 첫 끼 최고였어. 다음 끼니도 이렇게 골고루 먹어 줘!",
)

@Preview(showBackground = true, widthDp = 390, heightDp = 1100)
@Composable
private fun MealDetailScreenPreview() {
    NyummyTheme {
        MealDetailScreen(
            uiState = MealDetailUIState(
                meal = previewMeal,
                isLoading = false,
                feedback = AnalysisFeedbackUiState(ratingHalfSteps = 7, selectedReason = AnalysisFeedbackReason.WRONG_PORTION),
            ),
            onIntent = {},
        )
    }
}
