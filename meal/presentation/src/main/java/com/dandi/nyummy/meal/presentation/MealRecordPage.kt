package com.dandi.nyummy.meal.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dandi.nyummy.common.presentation.component.DandiText
import com.dandi.nyummy.common.presentation.component.NyummyButton
import com.dandi.nyummy.common.presentation.component.NyummyButtonSize
import com.dandi.nyummy.common.presentation.component.NyummyButtonStyle
import com.dandi.nyummy.common.presentation.component.NyummyPhotoPicker
import com.dandi.nyummy.common.presentation.component.NyummyScreenHeader
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemTheme
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemThemeImpl
import com.dandi.nyummy.meal.entity.MealRecordVO
import kotlinx.collections.immutable.ImmutableList
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * 식사 기록 화면입니다.
 *
 * 사진·음식 설명·촬영 시각·음식 아이콘을 확인하고 기록을 저장하는 화면으로,
 * 상태 수집과 [MealRecordIntent] 전달만 담당합니다.
 */
@Composable
fun MealRecordPage(
    modifier: Modifier = Modifier,
    viewModel: MealRecordViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    MealRecordScreen(
        uiState = uiState,
        onIntent = viewModel::onIntent,
        modifier = modifier,
    )
}

@Composable
private fun MealRecordScreen(
    uiState: MealRecordUIState,
    onIntent: (MealRecordIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = DesignSystemThemeImpl.designSystemColor
    val spacing = DesignSystemThemeImpl.designSystemSpacing
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bgSurfaceIvory),
    ) {
        NyummyScreenHeader(
            title = stringResource(R.string.meal_record_title),
            onBackClick = { onIntent(MealRecordIntent.ClickBack) },
            backContentDescription = stringResource(R.string.meal_record_back_content_description),
            modifier = Modifier.padding(horizontal = spacing.space16),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.space20),
        ) {
            Spacer(Modifier.height(spacing.space12))
            DandiText(
                text = stringResource(R.string.meal_record_heading),
                color = colors.contentDefaultLevel0,
                style = DesignSystemThemeImpl.typeScale.displayRegularXL,
            )
            Spacer(Modifier.height(spacing.space12))
            NyummyPhotoPicker(
                state = uiState.photoState,
                onClick = { onIntent(MealRecordIntent.ClickChangePhoto) },
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Spacer(Modifier.height(spacing.space16))
            MealDescriptionField(
                value = uiState.record.description,
                onValueChange = { onIntent(MealRecordIntent.ChangeDescription(it)) },
            )
            Spacer(Modifier.height(spacing.space16))
            MealTimeRow(capturedAt = uiState.record.capturedAt)
            Spacer(Modifier.height(spacing.space12))
            FoodIconSection(
                foodIcons = uiState.foodIcons,
                selectedFoodIconId = uiState.record.foodIconId,
                onIconSelect = { onIntent(MealRecordIntent.SelectFoodIcon(it)) },
                onViewAllClick = { onIntent(MealRecordIntent.ClickViewAllIcons) },
            )
            Spacer(Modifier.height(spacing.space24))
        }
        Column(
            modifier = Modifier.padding(horizontal = spacing.space20),
        ) {
            NyummyButton(
                label = stringResource(R.string.meal_record_cta),
                modifier = Modifier.fillMaxWidth(),
                style = NyummyButtonStyle.Primary,
                size = NyummyButtonSize.Large,
                enabled = uiState.isSaveEnabled,
                onClick = { onIntent(MealRecordIntent.ClickSave) },
            )
            Spacer(Modifier.height(spacing.space8))
            DandiText(
                text = stringResource(R.string.meal_record_cta_note),
                modifier = Modifier.fillMaxWidth(),
                color = colors.contentDefaultLevel2,
                textAlign = TextAlign.Center,
                style = DesignSystemThemeImpl.typeScale.textRegularS,
            )
            Spacer(Modifier.height(spacing.space12))
        }
    }
}

