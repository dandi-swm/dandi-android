package com.dandi.nyummy.common.presentation.component

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateValue
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.dandi.nyummy.common.presentation.R
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemTheme
import kotlin.math.roundToInt

/**
 * 스프라이트 시트 메타데이터: 같은 크기의 프레임이 격자로 배열된 단일 이미지이며,
 * 0번 프레임부터 행 우선(row-major) 순서로 애니메이션된다.
 * 해당 순서로 애니메이션 되는 이유는 스프라이트 이미지 리소스를 생성하는 ai (pixelab ai)의 스프라이트시트가
 * 해당 순서로 만들어지기 때문!
 *
 * 시트 1장으로 모든 밀도를 커버한다: 시트는 한 번만 제작해 `drawable-nodpi`에 두고
 * (플랫폼이 임의로 리스케일하지 않도록), [NyummySpriteView]가 그리기 시점에 `dstSize`로
 * 요청된 dp 크기에 맞게 스케일링한다.
 *
 * 원본 해상도 가이드는 아트 스타일에 따라 다르다:
 * - 픽셀아트(냐미): 도트 원본 크기(예: 136px) 그대로 제작하고 `pixelArt = true`로 그린다.
 *   니어리스트 업스케일은 픽셀 경계를 보존하므로 원본이 표시 크기보다 작아도 품질 손실이 없다.
 * - 스무스 일러스트: 업스케일 시 보간으로 뭉개지므로, 최대 표시 dp의 3배(xxhdpi) 이상으로
 *   제작해 기기에서 다운스케일만 일어나게 한다.
 *
 * @param imageRes 시트 drawable. 프레임 px 계산이 정확하도록 `drawable-nodpi`를 사용한다.
 * @param frameWidth 시트 내 프레임 1칸의 가로 px.
 * @param frameHeight 시트 내 프레임 1칸의 세로 px.
 * @param totalFrames 애니메이션이 순환하는 총 프레임 수.
 * @param framesPerRow 시트 한 줄당 프레임 수.
 * @param frameDurationMillis 프레임 1장이 화면에 머무는 시간. 기본 50ms(~20fps).
 */
@Immutable
@ConsistentCopyVisibility
data class NyummySpriteSheet private constructor(
    @DrawableRes val imageRes: Int,
    val frameWidth: Int,
    val frameHeight: Int,
    val totalFrames: Int,
    val framesPerRow: Int,
    val frameDurationMillis: Int,
) {
    companion object {
        /**
         * frameWidth/frameHeight/totalFrames/framesPerRow는 그리기 시점 나눗셈·나머지 연산의
         * 분모가 되므로, 0 이하 값이 들어오면 크래시 대신 최소 1로 보정해 생성한다.
         */
        operator fun invoke(
            @DrawableRes imageRes: Int,
            frameWidth: Int,
            frameHeight: Int,
            totalFrames: Int,
            framesPerRow: Int,
            frameDurationMillis: Int = 50,
        ): NyummySpriteSheet = NyummySpriteSheet(
            imageRes = imageRes,
            frameWidth = frameWidth.coerceAtLeast(1),
            frameHeight = frameHeight.coerceAtLeast(1),
            totalFrames = totalFrames.coerceAtLeast(1),
            framesPerRow = framesPerRow.coerceAtLeast(1),
            frameDurationMillis = frameDurationMillis,
        )
    }
}

/**
 * 스프라이트 시트 애니메이션을 고정 dp 크기로 무한 반복 그린다.
 *
 * 프레임 인덱스는 Compose 프레임 클럭([rememberInfiniteTransition]) 위에서 애니메이션되므로
 * vsync에 정렬되고, 창이 안 보이면 멈추며, 컴포지션 이탈 시 자동 해제된다.
 * 인덱스는 드로우 단계에서만 읽는다: 프레임이 넘어가도 리컴포지션 없이 캔버스만 다시 그린다.
 *
 * @param sheet 시트 메타데이터와 타이밍.
 * @param displayWidth 화면에 그릴 가로 크기(dp). 모든 밀도에서 동일한 물리 크기가 된다.
 * 세로는 프레임 비율을 따라 자동 계산된다.
 * @param playing true인 동안 재생되고, false면 0번 프레임에 정지한다.
 * @param iterations 재생 횟수. null이면 무한 반복(기본), n이면 n회 재생 후
 * 마지막 프레임에 멈춘다. 다시 재생하려면 key()로 새로 만들거나 playing을 토글한다.
 * @param onAnimationEnd 유한 재생(iterations != null)이 모두 끝났을 때 1회 호출된다.
 * 무한 반복에서는 호출되지 않는다.
 * @param flipX 스프라이트를 좌우 반전한다(예: 바라보는 방향).
 * @param pixelArt 니어리스트 샘플링으로 스케일링 시 픽셀 경계를 보존한다.
 * @param contentDescription 스프라이트가 의미를 전달하면 지정하고, 장식용이면 null로 둔다.
 */
