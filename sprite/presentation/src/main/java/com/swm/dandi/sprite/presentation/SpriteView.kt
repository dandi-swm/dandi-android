package com.swm.dandi.sprite.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.swm.dandi.common.presentation.ui.theme.DesignSystemTheme
import com.swm.dandi.sprite.entity.SpriteAlignElement
import com.swm.dandi.sprite.domain.SpriteFrameCalculator
import com.swm.dandi.sprite.entity.SpriteFrameVO
import com.swm.dandi.sprite.entity.SpriteVO
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * [SpriteVO] 명세에 따라 스프라이트 시트의 현재 frame 만 Canvas 에 그리는 범용 렌더러.
 *
 * [frameIndex] 가 null 이면 내부 frame state 로 자동 재생하고, 값이 들어오면 parent 가
 * frame 을 소유하는 controlled mode 로 동작한다. SpritePage 는 frame counter /
 * preview highlight 를 같은 값으로 맞추기 위해 controlled mode 를 사용한다.
 */
@Composable
fun SpriteView(
    spec: SpriteVO,
    modifier: Modifier = Modifier,
    playing: Boolean = true,
    frameIndex: Int? = null,
    onFrameChanged: (Int) -> Unit = {},
    filterQuality: FilterQuality = FilterQuality.None,
) {
    val spriteSheet = ImageBitmap.imageResource(spec.spriteSheetRes)
    val currentOnFrameChanged by rememberUpdatedState(onFrameChanged)
    var internalFrame by remember(spec) { mutableIntStateOf(spec.startFrame) }
    val currentFrame = (frameIndex ?: internalFrame).coerceIn(0, spec.totalFrames - 1)

    LaunchedEffect(spec) {
        internalFrame = spec.startFrame
    }

    LaunchedEffect(spec, playing, frameIndex) {
        if (frameIndex != null || !playing) return@LaunchedEffect

        var nextFrame = internalFrame
        while (true) {
            delay(spec.frameDurationMillis)
            val advancedFrame = SpriteFrameCalculator.nextFrame(spec, nextFrame)
            if (advancedFrame == nextFrame) break
            nextFrame = advancedFrame
            internalFrame = advancedFrame
        }
    }

    LaunchedEffect(spec, playing, frameIndex, currentFrame) {
        if (frameIndex == null || !playing) return@LaunchedEffect

        // Controlled mode 에서는 parent state 를 직접 바꾸지 않고 다음 frame 후보만
        // callback 으로 알린다.
        delay(spec.frameDurationMillis)
        val nextFrame = SpriteFrameCalculator.nextFrame(spec, currentFrame)
        if (nextFrame != currentFrame) {
            currentOnFrameChanged(nextFrame)
        }
    }

    val spriteFrame = SpriteFrameCalculator.frameAt(spec, currentFrame)
    Canvas(modifier = modifier) {
        drawSpriteFrame(
            spriteSheet = spriteSheet,
            frame = spriteFrame,
            filterQuality = filterQuality,
        )
    }
}

private fun DrawScope.drawSpriteFrame(
    spriteSheet: ImageBitmap,
    frame: SpriteFrameVO,
    filterQuality: FilterQuality,
) {
    val targetWidth = size.width.roundToInt()
    val targetHeight = size.height.roundToInt()
    if (targetWidth <= 0 || targetHeight <= 0) return

    // 원본 시트의 frame 영역(src)을 Canvas 전체(dst)로 확대/축소해서 그린다.
    drawImage(
        image = spriteSheet,
        srcOffset = IntOffset(frame.srcX, frame.srcY),
        srcSize = IntSize(frame.widthPx, frame.heightPx),
        dstOffset = IntOffset.Zero,
        dstSize = IntSize(targetWidth, targetHeight),
        filterQuality = filterQuality,
    )
}

@Preview
@Composable
private fun SpriteViewPreview() {
    DesignSystemTheme {
        SpriteView(
            spec = SpriteSampleAnimation.Hello.spec(SpriteAlignElement.Grid),
            modifier = Modifier.size(156.dp),
            playing = false,
        )
    }
}