@Composable
private fun MealDescriptionField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = DesignSystemThemeImpl.designSystemColor
    val spacing = DesignSystemThemeImpl.designSystemSpacing
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(DesignSystemThemeImpl.designSystemRadius.radius20),
        color = colors.bgInputDefault,
        border = BorderStroke(MealCardBorderWidth, colors.borderMealPhoto),
    ) {
        Column(modifier = Modifier.padding(spacing.space16)) {
            DandiText(
                text = stringResource(R.string.meal_record_description_label),
                color = colors.contentDefaultLevel0,
                style = DesignSystemThemeImpl.typeScale.labelStrongS,
            )
            Spacer(Modifier.height(spacing.space8))
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = DesignSystemThemeImpl.typeScale.textStrongM.merge(
                    TextStyle(color = colors.contentInputValue),
                ),
                cursorBrush = SolidColor(colors.contentSelectionPrimary),
            )
            Spacer(Modifier.height(spacing.space8))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(MealDividerHeight)
                    .background(colors.borderMealPhoto),
            )
            Spacer(Modifier.height(spacing.space12))
            DandiText(
                text = stringResource(R.string.meal_record_description_helper),
                color = colors.contentDefaultLevel2,
                style = DesignSystemThemeImpl.typeScale.textRegularS,
            )
        }
    }
}

@Composable
private fun MealTimeRow(
    capturedAt: String,
    modifier: Modifier = Modifier,
) {
    val colors = DesignSystemThemeImpl.designSystemColor
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(MealTimeRowHeight),
        shape = RoundedCornerShape(DesignSystemThemeImpl.designSystemRadius.radius20),
        color = colors.bgInputDefault,
        border = BorderStroke(MealCardBorderWidth, colors.borderMealPhoto),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = DesignSystemThemeImpl.designSystemSpacing.space16),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DandiText(
                text = stringResource(R.string.meal_record_time_label),
                color = colors.contentDefaultLevel2,
                style = DesignSystemThemeImpl.typeScale.labelRegularXS,
            )
            Spacer(Modifier.weight(1f))
            if (capturedAt.isEmpty()) {
                DandiText(
                    text = stringResource(R.string.meal_record_time_photo_required),
                    color = colors.contentDefaultLevel2,
                    style = DesignSystemThemeImpl.typeScale.textRegularS,
                )
            } else {
                DandiText(
                    text = formatCapturedAt(capturedAt),
                    color = colors.contentDefaultLevel0,
                    style = DesignSystemThemeImpl.typeScale.textStrongXL,
                )
            }
        }
    }
}

@Composable
private fun FoodIconSection(
    foodIcons: ImmutableList<MealFoodIcon>,
    selectedFoodIconId: String,
    onIconSelect: (String) -> Unit,
    onViewAllClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = DesignSystemThemeImpl.designSystemColor
    val spacing = DesignSystemThemeImpl.designSystemSpacing
    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            DandiText(
                text = stringResource(R.string.meal_record_icon_section_label),
                color = colors.contentDefaultLevel0,
                style = DesignSystemThemeImpl.typeScale.labelStrongS,
            )
            Spacer(Modifier.weight(1f))
            Surface(
                onClick = onViewAllClick,
                modifier = Modifier
                    .width(ViewAllButtonWidth)
                    .height(DesignSystemThemeImpl.designSystemSize.minimumTouchTarget),
                shape = RoundedCornerShape(DesignSystemThemeImpl.designSystemRadius.radius16),
                color = colors.bgActionSecondaryDefault,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    DandiText(
                        text = stringResource(R.string.meal_record_icon_view_all),
                        color = colors.contentActionSecondary,
                        textAlign = TextAlign.Center,
                        style = DesignSystemThemeImpl.typeScale.labelStrongXS,
                    )
                }
            }
        }
        Spacer(Modifier.height(spacing.space4))
        // 고정 76dp 셀 4개(328dp)가 좁은 화면(360dp 이하) 가용 폭을 넘을 수 있어 가로 스크롤로 접근성 보장.
        Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
            foodIcons.forEach { icon ->
                FoodIconCell(
                    icon = icon,
                    selected = icon.id == selectedFoodIconId,
                    onClick = { onIconSelect(icon.id) },
                )
                if (icon != foodIcons.last()) {
                    Spacer(Modifier.width(spacing.space8))
                }
            }
        }
    }
}

