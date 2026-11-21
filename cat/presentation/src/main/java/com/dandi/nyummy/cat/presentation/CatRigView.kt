package com.dandi.nyummy.cat.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.platform.LocalContext
import com.dandi.nyummy.cat.domain.rig.CatRigAnimator
import com.dandi.nyummy.cat.domain.rig.CatRigModel
import com.dandi.nyummy.cat.entity.CatState
import com.dandi.nyummy.cat.entity.rig.CatRigCharacterVO
import kotlin.coroutines.coroutineContext

/**
 * 리그 냐미가 할 동작 하나. 스프라이트 재생([com.dandi.nyummy.common.presentation.designsystem.component.NyummySpriteSequenceView])과
 * 같은 계약이다: 들어가기 -> 되풀이 x [loopTimes] -> 돌아오기 -> [restMillis] 쉬기 -> 끝났다고 알림.
 *
 * @property motion 스프라이트 동작 이름(앱 에셋 animations.json의 이름)
 * @property state 동작을 고른 상황(이름으로 클립이 없을 때 상황 기본 클립을 쓴다)
 * @property playId 바뀌면 같은 동작이어도 처음부터 다시 튼다
 */
@Immutable
data class CatRigMotion(
    val motion: String,
    val state: CatState?,
    val playId: Int,
    val restMillis: Long,
    val loopTimes: Int = CatRigAnimator.DEFAULT_LOOP_TIMES,
)

/**
 * 리그로 그리는 냐미. 크기는 [modifier]가 정하고(정사각 칸), 리그 좌표 136u가 칸 폭에 맞는다(스프라이트와 같은 자리).
 *
 * - 동작이 바뀌면 지금 그려진 포즈에서 관성 블렌딩으로 이어진다(끊김 없음).
 * - 쉬는 동안에도 숨, 깜빡임, 꼬리, 귀 씰룩임은 계속된다.
 * - 화면이 보이지 않으면 프레임 시계가 멈춰 그리지 않는다.
 * - 접근성 "애니메이션 삭제"(MotionDurationScale 0)면 포즈를 바꾸지 않고, 동작 시간만 지켜 [onMotionFinished]를 부른다.
 *
 * @param character 그릴 고양이(스킨 + 체형 + 끼운 아이템). 바뀌면 그림을 다시 읽는다.
 * @param motion 지금 할 동작. null이면 대기(숨쉬기)만 한다.
 * @param onMotionFinished 동작이 쉬는 시간까지 끝나면 그 동작의 playId로 한 번 부른다
 * @param onShown 그림을 읽어 처음 그린 뒤 한 번 부른다(TTI)
 * @param onLoadFailed 그림을 하나도 읽지 못했을 때 부른다(부른 쪽이 스프라이트로 되돌린다)
 */
@Composable
fun CatRigView(
    character: CatRigCharacterVO,
    motion: CatRigMotion?,
    onMotionFinished: (playId: Int) -> Unit,
    modifier: Modifier = Modifier,
    onShown: () -> Unit = {},
    onLoadFailed: () -> Unit = {},
) {
    val context = LocalContext.current
    val colors = SampleArtColors.fromTheme()
    val textures by produceState<CatRigTextures?>(null, character, colors) {
        value = CatRigTextureLoader.load(context, character, colors)
    }
    val model = remember(character) { CatRigModel(character) }
    // 애니메이터는 고양이(체형, 스킨, 착용)가 바뀌어도 하나를 이어 쓴다. 새로 만들면 재생 중이던 동작이 사라져
    // 끝남 알림을 영영 못 받는다. 체형이 바뀌면 프레임 루프가 새 스켈레톤만 넘긴다(포즈와 스프링은 이어 간다).
    val animator = remember { CatRigAnimator(model.skeleton) }
    val renderer = remember { CatRigRenderer() }
    var frameTime by remember { mutableLongStateOf(0L) }
    val currentOnFinished by rememberUpdatedState(onMotionFinished)
    val currentOnShown by rememberUpdatedState(onShown)
    val currentOnFailed by rememberUpdatedState(onLoadFailed)

    LaunchedEffect(animator, motion?.playId, motion?.motion) {
        val m = motion ?: return@LaunchedEffect
        animator.play(m.motion, m.state, loopTimes = m.loopTimes, restMillis = m.restMillis)
        val playId = m.playId
        // 동작이 끝날 때까지 기다렸다 알린다(프레임 루프가 시간을 진행한다)
        while (!animator.motionFinished) withFrameNanos { }
        currentOnFinished(playId)
    }

    LaunchedEffect(model, animator) {
        animator.setSkeleton(model.skeleton)
        val reduceMotion = (coroutineContext[MotionDurationScale]?.scaleFactor ?: 1f) == 0f
        var last = withFrameNanos { it }
        model.update(animator.pose, animator.time)
        while (true) {
            withFrameNanos { now ->
                animator.advance((now - last) / NANOS_PER_SECOND)
                last = now
                if (!reduceMotion) model.update(animator.pose, animator.time)
                frameTime = now
            }
        }
    }

    val loaded = textures
    LaunchedEffect(loaded) {
        if (loaded == null) return@LaunchedEffect
        if (loaded.keys.none { it.startsWith("part:") }) {
            currentOnFailed()
        } else {
            withFrameNanos { }
            currentOnShown()
        }
    }

    Canvas(modifier = modifier) {
        frameTime // 프레임마다 다시 그린다(그리기 단계만 무효화)
        val t = loaded ?: return@Canvas
        drawIntoCanvas { canvas ->
            renderer.draw(
                canvas = canvas.nativeCanvas,
                frame = model.frame,
                textures = t,
                spec = character.spec,
                palette = character.bodyRig.palette,
                scale = size.width / RIG_SIZE,
            )
        }
    }
}

/** 리그 논리 좌표 한 변(u) */
const val RIG_SIZE = 136f
private const val NANOS_PER_SECOND = 1_000_000_000f
