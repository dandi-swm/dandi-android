package com.dandi.nyummy.common.presentation.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.dandi.nyummy.common.presentation.designsystem.foundation.NyummyComponentDimens
import com.dandi.nyummy.common.presentation.designsystem.foundation.rememberNyummyInteractionSource
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import java.text.NumberFormat
import java.util.Locale

/**
 * 여러 줄 입력칸. 높이 180 고정이고 내용이 많으면 안에서 스크롤된다.
 * 오른쪽 아래에 글자 수(예: 0/1,000)를 보여 주고, [maxLength]를 넘는 입력은 잘라낸다.
 *
 * 기본 테두리는 1px이고, Focused와 Error에서만 2px로 굵어진다.
 */
@Composable
fun NyummyTextArea(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    errorMessage: String? = null,
    enabled: Boolean = true,
    maxLength: Int = DefaultMaxLength,
) {
    val theme = NyummyTheme
    val interactionSource = rememberNyummyInteractionSource()
    val focused by interactionSource.collectIsFocusedAsState()
    val isError = errorMessage != null
    val shape = RoundedCornerShape(theme.radius.s)
    val emphasized = enabled && (isError || focused)
    val borderWidth = if (emphasized) theme.borderWidth.bold else theme.borderWidth.hairline
    val borderColor = when {
        !enabled -> theme.colors.border.default
        isError -> theme.colors.border.danger
        focused -> theme.colors.border.focus
        else -> theme.colors.border.default
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(NyummyComponentDimens.FieldLabelGap),
    ) {
        NyummyText(text = label, style = theme.typography.labelM, color = theme.colors.content.secondary)
        BasicTextField(
            value = value,
            onValueChange = { onValueChange(it.take(maxLength)) },
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = label
                    if (errorMessage != null && enabled) error(errorMessage)
                },
            enabled = enabled,
            textStyle = theme.typography.bodyM.copy(color = theme.colors.content.primary),
            cursorBrush = SolidColor(theme.colors.border.focus),
            interactionSource = interactionSource,
            decorationBox = { innerTextField ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(NyummyComponentDimens.TextAreaHeight)
                        .background(if (enabled) theme.colors.bg.surface else theme.colors.bg.actionDisabled, shape)
                        .border(borderWidth, borderColor, shape)
                        .padding(
                            start = theme.spacing.s16,
                            end = theme.spacing.s16,
                            top = NyummyComponentDimens.TextAreaTopPadding,
                            bottom = theme.spacing.s12,
                        ),
                    verticalArrangement = Arrangement.spacedBy(theme.spacing.s8),
                ) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        if (value.isEmpty() && placeholder != null) {
                            NyummyText(text = placeholder, style = theme.typography.bodyM, color = theme.colors.content.tertiary)
                        }
                        innerTextField()
                    }
                    NyummyText(
                        text = "${value.length.formatted()}/${maxLength.formatted()}",
                        style = theme.typography.labelS,
                        color = theme.colors.content.tertiary,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
        )
        if (errorMessage != null && enabled) {
            NyummyText(text = errorMessage, style = theme.typography.bodyS, color = theme.colors.content.danger)
        }
    }
}

private const val DefaultMaxLength = 1_000

private fun Int.formatted(): String = NumberFormat.getIntegerInstance(Locale.KOREA).format(this)

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun NyummyTextAreaPreview() {
    NyummyTheme {
        Column(
            modifier = Modifier.padding(NyummyTheme.spacing.gutter),
            verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s16),
        ) {
            NyummyTextArea(value = "", onValueChange = {}, label = "문의 내용", placeholder = "어떤 점이 궁금하거나 불편했는지 알려 주세요")
            NyummyTextArea(value = "", onValueChange = {}, label = "문의 내용", errorMessage = "내용을 입력해 주세요")
        }
    }
}
