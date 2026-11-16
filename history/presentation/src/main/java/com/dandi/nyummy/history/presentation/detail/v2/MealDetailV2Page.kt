package com.dandi.nyummy.history.presentation.detail.v2

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dandi.nyummy.common.presentation.cat.catNameStringResource
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyButton
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyButtonSize
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyCircularProgress
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyFoodIcon
import com.dandi.nyummy.common.presentation.designsystem.component.NyummySpinner
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyText
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTopBar
import com.dandi.nyummy.common.presentation.designsystem.foundation.nyummyClickable
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.history.entity.MealDetailV2VO
import com.dandi.nyummy.history.entity.MealFoodVO
import com.dandi.nyummy.history.entity.MealHistoryVO
import com.dandi.nyummy.history.presentation.R
import com.dandi.nyummy.history.presentation.component.MealDeleteDialog
import com.dandi.nyummy.history.presentation.detail.AnalysisFeedbackSection
import com.dandi.nyummy.history.presentation.detail.AnalysisFeedbackUiState
import com.dandi.nyummy.history.presentation.detail.CatCommentCard
import com.dandi.nyummy.history.presentation.detail.EditNameDialog
import com.dandi.nyummy.history.presentation.detail.FloatingBackButton
import com.dandi.nyummy.history.presentation.detail.FloatingDeleteButton
import com.dandi.nyummy.history.presentation.detail.LoadFailed
import com.dandi.nyummy.history.presentation.detail.MealDetailIntent
import com.dandi.nyummy.history.presentation.detail.MealDetailMode
import com.dandi.nyummy.history.presentation.detail.MealNutrition
import com.dandi.nyummy.history.presentation.detail.MealPhoto
import com.dandi.nyummy.history.presentation.detail.mealMetaOf
import com.dandi.nyummy.history.presentation.detail.scrollToBottomIgnoringUser
import com.dandi.nyummy.history.presentation.model.numberLabelOf
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlin.math.roundToInt
import com.dandi.nyummy.common.presentation.R as CommonR

/**
 * 음식 목록 방식(V2) 식사 상세 화면(History · 06v3).
 *
 * - 맨 위에 화면 폭 정사각 사진, 그 위에 흰 원 뒤로 버튼(왼쪽)과 삭제 버튼(오른쪽).
 * - 아래로 날짜와 시각, 식사 이름과 연필, "사진 속 음식"(음식 줄, 음식 추가), 이 식사 영양, 냐미의 한마디, AI 분석 평가 카드.
 * - 음식 줄은 손잡이를 끌어 순서를 바꾼다. 맨 위 줄이 대표 음식이다. 연필을 누르면 음식 고치기 시트가 열린다.
 * - 서버가 영양을 다시 계산하는 동안에는 음식 줄 목록 끝("음식 추가" 바로 위)에 계산 중 안내를 보여 주고 음식 목록을 고칠 수 없다.
 *
 * @param initial 들어오기 전에 이미 받아 둔 상세. 있으면 다시 조회하지 않는다.
 */
@Composable
fun MealDetailV2Page(
    mealId: Long,
    modifier: Modifier = Modifier,
    initial: MealDetailV2VO? = null,
    viewModel: MealDetailV2ViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(mealId) {
        viewModel.onIntent(MealDetailV2Intent.Enter(mealId, initial))
    }
    MealDetailV2Screen(uiState = uiState, onIntent = viewModel::onIntent, modifier = modifier)
}

@Composable
internal fun MealDetailV2Screen(
    uiState: MealDetailV2UIState,
    onIntent: (MealDetailV2Intent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val meal = uiState.meal
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NyummyTheme.colors.bg.canvas),
    ) {
        if (meal != null) {
            MealDetailV2Content(meal = meal, uiState = uiState, onIntent = onIntent)
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
            ) {
                NyummyTopBar(title = "", onBackClick = { onIntent(MealDetailV2Intent.ClickBack) })
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = NyummyTheme.spacing.gutter),
                ) {
                    if (uiState.isLoadFailed) {
                        LoadFailed(
                            onRetry = { onIntent(MealDetailV2Intent.RetryLoad) },
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
            onDraftChange = { onIntent(MealDetailV2Intent.ChangeNameDraft(it)) },
            onConfirm = { onIntent(MealDetailV2Intent.ConfirmEditName) },
            onCancel = { onIntent(MealDetailV2Intent.CancelEditName) },
        )

        MealDetailMode.ConfirmingDelete -> MealDeleteDialog(
            mealName = meal.name,
            isDeleting = uiState.isActionInFlight,
            onConfirm = { onIntent(MealDetailV2Intent.ConfirmDelete) },
            onCancel = { onIntent(MealDetailV2Intent.CancelDelete) },
        )
    }
    uiState.foodSheet?.let { sheet ->
        MealFoodEditSheet(sheet = sheet, isSaving = uiState.isSavingFoods, onIntent = onIntent)
    }
}

