package com.dandi.nyummy.common.presentation.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import com.dandi.nyummy.common.presentation.R
import com.dandi.nyummy.common.presentation.designsystem.foundation.NyummyComponentDimens
import com.dandi.nyummy.common.presentation.designsystem.foundation.rememberNyummyInteractionSource
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme

/** List Row 오른쪽 요소. 행 전체가 터치 영역이고, 오른쪽 컨트롤은 표시만 한다. */
@Immutable
sealed interface NyummyListRowTrailing {
    /** 다른 화면으로 이동. */
    data object Chevron : NyummyListRowTrailing

    /** 오른쪽 요소 없음. */
    data object None : NyummyListRowTrailing

    /** 현재 값 + 이동. */
    data class Value(val text: String) : NyummyListRowTrailing

    /** 즉시 반영 토글. 행을 누르면 [onCheckedChange]가 호출된다. */
    data class Switch(val checked: Boolean, val onCheckedChange: (Boolean) -> Unit) : NyummyListRowTrailing

    /** 여러 개 선택. 행을 누르면 [onCheckedChange]가 호출된다. */
    data class Checkbox(val checked: Boolean, val onCheckedChange: (Boolean) -> Unit) : NyummyListRowTrailing

    /** 하나만 선택. 행을 누르면 [onClick]이 호출된다. */
    data class Radio(val selected: Boolean, val onClick: () -> Unit) : NyummyListRowTrailing
}

/**
 * 설정, 공지, 우편 목록의 한 행. 최소 높이 56, 위아래 12, 행 전체가 터치 영역.
 * 앞 아이콘은 움푹한 40dp 칸 안의 20dp Lucide 아이콘이다.
 *
 * Chevron, Value, None은 [onClick]으로, Switch, Checkbox, Radio는 각 trailing의 콜백으로 반응한다.
 */
@Composable
fun NyummyListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    @DrawableRes leadingIcon: Int? = null,
    trailing: NyummyListRowTrailing = NyummyListRowTrailing.Chevron,
    onClick: (() -> Unit)? = null,
) {
    val interactionSource = rememberNyummyInteractionSource()
    val interaction = when (trailing) {
        is NyummyListRowTrailing.Switch -> Modifier.toggleable(
            value = trailing.checked,
            interactionSource = interactionSource,
            indication = null,
            role = Role.Switch,
            onValueChange = trailing.onCheckedChange,
        )
        is NyummyListRowTrailing.Checkbox -> Modifier.toggleable(
            value = trailing.checked,
            interactionSource = interactionSource,
            indication = null,
            role = Role.Checkbox,
            onValueChange = trailing.onCheckedChange,
        )
        is NyummyListRowTrailing.Radio -> Modifier.selectable(
            selected = trailing.selected,
            interactionSource = interactionSource,
            indication = null,
            role = Role.RadioButton,
            onClick = trailing.onClick,
        )
        else -> if (onClick != null) {
            Modifier.clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
        } else {
            Modifier
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = NyummyComponentDimens.ListRowMinHeight)
            .then(interaction)
            .padding(vertical = NyummyTheme.spacing.s12),
        horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingIcon != null) {
            Box(
                modifier = Modifier
                    .size(NyummyComponentDimens.ListRowLeadingSize)
                    .background(NyummyTheme.colors.bg.surfaceSunken, RoundedCornerShape(NyummyTheme.radius.s)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(leadingIcon),
                    contentDescription = null,
                    tint = NyummyTheme.colors.content.secondary,
                    modifier = Modifier.size(NyummyTheme.size.iconM),
                )
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s2),
        ) {
            NyummyText(text = title, style = NyummyTheme.typography.titleS, maxLines = 1)
            if (subtitle != null) {
                NyummyText(
                    text = subtitle,
                    style = NyummyTheme.typography.bodyS,
                    color = NyummyTheme.colors.content.tertiary,
                    maxLines = 1,
                )
            }
        }
        ListRowTrailing(trailing)
    }
}

@Composable
private fun ListRowTrailing(trailing: NyummyListRowTrailing) {
    when (trailing) {
        NyummyListRowTrailing.None -> Unit
        NyummyListRowTrailing.Chevron -> Icon(
            painter = painterResource(R.drawable.nyummy_ic_chevron_right),
            contentDescription = null,
            tint = NyummyTheme.colors.content.tertiary,
            modifier = Modifier.size(NyummyTheme.size.iconL),
        )
        is NyummyListRowTrailing.Value -> Row(
            horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NyummyText(
                text = trailing.text,
                style = NyummyTheme.typography.bodyM,
                color = NyummyTheme.colors.content.tertiary,
                maxLines = 1,
            )
            Icon(
                painter = painterResource(R.drawable.nyummy_ic_chevron_right),
                contentDescription = null,
                tint = NyummyTheme.colors.content.tertiary,
                modifier = Modifier.size(NyummyTheme.size.iconM),
            )
        }
        is NyummyListRowTrailing.Switch -> NyummySwitch(checked = trailing.checked, onCheckedChange = null)
        is NyummyListRowTrailing.Checkbox -> NyummyCheckbox(checked = trailing.checked, onCheckedChange = null)
        is NyummyListRowTrailing.Radio -> NyummyRadio(selected = trailing.selected, onClick = null)
    }
}

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun NyummyListRowPreview() {
    NyummyTheme {
        Column(modifier = Modifier.padding(horizontal = NyummyTheme.spacing.gutter)) {
            NyummyListRow(title = "알림", subtitle = "끼니 시간에 알려 드려요", leadingIcon = R.drawable.nyummy_ic_bell, onClick = {})
            NyummyListRow(
                title = "알림",
                subtitle = "끼니 시간에 알려 드려요",
                leadingIcon = R.drawable.nyummy_ic_bell,
                trailing = NyummyListRowTrailing.Switch(checked = true, onCheckedChange = {}),
            )
            NyummyListRow(title = "앱 버전", trailing = NyummyListRowTrailing.Value("v1.2.0"), onClick = {})
        }
    }
}
