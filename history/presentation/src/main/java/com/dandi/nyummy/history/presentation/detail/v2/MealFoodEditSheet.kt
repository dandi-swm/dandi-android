package com.dandi.nyummy.history.presentation.detail.v2

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyBottomSheet
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyButton
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyButtonStyle
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyCheckbox
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyChip
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyFoodIcon
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyText
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTextField
import com.dandi.nyummy.common.presentation.designsystem.foundation.nyummyClickable
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.common.presentation.image.NyummyFoodIconCategory
import com.dandi.nyummy.history.entity.MealFoodVO
import com.dandi.nyummy.history.presentation.R
import com.dandi.nyummy.common.presentation.R as CommonR

/**
 * 음식 고치기 시트(History · 06v3 시트).
 *
 * 아이콘(분류 칩 "전체"와 다섯 분류, 세 줄 일곱 칸씩 옆으로 넘기는 쪽), 음식 이름, 인분(0.5 단위, 0.5~5), 영양(직접 설정하기를 체크해야
 * 고칠 수 있다), 이 음식 지우기, 취소·저장하기. 변경을 보내는 동안에는 입력과 닫기를 막는다.
 * 좌우·아래 여백은 [NyummyBottomSheet]가 준다.
 */
@Composable
internal fun MealFoodEditSheet(
    sheet: MealFoodSheetUiState,
    isSaving: Boolean,
    onIntent: (MealDetailV2Intent) -> Unit,
) {
    NyummyBottomSheet(
        onDismissRequest = { onIntent(MealDetailV2Intent.CancelFoodSheet) },
        dismissible = !isSaving,
    ) {
        MealFoodEditSheetContent(sheet = sheet, isSaving = isSaving, onIntent = onIntent)
    }
}

@Composable
internal fun ColumnScope.MealFoodEditSheetContent(
    sheet: MealFoodSheetUiState,
    isSaving: Boolean,
    onIntent: (MealDetailV2Intent) -> Unit,
) {
    val enabled = !isSaving
    Column(
        modifier = Modifier
            .weight(1f, fill = false)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(top = NyummyTheme.spacing.s4, bottom = NyummyTheme.spacing.s16),
        verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s20),
    ) {
        NyummyText(
            text = stringResource(R.string.history_food_sheet_title),
            style = NyummyTheme.typography.titleM,
        )
        IconSection(sheet = sheet, enabled = enabled, onIntent = onIntent)
        Column(
            verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s20),
        ) {
            NyummyTextField(
                value = sheet.name,
                onValueChange = { onIntent(MealDetailV2Intent.ChangeFoodName(it)) },
                label = stringResource(R.string.history_food_sheet_name),
                errorMessage = if (sheet.name.isBlank()) stringResource(R.string.history_food_sheet_name_empty) else null,
                enabled = enabled,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                modifier = Modifier.testTag(MealFoodSheetNameTag),
            )
            PortionSection(sheet = sheet, enabled = enabled, onIntent = onIntent)
            NutritionSection(sheet = sheet, enabled = enabled, onIntent = onIntent)
            if (sheet.canDelete) {
                val deleteColor = if (enabled) NyummyTheme.colors.content.danger else NyummyTheme.colors.content.disabled
                Row(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .nyummyClickable(onClick = { onIntent(MealDetailV2Intent.ClickDeleteFood) }, enabled = enabled)
                        .padding(NyummyTheme.spacing.s8),
                    horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s6),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(CommonR.drawable.nyummy_ic_trash_2),
                        contentDescription = null,
                        tint = if (enabled) NyummyTheme.colors.content.primary else deleteColor,
                        modifier = Modifier.size(DeleteIconSize),
                    )
                    NyummyText(
                        text = stringResource(R.string.history_food_sheet_delete),
                        style = NyummyTheme.typography.labelM,
                        color = deleteColor,
                    )
                }
            }
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s8),
    ) {
        NyummyButton(
            text = stringResource(R.string.history_food_sheet_cancel),
            onClick = { onIntent(MealDetailV2Intent.CancelFoodSheet) },
            style = NyummyButtonStyle.Neutral,
            enabled = enabled,
            modifier = Modifier.weight(1f),
        )
        NyummyButton(
            text = stringResource(R.string.history_food_sheet_save),
            onClick = { onIntent(MealDetailV2Intent.SaveFoodSheet) },
            enabled = enabled && sheet.canSave,
            modifier = Modifier.weight(1f),
        )
    }
}

