package com.dandi.nyummy.common.presentation.designsystem.component

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.dandi.nyummy.common.presentation.R
import com.dandi.nyummy.common.presentation.designsystem.foundation.NyummyComponentDimens
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import kotlin.math.roundToInt

/**
 * 별점 입력. 별 5개로 0.5점 단위를 고른다. 값은 반 칸 수([halfSteps], 0 = 고르지 않음, 1~10 = 0.5~5.0점)다.
 *
 * 별의 왼쪽 절반을 누르면 반 칸, 오른쪽 절반을 누르면 한 칸까지 채우고, 가로로 끌어서도 고른다. 별 사이 틈은 왼쪽 별로 본다.
 * 빈 별은 content/disabled, 채운 부분은 data/coin(금색 채움)이다. 화면 읽기 프로그램에서는 조절 막대처럼 값을 올리고 내린다.
 */
@Composable
fun NyummyStarRating(
    halfSteps: Int,
    onHalfStepsChange: (Int) -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    starSize: Dp = NyummyComponentDimens.StarRatingStarSize,
) {
    val currentOnChange by rememberUpdatedState(onHalfStepsChange)
    val gap = NyummyComponentDimens.StarRatingGap
    val stateText = stringResource(R.string.nyummy_star_rating_state, scoreLabelOf(halfSteps))

    Row(
        modifier = modifier
            .semantics(mergeDescendants = true) {
                this.contentDescription = contentDescription
                stateDescription = stateText
                progressBarRangeInfo = ProgressBarRangeInfo(halfSteps / 2f, 0f..MAX_SCORE, steps = MAX_HALF_STEPS - 1)
                if (enabled) {
                    setProgress { target ->
                        currentOnChange((target * 2).roundToInt().coerceIn(1, MAX_HALF_STEPS))
                        true
                    }
                } else {
                    disabled()
                }
            }
            .pointerInput(enabled, starSize, gap) {
                if (!enabled) return@pointerInput
                detectTapGestures { offset ->
                    currentOnChange(halfStepsAt(offset.x, starSize.toPx(), gap.toPx()))
                }
            }
            .pointerInput(enabled, starSize, gap) {
                if (!enabled) return@pointerInput
                detectHorizontalDragGestures(
                    onDragStart = { offset -> currentOnChange(halfStepsAt(offset.x, starSize.toPx(), gap.toPx())) },
                ) { change, _ ->
                    currentOnChange(halfStepsAt(change.position.x, starSize.toPx(), gap.toPx()))
                }
            },
        horizontalArrangement = Arrangement.spacedBy(gap),
    ) {
        repeat(STAR_COUNT) { index ->
            Star(fill = (halfSteps - index * 2).coerceIn(0, 2) / 2f, size = starSize)
        }
    }
}

/** 별 하나. [fill]만큼(0, 0.5, 1) 왼쪽부터 금색으로 채운다. 빈 별은 흰 바탕에서도 보이게 content/disabled로 그린다. */
@Composable
private fun Star(fill: Float, size: Dp) {
    val painter = painterResource(R.drawable.nyummy_ic_star_filled)
    Box(Modifier.size(size)) {
        Icon(
            painter = painter,
            contentDescription = null,
            tint = NyummyTheme.colors.content.disabled,
            modifier = Modifier.size(size),
        )
        if (fill > 0f) {
            Icon(
                painter = painter,
                contentDescription = null,
                tint = NyummyTheme.colors.data.coin,
                modifier = Modifier
                    .size(size)
                    .drawWithContent {
                        clipRect(right = this.size.width * fill) { this@drawWithContent.drawContent() }
                    },
            )
        }
    }
}

/** 가로 위치 [x]가 가리키는 반 칸 수. 별의 왼쪽 절반이면 반 칸, 오른쪽 절반이나 뒤 틈이면 한 칸이다. */
private fun halfStepsAt(x: Float, starPx: Float, gapPx: Float): Int {
    val cell = starPx + gapPx
    val index = (x / cell).toInt().coerceIn(0, STAR_COUNT - 1)
    val inStar = x - index * cell
    val half = if (inStar < starPx / 2f) 1 else 2
    return (index * 2 + half).coerceIn(1, MAX_HALF_STEPS)
}

/** "3.5", "4" 처럼 점수를 쓴다. */
private fun scoreLabelOf(halfSteps: Int): String =
    if (halfSteps % 2 == 0) "${halfSteps / 2}" else "${halfSteps / 2}.5"

private const val STAR_COUNT = 5
private const val MAX_HALF_STEPS = STAR_COUNT * 2
private const val MAX_SCORE = STAR_COUNT.toFloat()

@Preview(showBackground = true)
@Composable
private fun NyummyStarRatingPreview() {
    NyummyTheme {
        Column(verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s12)) {
            var halfSteps by remember { mutableIntStateOf(7) }
            NyummyStarRating(halfSteps = halfSteps, onHalfStepsChange = { halfSteps = it }, contentDescription = "별점")
            NyummyStarRating(halfSteps = 0, onHalfStepsChange = {}, contentDescription = "별점")
            NyummyStarRating(halfSteps = 10, onHalfStepsChange = {}, contentDescription = "별점", enabled = false)
        }
    }
}