@Composable
private fun MealDetailV2Content(
    meal: MealHistoryVO,
    uiState: MealDetailV2UIState,
    onIntent: (MealDetailV2Intent) -> Unit,
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
            // 음식 이름이나 평가를 적을 때 키보드에 가리지 않도록 키보드 높이만큼 비우고 스크롤로 올린다.
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
                MealHead(meta = mealMetaOf(meal), name = name, onEditName = { onIntent(MealDetailV2Intent.ClickEditName) })
                FoodsSection(
                    foods = uiState.foods,
                    canEdit = uiState.canEditFoods,
                    canReorder = uiState.canReorder,
                    canAdd = uiState.canAddFood,
                    isCalculating = uiState.isCalculating,
                    addNameInput = uiState.addNameInput,
                    canConfirmAdd = uiState.canConfirmAdd,
                    onIntent = onIntent,
                )
                MealNutrition(meal = meal)
                CatCommentCard(comment = meal.catComment.ifBlank { catNameStringResource(R.string.history_detail_cat_comment_empty) })
                // 평가 카드가 나타나거나 펼쳐지면 카드 끝까지 내려 보여 준다.
                AnalysisFeedbackSection(
                    feedback = uiState.feedback,
                    onIntent = { it.toV2Intent()?.let(onIntent) },
                    revealBottom = { scrollState.scrollToBottomIgnoringUser() },
                )
            }
        }
        FloatingBackButton(
            onClick = { onIntent(MealDetailV2Intent.ClickBack) },
            modifier = Modifier
                .statusBarsPadding()
                .padding(start = NyummyTheme.spacing.s16, top = NyummyTheme.spacing.s8),
        )
        FloatingDeleteButton(
            onClick = { onIntent(MealDetailV2Intent.ClickDelete) },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(end = NyummyTheme.spacing.s16, top = NyummyTheme.spacing.s8),
        )
        AnimatedVisibility(visible = showTopBar, enter = fadeIn(), exit = fadeOut()) {
            NyummyTopBar(
                title = name,
                onBackClick = { onIntent(MealDetailV2Intent.ClickBack) },
                containerColor = NyummyTheme.colors.bg.surface,
                modifier = Modifier
                    .background(NyummyTheme.colors.bg.surface)
                    .statusBarsPadding()
                    .onSizeChanged { topBarHeightPx = it.height },
            )
        }
    }
}

/** 평가 카드는 기존 상세와 같이 쓴다. 그 카드가 보내는 입력을 이 화면의 입력으로 옮긴다. */
private fun MealDetailIntent.toV2Intent(): MealDetailV2Intent? = when (this) {
    is MealDetailIntent.ChangeFeedbackRating -> MealDetailV2Intent.ChangeFeedbackRating(halfSteps)
    is MealDetailIntent.SelectFeedbackReason -> MealDetailV2Intent.SelectFeedbackReason(reason)
    is MealDetailIntent.ChangeFeedbackComment -> MealDetailV2Intent.ChangeFeedbackComment(text)
    MealDetailIntent.SubmitFeedback -> MealDetailV2Intent.SubmitFeedback
    MealDetailIntent.DismissFeedback -> MealDetailV2Intent.DismissFeedback
    else -> null
}

/** 날짜와 시각, 그 아래 식사 이름과 이름 고치기 연필. */
@Composable
private fun MealHead(meta: String, name: String, onEditName: () -> Unit) {
    val editDescription = stringResource(R.string.history_detail_v2_edit_name_description)
    Column(verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s4)) {
        NyummyText(text = meta, style = NyummyTheme.typography.bodyS, color = NyummyTheme.colors.content.tertiary)
        Row(verticalAlignment = Alignment.CenterVertically) {
            NyummyText(
                text = name,
                style = NyummyTheme.typography.titleXl,
                maxLines = 2,
                modifier = Modifier.weight(1f, fill = false),
            )
            Box(
                modifier = Modifier
                    .padding(start = NyummyTheme.spacing.s2)
                    .size(EditNameButtonSize)
                    .nyummyClickable(onClick = onEditName)
                    .semantics { contentDescription = editDescription },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(CommonR.drawable.nyummy_ic_pencil),
                    contentDescription = null,
                    tint = NyummyTheme.colors.content.tertiary,
                    modifier = Modifier.size(EditNameIconSize),
                )
            }
        }
    }
}