/** 아이콘 고르기: 분류 칩 한 줄(전체가 맨 앞)과 세 줄 일곱 칸짜리 쪽. 옆으로 넘기고, 아래 점이 지금 쪽을 알려 준다. */
@Composable
private fun IconSection(sheet: MealFoodSheetUiState, enabled: Boolean, onIntent: (MealDetailV2Intent) -> Unit) {
    val category = sheet.iconCategory
    val pages = remember(category) {
        val iconIds = if (category == null) NyummyFoodIconCategory.allIconIds else NyummyFoodIconCategory.iconIdsOf(category)
        iconIds.chunked(ICON_ROWS * ICON_COLUMNS)
    }
    val pagerState = rememberPagerState { pages.size }
    // 시트를 열거나 분류를 바꾸면 지금 아이콘이 있는 쪽에서, 없으면 첫 쪽에서 시작한다.
    LaunchedEffect(category) {
        pagerState.scrollToPage(pages.indexOfFirst { sheet.iconId in it }.coerceAtLeast(0))
    }
    Column(verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s10)) {
        NyummyText(
            text = stringResource(R.string.history_food_sheet_icon),
            style = NyummyTheme.typography.labelM,
        )
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s8),
        ) {
            NyummyChip(
                text = stringResource(R.string.history_food_sheet_category_all),
                selected = category == null,
                onClick = { onIntent(MealDetailV2Intent.SelectIconCategory(null)) },
                enabled = enabled,
            )
            NyummyFoodIconCategory.entries.forEach { entry ->
                NyummyChip(
                    text = stringResource(entry.labelRes),
                    selected = category == entry,
                    onClick = { onIntent(MealDetailV2Intent.SelectIconCategory(entry)) },
                    enabled = enabled,
                )
            }
        }
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(MealFoodSheetIconGridTag),
            verticalAlignment = Alignment.Top,
        ) { page ->
            // 마지막 쪽이 덜 차도 높이가 줄지 않게 늘 세 줄을 둔다.
            Column(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s4),
            ) {
                val rows = pages[page].chunked(ICON_COLUMNS)
                repeat(ICON_ROWS) { rowIndex ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        val row = rows.getOrNull(rowIndex).orEmpty()
                        repeat(ICON_COLUMNS) { column ->
                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                val iconId = row.getOrNull(column)
                                if (iconId == null) {
                                    Box(Modifier.size(IconCellSize))
                                } else {
                                    IconCell(
                                        iconId = iconId,
                                        selected = iconId == sheet.iconId,
                                        enabled = enabled,
                                        onClick = { onIntent(MealDetailV2Intent.SelectIcon(iconId)) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        if (pages.size > 1) {
            Row(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s4),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(pages.size) { index ->
                    val current = index == pagerState.currentPage
                    Box(
                        Modifier
                            .size(width = if (current) PageDotCurrentWidth else PageDotSize, height = PageDotSize)
                            .background(
                                if (current) NyummyTheme.colors.bg.actionPrimary else NyummyTheme.colors.bg.actionNeutral,
                                CircleShape,
                            ),
                    )
                }
            }
        }
    }
}

/** 아이콘 한 칸. 고른 아이콘만 연초록 바탕과 초록 테두리를 두른다. */
@Composable
private fun IconCell(iconId: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val description = stringResource(R.string.history_food_sheet_icon_description, iconId)
    val shape = RoundedCornerShape(NyummyTheme.radius.s)
    Box(
        modifier = Modifier
            .size(IconCellSize)
            .then(
                if (selected) {
                    Modifier
                        .background(NyummyTheme.colors.bg.brandSubtle, shape)
                        .border(NyummyTheme.borderWidth.bold, NyummyTheme.colors.border.focus, shape)
                } else {
                    Modifier
                },
            )
            .nyummyClickable(onClick = onClick, enabled = enabled)
            .semantics {
                contentDescription = description
                this.selected = selected
            }
            .padding(NyummyTheme.spacing.s6),
        contentAlignment = Alignment.Center,
    ) {
        NyummyFoodIcon(foodIconId = iconId)
    }
}

/** 인분: 왼쪽에 이름과 안내, 오른쪽에 − 값 +. 0.5 단위로 0.5~5. */
@Composable
private fun PortionSection(sheet: MealFoodSheetUiState, enabled: Boolean, onIntent: (MealDetailV2Intent) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s2)) {
            NyummyText(text = stringResource(R.string.history_food_sheet_portion), style = NyummyTheme.typography.labelM)
            NyummyText(
                text = stringResource(R.string.history_food_sheet_portion_hint),
                style = NyummyTheme.typography.bodyS,
                color = NyummyTheme.colors.content.tertiary,
            )
        }
        Row(
            modifier = Modifier
                .background(NyummyTheme.colors.bg.actionNeutral, RoundedCornerShape(NyummyTheme.radius.s))
                .padding(NyummyTheme.spacing.s4),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StepButton(
                icon = CommonR.drawable.nyummy_ic_minus,
                description = stringResource(R.string.history_food_sheet_portion_decrease),
                enabled = enabled && sheet.canDecreasePortion,
                onClick = { onIntent(MealDetailV2Intent.StepPortion(increase = false)) },
            )
            Box(modifier = Modifier.size(width = PortionValueWidth, height = StepButtonSize), contentAlignment = Alignment.Center) {
                NyummyText(
                    text = stringResource(R.string.history_detail_v2_portion, portionLabelOf(sheet.portion)),
                    style = NyummyTheme.typography.labelL,
                    modifier = Modifier.testTag(MealFoodSheetPortionTag),
                )
            }
            StepButton(
                icon = CommonR.drawable.nyummy_ic_plus,
                description = stringResource(R.string.history_food_sheet_portion_increase),
                enabled = enabled && sheet.canIncreasePortion,
                onClick = { onIntent(MealDetailV2Intent.StepPortion(increase = true)) },
            )
        }
    }
}

