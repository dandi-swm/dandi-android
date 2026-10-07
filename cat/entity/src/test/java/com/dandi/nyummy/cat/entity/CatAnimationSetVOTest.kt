package com.dandi.nyummy.cat.entity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CatAnimationSetVOTest {

    private fun animation(state: CatState) = CatAnimationVO(
        state = state,
        frame = SpriteFrameVO(width = 136, height = 136, framesPerRow = 4, durationMs = 100),
        groups = listOf(listOf(SpriteClipVO(url = "https://cdn/${state.name}.png", frames = 4))),
        lines = listOf(state.name),
    )

    @Test
    fun `서버 상태 이름을 같은 이름의 상태로 바꾸고, 모르는 이름은 UNKNOWN으로 둔다`() {
        assertEquals(CatState.CONFLICTED_CAUTIOUS, CatState.from("CONFLICTED_CAUTIOUS"))
        assertEquals(CatState.UNKNOWN, CatState.from("SOMETHING_NEW"))
        assertEquals(CatState.UNKNOWN, CatState.from("UNKNOWN"))
        assertEquals(CatState.UNKNOWN, CatState.from(null))
    }

    @Test
    fun `고른 상태가 있으면 그 애니메이션을 준다`() {
        val set = CatAnimationSetVO(animations = mapOf(CatState.RELAXED to animation(CatState.RELAXED), CatState.FRIENDLY to animation(CatState.FRIENDLY)))

        assertEquals(CatState.RELAXED, set.animationFor(CatState.RELAXED)?.state)
    }

    @Test
    fun `고른 상태가 없으면 기본 인사 상태로 대신하고, 그것도 없으면 null이다`() {
        val withFriendly = CatAnimationSetVO(animations = mapOf(CatState.FRIENDLY to animation(CatState.FRIENDLY)))

        assertEquals(CatState.FRIENDLY, withFriendly.animationFor(CatState.CONTENT)?.state)
        assertNull(CatAnimationSetVO.empty.animationFor(CatState.CONTENT))
    }
}
