package com.dandi.nyummy.common.presentation.coachmark

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTutorialExpression

/** 코치마크가 가리킬 수 있는 대상의 이름. 상수는 그 대상을 가진 모듈에 둔다. */
@JvmInline
value class CoachMarkKey(val id: String)

/**
 * 지금 띄울 코치마크 한 장. 화면이 [ShowCoachMark]로 올린다.
 *
 * @property target 밝게 비울 대상. null이면 대상 없이 카드만 보이는 소개 단계다.
 * @property anchor 대상이 없을 때 카드를 이 대상 바로 위에 둔다(예: 바텀 네비, 보관함). null이면 화면 아래.
 * @property waitsForTarget true면 카드를 눌러도 넘어가지 않고 대상을 눌러야 진행된다.
 * @property onAdvance 카드를 눌렀다(대사 넘김). [waitsForTarget]이면 부르지 않는다.
 * @property onTargetClick 밝게 비운 대상을 눌렀다. 대상이 등록한 동작 뒤에 부른다.
 * @property blocksTouches false면 화면을 덮지 않고 터치도 막지 않는다. 대상에 빛 링과 발바닥, 카드만 얹어
 *   사용자가 아래 화면을 직접 만지게 한다(예: 마이룸에서 놓은 아이템을 움직여 보며 저장하기). 대상은 원래 클릭 그대로 눌린다.
 * @property onSkip 건너뛰기. null이면 건너뛸 수 없는 단계라 건너뛰기를 그리지 않는다. 대상이 건너뛰기 자리(화면 오른쪽 위)와
 *   겹쳐도 그리지 않는다.
 *
 * 뒤로 가기는 가로채지 않는다. 화면이 평소처럼 처리하고, 단계는 마친 것으로 남기 전이라 다음에 다시 뜬다.
 */
@Immutable
data class CoachMark(
    val title: String,
    val expression: NyummyTutorialExpression,
    val target: CoachMarkKey? = null,
    val body: String? = null,
    val anchor: CoachMarkKey? = null,
    val waitsForTarget: Boolean = target != null,
    val onAdvance: () -> Unit = {},
    val onTargetClick: () -> Unit = {},
    val onSkip: (() -> Unit)? = null,
    val blocksTouches: Boolean = true,
)

/** 코치마크 대상 하나. 루트 기준 영역과, 대상을 눌렀을 때 하던 동작. */
internal class CoachMarkTarget(val bounds: Rect, val onClick: () -> Unit)

/**
 * 코치마크 대상과 지금 띄울 코치마크를 갖는 루트의 상태. 루트가 [LocalCoachMarkHost]로 내려 주고 맨 위 층에서 그린다.
 * 화면 전환이 없어 한 번에 한 화면의 코치마크만 뜬다.
 */
@Stable
class CoachMarkHost {
    internal val targets = mutableStateMapOf<CoachMarkKey, CoachMarkTarget>()

    var mark: CoachMark? by mutableStateOf(null)
        internal set
}

/** 루트가 내려 주는 코치마크 호스트. 없으면(미리보기, 단독 화면) 대상 등록과 코치마크가 아무 일도 하지 않는다. */
val LocalCoachMarkHost = staticCompositionLocalOf<CoachMarkHost?> { null }

/**
 * 이 요소를 코치마크 대상 [key]로 등록한다. 코치마크가 이곳을 비우고, 비운 곳을 누르면 [onClick]을 부른다.
 * [onClick]은 원래 이 요소를 눌렀을 때의 동작을 그대로 넘긴다. 그래야 코치마크가 다른 동작을 다시 만들지 않는다.
 */
@Composable
fun Modifier.coachMarkTarget(key: CoachMarkKey, onClick: () -> Unit): Modifier {
    val host = LocalCoachMarkHost.current ?: return this
    val latestOnClick by rememberUpdatedState(onClick)
    DisposableEffect(host, key) {
        onDispose { host.targets.remove(key) }
    }
    return onGloballyPositioned { coordinates ->
        val bounds = coordinates.boundsInRoot()
        if (host.targets[key]?.bounds != bounds) host.targets[key] = CoachMarkTarget(bounds) { latestOnClick() }
    }
}

/** [mark]를 지금 띄울 코치마크로 올린다. null이거나 이 화면이 사라지면 내린다. */
@Composable
fun ShowCoachMark(mark: CoachMark?) {
    val host = LocalCoachMarkHost.current ?: return
    DisposableEffect(host, mark) {
        host.mark = mark
        onDispose { if (host.mark == mark) host.mark = null }
    }
}