@Composable
private fun StepButton(icon: Int, description: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(StepButtonSize)
            .background(
                if (enabled) NyummyTheme.colors.bg.surface else NyummyTheme.colors.bg.actionNeutral,
                RoundedCornerShape(NyummyTheme.radius.xs),
            )
            .nyummyClickable(onClick = onClick, enabled = enabled)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = if (enabled) NyummyTheme.colors.content.primary else NyummyTheme.colors.content.disabled,
            modifier = Modifier.size(StepIconSize),
        )
    }
}

/** 영양 네 칸. "직접 설정하기"를 체크해야 고칠 수 있고, 체크하지 않으면 저장해도 영양은 그대로다. */
@Composable
private fun NutritionSection(sheet: MealFoodSheetUiState, enabled: Boolean, onIntent: (MealDetailV2Intent) -> Unit) {
    val manual = sheet.isManualNutrition
    Column(verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s8)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            NyummyText(
                text = stringResource(R.string.history_food_sheet_nutrition),
                style = NyummyTheme.typography.labelM,
                modifier = Modifier.weight(1f),
            )
            Row(
                modifier = Modifier
                    .nyummyClickable(
                        onClick = { onIntent(MealDetailV2Intent.SetManualNutrition(!manual)) },
                        enabled = enabled,
                        pressedScale = 1f,
                    )
                    .testTag(MealFoodSheetManualTag),
                horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s6),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NyummyCheckbox(checked = manual, onCheckedChange = null, enabled = enabled)
                NyummyText(text = stringResource(R.string.history_food_sheet_manual), style = NyummyTheme.typography.bodyM)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s8)) {
            NutrientField(MealFoodNutrient.CALORIE, sheet.calorie, R.string.history_food_sheet_calorie, R.string.history_food_sheet_unit_kcal, manual && enabled, onIntent, Modifier.weight(1f))
            NutrientField(MealFoodNutrient.CARBOHYDRATE, sheet.carbohydrate, R.string.history_food_sheet_carb, R.string.history_food_sheet_unit_gram, manual && enabled, onIntent, Modifier.weight(1f))
            NutrientField(MealFoodNutrient.PROTEIN, sheet.protein, R.string.history_food_sheet_protein, R.string.history_food_sheet_unit_gram, manual && enabled, onIntent, Modifier.weight(1f))
            NutrientField(MealFoodNutrient.FAT, sheet.fat, R.string.history_food_sheet_fat, R.string.history_food_sheet_unit_gram, manual && enabled, onIntent, Modifier.weight(1f))
        }
        NyummyText(
            text = stringResource(if (manual) R.string.history_food_sheet_manual_on_hint else R.string.history_food_sheet_manual_off_hint),
            style = NyummyTheme.typography.bodyS,
            color = NyummyTheme.colors.content.tertiary,
        )
    }
}

