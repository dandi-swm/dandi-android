package com.dandi.nyummy.common.presentation.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.dandi.nyummy.common.presentation.R
import com.dandi.nyummy.common.presentation.designsystem.foundation.NyummyComponentDimens
import com.dandi.nyummy.common.presentation.designsystem.foundation.NyummyComponentShadows
import com.dandi.nyummy.common.presentation.designsystem.foundation.NyummyPressScale
import com.dandi.nyummy.common.presentation.designsystem.foundation.rememberNyummyInteractionSource
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.common.presentation.designsystem.theme.nyummyShadow

/**
 * 체크박스(22, 모서리 6). 꺼지면 흰 바탕 + 1.5 테두리, 켜지면 초록 채움 + 흰 체크.
 * 누를 수 없으면 비활성 바탕에 비활성 색 테두리(꺼짐)나 체크(켜짐)를 그린다.
 * [onCheckedChange]가 null이면 상위(List Row 등)가 클릭을 맡는 표시 전용이다.
 */
@Composable
fun NyummyCheckbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val bg = NyummyTheme.colors.bg
    val content = NyummyTheme.colors.content
    val (container, border, check) = when {
        checked && enabled -> Triple(bg.actionPrimary, Color.Transparent, content.onAction)
        checked -> Triple(bg.actionDisabled, Color.Transparent, content.disabled)
        enabled -> Triple(bg.surface, NyummyTheme.colors.border.strong, Color.Transparent)
        else -> Triple(bg.actionDisabled, content.disabled, Color.Transparent)
    }
    val shape = RoundedCornerShape(NyummyComponentDimens.CheckboxRadius)
    val toggle = if (onCheckedChange != null) {
        Modifier
            .toggleable(
                value = checked,
                interactionSource = rememberNyummyInteractionSource(),
                indication = null,
                enabled = enabled,
                role = Role.Checkbox,
                onValueChange = onCheckedChange,
            )
    } else {
        Modifier
    }
    Box(
        modifier = modifier
            .then(toggle)
            .size(NyummyComponentDimens.CheckboxSize)
            .background(container, shape)
            .border(NyummyComponentDimens.FieldActiveBorderWidth, border, shape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.nyummy_ic_check_bold),
            contentDescription = null,
            tint = check,
            modifier = Modifier.size(NyummyComponentDimens.CheckboxCheckSize),
        )
    }
}

/**
 * 라디오(24). 미선택은 strong 링, 선택은 에버그린 링 + 가운데 점 12.
 * [onClick]이 null이면 상위(List Row 등)가 클릭을 맡는 표시 전용이다.
 */
@Composable
fun NyummyRadio(
    selected: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val ring = when {
        !enabled -> NyummyTheme.colors.border.subtle
        selected -> NyummyTheme.colors.border.selected
        else -> NyummyTheme.colors.border.strong
    }
    val select = if (onClick != null) {
        Modifier
            .selectable(
                selected = selected,
                interactionSource = rememberNyummyInteractionSource(),
                indication = null,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick,
            )
    } else {
        Modifier
    }
    Box(
        modifier = modifier
            .then(select)
            .size(NyummyComponentDimens.SelectionControlSize)
            .border(NyummyTheme.borderWidth.bold, ring, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (selected && enabled) {
            Box(
                Modifier
                    .size(NyummyComponentDimens.RadioDotSize)
                    .background(NyummyTheme.colors.bg.actionPrimary, CircleShape),
            )
        }
    }
}

/**
 * 스위치(48×28). 켜짐 초록, 꺼짐 회색(disabled), 흰 썸 22에 옅은 그림자.
 * 설정처럼 바로 반영되는 토글에 쓴다. [onCheckedChange]가 null이면 표시 전용이다.
 */
@Composable
fun NyummySwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val dimens = NyummyComponentDimens
    val trackColor by animateColorAsState(
        targetValue = when {
            checked && enabled -> NyummyTheme.colors.bg.actionPrimary
            checked -> NyummyTheme.colors.bg.selected
            enabled -> NyummyTheme.colors.content.disabled
            else -> NyummyTheme.colors.bg.actionDisabled
        },
        animationSpec = tween(NyummyPressScale.DurationMillis),
        label = "switchTrack",
    )
    val thumbTravel = dimens.SwitchWidth - dimens.SwitchThumbSize - dimens.SwitchThumbInset * 2
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) thumbTravel else 0.dp,
        animationSpec = tween(NyummyPressScale.DurationMillis),
        label = "switchThumb",
    )
    val toggle = if (onCheckedChange != null) {
        Modifier
            .toggleable(
                value = checked,
                interactionSource = rememberNyummyInteractionSource(),
                indication = null,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            )
    } else {
        Modifier
    }
    Box(
        modifier = modifier
            .then(toggle)
            .size(DpSize(dimens.SwitchWidth, dimens.SwitchHeight))
            .background(trackColor, RoundedCornerShape(NyummyTheme.radius.full))
            .padding(dimens.SwitchThumbInset),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .offset(x = thumbOffset)
                .size(dimens.SwitchThumbSize)
                .nyummyShadow(CircleShape, NyummyComponentShadows.SwitchThumb)
                .background(NyummyTheme.colors.bg.surface, CircleShape),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NyummySelectionControlsPreview() {
    NyummyTheme {
        var checked by remember { mutableStateOf(true) }
        Row(
            modifier = Modifier.padding(NyummyTheme.spacing.s16),
            horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s8),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NyummyCheckbox(checked = checked, onCheckedChange = { checked = it })
            NyummyCheckbox(checked = false, onCheckedChange = {}, enabled = false)
            NyummyRadio(selected = checked, onClick = { checked = !checked })
            NyummySwitch(checked = checked, onCheckedChange = { checked = it })
            NyummySwitch(checked = true, onCheckedChange = {}, enabled = false)
        }
    }
}
