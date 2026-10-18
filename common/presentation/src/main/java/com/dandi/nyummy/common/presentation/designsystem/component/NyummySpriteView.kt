package com.dandi.nyummy.common.presentation.designsystem.component

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
import androidx.compose.foundation.layout.Box
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
import kotlinx.collections.immutable.persistentListOf
import kotlin.math.roundToInt

/**
 * 스프라이트 시트 이미지를 어디서 가져오는지.
 */
@Immutable
sealed interface NyummySpriteSource {
    /** 앱에 든 시트. 프레임 px 계산이 정확하도록 `drawable-nodpi`에 둔다. 그리는 즉시 보인다. */
    data class Resource(@DrawableRes val resId: Int) : NyummySpriteSource

    /** 서버(CDN)의 시트. 앱 공용 이미지 로더(메모리·디스크 캐시)로 받으며, 받는 동안은 placeholder를 보여 준다. */
    data class Url(val url: String) : NyummySpriteSource
}

/**
 * 스프라이트 시트 메타데이터: 같은 크기의 프레임이 격자로 배열된 단일 이미지이며,
 * 0번 프레임부터 행 우선(row-major) 순서로 애니메이션된다.
 * 해당 순서로 애니메이션 되는 이유는 스프라이트 이미지 리소스를 생성하는 ai (pixelab ai)의 스프라이트시트가
 * 해당 순서로 만들어지기 때문!
 *
 * 시트 1장으로 모든 밀도를 커버한다: 시트는 한 번만 제작해 `drawable-nodpi`에 두고
 * (플랫폼이 임의로 리스케일하지 않도록), [NyummySpriteView]가 그리기 시점에 `dstSize`로
 * 요청된 dp 크기에 맞게 스케일링한다. 서버에서 내려주는 시트는 `url`로 만든다(원본 px 그대로 받는다).
 *
 * 원본 해상도 가이드는 아트 스타일에 따라 다르다:
 * - 픽셀아트(냐미): 도트 원본 크기(예: 136px) 그대로 제작하고 `pixelArt = true`로 그린다.
 *   니어리스트 업스케일은 픽셀 경계를 보존하므로 원본이 표시 크기보다 작아도 품질 손실이 없다.
 * - 스무스 일러스트: 업스케일 시 보간으로 뭉개지므로, 최대 표시 dp의 3배(xxhdpi) 이상으로
 *   제작해 기기에서 다운스케일만 일어나게 한다.
 *
 * @param source 시트 이미지의 위치. 앱 리소스면 `imageRes`, 원격이면 `url` 팩토리로 만든다.
 * @param frameWidth 시트 내 프레임 1칸의 가로 px.
 * @param frameHeight 시트 내 프레임 1칸의 세로 px.
 * @param totalFrames 애니메이션이 순환하는 총 프레임 수.
 * @param framesPerRow 시트 한 줄당 프레임 수.
 * @param frameDurationMillis 프레임 1장이 화면에 머무는 시간. 기본 50ms(~20fps).
 */
@Immutable
@ConsistentCopyVisibility
data class NyummySpriteSheet private constructor(
    val source: NyummySpriteSource,
    val frameWidth: Int,
    val frameHeight: Int,
    val totalFrames: Int,
    val framesPerRow: Int,
    val frameDurationMillis: Int,
) {
    companion object {
        /**
         * frameWidth/frameHeight/totalFrames/framesPerRow는 그리기 시점 나눗셈·나머지 연산의
         * 분모가 되므로 0 이하 값이 들어오면 크래시 대신 최소 1로 보정하고,
         * frameDurationMillis는 tween durationMillis의 곱셈 인자가 되므로
         * 0 이하 값이 들어오면 기본값 50ms로 대체해 생성한다.
         */
        operator fun invoke(
            @DrawableRes imageRes: Int,
            frameWidth: Int,
            frameHeight: Int,
            totalFrames: Int,
            framesPerRow: Int,
            frameDurationMillis: Int = 50,
        ): NyummySpriteSheet = create(
            source = NyummySpriteSource.Resource(imageRes),
            frameWidth = frameWidth,
            frameHeight = frameHeight,
            totalFrames = totalFrames,
            framesPerRow = framesPerRow,
            frameDurationMillis = frameDurationMillis,
        )

        /** 서버(CDN)의 시트. 보정 규칙은 리소스 시트와 같다. */
        operator fun invoke(
            url: String,
            frameWidth: Int,
            frameHeight: Int,
            totalFrames: Int,
            framesPerRow: Int,
            frameDurationMillis: Int = 50,
        ): NyummySpriteSheet = create(
            source = NyummySpriteSource.Url(url),
            frameWidth = frameWidth,
            frameHeight = frameHeight,
            totalFrames = totalFrames,
            framesPerRow = framesPerRow,
            frameDurationMillis = frameDurationMillis,
        )

        private fun create(
            source: NyummySpriteSource,
            frameWidth: Int,
            frameHeight: Int,
            totalFrames: Int,
            framesPerRow: Int,
            frameDurationMillis: Int,
        ): NyummySpriteSheet = NyummySpriteSheet(
            source = source,
            frameWidth = frameWidth.coerceAtLeast(1),
            frameHeight = frameHeight.coerceAtLeast(1),
            totalFrames = totalFrames.coerceAtLeast(1),
            framesPerRow = framesPerRow.coerceAtLeast(1),
            frameDurationMillis = if (frameDurationMillis > 0) frameDurationMillis else 50,
        )
    }
}

