package com.dandi.nyummy.common.presentation.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameMillis
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.Dp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

/**
 * [NyummySpriteSequenceView]가 이어서 재생하는 시트 한 장.
 *
 * @property sheet 재생할 시트
 * @property loop 되풀이해도 되는 구간인지
 */
@Immutable
data class NyummySpriteClip(
    val sheet: NyummySpriteSheet,
    val loop: Boolean = false,
)

/**
 * 시트 여러 장을 이어서 재생한다. 냐미 동작 하나가 시트 여러 장으로 나뉜 경우에 쓴다.
 * 시트 한 장 한 장은 [NyummySpriteView]가 그리고, 이 컴포넌트는 순서와 쉬는 시간만 정한다.
 *
 * 재생 순서:
 * 1. 클립을 순서대로 재생한다. 중간의 되풀이 구간은 [loopTimes]번 재생한다.
 * 2. 마지막 클립까지 끝나면 [restMillis] 동안 쉰다. 마지막 클립이 되풀이 구간이면 쉬는 동안 그 구간을 한 바퀴씩 이어서 돌고,
 *    아니면 마지막 프레임에 멈춰 있다.
 * 3. 쉬고 나면 [onFinished]를 부른다. 다음 동작은 부른 쪽이 정한다.
 *    재생을 시작할 때 받은 [onFinished]를 부르므로, 부른 쪽은 어떤 재생이 끝났는지 콜백에 담아 둘 수 있다.
 *
 * 원격 시트를 받는 동안에는 [placeholder]를, 한 장이라도 받지 못하면 [error]를 보여 준다.
 * 다른 동작으로 바뀌어 새 시트를 받는 동안에는 직전 동작을 계속 그려 캐릭터가 깜빡이지 않는다.
 *
 * [clips], [restMillis], [loopTimes], [playId] 중 하나라도 바뀌면 처음부터 다시 재생한다.
 *
 * @param displayWidth 화면에 그릴 가로 크기(dp). 세로는 첫 시트의 프레임 비율을 따른다.
 * @param playId 값이 바뀌면 [clips]가 같아도 처음부터 다시 재생한다.
 * @param integerScale [NyummySpriteView]와 같은 계약. 픽셀아트 캐릭터는 true로 둔다.
 * @param onShown 시트를 받아 그렸거나 받지 못해 [error]를 보여 준 뒤 한 번 불린다(받는 중에는 부르지 않는다). 화면 표시 시점 계측(TTI)에 쓴다.
 */
@Composable
fun NyummySpriteSequenceView(
    clips: ImmutableList<NyummySpriteClip>,
    restMillis: Long,
    onFinished: () -> Unit,
    displayWidth: Dp,
    modifier: Modifier = Modifier,
    playId: Int = 0,
    loopTimes: Int = DefaultLoopTimes,
    integerScale: Boolean = false,
    contentDescription: String? = null,
    placeholder: @Composable () -> Unit = {},
    error: @Composable () -> Unit = placeholder,
    onShown: () -> Unit = {},
) {
    val sheets = remember(clips) { clips.map { it.sheet }.toImmutableList() }
    val images = rememberSpriteImages(sheets)

    // 시트를 받아 그렸거나, 받지 못해 [error]를 보여 준 순간을 한 번만 알린다(화면에 무언가 보인 시점).
    // 그 상태가 그려진 다음 프레임까지 기다려 실제로 보인 뒤에 부른다.
    val currentOnShown by rememberUpdatedState(onShown)
    val shown = remember { booleanArrayOf(false) }
    LaunchedEffect(images is SpriteImages.Loading) {
        if (images !is SpriteImages.Loading && !shown[0]) {
            withFrameNanos { }
            shown[0] = true
            currentOnShown()
        }
    }

    // 마지막으로 시트를 다 받은 재생. 다음 동작의 시트를 받는 동안 이것을 계속 그린다.
    // 화면을 다시 그리게 할 필요가 없는 값이라 상태로 두지 않는다.
    val held = remember { arrayOfNulls<SpritePlayback>(1) }
    val playback = if (images is SpriteImages.Loaded) {
        SpritePlayback(clips, images.images, restMillis, loopTimes, playId).also { held[0] = it }
    } else {
        held[0]
    }
    when {
        images is SpriteImages.Failed -> Box(modifier.sequenceSize(clips, displayWidth)) { error() }
        playback == null -> Box(modifier.sequenceSize(clips, displayWidth)) { placeholder() }
        else -> SpritePlaybackView(
            playback = playback,
            active = images is SpriteImages.Loaded,
            onFinished = onFinished,
            displayWidth = displayWidth,
            integerScale = integerScale,
            contentDescription = contentDescription,
            modifier = modifier,
        )
    }
}

/**
 * 이미 디코딩한 시트를 받는 [NyummySpriteSequenceView]. [images]는 [clips]와 순서가 같아야 한다.
 * 미리보기와 테스트에서 비트맵을 직접 넣을 때 쓴다.
 */
@Composable
fun NyummySpriteSequenceView(
    images: ImmutableList<ImageBitmap>,
    clips: ImmutableList<NyummySpriteClip>,
    restMillis: Long,
    onFinished: () -> Unit,
    displayWidth: Dp,
    modifier: Modifier = Modifier,
    playId: Int = 0,
    loopTimes: Int = DefaultLoopTimes,
    integerScale: Boolean = false,
    contentDescription: String? = null,
) {
    SpritePlaybackView(
        playback = SpritePlayback(clips, images, restMillis, loopTimes, playId),
        active = true,
        onFinished = onFinished,
        displayWidth = displayWidth,
        integerScale = integerScale,
        contentDescription = contentDescription,
        modifier = modifier,
    )
}