@Composable
private fun NutrientField(
    nutrient: MealFoodNutrient,
    value: String,
    labelRes: Int,
    unitRes: Int,
    editable: Boolean,
    onIntent: (MealDetailV2Intent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(NyummyTheme.radius.s)
    val textColor = if (editable) NyummyTheme.colors.content.primary else NyummyTheme.colors.content.tertiary
    Column(
        modifier = modifier
            .background(if (editable) NyummyTheme.colors.bg.surface else NyummyTheme.colors.bg.surfaceSubtle, shape)
            .then(if (editable) Modifier.border(NyummyTheme.borderWidth.bold, NyummyTheme.colors.border.strong, shape) else Modifier)
            .padding(horizontal = NyummyTheme.spacing.s10, vertical = NyummyTheme.spacing.s8),
        verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s2),
    ) {
        NyummyText(text = stringResource(labelRes), style = NyummyTheme.typography.bodyS, color = NyummyTheme.colors.content.tertiary)
        Row(
            horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s2),
            verticalAlignment = Alignment.Bottom,
        ) {
            BasicTextField(
                value = value,
                onValueChange = { onIntent(MealDetailV2Intent.ChangeNutrient(nutrient, it)) },
                enabled = editable,
                singleLine = true,
                textStyle = NyummyTheme.typography.labelL.copy(color = textColor),
                cursorBrush = SolidColor(NyummyTheme.colors.content.brand),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                // 값 길이만큼만 차지해 단위가 바로 옆에 붙는다. 비어도 누를 수 있게 최소 폭을 둔다.
                modifier = Modifier
                    .width(IntrinsicSize.Min)
                    .widthIn(min = NutrientValueMinWidth)
                    .weight(1f, fill = false)
                    .testTag(nutrientFieldTagOf(nutrient)),
            )
            NyummyText(text = stringResource(unitRes), style = NyummyTheme.typography.bodyS, color = NyummyTheme.colors.content.secondary)
        }
    }
}

private val NyummyFoodIconCategory.labelRes: Int
    get() = when (this) {
        NyummyFoodIconCategory.MAIN -> R.string.history_food_sheet_category_main
        NyummyFoodIconCategory.SIDE -> R.string.history_food_sheet_category_side
        NyummyFoodIconCategory.FRUIT -> R.string.history_food_sheet_category_fruit
        NyummyFoodIconCategory.SNACK -> R.string.history_food_sheet_category_snack
        NyummyFoodIconCategory.DRINK -> R.string.history_food_sheet_category_drink
    }

const val MealFoodSheetNameTag = "meal_food_sheet_name"
const val MealFoodSheetIconGridTag = "meal_food_sheet_icon_grid"
const val MealFoodSheetPortionTag = "meal_food_sheet_portion"
const val MealFoodSheetManualTag = "meal_food_sheet_manual"

/** 영양 입력 칸. */
fun nutrientFieldTagOf(nutrient: MealFoodNutrient): String = "meal_food_sheet_nutrient_${nutrient.name}"

private const val ICON_ROWS = 3
private const val ICON_COLUMNS = 7
private val IconCellSize = 44.dp
private val PageDotSize = 6.dp
private val PageDotCurrentWidth = 16.dp
private val StepButtonSize = 36.dp
private val StepIconSize = 16.dp
private val PortionValueWidth = 72.dp
private val NutrientValueMinWidth = 12.dp
private val DeleteIconSize = 18.dp

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun MealFoodEditSheetPreview() {
    NyummyTheme {
        Column {
            MealFoodEditSheetContent(
                sheet = MealFoodSheetUiState.of(
                    MealFoodVO(3, "불고기", 1.5, true, 150, 6, 9, 9, foodIconId = "14"),
                    canDelete = true,
                ).copy(isManualNutrition = true),
                isSaving = false,
                onIntent = {},
            )
        }
    }
}
