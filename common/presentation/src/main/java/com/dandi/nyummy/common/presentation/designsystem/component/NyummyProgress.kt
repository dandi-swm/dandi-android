package com.dandi.nyummy.common.presentation.designsystem.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dandi.nyummy.common.presentation.designsystem.foundation.NyummyComponentDimens
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme

/** 막대형 진행률. 두께 10, 양끝 둥근 막대. [progress]는 0~1로 잘린다. */
@Composable
fun NyummyLinearProgress(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    val value = progress.coerceIn(0f, 1f)
    val shape = RoundedCornerShape(NyummyTheme.radius.full)
    Box(
        modifier = modifier
            .height(NyummyComponentDimens.LinearProgressHeight)
            .clip(shape)
            .background(NyummyTheme.colors.data.progressTrack)
            .semantics { progressBarRangeInfo = ProgressBarRangeInfo(value, 0f..1f) },
    ) {
        if (value > 0f) {
            Box(
                Modifier
                    .fillMaxWidth(value)
                    .fillMaxHeight()
                    .background(NyummyTheme.colors.data.progressFill, shape),
            )
        }
    }
}

enum class NyummyCircularProgressSize(val diameter: Dp, val strokeWidth: Dp) {
    S(20.dp, 2.5.dp),
    M(24.dp, 3.dp),
    L(40.dp, 4.dp),
}

/** 원형 로딩 표시. 연한 트랙 위에서 1/4 호가 돈다. 끝을 알 수 없는 기다림에만 쓴다. */
@Composable
fun NyummyCircularProgress(
    modifier: Modifier = Modifier,
    size: NyummyCircularProgressSize = NyummyCircularProgressSize.M,
) {
    val track = NyummyTheme.colors.data.progressTrack
    val indicator = NyummyTheme.colors.data.progressFill
    val rotation by rememberInfiniteTransition(label = "circularProgress").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(RotationMillis, easing = LinearEasing), RepeatMode.Restart),
        label = "circularProgressRotation",
    )
    Canvas(
        modifier = modifier
            .size(size.diameter)
            .semantics { progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate }
            .graphicsLayer { rotationZ = rotation },
    ) {
        val stroke = size.strokeWidth.toPx()
        val inset = stroke / 2
        val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
        drawArc(track, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
        drawArc(
            indicator,
            -90f,
            90f,
            false,
            Offset(inset, inset),
            arcSize,
            style = Stroke(stroke, cap = StrokeCap.Round),
        )
    }
}

private const val RotationMillis = 900

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun NyummyProgressPreview() {
    NyummyTheme {
        Column(
            modifier = Modifier.padding(NyummyTheme.spacing.gutter),
            verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s12),
        ) {
            listOf(0f, 0.45f, 1f).forEach { NyummyLinearProgress(progress = it, modifier = Modifier.fillMaxWidth()) }
            Row(horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s16)) {
                NyummyCircularProgressSize.entries.forEach { NyummyCircularProgress(size = it) }
            }
        }
    }
}