/** 서버가 영양을 다시 계산하는 동안의 안내. 연초록 띠에 진행 표시. 음식 줄 목록 끝, "음식 추가" 바로 위에 뜬다. */
@Composable
private fun CalculatingNotice(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(NyummyTheme.colors.bg.brandSubtle, RoundedCornerShape(NyummyTheme.radius.m))
            .padding(horizontal = NyummyTheme.spacing.s16, vertical = NyummyTheme.spacing.s12)
            .testTag(MealDetailV2CalculatingTag),
        horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NyummyCircularProgress()
        NyummyText(
            text = catNameStringResource(R.string.history_detail_v2_calculating),
            style = NyummyTheme.typography.bodyM,
            color = NyummyTheme.colors.content.brand,
            modifier = Modifier.weight(1f),
        )
    }
}

/** "사진 속 음식 N", 안내 한 줄, 음식 줄 목록, (계산 중이면) 계산 중 안내, 음식 추가. */
@Composable
private fun FoodsSection(
    foods: ImmutableList<MealFoodVO>,
    canEdit: Boolean,
    canReorder: Boolean,
    canAdd: Boolean,
    isCalculating: Boolean,
    addNameInput: String?,
    canConfirmAdd: Boolean,
    onIntent: (MealDetailV2Intent) -> Unit,
) {
    Column {
        Row(
            horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s6),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NyummyText(text = stringResource(R.string.history_detail_v2_foods_title), style = NyummyTheme.typography.titleS)
            NyummyText(
                text = foods.size.toString(),
                style = NyummyTheme.typography.bodyM,
                color = NyummyTheme.colors.content.tertiary,
            )
        }
        if (canReorder) {
            NyummyText(
                text = stringResource(R.string.history_detail_v2_foods_hint),
                style = NyummyTheme.typography.bodyS,
                color = NyummyTheme.colors.content.tertiary,
                modifier = Modifier.padding(top = NyummyTheme.spacing.s10),
            )
        }
        ReorderableFoodList(
            foods = foods,
            canEdit = canEdit,
            canReorder = canReorder,
            onMove = { from, to -> onIntent(MealDetailV2Intent.MoveFood(from, to)) },
            onEdit = { onIntent(MealDetailV2Intent.ClickEditFood(it)) },
            modifier = Modifier.padding(top = NyummyTheme.spacing.s10),
        )
        // 계산 중 표시는 새 줄이 생길 자리, 곧 목록 끝과 "음식 추가" 사이에 둔다.
        if (isCalculating) CalculatingNotice(modifier = Modifier.padding(top = NyummyTheme.spacing.s10))
        when {
            addNameInput != null -> AddFoodInput(
                text = addNameInput,
                canConfirm = canConfirmAdd,
                onTextChange = { onIntent(MealDetailV2Intent.ChangeAddName(it)) },
                onConfirm = { onIntent(MealDetailV2Intent.ConfirmAddFood) },
                onCancel = { onIntent(MealDetailV2Intent.CancelAddFood) },
                modifier = Modifier.padding(top = NyummyTheme.spacing.s10),
            )
            canAdd -> AddFoodButton(
                enabled = canEdit,
                onClick = { onIntent(MealDetailV2Intent.ClickAddFood) },
                modifier = Modifier.padding(top = NyummyTheme.spacing.s10),
            )
        }
    }
}

/**
 * 음식 줄 목록. 손잡이를 끌면 그 줄이 손가락을 따라가고 사이의 줄이 자리를 비켜 준다. 놓으면 옮겨진 자리를 [onMove]로 알린다.
 * 줄 높이는 모두 같다고 보고(한 줄을 재서) 몇 칸 옮겼는지 계산한다.
 */
