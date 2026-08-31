package com.dandi.nyummy.meal.presentation.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dandi.nyummy.common.presentation.component.DandiText
import com.dandi.nyummy.common.presentation.component.NyummySpriteSheet
import com.dandi.nyummy.common.presentation.component.NyummySpriteView
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemTheme
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemThemeImpl
import com.dandi.nyummy.meal.presentation.R

/**
 * 카메라 프리뷰/촬영본 위에 얹는 장식 오버레이입니다.
 *
 * 네 모서리 뷰파인더 브래킷과 하단 우측 스파클·냐미 캐릭터를 그리고,
 * [showHint] 가 참이면 상단에 촬영 안내 필을 함께 보여줍니다.
 * 터치를 가로채지 않는 순수 장식 레이어라 상태를 갖지 않습니다.
 */
@Composable
fun MealCameraOverlay(
    showHint: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = DesignSystemThemeImpl.designSystemColor
    val spacing = DesignSystemThemeImpl.designSystemSpacing
    Box(modifier = modifier.fillMaxSize()) {
        CornerBrackets(
            color = colors.contentInverseDefault,
            modifier = Modifier.fillMaxSize(),
        )
        if (showHint) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = HintPillTopGap),
                shape = DesignSystemThemeImpl.designSystemShape.pill,
                color = colors.bgScrimDefault,
            ) {
                DandiText(
                    text = stringResource(R.string.meal_record_camera_hint),
                    modifier = Modifier.padding(
                        horizontal = spacing.space16,
                        vertical = spacing.space8,
                    ),
                    color = colors.contentInverseDefault,
                    style = DesignSystemThemeImpl.typeScale.textRegularM,
                )
            }
        }
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = MascotEndGap, bottom = MascotBottomGap),
            verticalAlignment = Alignment.Bottom,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_meal_sparkle),
                contentDescription = null,
                modifier = Modifier
                    .padding(bottom = SparkleBottomGap)
                    .size(SparkleSize),
                tint = colors.contentInverseDefault,
            )
            Spacer(Modifier.width(DesignSystemThemeImpl.designSystemSpacing.space8))
            NyummySpriteView(
                sheet = MealMascotSheet,
                displayWidth = MascotDisplayWidth,
            )
        }
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

/** 프리뷰 우하단 냐미 (엎드려 조는 8프레임 루프, 홈과 동일 시트). */
private val MealMascotSheet = NyummySpriteSheet(
    imageRes = R.drawable.nyami_sleep_loop_grid_136,
    frameWidth = 136,
    frameHeight = 136,
    totalFrames = 8,
    framesPerRow = 4,
    frameDurationMillis = 100,
)

private val BracketInset = 20.dp
private val BracketArmLength = 28.dp
private val BracketStrokeWidth = 3.dp
private val HintPillTopGap = 100.dp
private val SparkleSize = 20.dp
private val SparkleBottomGap = 48.dp
private val MascotDisplayWidth = 88.dp
private val MascotEndGap = 24.dp
private val MascotBottomGap = 16.dp

@Preview(showBackground = true, backgroundColor = 0xFF444444, widthDp = 350, heightDp = 520)
@Composable
private fun MealCameraOverlayPreview() {
    DesignSystemTheme {
        MealCameraOverlay(showHint = true)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF444444, widthDp = 350, heightDp = 520)
@Composable
private fun MealCameraOverlayCapturedPreview() {
    DesignSystemTheme {
        MealCameraOverlay(showHint = false)
    }
}
