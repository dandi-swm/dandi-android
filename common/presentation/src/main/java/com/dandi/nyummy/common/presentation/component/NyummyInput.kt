package com.dandi.nyummy.common.presentation.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemTheme
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemThemeImpl

/**
 * Canonical single-line / multiline input from Figma node `992:765`.
 *
 * The visual state is derived from focus, [value], [enabled], [readOnly], and [isError]; callers do
 * not pass a visual-only state that can drift from the real text-field behavior.
 */
@Composable
fun NyummyTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    label: String? = null,
    helperText: String? = null,
    isError: Boolean = false,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
) {
    var focused by remember { mutableStateOf(false) }
    NyummyTextFieldCore(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        placeholder = placeholder,
        label = label,
        helperText = helperText,
        isError = isError,
        enabled = enabled,
        readOnly = readOnly,
        singleLine = singleLine,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        focused = focused,
        onFocusChanged = { focused = it },
    )
}

@Composable
private fun NyummyTextFieldCore(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier,
    placeholder: String,
    label: String?,
    helperText: String?,
    isError: Boolean,
    enabled: Boolean,
    readOnly: Boolean,
    singleLine: Boolean,
    leadingIcon: (@Composable () -> Unit)?,
    trailingIcon: (@Composable () -> Unit)?,
    focused: Boolean,
    onFocusChanged: (Boolean) -> Unit,
) {
    val colors = DesignSystemThemeImpl.designSystemColor
    val borderColor = when {
        isError -> colors.borderInputError
        focused -> colors.borderInputFocused
        else -> colors.borderInputDefault
    }
    val borderWidth = if (isError || focused) InputEmphasisBorderWidth else InputBorderWidth
    val background = when {
        !enabled -> colors.bgInputDisabled
        readOnly -> colors.bgSurfaceSubtle
        else -> colors.bgInputDefault
    }
    val valueColor = if (enabled) colors.contentInputValue else colors.contentInputDisabled
    val placeholderColor = if (enabled) colors.contentInputPlaceholder else colors.contentInputDisabled
    val labelColor = if (enabled) colors.contentDefaultLevel1 else colors.contentInputDisabled
    val helperColor = when {
        !enabled -> colors.contentInputDisabled
        isError -> colors.contentError
        else -> colors.contentDefaultLevel2
    }
    val iconColor = if (enabled) colors.contentIconLevel1 else colors.contentInputDisabled
    val interactionSource = remember { MutableInteractionSource() }
    val visualStateDescription = when {
        !enabled -> DisabledStateDescription
        readOnly -> ReadOnlyStateDescription
        isError -> ErrorStateDescription
        focused -> FocusedStateDescription
        value.isNotEmpty() -> FilledStateDescription
        else -> EmptyStateDescription
    }

    Column(
        modifier = modifier,
    ) {
        if (label != null) {
            DandiText(
                text = label,
                color = labelColor,
                style = DesignSystemThemeImpl.typeScale.labelStrongS,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(InputLabelFieldGap))
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = if (singleLine) SingleLineFieldHeight else MultilineFieldHeight)
                .background(background, DesignSystemThemeImpl.designSystemShape.inputDefault)
                .border(
                    border = BorderStroke(borderWidth, borderColor),
                    shape = DesignSystemThemeImpl.designSystemShape.inputDefault,
                )
                .onFocusChanged { onFocusChanged(it.isFocused) }
                .semantics {
                    contentDescription = label ?: placeholder
                    stateDescription = visualStateDescription
                    if (isError) error(helperText ?: DefaultErrorDescription)
                }
                .padding(
                    horizontal = DesignSystemThemeImpl.designSystemSpacing.space16,
                    vertical = if (singleLine) InputNoVerticalPadding else MultilineVerticalPadding,
                ),
            enabled = enabled,
            readOnly = readOnly,
            singleLine = singleLine,
            maxLines = if (singleLine) 1 else InputMaximumLines,
            textStyle = DesignSystemThemeImpl.typeScale.textRegularM.merge(TextStyle(color = valueColor)),
            cursorBrush = SolidColor(colors.contentSelectionPrimary),
            interactionSource = interactionSource,
            decorationBox = { innerTextField ->
                Row(
                    verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top,
                ) {
                    if (leadingIcon != null) {
                        CompositionLocalProvider(LocalContentColor provides iconColor) {
                            Box(
                                modifier = Modifier.size(InputIconSize),
                                contentAlignment = Alignment.Center,
                                content = { leadingIcon() },
                            )
                        }
                        Spacer(Modifier.width(DesignSystemThemeImpl.designSystemSpacing.space8))
                    }
                    Box(Modifier.weight(1f)) {
                        if (value.isEmpty()) {
                            DandiText(
                                text = placeholder,
                                color = placeholderColor,
                                style = DesignSystemThemeImpl.typeScale.textRegularM,
                                maxLines = if (singleLine) 1 else InputMaximumLines,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        innerTextField()
                    }
                    if (trailingIcon != null) {
                        Spacer(Modifier.width(DesignSystemThemeImpl.designSystemSpacing.space8))
                        CompositionLocalProvider(LocalContentColor provides iconColor) {
                            Box(
                                modifier = Modifier.size(InputIconSize),
                                contentAlignment = Alignment.Center,
                                content = { trailingIcon() },
                            )
                        }
                    }
                }
            },
        )
        if (helperText != null) {
            Spacer(Modifier.height(InputFieldHelperGap))
            DandiText(
                text = helperText,
                color = helperColor,
                style = DesignSystemThemeImpl.typeScale.textRegularS,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private const val InputMaximumLines = 4
private const val EmptyStateDescription = "비어 있음"
private const val FocusedStateDescription = "입력 중"
private const val FilledStateDescription = "입력됨"
private const val ErrorStateDescription = "오류"
private const val DisabledStateDescription = "사용 안 함"
private const val ReadOnlyStateDescription = "읽기 전용"
private const val DefaultErrorDescription = "입력값을 확인해 주세요"
private val SingleLineFieldHeight = 56.dp
private val MultilineFieldHeight = 120.dp
private val InputLabelFieldGap = 6.dp
private val InputFieldHelperGap = 8.dp
private val MultilineVerticalPadding = 14.dp
private val InputNoVerticalPadding = 0.dp
private val InputBorderWidth = 1.dp
private val InputEmphasisBorderWidth = 2.dp
private val InputIconSize = 20.dp

@Preview(showBackground = true, widthDp = 760, heightDp = 560)
@Composable
private fun NyummyTextFieldMatrixPreview() {
    DesignSystemTheme {
        Column(
            modifier = Modifier.padding(DesignSystemThemeImpl.designSystemSpacing.space16),
            verticalArrangement = Arrangement.spacedBy(DesignSystemThemeImpl.designSystemSpacing.space16),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(DesignSystemThemeImpl.designSystemSpacing.space16)) {
                PreviewTextField(value = "", state = InputPreviewState.Empty)
                PreviewTextField(value = "", state = InputPreviewState.Focused)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(DesignSystemThemeImpl.designSystemSpacing.space16)) {
                PreviewTextField(value = "닭가슴살 포케", state = InputPreviewState.Filled)
                PreviewTextField(value = "닭가슴살 포케", state = InputPreviewState.Error)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(DesignSystemThemeImpl.designSystemSpacing.space16)) {
                PreviewTextField(value = "닭가슴살 포케", state = InputPreviewState.Disabled)
                PreviewTextField(value = "닭가슴살 포케", state = InputPreviewState.ReadOnly)
            }
        }
    }
}

private enum class InputPreviewState { Empty, Focused, Filled, Error, Disabled, ReadOnly }

@Composable
private fun PreviewTextField(
    value: String,
    state: InputPreviewState,
) {
    NyummyTextFieldCore(
        value = value,
        onValueChange = {},
        modifier = Modifier.width(360.dp),
        placeholder = "식사 이름을 입력해 주세요",
        label = "음식 이름",
        helperText = if (state == InputPreviewState.Error) {
            "입력 내용을 확인해 주세요"
        } else {
            "사진과 함께 음식 이름을 입력해 주세요"
        },
        isError = state == InputPreviewState.Error,
        enabled = state != InputPreviewState.Disabled,
        readOnly = state == InputPreviewState.ReadOnly,
        singleLine = true,
        leadingIcon = null,
        trailingIcon = null,
        focused = state == InputPreviewState.Focused,
        onFocusChanged = {},
    )
}
