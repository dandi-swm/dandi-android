package com.dandi.nyummy.common.presentation.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import com.dandi.nyummy.common.presentation.R
import com.dandi.nyummy.common.presentation.designsystem.foundation.NyummyComponentDimens
import com.dandi.nyummy.common.presentation.designsystem.foundation.rememberNyummyInteractionSource
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * 왼쪽으로 밀어 지우는 목록 행. 밀면 오른쪽에 연한 빨강 삭제 버튼(폭 64, 위아래 8 안쪽)이 드러나고 행은 80만큼 멈춘다.
 * 버튼을 누르거나 행 폭의 절반 넘게 끝까지 밀면 [onDelete]를 부른다. 열린 상태에서 행을 누르면 닫힌다.
 * 접근성 서비스에서는 "삭제" 동작으로 지울 수 있다.
 *
 * 행 내용은 바탕색을 칠해 삭제 버튼을 가린다. 지운 뒤 안내(되돌리기 스낵바)는 부르는 쪽이 한다.
 */
@Composable
fun NyummySwipeToDelete(
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val revealPx = with(density) { NyummyComponentDimens.SwipeRevealDistance.toPx() }
    val offset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val widthPx = remember { floatArrayOf(0f) }
    val deleteLabel = stringResource(R.string.nyummy_swipe_delete)

    fun settle(target: Float) {
        scope.launch { offset.animateTo(target) }
    }

    fun delete() {
        scope.launch {
            offset.animateTo(-widthPx[0])
            onDelete()
            offset.snapTo(0f)
        }
    }

    Box(
        modifier = modifier
            .onSizeChanged { widthPx[0] = it.width.toFloat() }
            .semantics {
                if (enabled) {
                    customActions = listOf(CustomAccessibilityAction(deleteLabel) { onDelete(); true })
                }
            },
    ) {
        if (offset.value < 0f) {
            SwipeDeleteAction(
                label = deleteLabel,
                onClick = ::delete,
                modifier = Modifier.matchParentSize(),
            )
        }
        Box(
            modifier = Modifier
                .offset { IntOffset(offset.value.roundToInt(), 0) }
                .background(NyummyTheme.colors.bg.canvas)
                .draggable(
                    orientation = Orientation.Horizontal,
                    enabled = enabled,
                    state = rememberDraggableState { delta ->
                        scope.launch { offset.snapTo((offset.value + delta).coerceIn(-widthPx[0], 0f)) }
                    },
                    onDragStopped = { velocity ->
                        val passedHalf = offset.value < -widthPx[0] / 2
                        val flungOpenRow = velocity < -FLING_DELETE_VELOCITY && offset.value < -revealPx
                        when {
                            passedHalf || flungOpenRow -> delete()
                            offset.value < -revealPx / 2 || velocity < -FLING_OPEN_VELOCITY -> settle(-revealPx)
                            else -> settle(0f)
                        }
                    },
                ),
        ) {
            content()
            // 열려 있을 때 행을 누르면 상세로 가지 않고 닫는다.
            if (offset.value < 0f) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable(
                            interactionSource = rememberNyummyInteractionSource(),
                            indication = null,
                            onClick = { settle(0f) },
                        ),
                )
            }
        }
    }
}

/** 오른쪽 끝에 붙는 삭제 버튼. 행 높이를 채우고 폭은 [NyummyComponentDimens.SwipeActionWidth]. */
@Composable
private fun SwipeDeleteAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.CenterEnd) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(vertical = NyummyComponentDimens.SwipeActionVerticalInset)
                .width(NyummyComponentDimens.SwipeActionWidth)
                .background(NyummyTheme.colors.bg.dangerSubtle, RoundedCornerShape(NyummyTheme.radius.m))
                .clickable(
                    interactionSource = rememberNyummyInteractionSource(),
                    indication = null,
                    role = Role.Button,
                    onClick = onClick,
                ),
            verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s4, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                painter = painterResource(R.drawable.nyummy_ic_trash_2),
                contentDescription = null,
                tint = NyummyTheme.colors.content.danger,
                modifier = Modifier.size(NyummyTheme.size.iconM),
            )
            NyummyText(
                text = label,
                style = NyummyTheme.typography.labelS,
                color = NyummyTheme.colors.content.danger,
            )
        }
    }
}

/** 이 속도(px/s)보다 빠르게 열린 행을 더 밀면 끝까지 밀지 않아도 지운다. */
private const val FLING_DELETE_VELOCITY = 3000f

/** 이 속도(px/s)보다 빠르게 밀면 절반을 넘지 않아도 버튼까지 연다. */
private const val FLING_OPEN_VELOCITY = 800f

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun NyummySwipeToDeletePreview() {
    NyummyTheme {
        NyummySwipeToDelete(onDelete = {}) {
            NyummyMailRow(
                type = "건의",
                time = "9월 28일",
                title = "히스토리에서 사진을 크게 보고 싶어요",
                status = "답장을 기다리는 중",
                isStatusEmphasized = false,
                onClick = {},
                modifier = Modifier.padding(horizontal = NyummyTheme.spacing.gutter),
            )
        }
    }
}
