package com.dandi.nyummy.meal.presentation.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemTheme
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemThemeImpl

/**
 * 카메라 프리뷰/촬영본 위에 얹는 장식 오버레이입니다.
 *
 * 네 모서리 뷰파인더 브래킷만 그리는 순수 장식 레이어라 상태를 갖지 않고
 * 터치도 가로채지 않습니다.
 */
@Composable
fun MealCameraOverlay(
    modifier: Modifier = Modifier,
) {
    val colors = DesignSystemThemeImpl.designSystemColor
    Box(modifier = modifier.fillMaxSize()) {
        CornerBrackets(
            color = colors.contentInverseDefault,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

/** 네 모서리에 L 자 뷰파인더 브래킷을 한 번의 드로우로 그린다. */
@Composable
private fun CornerBrackets(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val inset = BracketInset.toPx()
        val arm = BracketArmLength.toPx()
        val stroke = Stroke(width = BracketStrokeWidth.toPx(), cap = StrokeCap.Round)
        val right = size.width - inset
        val bottom = size.height - inset

        val path = Path().apply {
            // 좌상
            moveTo(inset, inset + arm)
            lineTo(inset, inset)
            lineTo(inset + arm, inset)
            // 우상
            moveTo(right - arm, inset)
            lineTo(right, inset)
            lineTo(right, inset + arm)
            // 우하
            moveTo(right, bottom - arm)
            lineTo(right, bottom)
            lineTo(right - arm, bottom)
            // 좌하
            moveTo(inset + arm, bottom)
            lineTo(inset, bottom)
            lineTo(inset, bottom - arm)
        }
        drawPath(path = path, color = color, style = stroke)
    }
}

private val BracketInset = 20.dp
private val BracketArmLength = 28.dp
private val BracketStrokeWidth = 3.dp

@Preview(showBackground = true, backgroundColor = 0xFF444444, widthDp = 350, heightDp = 520)
@Composable
private fun MealCameraOverlayPreview() {
    DesignSystemTheme {
        MealCameraOverlay()
    }
}
