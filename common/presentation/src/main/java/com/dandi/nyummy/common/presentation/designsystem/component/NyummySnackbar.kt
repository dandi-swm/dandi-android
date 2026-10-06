package com.dandi.nyummy.common.presentation.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.dandi.nyummy.common.presentation.R
import com.dandi.nyummy.common.presentation.designsystem.foundation.NyummyComponentDimens
import com.dandi.nyummy.common.presentation.designsystem.foundation.nyummyPressable
import com.dandi.nyummy.common.presentation.designsystem.foundation.rememberNyummyInteractionSource
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.common.presentation.designsystem.theme.nyummyShadow

/**
 * 시스템 알림 스낵바. 흰 카드 + 2px 테두리 + float 그림자, 최소 높이 56(긴 문구와 큰 글꼴에서는 늘어난다).
 * 되돌릴 수 있는 행동 뒤에는 [actionLabel]로 "실행 취소"를 준다. 냐미 목소리는 [NyummyVoiceToast]를 쓴다.
 * [onDismiss]가 있으면 오른쪽에 닫기(X) 버튼을 둔다. 사라지지 않는 스낵바에서 사용자가 닫을 수 있게 한다.
 */
@Composable
fun NyummySnackbar(
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
    onDismiss: (() -> Unit)? = null,
) {
    val theme = NyummyTheme
    val dimens = NyummyComponentDimens
    val shape = RoundedCornerShape(theme.radius.m)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = dimens.SnackbarHeight)
            .nyummyShadow(shape, theme.elevation.float)
            .background(theme.colors.bg.surface, shape)
            .border(theme.borderWidth.bold, theme.colors.border.default, shape)
            .semantics { liveRegion = LiveRegionMode.Polite }
            .padding(start = theme.spacing.s20, end = theme.spacing.s12, top = theme.spacing.s8, bottom = theme.spacing.s8),
        horizontalArrangement = Arrangement.spacedBy(theme.spacing.s12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NyummyText(text = message, style = theme.typography.bodyM, modifier = Modifier.weight(1f))
        if (actionLabel != null) {
            Box(
                modifier = Modifier
                    .nyummyPressable(
                        interactionSource = rememberNyummyInteractionSource(),
                        enabled = true,
                        role = Role.Button,
                        onClick = onAction,
                    )
                    .defaultMinSize(minWidth = dimens.SnackbarActionMinWidth)
                    .height(dimens.SnackbarActionHeight)
                    .padding(horizontal = theme.spacing.s12),
                contentAlignment = Alignment.Center,
            ) {
                NyummyText(text = actionLabel, style = theme.typography.labelM, color = theme.colors.content.brand, maxLines = 1)
            }
        }
        if (onDismiss != null) {
            Icon(
                painter = painterResource(R.drawable.nyummy_ic_x),
                contentDescription = "알림 닫기",
                tint = theme.colors.content.tertiary,
                modifier = Modifier
                    .size(theme.size.iconM)
                    .clickable(
                        interactionSource = rememberNyummyInteractionSource(),
                        indication = null,
                        role = Role.Button,
                        onClick = onDismiss,
                    ),
            )
        }
    }
}

/**
 * [SnackbarHostState]로 띄우는 스낵바를 [NyummySnackbar] 모양으로 그린다.
 * 화면 하단에 두고 좌우 gutter, 최대 너비 480을 지킨다.
 */
@Composable
fun NyummySnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    SnackbarHost(hostState = hostState, modifier = modifier) { data: SnackbarData ->
        NyummySnackbar(
            message = data.visuals.message,
            actionLabel = data.visuals.actionLabel,
            onAction = { data.performAction() },
            onDismiss = if (data.visuals.withDismissAction) ({ data.dismiss() }) else null,
            modifier = Modifier
                .padding(horizontal = NyummyTheme.spacing.gutter, vertical = NyummyTheme.spacing.s12)
                .widthIn(max = NyummyComponentDimens.ContentMaxWidth),
        )
    }
}

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun NyummySnackbarPreview() {
    NyummyTheme {
        Box(Modifier.padding(NyummyTheme.spacing.gutter)) {
            NyummySnackbar(message = "식사 기록을 삭제했어요", actionLabel = "실행 취소")
        }
    }
}
