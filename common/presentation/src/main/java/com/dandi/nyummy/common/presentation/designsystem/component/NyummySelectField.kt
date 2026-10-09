package com.dandi.nyummy.common.presentation.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import com.dandi.nyummy.common.presentation.R
import com.dandi.nyummy.common.presentation.designsystem.foundation.NyummyComponentDimens
import com.dandi.nyummy.common.presentation.designsystem.foundation.rememberNyummyInteractionSource
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme

/**
 * 고르기 칸. [NyummyTextField]와 같은 라벨, 높이 52, 2px 테두리에 오른쪽 아래 화살표를 둔다.
 * 직접 입력하지 않고, 누르면 [onClick]으로 바텀 시트 같은 선택 목록을 연다.
 * 고른 값과 화살표는 입력칸의 자리표시 문구와 같은 tertiary 색으로 쓴다.
 */
@Composable
fun NyummySelectField(
    value: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(NyummyTheme.radius.s)
    val contentColor = if (enabled) NyummyTheme.colors.content.tertiary else NyummyTheme.colors.content.disabled

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(NyummyComponentDimens.FieldLabelGap),
    ) {
        NyummyText(text = label, style = NyummyTheme.typography.labelM, color = NyummyTheme.colors.content.secondary)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(NyummyComponentDimens.TextFieldHeight)
                .background(
                    if (enabled) NyummyTheme.colors.bg.surface else NyummyTheme.colors.bg.surfaceSunken,
                    shape,
                )
                .border(
                    NyummyTheme.borderWidth.bold,
                    fieldBorderColor(enabled = enabled, isError = false, focused = false),
                    shape,
                )
                .clickable(
                    interactionSource = rememberNyummyInteractionSource(),
                    indication = null,
                    enabled = enabled,
                    role = Role.DropdownList,
                    onClick = onClick,
                )
                .semantics {
                    contentDescription = label
                    stateDescription = value
                }
                .padding(horizontal = NyummyTheme.spacing.s16),
            horizontalArrangement = Arrangement.spacedBy(NyummyComponentDimens.TextFieldIconGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NyummyText(
                text = value,
                style = NyummyTheme.typography.bodyL,
                color = contentColor,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
            Icon(
                painter = painterResource(R.drawable.nyummy_ic_chevron_down),
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(NyummyTheme.size.iconL),
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun NyummySelectFieldPreview() {
    NyummyTheme {
        Column(
            modifier = Modifier.padding(NyummyTheme.spacing.gutter),
            verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s16),
        ) {
            NyummySelectField(value = "단순 문의", label = "문의 유형", onClick = {})
            NyummySelectField(value = "단순 문의", label = "문의 유형", onClick = {}, enabled = false)
        }
    }
}
