package com.dandi.nyummy.meal.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dandi.nyummy.common.presentation.cat.catNameStringResource
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyButton
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyButtonSize
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyCheckbox
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyIconButton
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyIconButtonStyle
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyText
import com.dandi.nyummy.common.presentation.designsystem.foundation.nyummyClickable
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.meal.entity.FoundFoodVO
import com.dandi.nyummy.meal.presentation.R
import com.dandi.nyummy.meal.presentation.model.FoodPickDraft
import com.dandi.nyummy.meal.presentation.model.FoodPickLine
import com.dandi.nyummy.common.presentation.R as CommonR

/**
 * 찾은 음식 고르기 화면(식사 기록 06c). 서버가 찾은 음식을 체크로 고르고, 이름을 적어 음식을 추가한 뒤 가져간다.
 *
 * - 머리: 닫기와 "식사 기록".
 * - 제목 아래로 음식 줄(연초록, 체크박스 + 이름), 점선 "음식 추가" 또는 이름 입력 줄.
 * - 맨 아래 "N개 가져가기". 하나도 고르지 않았거나 보내는 중이면 누를 수 없고, 이름을 적는 동안에는 보이지 않는다.
 *
 * 기록 직후의 고르기와 기록 탭에서 들어온 고르기가 같이 쓴다. 닫기가 무엇을 하는지는 부르는 쪽이 정한다.
 */
@Composable
fun FoodPickContent(
    draft: FoodPickDraft,
    isSubmitting: Boolean,
    onClose: () -> Unit,
    onToggle: (Int) -> Unit,
    onOpenNameInput: () -> Unit,
    onNameInputChange: (String) -> Unit,
    onConfirmNameInput: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NyummyTheme.colors.bg.surface)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        FoodPickHeader(onClose = onClose)
        val scrollState = rememberScrollState()
        val isNameInputOpen = draft.nameInput != null
        // 이름 입력 줄은 목록 맨 아래에 열린다. 키보드가 올라와 보이는 영역이 줄어도 입력 줄이 보이게 끝까지 내린다.
        LaunchedEffect(isNameInputOpen, scrollState.maxValue) {
            if (isNameInputOpen) scrollState.animateScrollTo(scrollState.maxValue)
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(horizontal = NyummyTheme.spacing.gutter)
                .padding(top = NyummyTheme.spacing.s12, bottom = NyummyTheme.spacing.s16),
        ) {
            NyummyText(text = catNameStringResource(R.string.meal_food_pick_title), style = NyummyTheme.typography.titleL)
            NyummyText(
                text = stringResource(R.string.meal_food_pick_body),
                style = NyummyTheme.typography.bodyM,
                color = NyummyTheme.colors.content.secondary,
                modifier = Modifier.padding(top = NyummyTheme.spacing.s4),
            )
            Column(
                modifier = Modifier.padding(top = NyummyTheme.spacing.s16),
                verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s8),
            ) {
                draft.lines.forEach { line ->
                    FoodPickRow(line = line, enabled = !isSubmitting, onToggle = { onToggle(line.key) })
                }
                val nameInput = draft.nameInput
                when {
                    nameInput != null -> FoodNameInputRow(
                        text = nameInput,
                        canConfirm = draft.canConfirmAdd && !isSubmitting,
                        onTextChange = onNameInputChange,
                        onConfirm = onConfirmNameInput,
                    )
                    draft.canAdd -> AddFoodRow(enabled = !isSubmitting, onClick = onOpenNameInput)
                }
            }
        }
        // 이름을 적는 동안에는 키보드가 버튼 자리를 차지한다. 버튼을 키보드 위로 올리면 입력 줄을 가리므로 숨긴다.
        if (!isNameInputOpen) {
            NyummyButton(
                text = stringResource(R.string.meal_food_pick_submit, draft.checkedCount),
                onClick = onSubmit,
                enabled = draft.canSubmit && !isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = NyummyTheme.spacing.gutter)
                    .padding(bottom = NyummyTheme.spacing.s16)
                    .testTag(FoodPickSubmitTag),
            )
        }
    }
}

