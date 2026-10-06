package com.dandi.nyummy.common.presentation.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import com.dandi.nyummy.common.presentation.designsystem.foundation.NyummyComponentDimens
import com.dandi.nyummy.common.presentation.designsystem.foundation.rememberNyummyInteractionSource
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme

/**
 * 자리별 인증 코드 입력. 셀은 48×56, 2px 테두리, 숫자는 number/m.
 *
 * 실제 입력은 투명한 [BasicTextField] 하나가 받고 셀은 장식으로만 그린다.
 * 숫자만 받고 [length]를 넘는 입력은 잘라낸다.
 * 셀 테두리: 빈칸 default, 입력할 칸 focus, 채운 칸 strong, 오류 시 전부 danger.
 */
@Composable
fun NyummyCodeInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    length: Int = DefaultCodeLength,
    isError: Boolean = false,
    enabled: Boolean = true,
    contentDescription: String = DefaultContentDescription,
) {
    val theme = NyummyTheme
    val interactionSource = rememberNyummyInteractionSource()
    val focused by interactionSource.collectIsFocusedAsState()

    BasicTextField(
        value = value,
        onValueChange = { input -> onValueChange(input.filter(Char::isDigit).take(length)) },
        modifier = modifier.semantics { this.contentDescription = contentDescription },
        enabled = enabled,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        cursorBrush = SolidColor(theme.colors.border.focus),
        interactionSource = interactionSource,
        decorationBox = { innerTextField ->
            Box {
                Box(modifier = Modifier.matchParentSize().alpha(0f)) { innerTextField() }
                Row(horizontalArrangement = Arrangement.spacedBy(theme.spacing.s8)) {
                    repeat(length) { index ->
                        CodeCell(
                            digit = value.getOrNull(index),
                            active = enabled && focused && index == value.length.coerceAtMost(length - 1),
                            isError = isError,
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun CodeCell(digit: Char?, active: Boolean, isError: Boolean) {
    val theme = NyummyTheme
    val shape = RoundedCornerShape(theme.radius.s)
    val borderColor = when {
        isError -> theme.colors.border.danger
        active -> theme.colors.border.focus
        digit != null -> theme.colors.border.strong
        else -> theme.colors.border.default
    }
    Box(
        modifier = Modifier
            .size(DpSize(NyummyComponentDimens.CodeCellWidth, NyummyComponentDimens.CodeCellHeight))
            .background(theme.colors.bg.surface, shape)
            .border(theme.borderWidth.bold, borderColor, shape),
        contentAlignment = Alignment.Center,
    ) {
        if (digit != null) {
            NyummyText(text = digit.toString(), style = theme.typography.numberM)
        }
    }
}

private const val DefaultCodeLength = 6
private const val DefaultContentDescription = "인증 코드 입력"

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun NyummyCodeInputPreview() {
    NyummyTheme {
        Column(
            modifier = Modifier.padding(NyummyTheme.spacing.gutter),
            verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s16),
        ) {
            NyummyCodeInput(value = "427", onValueChange = {})
            NyummyCodeInput(value = "427915", onValueChange = {}, isError = true)
        }
    }
}