@Composable
private fun ReorderableFoodList(
    foods: ImmutableList<MealFoodVO>,
    canEdit: Boolean,
    canReorder: Boolean,
    onMove: (from: Int, to: Int) -> Unit,
    onEdit: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var rowHeightPx by remember { mutableIntStateOf(0) }
    val gapPx = with(LocalDensity.current) { NyummyTheme.spacing.s8.toPx() }
    val stride = rowHeightPx + gapPx
    val currentOnMove by rememberUpdatedState(onMove)
    val lastIndex = foods.lastIndex

    /** 끌고 있는 줄이 지금 놓일 자리. */
    fun targetIndexOf(from: Int): Int =
        if (stride <= 0f) from else (from + (dragOffset / stride).roundToInt()).coerceIn(0, lastIndex)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s8)) {
        foods.forEachIndexed { index, food ->
            key(food.mealFoodId) {
                val dragging = draggingIndex
                val isDragged = dragging == index
                // 끌고 있는 줄이 지나간 자리의 줄은 한 칸 비켜 준다.
                val shift = when {
                    dragging == null || isDragged -> 0f
                    else -> {
                        val target = targetIndexOf(dragging)
                        when {
                            dragging < index && index <= target -> -stride
                            target <= index && index < dragging -> stride
                            else -> 0f
                        }
                    }
                }
                val animatedShift by animateFloatAsState(targetValue = shift, label = "foodRowShift")
                FoodRow(
                    food = food,
                    isMain = index == 0,
                    canEdit = canEdit,
                    showHandle = canReorder,
                    onEdit = { onEdit(food.mealFoodId) },
                    handleModifier = if (canEdit && canReorder) {
                        Modifier.pointerInput(index, lastIndex) {
                            detectDragGestures(
                                onDragStart = {
                                    draggingIndex = index
                                    dragOffset = 0f
                                },
                                onDrag = { change, amount ->
                                    change.consume()
                                    dragOffset += amount.y
                                },
                                onDragEnd = {
                                    val to = targetIndexOf(index)
                                    draggingIndex = null
                                    dragOffset = 0f
                                    if (to != index) currentOnMove(index, to)
                                },
                                onDragCancel = {
                                    draggingIndex = null
                                    dragOffset = 0f
                                },
                            )
                        }
                    } else {
                        Modifier
                    },
                    modifier = Modifier
                        .onSizeChanged { if (index == 0) rowHeightPx = it.height }
                        .zIndex(if (isDragged) 1f else 0f)
                        .graphicsLayer { translationY = if (isDragged) dragOffset else animatedShift },
                )
            }
        }
    }
}

/**
 * 음식 한 줄: 끌기 손잡이, 아이콘, 이름(맨 위 줄이면 "대표"), 인분과 열량, 탄단지, 고치기 연필.
 * 맨 위 줄(대표 음식)은 연초록, 나머지는 연회색이다. 영양을 아직 계산하지 못한 줄은 영양 대신 계산 중이라고 적는다.
 */
@Composable
private fun FoodRow(
    food: MealFoodVO,
    isMain: Boolean,
    canEdit: Boolean,
    showHandle: Boolean,
    onEdit: () -> Unit,
    handleModifier: Modifier,
    modifier: Modifier = Modifier,
) {
    val editDescription = stringResource(R.string.history_detail_v2_edit_food_description, food.name)
    val dragDescription = stringResource(R.string.history_detail_v2_drag_description, food.name)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                if (isMain) NyummyTheme.colors.bg.brandSubtle else NyummyTheme.colors.bg.surfaceSubtle,
                RoundedCornerShape(NyummyTheme.radius.m),
            )
            .heightIn(min = FoodRowMinHeight)
            .padding(start = NyummyTheme.spacing.s4, end = NyummyTheme.spacing.s4)
            .padding(vertical = NyummyTheme.spacing.s12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showHandle) {
            Box(
                modifier = handleModifier
                    .size(width = HandleTouchWidth, height = HandleTouchHeight)
                    .semantics { contentDescription = dragDescription },
                contentAlignment = Alignment.Center,
            ) {
                DragHandleDots(enabled = canEdit)
            }
        } else {
            Box(Modifier.size(width = NyummyTheme.spacing.s12, height = HandleTouchHeight))
        }
        Box(modifier = Modifier.size(FoodIconSize), contentAlignment = Alignment.Center) {
            NyummyFoodIcon(foodIconId = food.foodIconId)
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = NyummyTheme.spacing.s12),
            verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s2),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s6),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NyummyText(
                    text = food.name,
                    style = NyummyTheme.typography.bodyL,
                    maxLines = 1,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (isMain) MainBadge()
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s8),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PortionChip(portion = food.portion)
                NyummyText(
                    text = if (food.hasNutrition) {
                        stringResource(R.string.history_nutrition_kcal, numberLabelOf(food.calorieKcal))
                    } else {
                        stringResource(R.string.history_detail_v2_food_pending)
                    },
                    style = NyummyTheme.typography.labelS,
                    color = if (food.hasNutrition) NyummyTheme.colors.content.primary else NyummyTheme.colors.content.tertiary,
                )
            }
            if (food.hasNutrition) {
                Row(horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s8)) {
                    Macro(label = stringResource(R.string.history_detail_v2_macro_carb), gram = food.carbohydrateGram)
                    Macro(label = stringResource(R.string.history_detail_v2_macro_protein), gram = food.proteinGram)
                    Macro(label = stringResource(R.string.history_detail_v2_macro_fat), gram = food.fatGram)
                }
            }
        }
        Box(
            modifier = Modifier
                .size(EditFoodTouchSize)
                .nyummyClickable(onClick = onEdit, enabled = canEdit)
                .semantics { contentDescription = editDescription },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(CommonR.drawable.nyummy_ic_pencil),
                contentDescription = null,
                tint = if (canEdit) NyummyTheme.colors.content.tertiary else NyummyTheme.colors.content.disabled,
                modifier = Modifier.size(EditFoodIconSize),
            )
        }
    }
}

