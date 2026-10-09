package com.dandi.nyummy.common.presentation.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.roundToInt

/**
 * 원격 스프라이트 시트 여러 장을 이어서 재생한다. 냐미 픽셀 애니메이션처럼 동작 하나가 시트 여러 장으로 나뉜 경우에 쓴다.
 *
 * 재생 순서:
 * 1. 클립을 순서대로 재생한다. 중간의 되풀이 구간은 [loopTimes]번 재생한다.
 * 2. 마지막 클립까지 끝나면 [restMillis] 동안 쉰다. 마지막 클립이 되풀이 구간이면 쉬는 동안 그 구간을 계속 되풀이하고,
 *    아니면 마지막 프레임에 멈춰 있다.
 * 3. 쉬고 나면 [onFinished]를 부른다. 다음 동작은 부른 쪽이 정한다.
 *    재생을 시작할 때 받은 [onFinished]를 부르므로, 부른 쪽은 어떤 재생이 끝났는지 콜백에 담아 둘 수 있다.
 *
 * 픽셀이 고르게 보이도록 셀을 칸 폭에 가장 가까운 정수배로 키우고, 칸의 가운데 아래에 맞춰 그린다.
 * 이미지를 받는 동안에는 [placeholder]를, 한 장이라도 받지 못하면 [error]를 보여 준다.
 * 다른 동작으로 바뀌어 새 시트를 받는 동안에는 직전에 그린 프레임을 그대로 두어 캐릭터가 깜빡이지 않는다.
 *
 * [clips], [frame], [restMillis], [loopTimes] 중 하나라도 바뀌면 처음부터 다시 재생한다.
 *
 * @param playId 값이 바뀌면 [clips]가 같아도 처음부터 다시 재생한다.
 * @param onShown 시트를 받아 그렸거나 받지 못해 [error]를 보여 준 뒤 한 번 불린다(받는 중에는 부르지 않는다). 화면 표시 시점 계측(TTI)에 쓴다.
 */
@Composable
fun NyummySpriteAnimation(
    clips: ImmutableList<NyummySpriteClip>,
    frame: NyummySpriteFrame,
    restMillis: Long,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    playId: Int = 0,
    loopTimes: Int = DefaultLoopTimes,
    contentDescription: String? = null,
    placeholder: @Composable () -> Unit = {},
    error: @Composable () -> Unit = placeholder,
    onShown: () -> Unit = {},
) {
    val lastDrawn = remember { LastDrawnCell() }
    val sheets = rememberSpriteSheets(clips)
    // 시트를 받아 그렸거나, 받지 못해 [error]를 보여 준 순간을 한 번만 알린다(화면에 무언가 보인 시점).
    // 그 상태가 그려진 다음 프레임까지 기다려 실제로 보인 뒤에 부른다.
    val currentOnShown by rememberUpdatedState(onShown)
    val shown = remember { booleanArrayOf(false) }
    LaunchedEffect(sheets is SpriteSheets.Loading) {
        if (sheets !is SpriteSheets.Loading && !shown[0]) {
            withFrameNanos { }
            shown[0] = true
            currentOnShown()
        }
    }
    when (sheets) {
        SpriteSheets.Loading -> {
            val held = lastDrawn.image
            if (held != null) {
                Canvas(modifier = modifier.spriteSemantics(contentDescription)) {
                    drawSpriteCell(held, lastDrawn.frame, lastDrawn.index)
                }
            } else {
                Box(modifier) { placeholder() }
            }
        }
        SpriteSheets.Failed -> Box(modifier) { error() }
        is SpriteSheets.Loaded -> SpriteAnimationCanvas(
            images = sheets.images,
            clips = clips,
            frame = frame,
            restMillis = restMillis,
            onFinished = onFinished,
            modifier = modifier,
            playId = playId,
            loopTimes = loopTimes,
            contentDescription = contentDescription,
            lastDrawn = lastDrawn,
        )
    }
}

/**
 * 이미 디코딩한 시트를 받는 [NyummySpriteAnimation]. [images]는 [clips]와 순서가 같아야 한다.
 * 미리보기와 테스트에서 비트맵을 직접 넣을 때 쓴다.
 */
@Composable
fun NyummySpriteAnimation(
    images: ImmutableList<ImageBitmap>,
    clips: ImmutableList<NyummySpriteClip>,
    frame: NyummySpriteFrame,
    restMillis: Long,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    playId: Int = 0,
    loopTimes: Int = DefaultLoopTimes,
    contentDescription: String? = null,
) {
    SpriteAnimationCanvas(
        images = images,
        clips = clips,
        frame = frame,
        restMillis = restMillis,
        onFinished = onFinished,
        modifier = modifier,
        playId = playId,
        loopTimes = loopTimes,
        contentDescription = contentDescription,
        lastDrawn = null,
    )
}

