package com.dandi.nyummy.cat.data.dto

import com.dandi.nyummy.cat.entity.CatAnimationSetVO
import com.dandi.nyummy.cat.entity.CatAnimationVO
import com.dandi.nyummy.cat.entity.CatMotionVO
import com.dandi.nyummy.cat.entity.CatState
import com.dandi.nyummy.cat.entity.SpriteClipVO
import com.dandi.nyummy.cat.entity.SpriteFrameVO
import kotlinx.serialization.Serializable

/**
 * 앱 에셋 `cats/<체형>/animations.json`. `tools/cat-assets/export_app_cats.py`가 만든다.
 *
 * @property animations 상황별 애니메이션
 */
@Serializable
data class CatAnimationSetDTO(
    val animations: List<CatAnimationDTO>? = null,
) {
    /**
     * [baseUrl]은 그 체형 에셋 폴더 주소다. 앱이 모르는 상황, 프레임 규격이 없는 상황, 고를 동작이 없는 상황은 뺀다.
     * 같은 상황이 두 번 오면 앞의 것을 쓴다.
     */
    fun toVO(weight: String, baseUrl: String): CatAnimationSetVO = CatAnimationSetVO(
        weight = weight,
        animations = animations.orEmpty()
            .mapNotNull { it.toVO(baseUrl) }
            .distinctBy { it.state }
            .associateBy { it.state },
    )
}

/**
 * @property state 상황 이름
 * @property frame 프레임 규격
 * @property motions 이 상황에서 고를 동작
 */
@Serializable
data class CatAnimationDTO(
    val state: String? = null,
    val frame: SpriteFrameDTO? = null,
    val motions: List<CatMotionDTO>? = null,
) {
    fun toVO(baseUrl: String): CatAnimationVO? {
        val catState = CatState.from(state)
        if (catState == CatState.UNKNOWN) return null
        val spriteFrame = frame?.toVO() ?: return null
        val picked = motions.orEmpty().mapNotNull { it.toVO(baseUrl) }.distinctBy { it.name }
        if (picked.isEmpty()) return null
        return CatAnimationVO(state = catState, frame = spriteFrame, motions = picked)
    }
}

/**
 * @property name 동작 이름
 * @property weight 이 상황에서 나올 비율. 0이면 고르지 않는다.
 * @property clips 이어서 재생할 시트
 */
@Serializable
data class CatMotionDTO(
    val name: String? = null,
    val weight: Double? = null,
    val clips: List<SpriteClipDTO>? = null,
) {
    /** 이름이나 클립이 없거나 가중치가 0 이하면 고를 수 없으므로 버린다. */
    fun toVO(baseUrl: String): CatMotionVO? {
        val motionName = name?.takeIf { it.isNotBlank() } ?: return null
        val motionWeight = weight?.takeIf { it > 0 } ?: return null
        val sheets = clips.orEmpty().mapNotNull { it.toVO(baseUrl) }
        if (sheets.isEmpty()) return null
        return CatMotionVO(name = motionName, weight = motionWeight, clips = sheets)
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
 * @property src 체형 에셋 폴더 기준 상대 경로
 * @property frames 시트에 든 프레임 수
 * @property loop 되풀이해도 되는 구간인지. false면 빠질 수 있다.
 * @property loopTimes 가운데 되풀이 구간을 몇 번 돌지. 없으면 재생 기본값이다.
 */
@Serializable
data class SpriteClipDTO(
    val src: String? = null,
    val frames: Int? = null,
    val loop: Boolean? = null,
    val loopTimes: Int? = null,
) {
    fun toVO(baseUrl: String): SpriteClipVO? {
        val path = src?.takeIf { it.isNotBlank() } ?: return null
        val count = frames?.takeIf { it > 0 } ?: return null
        return SpriteClipVO(
            url = joinUrl(baseUrl, path),
            frames = count,
            loop = loop ?: false,
            loopTimes = loopTimes?.takeIf { it > 0 },
        )
    }
}

/** 슬래시가 겹치거나 빠지지 않게 잇는다. */
internal fun joinUrl(baseUrl: String, path: String): String = when {
    baseUrl.isEmpty() -> path
    else -> baseUrl.trimEnd('/') + "/" + path.trimStart('/')
}