/** 끌기 손잡이. 점 여섯 개(두 줄 세 칸). */
@Composable
private fun DragHandleDots(enabled: Boolean) {
    val color = if (enabled) NyummyTheme.colors.content.quiet else NyummyTheme.colors.content.disabled
    Row(horizontalArrangement = Arrangement.spacedBy(HandleDotGap)) {
        repeat(2) {
            Column(verticalArrangement = Arrangement.spacedBy(HandleDotGap)) {
                repeat(3) { Box(Modifier.size(HandleDotSize).background(color, CircleShape)) }
            }
        }
    }
}

@Composable
private fun MainBadge() {
    NyummyText(
        text = stringResource(R.string.history_detail_v2_main_badge),
        style = NyummyTheme.typography.labelS,
        color = NyummyTheme.colors.content.onAction,
        modifier = Modifier
            .background(NyummyTheme.colors.bg.actionPrimary, RoundedCornerShape(NyummyTheme.radius.xs))
            .padding(horizontal = NyummyTheme.spacing.s6, vertical = NyummyTheme.spacing.s2),
    )
}

@Composable
private fun PortionChip(portion: Double) {
    NyummyText(
        text = stringResource(R.string.history_detail_v2_portion, portionLabelOf(portion)),
        style = NyummyTheme.typography.bodyS,
        color = NyummyTheme.colors.content.secondary,
        modifier = Modifier
            .background(NyummyTheme.colors.bg.surface, RoundedCornerShape(NyummyTheme.radius.xs))
            .padding(horizontal = NyummyTheme.spacing.s6, vertical = NyummyTheme.spacing.s2),
    )
}

@Composable
private fun Macro(label: String, gram: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s2)) {
        NyummyText(text = label, style = NyummyTheme.typography.bodyS, color = NyummyTheme.colors.content.tertiary)
        NyummyText(
            text = stringResource(R.string.history_detail_v2_gram, gram),
            style = NyummyTheme.typography.bodyS,
            color = NyummyTheme.colors.content.secondary,
        )
    }
}

/** 점선 테두리의 "음식 추가". 누르면 이름 입력 줄로 바뀐다. */
@Composable
private fun AddFoodButton(enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val lineColor = if (enabled) NyummyTheme.colors.content.brand else NyummyTheme.colors.content.disabled
    val radius = NyummyTheme.radius.m
    Row(
        modifier = modifier
            .fillMaxWidth()
            .nyummyClickable(onClick = onClick, enabled = enabled, pressedScale = 1f)
            .drawBehind {
                val stroke = DashStrokeWidth.toPx()
                drawRoundRect(
                    color = lineColor,
                    topLeft = Offset(stroke / 2, stroke / 2),
                    size = Size(size.width - stroke, size.height - stroke),
                    cornerRadius = CornerRadius(radius.toPx()),
                    style = Stroke(
                        width = stroke,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(DashLength.toPx(), DashGap.toPx())),
                    ),
                )
            }
            .padding(vertical = NyummyTheme.spacing.s12),
        horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s6, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(CommonR.drawable.nyummy_ic_plus),
            contentDescription = null,
            tint = NyummyTheme.colors.content.secondary,
            modifier = Modifier.size(AddIconSize),
        )
        NyummyText(text = stringResource(R.string.history_detail_v2_add_food), style = NyummyTheme.typography.bodyM, color = lineColor)
    }
}