@Composable
private fun FoodIconCell(
    icon: MealFoodIcon,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = DesignSystemThemeImpl.designSystemColor
    Surface(
        modifier = modifier
            .size(FoodIconCellWidth, FoodIconCellHeight)
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onClick,
            ),
        shape = RoundedCornerShape(DesignSystemThemeImpl.designSystemRadius.radius16),
        color = if (selected) colors.bgCalendarSelected else colors.bgSurfaceIvory,
        border = if (selected) {
            BorderStroke(FoodIconCellSelectedBorderWidth, colors.borderSelectionPrimary)
        } else {
            BorderStroke(MealCardBorderWidth, colors.borderActionSecondary)
        },
    ) {
        Box(Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(icon.iconRes),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = FoodIconTopGap)
                    .size(FoodIconImageSize),
            )
            DandiText(
                text = stringResource(icon.labelRes),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = FoodIconLabelBottomGap),
                color = if (selected) colors.contentActionSecondary else colors.contentDefaultLevel1,
                textAlign = TextAlign.Center,
                style = DesignSystemThemeImpl.typeScale.labelStrongXS,
            )
            if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = FoodIconCheckBadgeGap, end = FoodIconCheckBadgeGap)
                        .size(FoodIconCheckBadgeSize)
                        .background(colors.bgSelectionPrimary, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    DandiText(
                        text = SelectedCheckGlyph,
                        color = colors.contentActionPrimary,
                        textAlign = TextAlign.Center,
                        style = DesignSystemThemeImpl.typeScale.labelStrongXS,
                    )
                }
            }
        }
    }
}

/**
 * 서버 timestamp(ISO-8601) 문자열을 시각 표시 문자열로 바꾼다.
 * 형식이 예상과 다르면 원문을 그대로 보여준다. 서버 형식 확정 시 data 레이어 변환과 함께 정리한다.
 */
private fun formatCapturedAt(capturedAt: String): String = runCatching {
    val parsed = requireNotNull(SimpleDateFormat(ServerTimestampPattern, Locale.US).parse(capturedAt))
    SimpleDateFormat(CapturedAtPattern, Locale.getDefault()).format(parsed)
}.getOrDefault(capturedAt)

private const val ServerTimestampPattern = "yyyy-MM-dd'T'HH:mm:ssXXX"
private const val CapturedAtPattern = "H:mm"
private const val SelectedCheckGlyph = "✓"
private val MealCardBorderWidth = 1.dp
private val MealDividerHeight = 1.dp
private val MealTimeRowHeight = 64.dp
private val ViewAllButtonWidth = 88.dp
private val FoodIconCellWidth = 76.dp
private val FoodIconCellHeight = 80.dp
private val FoodIconCellSelectedBorderWidth = 2.dp
private val FoodIconImageSize = 40.dp
private val FoodIconTopGap = 4.dp
private val FoodIconLabelBottomGap = 8.dp
private val FoodIconCheckBadgeGap = 2.dp
private val FoodIconCheckBadgeSize = 18.dp

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun MealRecordScreenEmptyPreview() {
    DesignSystemTheme {
        MealRecordScreen(
            uiState = MealRecordUIState.empty,
            onIntent = {},
        )
    }
}

/** 프리뷰 전용 고정 촬영 시각. 디자인 시안의 예시 표기(12:36)와 같은 값으로 렌더링된다. */
private const val PreviewCapturedAt = "2026-07-24T12:36:00+09:00"

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun MealRecordScreenFilledPreview() {
    DesignSystemTheme {
        MealRecordScreen(
            uiState = MealRecordUIState(
                record = MealRecordVO(
                    photoUri = "content://meal/sample",
                    description = "닭가슴살 포케와 현미밥, 채소",
                    capturedAt = PreviewCapturedAt,
                    foodIconId = MealFoodIcon.Salad.id,
                ),
            ),
            onIntent = {},
        )
    }
}
