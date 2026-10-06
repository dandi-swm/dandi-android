package com.dandi.nyummy.common.presentation.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import com.dandi.nyummy.common.presentation.R
import com.dandi.nyummy.common.presentation.designsystem.foundation.NyummyComponentDimens
import com.dandi.nyummy.common.presentation.designsystem.foundation.rememberNyummyInteractionSource
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme

/**
 * 한 줄 입력칸. 라벨은 항상 보이고, 높이 52, 2px 테두리.
 *
 * - Focused: focus 테두리
 * - Error([errorMessage] != null): 빨강 테두리 + 아래에 원인과 해결 방법
 * - Disabled: 움푹한 바탕 + 옅은 테두리
 *
 * [trailing]에는 비밀번호 보기 같은 24dp 아이콘 버튼을 넣는다.
 */
@Composable
fun NyummyTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    helperText: String? = null,
    errorMessage: String? = null,
    enabled: Boolean = true,
    @DrawableRes leadingIcon: Int? = null,
    trailing: (@Composable () -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    interactionSource: MutableInteractionSource = rememberNyummyInteractionSource(),
) {
    val focused by interactionSource.collectIsFocusedAsState()
    val isError = errorMessage != null
    val shape = RoundedCornerShape(NyummyTheme.radius.s)
    val borderColor = fieldBorderColor(enabled = enabled, isError = isError, focused = focused)
    val textColor = if (enabled) NyummyTheme.colors.content.primary else NyummyTheme.colors.content.disabled

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(NyummyComponentDimens.FieldLabelGap),
    ) {
        NyummyText(text = label, style = NyummyTheme.typography.labelM, color = NyummyTheme.colors.content.secondary)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = label
                    if (errorMessage != null) error(errorMessage)
                },
            enabled = enabled,
            singleLine = true,
            textStyle = NyummyTheme.typography.bodyL.copy(color = textColor),
            cursorBrush = SolidColor(NyummyTheme.colors.border.focus),
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            visualTransformation = visualTransformation,
            interactionSource = interactionSource,
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(NyummyComponentDimens.TextFieldHeight)
                        .background(
                            if (enabled) NyummyTheme.colors.bg.surface else NyummyTheme.colors.bg.surfaceSunken,
                            shape,
                        )
                        .border(NyummyTheme.borderWidth.bold, borderColor, shape)
                        .padding(horizontal = NyummyTheme.spacing.s16),
                    horizontalArrangement = Arrangement.spacedBy(NyummyComponentDimens.TextFieldIconGap),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (leadingIcon != null) {
                        Icon(
                            painter = painterResource(leadingIcon),
                            contentDescription = null,
                            tint = if (enabled) {
                                NyummyTheme.colors.content.tertiary
                            } else {
                                NyummyTheme.colors.content.disabled
                            },
                            modifier = Modifier.size(NyummyTheme.size.iconM),
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        if (value.isEmpty() && placeholder != null) {
                            NyummyText(
                                text = placeholder,
                                style = NyummyTheme.typography.bodyL,
                                color = if (enabled) {
                                    NyummyTheme.colors.content.tertiary
                                } else {
                                    NyummyTheme.colors.content.disabled
                                },
                                maxLines = 1,
                            )
                        }
                        innerTextField()
                    }
                    trailing?.invoke()
                }
            },
        )
        val supporting = errorMessage ?: helperText
        if (supporting != null) {
            NyummyText(
                text = supporting,
                style = NyummyTheme.typography.bodyS,
                color = when {
                    !enabled -> NyummyTheme.colors.content.disabled
                    isError -> NyummyTheme.colors.content.danger
                    else -> NyummyTheme.colors.content.tertiary
                },
            )
        }
    }
}

@Composable
internal fun fieldBorderColor(enabled: Boolean, isError: Boolean, focused: Boolean): Color {
    val border = NyummyTheme.colors.border
    return when {
        !enabled -> border.subtle
        isError -> border.danger
        focused -> border.focus
        else -> border.default
    }
}

/**
 * 비밀번호 입력칸의 보기/숨기기 아이콘. [NyummyTextField]의 trailing에 넣는다.
 * 아이콘은 지금 상태를 나타낸다. 가려져 있으면 eye-off, 보이고 있으면 eye.
 * 레이아웃은 아이콘 크기(24dp) 그대로이고, 터치 영역은 Compose 최소 터치 영역(48dp)으로 넓어진다.
 */
@Composable
fun NyummyPasswordVisibilityToggle(
    visible: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Icon(
        painter = painterResource(if (visible) R.drawable.nyummy_ic_eye else R.drawable.nyummy_ic_eye_off),
        contentDescription = stringResource(
            if (visible) R.string.nyummy_password_hide_description else R.string.nyummy_password_show_description,
        ),
        tint = NyummyTheme.colors.content.tertiary,
        modifier = modifier
            .size(NyummyTheme.size.iconL)
            .clickable(
                interactionSource = rememberNyummyInteractionSource(),
                indication = null,
                role = Role.Button,
                onClick = onToggle,
            ),
    )
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun NyummyTextFieldPreview() {
    NyummyTheme {
        Column(
            modifier = Modifier.padding(NyummyTheme.spacing.gutter),
            verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s16),
        ) {
            NyummyTextField(value = "", onValueChange = {}, label = "이메일", placeholder = "example@nyummy.com", helperText = "도움말")
            NyummyTextField(value = "nyummy@", onValueChange = {}, label = "이메일", errorMessage = "이메일 형식을 확인해 주세요")
            var visible by remember { mutableStateOf(false) }
            NyummyTextField(
                value = "password",
                onValueChange = {},
                label = "비밀번호",
                visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                trailing = { NyummyPasswordVisibilityToggle(visible = visible, onToggle = { visible = !visible }) },
            )
            NyummyTextField(value = "", onValueChange = {}, label = "이메일", placeholder = "example@nyummy.com", enabled = false)
        }
    }
}
