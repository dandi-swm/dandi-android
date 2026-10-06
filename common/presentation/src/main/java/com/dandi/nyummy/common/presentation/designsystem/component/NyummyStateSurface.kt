package com.dandi.nyummy.common.presentation.designsystem.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.dandi.nyummy.common.presentation.designsystem.foundation.NyummyComponentDimens
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme

/** State Surface 버튼. 오류는 주 버튼(다시 시도), 권한은 보조 버튼(설정으로 이동). */
@Immutable
data class NyummyStateAction(val text: String, val onClick: () -> Unit)

/**
 * 빈 상태, 오류, 권한, 로딩 화면. 냐미 말풍선 + 포즈 + 안내 문구 + 선택 버튼으로 구성한다.
 * 정보는 냐미 목소리(말풍선)로 먼저 전하고, 아래 문구에서 구체적인 원인과 방법을 알려 준다.
 * 말풍선 문구는 죄책감을 주지 않게 쓴다.
 */
object NyummyStateSurface {

    /** 빈 상태(Sleep 160). 과거 날짜처럼 할 일이 없으면 [action]을 두지 않는다. */
    @Composable
    fun Empty(
        voice: String,
        message: String,
        modifier: Modifier = Modifier,
        action: NyummyStateAction? = null,
    ) {
        StateSurfaceLayout(
            voice = voice,
            message = message,
            pose = NyummyPose.Sleep,
            poseLarge = true,
            modifier = modifier,
        ) {
            if (action != null) {
                NyummyButton(text = action.text, onClick = action.onClick, modifier = Modifier.fillMaxWidth())
            }
        }
    }

    /** 오류(Worry 120) + 다시 시도 주 버튼. */
    @Composable
    fun Error(
        voice: String,
        message: String,
        retry: NyummyStateAction,
        modifier: Modifier = Modifier,
    ) {
        StateSurfaceLayout(voice = voice, message = message, pose = NyummyPose.Worry, poseLarge = false, modifier = modifier) {
            NyummyButton(text = retry.text, onClick = retry.onClick, modifier = Modifier.fillMaxWidth())
        }
    }

    /** 권한 요청(Ask 120) + 설정 이동 보조 버튼. */
    @Composable
    fun Permission(
        voice: String,
        message: String,
        openSettings: NyummyStateAction,
        modifier: Modifier = Modifier,
    ) {
        StateSurfaceLayout(voice = voice, message = message, pose = NyummyPose.Ask, poseLarge = false, modifier = modifier) {
            NyummyButton(
                text = openSettings.text,
                onClick = openSettings.onClick,
                style = NyummyButtonStyle.Secondary,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    /** 분석 대기(Taste 120) + 끝을 알 수 없는 진행 막대. */
    @Composable
    fun Loading(
        voice: String,
        message: String,
        modifier: Modifier = Modifier,
    ) {
        StateSurfaceLayout(voice = voice, message = message, pose = NyummyPose.Taste, poseLarge = false, modifier = modifier) {
            IndeterminateBar()
        }
    }
}

@Composable
private fun StateSurfaceLayout(
    voice: String,
    message: String,
    pose: NyummyPose,
    poseLarge: Boolean,
    modifier: Modifier,
    bottom: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .widthIn(max = NyummyComponentDimens.ContentMaxWidth)
            .fillMaxWidth()
            .padding(NyummyTheme.spacing.s24),
        verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s12),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        NyummyVoiceBubble(text = voice, tail = NyummyBubbleTail.Bottom)
        NyummyPoseImage(pose = pose, size = if (poseLarge) NyummyTheme.size.characterL else NyummyTheme.size.characterM)
        NyummyText(
            text = message,
            style = NyummyTheme.typography.bodyM,
            color = NyummyTheme.colors.content.secondary,
            textAlign = TextAlign.Center,
        )
        bottom()
    }
}

/** 트랙 위에서 짧은 막대가 좌우로 오가는 대기 표시. */
@Composable
private fun IndeterminateBar() {
    val dimens = NyummyComponentDimens
    val shape = RoundedCornerShape(NyummyTheme.radius.full)
    val position by rememberInfiniteTransition(label = "stateLoading").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(LoadingSweepMillis, easing = LinearEasing), RepeatMode.Reverse),
        label = "stateLoadingPosition",
    )
    Box(
        modifier = Modifier
            .size(dimens.StateLoadingBarWidth, dimens.StateLoadingBarHeight)
            .clip(shape)
            .background(NyummyTheme.colors.data.progressTrack)
            .semantics { progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate },
    ) {
        Box(
            Modifier
                .fillMaxWidth(LoadingSegmentFraction)
                .fillMaxHeight()
                .graphicsLayer {
                    translationX = (dimens.StateLoadingBarWidth.toPx() * (1 - LoadingSegmentFraction)) * position
                }
                .background(NyummyTheme.colors.data.progressFill, shape),
        )
    }
}

private const val LoadingSegmentFraction = 0.35f
private const val LoadingSweepMillis = 900

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun NyummyStateSurfacePreview() {
    NyummyTheme {
        Column {
            NyummyStateSurface.Empty(voice = "이날은 쉬어 갔어", message = "기록이 없는 날이에요")
            NyummyStateSurface.Error(
                voice = "앗, 전송이 안 됐어",
                message = "네트워크를 확인하고 다시 시도해 주세요",
                retry = NyummyStateAction("다시 시도") {},
            )
        }
    }
}