@Composable
private fun SpriteAnimationCanvas(
    images: ImmutableList<ImageBitmap>,
    clips: ImmutableList<NyummySpriteClip>,
    frame: NyummySpriteFrame,
    restMillis: Long,
    onFinished: () -> Unit,
    modifier: Modifier,
    playId: Int,
    loopTimes: Int,
    contentDescription: String?,
    lastDrawn: LastDrawnCell?,
) {
    var clipIndex by remember(clips, frame, restMillis, loopTimes, playId) { mutableIntStateOf(0) }
    var frameIndex by remember(clips, frame, restMillis, loopTimes, playId) { mutableIntStateOf(0) }

    LaunchedEffect(clips, frame, restMillis, loopTimes, playId) {
        if (clips.isEmpty()) return@LaunchedEffect
        val frameMillis = frame.durationMs.coerceAtLeast(1).toLong()
        suspend fun playOnce(index: Int) {
            clipIndex = index
            for (f in 0 until clips[index].frames) {
                frameIndex = f
                lastDrawn?.update(images.getOrNull(index), frame, f)
                delay(frameMillis)
            }
        }

        spriteSteps(clips, loopTimes).forEach { step -> repeat(step.times) { playOnce(step.clipIndex) } }

        val last = clips.lastIndex
        if (clips[last].loop) {
            // 쉬는 동안 마지막 되풀이 구간을 이어서 재생한다. 구간을 중간에 끊지 않고 한 바퀴씩 센다.
            val cycleMillis = clips[last].frames * frameMillis
            var rested = 0L
            while (rested < restMillis) {
                playOnce(last)
                rested += cycleMillis
            }
        } else {
            delay(restMillis)
        }
        onFinished()
    }

    Canvas(modifier = modifier.spriteSemantics(contentDescription)) {
        val image = images.getOrNull(clipIndex) ?: return@Canvas
        val clip = clips.getOrNull(clipIndex) ?: return@Canvas
        val index = frameIndex.coerceIn(0, (clip.frames - 1).coerceAtLeast(0))
        drawSpriteCell(image, frame, index)
    }
}

/**
 * 마지막으로 보여 준 칸. 새 시트를 받는 동안 그대로 보여 주려고 재생하면서 기억해 둔다.
 * 화면을 다시 그리게 할 필요가 없는 값이라 상태로 두지 않는다.
 */
private class LastDrawnCell {
    var image: ImageBitmap? = null
        private set
    var frame: NyummySpriteFrame = NyummySpriteFrame(width = 1, height = 1, framesPerRow = 1, durationMs = 1)
        private set
    var index: Int = 0
        private set

    fun update(image: ImageBitmap?, frame: NyummySpriteFrame, index: Int) {
        if (image == null) return
        this.image = image
        this.frame = frame
        this.index = index
    }
}

private fun Modifier.spriteSemantics(contentDescription: String?): Modifier =
    if (contentDescription != null) semantics { this.contentDescription = contentDescription } else this

/** 시트의 [index]번째 칸을 칸 폭에 가장 가까운 정수배로 키워 가운데 아래에 그린다. */
private fun DrawScope.drawSpriteCell(image: ImageBitmap, frame: NyummySpriteFrame, index: Int) {
    val perRow = frame.framesPerRow.coerceAtLeast(1)
    val scale = spritePixelScale(frame.width, size.width)
    val dstWidth = frame.width * scale
    val dstHeight = frame.height * scale
    drawImage(
        image = image,
        srcOffset = IntOffset((index % perRow) * frame.width, (index / perRow) * frame.height),
        srcSize = IntSize(frame.width, frame.height),
        dstOffset = IntOffset(
            x = ((size.width - dstWidth) / 2f).roundToInt(),
            y = (size.height - dstHeight).roundToInt(),
        ),
        dstSize = IntSize(dstWidth, dstHeight),
        filterQuality = FilterQuality.None,
    )
}

/**
 * 곧 재생할 시트를 미리 받아 앱 공용 이미지 캐시에 넣어 둔다. 동작이 바뀔 때 시트를 받느라 기다리지 않게 한다.
 * [urls]가 바뀔 때마다 한 번씩 요청한다.
 */
@Composable
fun NyummySpritePrefetch(urls: ImmutableList<String>) {
    val context = LocalContext.current
    LaunchedEffect(urls) {
        val loader = SingletonImageLoader.get(context)
        urls.forEach { url -> loader.enqueue(ImageRequest.Builder(context).data(url).allowHardware(false).build()) }
    }
}

private sealed interface SpriteSheets {
    data object Loading : SpriteSheets
    data object Failed : SpriteSheets
    class Loaded(val images: ImmutableList<ImageBitmap>) : SpriteSheets
}

/**
 * [clips]의 시트를 모두 받아 [ImageBitmap]으로 돌려준다. 한 장이라도 받지 못하면 실패다.
 * 앱 공용 이미지 로더의 메모리와 디스크 캐시를 그대로 쓴다. 캔버스에서 일부 영역을 잘라 그리므로 하드웨어 비트맵은 쓰지 않는다.
 */
@Composable
private fun rememberSpriteSheets(clips: ImmutableList<NyummySpriteClip>): SpriteSheets {
    val context = LocalContext.current
    val sheets by produceState<SpriteSheets>(initialValue = SpriteSheets.Loading, clips) {
        value = SpriteSheets.Loading
        val loader = SingletonImageLoader.get(context)
        value = try {
            coroutineScope {
                clips.map { clip ->
                    async {
                        val request = ImageRequest.Builder(context).data(clip.url).allowHardware(false).build()
                        val result = loader.execute(request) as? SuccessResult ?: error("sprite load failed: ${clip.url}")
                        result.image.toBitmap().asImageBitmap()
                    }
                }.awaitAll()
            }.toImmutableList().let(SpriteSheets::Loaded)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            SpriteSheets.Failed
        }
    }
    return sheets
}

private const val DefaultLoopTimes = 3