@Composable
private fun FoodPickHeader(onClose: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = NyummyTheme.spacing.s16, vertical = NyummyTheme.spacing.s8),
    ) {
        NyummyIconButton(
            icon = CommonR.drawable.nyummy_ic_x,
            contentDescription = stringResource(R.string.meal_record_close_content_description),
            onClick = onClose,
            modifier = Modifier.align(Alignment.CenterStart),
            style = NyummyIconButtonStyle.Ghost,
        )
        NyummyText(
            text = stringResource(R.string.meal_record_title),
            style = NyummyTheme.typography.titleS,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

/** 음식 한 줄. 줄 어디를 눌러도 체크가 바뀐다. */
@Composable
private fun FoodPickRow(line: FoodPickLine, enabled: Boolean, onToggle: () -> Unit) {
    val stateText = stringResource(if (line.isChecked) R.string.meal_food_pick_checked else R.string.meal_food_pick_unchecked)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(NyummyTheme.colors.bg.brandSubtle, RoundedCornerShape(NyummyTheme.radius.m))
            .nyummyClickable(enabled = enabled, onClick = onToggle, pressedScale = 1f)
            .semantics(mergeDescendants = true) {
                role = Role.Checkbox
                stateDescription = stateText
            }
            .heightIn(min = RowMinHeight)
            .padding(start = RowStartPadding, end = NyummyTheme.spacing.s12)
            .padding(vertical = NyummyTheme.spacing.s12),
        horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NyummyCheckbox(checked = line.isChecked, onCheckedChange = null)
        NyummyText(
            text = line.name,
            style = NyummyTheme.typography.titleS,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
    }
}

/** 점선 테두리의 "음식 추가". 누르면 이름 입력 줄로 바뀐다. */
@Composable
private fun AddFoodRow(enabled: Boolean, onClick: () -> Unit) {
    val lineColor = NyummyTheme.colors.content.brand
    val radius = NyummyTheme.radius.m
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .nyummyClickable(singleClick = true, enabled = enabled, onClick = onClick, pressedScale = 1f)
            .drawBehind { drawDashedOutline(color = lineColor, cornerRadius = radius.toPx()) }
            .heightIn(min = RowMinHeight)
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
        NyummyText(
            text = stringResource(R.string.meal_food_pick_add),
            style = NyummyTheme.typography.bodyM,
            color = NyummyTheme.colors.content.brand,
        )
    }
}

/** 추가할 음식 이름 입력 줄. 열리면 바로 입력할 수 있게 키보드를 띄운다. */
@Composable
private fun FoodNameInputRow(
    text: String,
    canConfirm: Boolean,
    onTextChange: (String) -> Unit,
    onConfirm: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    val shape = RoundedCornerShape(NyummyTheme.radius.m)
    Column(verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s8)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(NyummyTheme.colors.bg.surface, shape)
                .border(NyummyTheme.borderWidth.bold, NyummyTheme.colors.border.focus, shape)
                .heightIn(min = RowMinHeight)
                .padding(start = RowStartPadding, end = NyummyTheme.spacing.s8)
                .padding(vertical = NyummyTheme.spacing.s8),
            horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s12),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NyummyCheckbox(checked = true, onCheckedChange = null)
            BasicTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester)
                    .testTag(FoodPickNameInputTag),
                singleLine = true,
                textStyle = NyummyTheme.typography.titleS.copy(color = NyummyTheme.colors.content.primary),
                cursorBrush = SolidColor(NyummyTheme.colors.content.brand),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (canConfirm) onConfirm() }),
            )
            NyummyButton(
                text = stringResource(R.string.meal_food_pick_add_confirm),
                onClick = onConfirm,
                size = NyummyButtonSize.S,
                enabled = canConfirm,
            )
        }
        NyummyText(
            text = stringResource(R.string.meal_food_pick_add_hint),
            style = NyummyTheme.typography.bodyS,
            color = NyummyTheme.colors.content.tertiary,
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawDashedOutline(color: Color, cornerRadius: Float) {
    val stroke = DashStrokeWidth.toPx()
    drawRoundRect(
        color = color,
        topLeft = androidx.compose.ui.geometry.Offset(stroke / 2, stroke / 2),
        size = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke),
        cornerRadius = CornerRadius(cornerRadius),
        style = Stroke(
            width = stroke,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(DashLength.toPx(), DashGap.toPx())),
        ),
    )
}

/** 가져가기 버튼. UI 테스트가 문구와 무관하게 찾는다. */
const val FoodPickSubmitTag = "food_pick_submit"

/** 음식 이름 입력란. */
const val FoodPickNameInputTag = "food_pick_name_input"

private val RowMinHeight = 48.dp
private val RowStartPadding = 14.dp
private val AddIconSize = 16.dp
private val DashStrokeWidth = 1.dp
private val DashLength = 4.dp
private val DashGap = 3.dp

private val previewFoods = listOf("밥", "된장찌개", "불고기", "시금치나물", "배추김치").mapIndexed { index, name ->
    FoundFoodVO(mealFoodId = index + 1L, name = name)
}

@Composable
private fun PreviewFoodPick(draft: FoodPickDraft, isSubmitting: Boolean = false) {
    NyummyTheme {
        FoodPickContent(
            draft = draft,
            isSubmitting = isSubmitting,
            onClose = {},
            onToggle = {},
            onOpenNameInput = {},
            onNameInputChange = {},
            onConfirmNameInput = {},
            onSubmit = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun FoodPickAllCheckedPreview() {
    PreviewFoodPick(FoodPickDraft.of(previewFoods))
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun FoodPickOneUncheckedPreview() {
    PreviewFoodPick(FoodPickDraft.of(previewFoods).toggle(5))
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun FoodPickNameInputPreview() {
    PreviewFoodPick(FoodPickDraft.of(previewFoods).toggle(5).openNameInput().changeNameInput("김밥"))
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun FoodPickFullPreview() {
    PreviewFoodPick(FoodPickDraft.of(previewFoods + previewFoods.map { it.copy(mealFoodId = it.mealFoodId + 10) }))
}