@Composable
fun NyummySpriteView(
    sheet: NyummySpriteSheet,
    modifier: Modifier = Modifier,
    displayWidth: Dp = sheet.frameWidth.dp / 3,
    playing: Boolean = true,
    iterations: Int? = null,
    onAnimationEnd: (() -> Unit)? = null,
    flipX: Boolean = false,
    pixelArt: Boolean = true,
    contentDescription: String? = null,
) {
    NyummySpriteView(
        image = ImageBitmap.imageResource(sheet.imageRes),
        sheet = sheet,
        modifier = modifier,
        displayWidth = displayWidth,
        playing = playing,
        iterations = iterations,
        onAnimationEnd = onAnimationEnd,
        flipX = flipX,
        pixelArt = pixelArt,
        contentDescription = contentDescription,
    )
}

/**
 * 디코딩된 [ImageBitmap]을 직접 받는 [NyummySpriteView] 오버로드.
 * 리소스 버전이 여기로 위임하며, 프리뷰/테스트에서 비트맵을 직접 주입할 수 있다.
 */
@Composable
fun NyummySpriteView(
    image: ImageBitmap,
    sheet: NyummySpriteSheet,
    modifier: Modifier = Modifier,
    displayWidth: Dp = sheet.frameWidth.dp / 3,
    playing: Boolean = true,
    iterations: Int? = null,
    onAnimationEnd: (() -> Unit)? = null,
    flipX: Boolean = false,
    pixelArt: Boolean = true,
    contentDescription: String? = null,
) {
    val displayHeight = displayWidth * (sheet.frameHeight.toFloat() / sheet.frameWidth)
    val frameState = spriteFrameState(
        sheet = sheet,
        playing = playing,
        iterations = iterations,
        onAnimationEnd = onAnimationEnd,
    )

    val semantics = if (contentDescription != null) {
        Modifier.semantics { this.contentDescription = contentDescription }
    } else {
        Modifier
    }

    Canvas(
        modifier = modifier
            .size(displayWidth, displayHeight)
            .then(semantics),
    ) {
        // onDraw()->: 프레임이 바뀌면 이 블록만 무효화되고 컴포지션은 건드리지 않는다.
        val frame = frameState.value % sheet.totalFrames
        val column = frame % sheet.framesPerRow
        val row = frame / sheet.framesPerRow

        scale(scaleX = if (flipX) -1f else 1f, scaleY = 1f) {
            drawImage(
                image = image,
                srcOffset = IntOffset(column * sheet.frameWidth, row * sheet.frameHeight),
                srcSize = IntSize(sheet.frameWidth, sheet.frameHeight),
                // dstSize는 캔버스 px 크기(= displayWidth dp × 기기 density).
                // 시트 1장으로 밀도 대응이 끝나는 지점이 여기다.
                dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
                filterQuality = if (pixelArt) FilterQuality.None else FilterQuality.Medium,
            )
        }
    }
}

@Composable
private fun spriteFrameState(
    sheet: NyummySpriteSheet,
    playing: Boolean,
    iterations: Int?,
    onAnimationEnd: (() -> Unit)?,
): State<Int> = when {
    // 정지: 0번 프레임 고정
    !playing -> remember { mutableIntStateOf(0) }

    // 무한 반복: InfiniteTransition — 끝이라는 개념이 없으므로 콜백도 없다
    // 화면에서 벗어나면 알아서 취소된다.
    iterations == null -> rememberInfiniteTransition(label = "NyummySprite").animateValue(
        initialValue = 0,
        targetValue = sheet.totalFrames,
        typeConverter = Int.VectorConverter,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = sheet.totalFrames * sheet.frameDurationMillis,
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Restart,
        ),
        label = "NyummySpriteFrame",
    )

    // 유한 재생: Animatable — animateTo()가 끝나는 시점을 알 수 있어
    // 마지막 프레임 유지와 종료 콜백이 가능하다
    else -> {
        val frame = remember(sheet, iterations) { mutableIntStateOf(0) }
        // 재생 도중 콜백 람다가 바뀌어도 애니메이션 재시작 없이 최신 람다를 부르게 한다
        val currentOnEnd by rememberUpdatedState(onAnimationEnd)

        LaunchedEffect(sheet, iterations) {
            val animatable = Animatable(initialValue = 0f)
            repeat(iterations) {
                animatable.snapTo(0f)
                animatable.animateTo(
                    targetValue = sheet.totalFrames.toFloat(),
                    animationSpec = tween(
                        durationMillis = sheet.totalFrames * sheet.frameDurationMillis,
                        easing = LinearEasing,
                    ),
                ) {
                    // coerceAtMost: 종료 순간 값이 totalFrames에 닿아도 마지막 프레임을 넘지 않게
                    frame.intValue = value.toInt().coerceAtMost(sheet.totalFrames - 1)
                }
            }
            frame.intValue = sheet.totalFrames - 1 // 재생이 끝나면 마지막 프레임에 머문다
            currentOnEnd?.invoke()
        }
        frame
    }
}
