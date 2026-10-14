package com.dandi.nyummy.auth.presentation

import android.provider.Settings
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.sin

/**
 * 로그인 배경의 민트 낙서(발자국, 잎, 숟가락 등)가 줄 맞춰 위로 천천히 흘러간다.
 *
 * 낙서는 한 줄씩 반 칸 엇갈린 격자에 정해진 순서로 놓이고, 격자 전체가 같은 속도로 움직인다.
 * 각 낙서는 격자를 따라 번지는 물결처럼 같은 리듬으로 둥실 떠올랐다 내려온다.
 * [clearAreaInWindow](제목과 문구) 근처를 지나는 낙서는 글자를 가리지 않도록 서서히 사라진다.
 * 미리보기와 시스템 애니메이션이 꺼진 기기에서는 멈춘 채로 그린다.
 */
@Composable
internal fun LoginDoodleBackground(
    clearAreaInWindow: Rect,
    modifier: Modifier = Modifier,
) {
    val sprites = DoodleSequence.map { ImageBitmap.imageResource(it) }
    val timeMillis = remember { mutableLongStateOf(0L) }
    val animates = !LocalInspectionMode.current && LocalContext.current.animationsEnabled()
    var originInWindow by remember { mutableStateOf(Offset.Zero) }

    if (animates) {
        LaunchedEffect(Unit) {
            val start = withInfiniteAnimationFrameMillis { it }
            while (true) {
                withInfiniteAnimationFrameMillis { timeMillis.longValue = it - start }
            }
        }
    }

    Spacer(
        modifier = modifier
            .onGloballyPositioned { originInWindow = it.positionInWindow() }
            .drawBehind {
                val seconds = timeMillis.longValue / 1000f
                val cell = DoodleCellSize.toPx()
                val flow = DoodleFlowSpeed.toPx() * seconds
                val bob = DoodleBobHeight.toPx()
                val clearArea = if (clearAreaInWindow.isEmpty) {
                    Rect.Zero
                } else {
                    clearAreaInWindow.translate(-originInWindow).inflate(DoodleClearPadding.toPx())
                }
                val clearFade = DoodleClearFade.toPx()
                // 격자는 끝없이 위로 밀려나므로, 지금 화면에 걸치는 칸만 골라 그린다.
                val originX = 0f
                val originY = -flow
                val firstRow = floor(-originY / cell).toInt() - 1
                val lastRow = floor((size.height - originY) / cell).toInt() + 1
                val firstColumn = floor(-originX / cell).toInt() - 1
                val lastColumn = floor((size.width - originX) / cell).toInt() + 1

                for (row in firstRow..lastRow) {
                    val stagger = if (row.mod(2) == 0) 0f else cell / 2
                    for (column in firstColumn..lastColumn) {
                        val image = sprites[(column + row * DoodleRowShift).mod(sprites.size)]
                        val wave = sin(2 * PI.toFloat() * seconds / DoodleBobSeconds - (column + row) * DoodleWaveStep)
                        val width = image.width * DoodleScale
                        val height = image.height * DoodleScale
                        val centerX = originX + column * cell + stagger + cell / 2
                        val centerY = originY + row * cell + cell / 2 + wave * bob
                        val visibility = clearArea.visibilityAt(centerX, centerY, clearFade)
                        if (visibility <= 0f) continue
                        translate(left = centerX - width / 2, top = centerY - height / 2) {
                            rotate(degrees = wave * DoodleWobbleDegrees, pivot = Offset(width / 2, height / 2)) {
                                drawImage(
                                    image = image,
                                    dstOffset = IntOffset.Zero,
                                    dstSize = IntSize(width.toInt(), height.toInt()),
                                    alpha = DoodleAlpha * visibility,
                                )
                            }
                        }
                    }
                }
            },
    )
}

/** 한 줄에 놓이는 낙서 순서. 줄마다 [DoodleRowShift]칸씩 밀어 같은 낙서가 대각선으로 이어진다. */
@DrawableRes
private val DoodleSequence: List<Int> = listOf(
    R.drawable.auth_doodle_paw,
    R.drawable.auth_doodle_sparkle,
    R.drawable.auth_doodle_leaf,
    R.drawable.auth_doodle_dot,
    R.drawable.auth_doodle_bowl,
    R.drawable.auth_doodle_sparkle,
    R.drawable.auth_doodle_sprig,
    R.drawable.auth_doodle_spoon,
)

/** 이 영역 안이면 0, [fade]만큼 벗어나면 1. 그 사이는 부드럽게 이어진다. 영역이 비어 있으면 늘 1이다. */
private fun Rect.visibilityAt(x: Float, y: Float, fade: Float): Float {
    if (isEmpty) return 1f
    val dx = maxOf(left - x, 0f, x - right)
    val dy = maxOf(top - y, 0f, y - bottom)
    val t = (hypot(dx, dy) / fade).coerceIn(0f, 1f)
    return t * t * (3 - 2 * t)
}

/** 개발자 옵션이나 접근성에서 애니메이션을 끈 기기는 낙서를 멈춰 둔다. */
private fun android.content.Context.animationsEnabled(): Boolean =
    Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f

private const val DoodleAlpha = 0.5f

/** 예전 정적 패턴이 화면을 채우며 확대되던 비율. 낙서 크기를 그때와 맞춘다. */
private const val DoodleScale = 1.4f
private const val DoodleRowShift = 3

/** 둥실 한 번 오르내리는 데 걸리는 시간(초). */
private const val DoodleBobSeconds = 3.2f

/** 이웃한 낙서끼리 물결이 어긋나는 정도(라디안). */
private const val DoodleWaveStep = 0.5f
private const val DoodleWobbleDegrees = 5f
private val DoodleCellSize = 76.dp

/** 초당 위로 움직이는 거리. */
private val DoodleFlowSpeed = 8.dp
private val DoodleBobHeight = 3.dp

/** 제목 영역에서 이만큼 떨어진 곳까지는 낙서를 두지 않는다. */
private val DoodleClearPadding = 16.dp

/** 비워 둔 영역 바깥에서 낙서가 다시 또렷해지기까지의 거리. */
private val DoodleClearFade = 40.dp
