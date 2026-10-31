package com.dandi.nyummy.common.presentation.cutscene

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset

/**
 * 만화 컷신 한 편의 정의입니다. 그림, 칸 모양, 시각만 적고 움직임은 [CutscenePlayer] 가 정해진 몇 가지로만 그립니다.
 *
 * 칸 모양은 모두 기준 캔버스([CutsceneCanvas]) 좌표(dp)이고, 보통 [CutscenePage] 로 페이지를 나눠 만듭니다.
 * 플레이어가 캔버스를 화면 안전 영역에 비율 그대로 맞추므로 화면 크기와 비율이 달라도 구도가 같습니다.
 * 새 컷신은 이 값과 그림만 더하면 되고 코드는 늘지 않습니다.
 *
 * @param panels 차례로 박히는 칸. [CutscenePanel.atMillis] 순서대로 적는다.
 * @param words 효과음 글자. 마지막 칸이 펼쳐지면 걷힌다.
 * @param finale 마지막 칸. 칸으로 박혔다가 화면 가득 펼쳐진다.
 * @param backdropChanges 페이지 바탕색이 바뀌는 때.
 */
@Immutable
data class CutsceneSpec(
    val backdrop: CutsceneBackdrop,
    val panels: List<CutscenePanel>,
    val words: List<CutsceneWord>,
    val finale: CutsceneFinale,
    val backdropChanges: List<CutsceneBackdropChange> = emptyList(),
) {
    /** 시작부터 마지막 칸이 펼쳐져 머물다 끝날 때까지. */
    val totalMillis: Long get() = finale.expandAtMillis + CutsceneExpandMillis + finale.holdMillis
}

/** 기준 캔버스. 20:9 폰의 안전 영역에 맞춘 크기이고, 큰 화면에서는 [MAX_SCALE] 배까지만 키운다. */
object CutsceneCanvas {
    const val WIDTH = 360f
    const val HEIGHT = 780f
    const val MAX_SCALE = 1.3f
}

/** 캔버스 좌표(dp)의 사각 칸. 꼭짓점은 왼쪽 위, 오른쪽 위, 오른쪽 아래, 왼쪽 아래 순서다. */
@Immutable
data class CutsceneShape(val corners: List<Offset>)

/**
 * 칸 하나.
 *
 * @param focus 칸에 다 들어가지 않을 때 남길 그림 속 자리(0..1). 칸 비율이 그림과 다르면 이 자리를 중심으로 자른다.
 */
@Immutable
data class CutscenePanel(
    @DrawableRes val image: Int,
    val shape: CutsceneShape,
    val atMillis: Long,
    val focus: Offset = CenterFocus,
)

/**
 * 효과음 글자. ([x], [y]) 가 글자 가운데 자리다.
 *
 * @param scale 기본 효과음 글자 크기 대비 배율.
 */
@Immutable
data class CutsceneWord(
    @StringRes val text: Int,
    val x: Float,
    val y: Float,
    val atMillis: Long,
    val tiltDegrees: Float = -6f,
    val scale: Float = 1f,
    val ink: CutsceneInk = CutsceneInk.Brand,
)

/**
 * 마지막 칸. [shape] 칸으로 박혀 화면 전체 그림의 일부를 보여 주다가, [expandAtMillis] 에 화면 가득 펼쳐진다.
 *
 * @param focus 어떤 화면 비율에서도 보이게 둘 그림 속 자리(0..1). 보통 냐미 얼굴이다. 칸 안에서는 이 자리가 칸 가운데 온다.
 * @param word 칸 위쪽에 박혔다가, 펼쳐지면 캔버스 [wordTop] 높이로 올라가며 [wordFullScale] 배로 커지는 글자.
 */
@Immutable
data class CutsceneFinale(
    @DrawableRes val image: Int,
    val shape: CutsceneShape,
    val atMillis: Long,
    val expandAtMillis: Long,
    val focus: Offset,
    @StringRes val word: Int,
    val wordTop: Float,
    val wordTiltDegrees: Float = -6f,
    val wordScale: Float = 1.2f,
    val wordFullScale: Float = 1.5f,
    val wordInk: CutsceneInk = CutsceneInk.Brand,
    val holdMillis: Long = 1_500L,
)

/** 페이지 바탕색 역할. 실제 색은 테마 토큰에서 고른다. */
enum class CutsceneBackdrop { Paper, Dawn, Night }

/** 효과음 글자 테두리 색 역할. 실제 색은 테마 토큰에서 고른다. */
enum class CutsceneInk { Brand, Hot, Sleepy, Sunny }

@Immutable
data class CutsceneBackdropChange(val atMillis: Long, val backdrop: CutsceneBackdrop)

/** 마지막 칸이 화면 가득 펼쳐지는 시간. */
const val CutsceneExpandMillis = 450L

private val CenterFocus = Offset(0.5f, 0.5f)
