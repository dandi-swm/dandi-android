package com.dandi.nyummy.cat.data.dto

import com.dandi.nyummy.cat.entity.CatAnimationSetVO
import com.dandi.nyummy.cat.entity.CatAnimationVO
import com.dandi.nyummy.cat.entity.CatState
import com.dandi.nyummy.cat.entity.SpriteClipVO
import com.dandi.nyummy.cat.entity.SpriteFrameVO
import kotlinx.serialization.Serializable

/**
 * GET /api/v1/cats/animations 응답.
 *
 * @property weight 체형 이름
 * @property baseUrl 스프라이트 이미지 주소의 앞부분. 각 클립의 `src`를 이어 붙이면 이미지 주소가 된다.
 * @property animations 상태별 애니메이션
 */
@Serializable
data class CatAnimationSetDTO(
    val weight: String? = null,
    val baseUrl: String? = null,
    val animations: List<CatAnimationDTO>? = null,
) {
    /** 앱이 모르는 상태, 프레임 규격이 없는 상태, 재생할 클립이 없는 상태는 뺀다. 같은 상태가 두 번 오면 앞의 것을 쓴다. */
    fun toVO(): CatAnimationSetVO = CatAnimationSetVO(
        weight = weight.orEmpty(),
        animations = animations.orEmpty()
            .mapNotNull { it.toVO(baseUrl.orEmpty()) }
            .distinctBy { it.state }
            .associateBy { it.state },
    )
}

/**
 * @property state 상태 이름
 * @property moment 이 상태를 보여 줄 상황 설명(서버 참고용, 앱에서는 쓰지 않는다)
 * @property frame 프레임 규격
 * @property animation 동작 묶음 목록. 묶음 하나는 순서대로 재생할 클립 목록이다.
 * @property text 대사
 */
@Serializable
data class CatAnimationDTO(
    val state: String? = null,
    val moment: String? = null,
    val frame: SpriteFrameDTO? = null,
    val animation: List<List<SpriteClipDTO>>? = null,
    val text: List<String>? = null,
) {
    fun toVO(baseUrl: String): CatAnimationVO? {
        val catState = CatState.from(state)
        if (catState == CatState.UNKNOWN) return null
        val spriteFrame = frame?.toVO() ?: return null
        val groups = animation.orEmpty()
            .map { group -> group.mapNotNull { it.toVO(baseUrl) } }
            .filter { it.isNotEmpty() }
        if (groups.isEmpty()) return null
        return CatAnimationVO(
            state = catState,
            frame = spriteFrame,
            groups = groups,
            lines = text.orEmpty().filter { it.isNotBlank() },
        )
    }
}

/**
 * @property width 한 프레임의 가로 픽셀
 * @property height 한 프레임의 세로 픽셀
 * @property framesPerRow 시트 한 줄의 프레임 수
 * @property durationMs 한 프레임을 보여 주는 시간
 */
@Serializable
data class SpriteFrameDTO(
    val width: Int? = null,
    val height: Int? = null,
    val framesPerRow: Int? = null,
    val durationMs: Int? = null,
) {
    /** 크기를 모르면 그릴 수 없으므로 버린다. 프레임 시간이 없으면 기본값을 쓴다. */
    fun toVO(): SpriteFrameVO? {
        val w = width?.takeIf { it > 0 } ?: return null
        val h = height?.takeIf { it > 0 } ?: return null
        val perRow = framesPerRow?.takeIf { it > 0 } ?: return null
        return SpriteFrameVO(
            width = w,
            height = h,
            framesPerRow = perRow,
            durationMs = durationMs?.takeIf { it > 0 } ?: DEFAULT_FRAME_DURATION_MS,
        )
    }

    private companion object {
        const val DEFAULT_FRAME_DURATION_MS = 100
    }
}

/**
 * @property src [CatAnimationSetDTO.baseUrl] 기준 상대 경로
 * @property frames 시트에 든 프레임 수
 * @property loop 되풀이해도 되는 구간인지. 서버는 false일 때 이 값을 빼고 보낼 수 있다.
 */
@Serializable
data class SpriteClipDTO(
    val src: String? = null,
    val frames: Int? = null,
    val loop: Boolean? = null,
) {
    fun toVO(baseUrl: String): SpriteClipVO? {
        val path = src?.takeIf { it.isNotBlank() } ?: return null
        val count = frames?.takeIf { it > 0 } ?: return null
        return SpriteClipVO(url = joinUrl(baseUrl, path), frames = count, loop = loop ?: false)
    }
}

/** 슬래시가 겹치거나 빠지지 않게 잇는다. `src`가 이미 전체 주소면 그대로 쓴다. */
internal fun joinUrl(baseUrl: String, path: String): String = when {
    path.startsWith("http://") || path.startsWith("https://") -> path
    baseUrl.isEmpty() -> path
    else -> baseUrl.trimEnd('/') + "/" + path.trimStart('/')
}