/**
 * 스프라이트 시트 애니메이션을 고정 dp 크기로 무한 반복 그린다.
 *
 * 프레임 인덱스는 Compose 프레임 클럭([rememberInfiniteTransition]) 위에서 애니메이션되므로
 * vsync에 정렬되고, 창이 안 보이면 멈추며, 컴포지션 이탈 시 자동 해제된다.
 * 인덱스는 드로우 단계에서만 읽는다: 프레임이 넘어가도 리컴포지션 없이 캔버스만 다시 그린다.
 * 동작 하나가 시트 여러 장으로 나뉜 경우는 [NyummySpriteSequenceView]로 이어서 재생한다.
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
 * @param integerScale 프레임을 캔버스 폭에 가장 가까운 정수배로 키워 가운데 아래에 맞춰 그린다.
 * 픽셀아트의 도트 크기가 고르게 보이는 대신, 그려지는 크기가 [displayWidth]와 조금 다를 수 있다.
 * @param contentDescription 스프라이트가 의미를 전달하면 지정하고, 장식용이면 null로 둔다.
 * @param placeholder 원격 시트를 받는 동안 [displayWidth] 크기 자리에 보여 줄 내용. 리소스 시트는 바로 그려지므로 쓰지 않는다.
 * @param error 원격 시트를 받지 못했을 때 보여 줄 내용.
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
    integerScale: Boolean = false,
    contentDescription: String? = null,
    placeholder: @Composable () -> Unit = {},
    error: @Composable () -> Unit = placeholder,
) {
    val source = sheet.source
    val remote = if (source is NyummySpriteSource.Url) {
        rememberSpriteImages(remember(sheet) { persistentListOf(sheet) })
    } else {
        null
    }
    val image = when {
        source is NyummySpriteSource.Resource -> ImageBitmap.imageResource(source.resId)
        remote is SpriteImages.Loaded -> remote.images.first()
        else -> {
            Box(modifier.spriteSize(sheet, displayWidth)) {
                if (remote is SpriteImages.Failed) error() else placeholder()
            }
            return
        }
    }
    NyummySpriteView(
        image = image,
        sheet = sheet,
        modifier = modifier,
        displayWidth = displayWidth,
        playing = playing,
        iterations = iterations,
        onAnimationEnd = onAnimationEnd,
        flipX = flipX,
        pixelArt = pixelArt,
        integerScale = integerScale,
        contentDescription = contentDescription,
    )
}

/**
 * 디코딩된 [ImageBitmap]을 직접 받는 [NyummySpriteView] 오버로드.
 * 리소스/원격 버전이 여기로 위임하며, 프리뷰/테스트에서 비트맵을 직접 주입할 수 있다.
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
    integerScale: Boolean = false,
    contentDescription: String? = null,
) {
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
            .spriteSize(sheet, displayWidth)
            .then(semantics),
    ) {
        // onDraw()->: 프레임이 바뀌면 이 블록만 무효화되고 컴포지션은 건드리지 않는다.
        val frame = frameState.value % sheet.totalFrames
        val column = frame % sheet.framesPerRow
        val row = frame / sheet.framesPerRow

        // dstSize는 캔버스 px 크기(= displayWidth dp × 기기 density).
        // 시트 1장으로 밀도 대응이 끝나는 지점이 여기다.
        var dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt())
        var dstOffset = IntOffset.Zero
        if (integerScale) {
            val pixelScale = spritePixelScale(sheet.frameWidth, size.width)
            dstSize = IntSize(sheet.frameWidth * pixelScale, sheet.frameHeight * pixelScale)
            dstOffset = IntOffset(
                x = ((size.width - dstSize.width) / 2f).roundToInt(),
                y = (size.height - dstSize.height).roundToInt(),
            )
        }

        scale(scaleX = if (flipX) -1f else 1f, scaleY = 1f) {
            drawImage(
                image = image,
                srcOffset = IntOffset(column * sheet.frameWidth, row * sheet.frameHeight),
                srcSize = IntSize(sheet.frameWidth, sheet.frameHeight),
                dstOffset = dstOffset,
                dstSize = dstSize,
                filterQuality = if (pixelArt) FilterQuality.None else FilterQuality.Medium,
            )
        }
    }
}

/** [displayWidth] 폭에 프레임 비율을 따른 높이. 받는 중 자리도 같은 크기를 잡는다. */
internal fun Modifier.spriteSize(sheet: NyummySpriteSheet, displayWidth: Dp): Modifier =
    size(displayWidth, displayWidth * (sheet.frameHeight.toFloat() / sheet.frameWidth))

/**
 * 프레임을 화면에 정수배로 키웠을 때의 배율. 픽셀이 고르게 보이도록 [availablePx]에 가장 가까운 정수배를 고르고, 최소 1배다.
 * 예: 프레임 136px을 407px 칸에 그리면 3배(408px)다.
 */
internal fun spritePixelScale(framePx: Int, availablePx: Float): Int {
    if (framePx <= 0) return 1
    return Math.round(availablePx / framePx).coerceAtLeast(1)
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
