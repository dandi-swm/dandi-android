package com.dandi.nyummy.cat.entity

import org.junit.Assert.assertEquals
import org.junit.Test

class CatAnimationSetVOTest {

    @Test
    fun `동작 목록의 상황 이름을 같은 이름의 상황으로 바꾸고, 모르는 이름은 UNKNOWN으로 둔다`() {
        assertEquals(CatState.HOME_DAY, CatState.from("HOME_DAY"))
        assertEquals(CatState.ROOM_IDLE, CatState.from("ROOM_IDLE"))
        assertEquals(CatState.UNKNOWN, CatState.from("SOMETHING_NEW"))
        assertEquals(CatState.UNKNOWN, CatState.from("UNKNOWN"))
        assertEquals(CatState.UNKNOWN, CatState.from(null))
    }

    @Test
    fun `동작의 되풀이 횟수는 횟수를 정한 첫 클립의 값이고, 없으면 null이다`() {
        val motion = CatMotionVO(
            name = "lie_down",
            weight = 1.0,
            clips = listOf(SpriteClipVO("a.png", 4), SpriteClipVO("b.png", 4, loop = true, loopTimes = 3), SpriteClipVO("c.png", 4, loopTimes = 5)),
        )

        assertEquals(3, motion.loopTimes)
        assertEquals(null, motion.copy(clips = listOf(SpriteClipVO("a.png", 4))).loopTimes)
    }
}