/** 추가할 음식 이름 입력 줄. 열리면 바로 입력할 수 있게 키보드를 띄운다. */
@Composable
private fun AddFoodInput(
    text: String,
    canConfirm: Boolean,
    onTextChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    // 뒤로 가기는 화면을 나가지 않고 입력 줄만 닫는다.
    BackHandler(onBack = onCancel)
    val shape = RoundedCornerShape(NyummyTheme.radius.m)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s8)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(NyummyTheme.colors.bg.surface, shape)
                .border(NyummyTheme.borderWidth.bold, NyummyTheme.colors.border.focus, shape)
                .padding(start = NyummyTheme.spacing.s16, end = NyummyTheme.spacing.s8)
                .padding(vertical = NyummyTheme.spacing.s8),
            horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s12),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester)
                    .testTag(MealDetailV2AddInputTag),
                singleLine = true,
                textStyle = NyummyTheme.typography.bodyL.copy(color = NyummyTheme.colors.content.primary),
                cursorBrush = SolidColor(NyummyTheme.colors.content.brand),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (canConfirm) onConfirm() }),
            )
            NyummyButton(
                text = stringResource(R.string.history_detail_v2_add_confirm),
                onClick = onConfirm,
                size = NyummyButtonSize.S,
                enabled = canConfirm,
            )
        }
        NyummyText(
            text = catNameStringResource(R.string.history_detail_v2_add_hint),
            style = NyummyTheme.typography.bodyS,
            color = NyummyTheme.colors.content.tertiary,
        )
    }
}

/** 인분 표기. 1.0은 "1", 1.5는 "1.5". */
internal fun portionLabelOf(portion: Double): String =
    if (portion % 1.0 == 0.0) portion.toInt().toString() else portion.toString()

/** 계산 중 안내. UI 테스트가 문구와 무관하게 찾는다. */
const val MealDetailV2CalculatingTag = "meal_detail_v2_calculating"

/** 음식 추가 이름 입력란. */
const val MealDetailV2AddInputTag = "meal_detail_v2_add_input"

private val EditNameButtonSize = 32.dp
private val EditNameIconSize = 20.dp
private val FoodRowMinHeight = 88.dp
private val FoodIconSize = 36.dp
private val HandleTouchWidth = 32.dp
private val HandleTouchHeight = 40.dp
private val HandleDotSize = 3.dp
private val HandleDotGap = 3.dp
private val EditFoodTouchSize = 40.dp
private val EditFoodIconSize = 18.dp
private val AddIconSize = 16.dp
private val DashStrokeWidth = 1.dp
private val DashLength = 4.dp
private val DashGap = 3.dp

private val previewMeal = MealHistoryVO(
    id = "1",
    name = "한정식",
    recordedAt = "08:10",
    calorieKcal = 612,
    carbohydrateGram = 82,
    proteinGram = 24,
    fatGram = 18,
    orderIndex = 1,
    catComment = "채소 가득한 비빔밥이네! 고추장은 조금만 넣었지? 계란 반숙까지 완벽해.",
)

private val previewFoods = persistentListOf(
    MealFoodVO(1, "밥", 1.0, true, 300, 66, 6, 1, foodIconId = "5"),
    MealFoodVO(2, "된장찌개", 1.0, true, 110, 8, 7, 5, foodIconId = "15"),
    MealFoodVO(3, "불고기", 1.5, true, 150, 6, 9, 9, foodIconId = "14"),
    MealFoodVO(4, "시금치나물", 1.0, true, 35, 2, 2, 2, foodIconId = "1"),
    MealFoodVO(5, "배추김치", 0.5, true, 17, 0, 0, 1, foodIconId = "177"),
)

@Preview(showBackground = true, widthDp = 390, heightDp = 1334)
@Composable
private fun MealDetailV2ScreenPreview() {
    NyummyTheme {
        MealDetailV2Screen(
            uiState = MealDetailV2UIState(meal = previewMeal, foods = previewFoods, isLoading = false, feedback = AnalysisFeedbackUiState()),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 900)
@Composable
private fun MealDetailV2SingleFoodCalculatingPreview() {
    NyummyTheme {
        MealDetailV2Screen(
            uiState = MealDetailV2UIState(
                meal = previewMeal,
                foods = persistentListOf(MealFoodVO(1, "김치찌개", 1.0, hasNutrition = false)),
                isLoading = false,
                isCalculating = true,
            ),
            onIntent = {},
        )
    }
}