/** 시트를 다 받은 재생 한 번. [clips], [restMillis], [loopTimes], [playId]가 같으면 같은 재생이다. */
@Immutable
private class SpritePlayback(
    val clips: ImmutableList<NyummySpriteClip>,
    val images: ImmutableList<ImageBitmap>,
    val restMillis: Long,
    val loopTimes: Int,
    val playId: Int,
)

/**
 * [playback]의 단계를 차례로 [NyummySpriteView]에 넘겨 재생한다.
 * [active]가 false면(다음 동작의 시트를 받는 중) 그리기만 하고 끝났다고 알리지 않는다.
 */
@Composable
private fun SpritePlaybackView(
    playback: SpritePlayback,
    active: Boolean,
    onFinished: () -> Unit,
    displayWidth: Dp,
    integerScale: Boolean,
    contentDescription: String?,
    modifier: Modifier,
) {
    val clips = playback.clips
    if (clips.isEmpty()) {
        Box(modifier.size(displayWidth))
        return
    }
    key(clips, playback.restMillis, playback.loopTimes, playback.playId) {
        val timeline = remember { spriteTimeline(clips, playback.loopTimes, playback.restMillis) }
        var stepIndex by remember { mutableIntStateOf(0) }
        var stepsDone by remember { mutableStateOf(false) }

        // 같은 시트가 이어지는 단계도 처음부터 다시 재생하도록 단계마다 새로 만든다.
        key(stepIndex) {
            val step = timeline.steps[stepIndex]
            NyummySpriteView(
                image = playback.images[step.clipIndex],
                sheet = clips[step.clipIndex].sheet,
                modifier = modifier,
                displayWidth = displayWidth,
                iterations = step.times,
                onAnimationEnd = {
                    if (stepIndex < timeline.steps.lastIndex) stepIndex++ else stepsDone = true
                },
                integerScale = integerScale,
                contentDescription = contentDescription,
            )
        }

        if (active) {
            LaunchedEffect(Unit) {
                val startMillis = withFrameMillis { it }
                snapshotFlow { stepsDone }.first { it }
                // 애니메이션 배율이 0(접근성 "애니메이션 삭제")이면 단계가 곧바로 끝난다.
                // 그래도 동작 하나에 걸리는 시간은 지켜, 다음 동작 요청이 몰리지 않게 한다.
                val elapsedMillis = withFrameMillis { it } - startMillis
                delay(maxOf(timeline.holdMillis, timeline.totalMillis - elapsedMillis))
                onFinished()
            }
        }
    }
}

private fun Modifier.sequenceSize(clips: ImmutableList<NyummySpriteClip>, displayWidth: Dp): Modifier =
    clips.firstOrNull()?.let { spriteSize(it.sheet, displayWidth) } ?: size(displayWidth)

/** 클립 [clipIndex]를 [times]번 재생하는 한 단계. */
internal data class SpriteStep(val clipIndex: Int, val times: Int)

/**
 * 시퀀스 재생 계획.
 *
 * @property steps 차례로 재생할 단계
 * @property holdMillis 마지막 단계가 끝난 뒤 마지막 프레임에 멈춰 있는 시간
 * @property totalMillis 재생을 시작해 끝났다고 알릴 때까지 걸리는 시간
 */
internal data class SpriteTimeline(val steps: List<SpriteStep>, val holdMillis: Long, val totalMillis: Long)

/**
 * 클립 목록을 재생 계획으로 펼친다.
 * 중간의 되풀이 구간은 [loopTimes]번, 나머지는 한 번 재생한다.
 * 마지막 클립이 되풀이 구간이면 한 번 재생한 뒤 쉬는 시간([restMillis])을 다 채울 때까지 한 바퀴씩 더 돈다(구간을 중간에 끊지 않는다).
 * 아니면 마지막 프레임에 멈춰 쉰다.
 */
internal fun spriteTimeline(clips: List<NyummySpriteClip>, loopTimes: Int, restMillis: Long): SpriteTimeline {
    val rest = restMillis.coerceAtLeast(0L)
    val steps = clips.mapIndexed { index, clip ->
        val times = when {
            index != clips.lastIndex -> if (clip.loop) loopTimes.coerceAtLeast(1) else 1
            clip.loop -> {
                val cycleMillis = clip.sheet.cycleMillis
                1 + ((rest + cycleMillis - 1) / cycleMillis).toInt()
            }
            else -> 1
        }
        SpriteStep(clipIndex = index, times = times)
    }
    val holdMillis = if (clips.lastOrNull()?.loop == true) 0L else rest
    val playMillis = steps.sumOf { clips[it.clipIndex].sheet.cycleMillis * it.times }
    return SpriteTimeline(steps = steps, holdMillis = holdMillis, totalMillis = playMillis + holdMillis)
}

/** 시트를 한 바퀴 재생하는 시간. */
private val NyummySpriteSheet.cycleMillis: Long
    get() = totalFrames.toLong() * frameDurationMillis

private const val DefaultLoopTimes = 3
