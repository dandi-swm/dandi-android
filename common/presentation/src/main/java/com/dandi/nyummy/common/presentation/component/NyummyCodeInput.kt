package com.dandi.nyummy.common.presentation.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemTheme
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemThemeImpl

/**
 * 자리별 박스형 인증 코드 입력 (Figma `회원가입`의 code-input 패턴).
 *
 * 실제 입력은 투명한 [BasicTextField] 하나가 받고, 자리 박스는 장식으로만 그린다.
 * 숫자만 허용하며 [length]를 넘는 입력은 잘라낸다.
 */
@Composable
fun NyummyCodeInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    length: Int = DefaultCodeLength,
    isError: Boolean = false,
    enabled: Boolean = true,
    inputDescription: String = DefaultInputDescription,
) {
    val colors = DesignSystemThemeImpl.designSystemColor
    var focused by remember { mutableStateOf(false) }
    BasicTextField(
        value = value,
        onValueChange = { input -> onValueChange(input.filter(Char::isDigit).take(length)) },
        modifier = modifier
            .onFocusChanged { focused = it.isFocused }
            .semantics { contentDescription = inputDescription },
        enabled = enabled,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        cursorBrush = SolidColor(colors.contentSelectionPrimary),
        decorationBox = { innerTextField ->
            Box {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .alpha(HiddenFieldAlpha),
                ) {
                    innerTextField()
                }
                Row(horizontalArrangement = Arrangement.spacedBy(CodeCellSpacing)) {
                    repeat(length) { index ->
                        CodeCell(
                            digit = value.getOrNull(index)?.toString().orEmpty(),
                            active = enabled && focused && index == value.length.coerceAtMost(length - 1),
                            isError = isError,
                            enabled = enabled,
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun CodeCell(
    digit: String,
    active: Boolean,
    isError: Boolean,
    enabled: Boolean,
) {
    val colors = DesignSystemThemeImpl.designSystemColor
    val shape = RoundedCornerShape(DesignSystemThemeImpl.designSystemRadius.radius12)
    val borderColor = when {
        isError -> colors.borderInputError
        active -> colors.borderBrandDefault
        else -> colors.borderDefaultLevel0
    }
    val borderWidth = if (isError || active) CodeCellEmphasisBorderWidth else CodeCellBorderWidth
    Box(
        modifier = Modifier
            .width(CodeCellWidth)
            .height(CodeCellHeight)
            .background(if (enabled) colors.bgDefaultLevel1 else colors.bgInputDisabled, shape)
            .border(BorderStroke(borderWidth, borderColor), shape),
        contentAlignment = Alignment.Center,
    ) {
        if (digit.isNotEmpty()) {
            DandiText(
                text = digit,
                color = if (enabled) colors.contentDefaultLevel0 else colors.contentInputDisabled,
                style = DesignSystemThemeImpl.typeScale.numberStrongL,
            )
        }
    }
}

private const val DefaultCodeLength = 6
private const val DefaultInputDescription = "인증 코드 입력"
private const val HiddenFieldAlpha = 0f
private val CodeCellSpacing = 10.dp
private val CodeCellWidth = 50.dp
private val CodeCellHeight = 60.dp
private val CodeCellBorderWidth = 1.5.dp
private val CodeCellEmphasisBorderWidth = 2.dp

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun NyummyCodeInputPreview() {
    DesignSystemTheme {
        Column(
            modifier = Modifier.padding(DesignSystemThemeImpl.designSystemLayout.mobileGutter),
            verticalArrangement = Arrangement.spacedBy(DesignSystemThemeImpl.designSystemSpacing.space16),
        ) {
            NyummyCodeInput(value = "427", onValueChange = {})
            NyummyCodeInput(value = "427", onValueChange = {}, isError = true)
        }
    }
}
