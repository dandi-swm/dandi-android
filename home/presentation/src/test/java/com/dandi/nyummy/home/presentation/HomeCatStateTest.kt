package com.dandi.nyummy.home.presentation

import com.dandi.nyummy.cat.entity.CatAnimationVO
import com.dandi.nyummy.cat.entity.CatState
import com.dandi.nyummy.cat.entity.SpriteClipVO
import com.dandi.nyummy.cat.entity.SpriteFrameVO
import com.dandi.nyummy.common.presentation.designsystem.component.NyummySpriteClip
import com.dandi.nyummy.common.presentation.designsystem.component.NyummySpriteFrame
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeCatStateTest {

    @Test
    fun `오늘 기록이 없으면 기다림, 있으면 여유다`() {
        assertEquals(CatState.CONFLICTED_CAUTIOUS, homeCatState(hasRecordedToday = false, recordedJustNow = false, isCelebrating = false))
        assertEquals(CatState.RELAXED, homeCatState(hasRecordedToday = true, recordedJustNow = false, isCelebrating = false))
    }

    @Test
    fun `방금 기록하고 돌아왔거나 먹는 반응 중이면 먹는 반응이다`() {
        assertEquals(CatState.CONTENT, homeCatState(hasRecordedToday = true, recordedJustNow = true, isCelebrating = false))
        assertEquals(CatState.CONTENT, homeCatState(hasRecordedToday = true, recordedJustNow = false, isCelebrating = true))
    }

    private val animation = CatAnimationVO(
        state = CatState.RELAXED,
        frame = SpriteFrameVO(width = 136, height = 120, framesPerRow = 4, durationMs = 90),
        groups = listOf(
            listOf(SpriteClipVO("https://cdn/a.png", 9), SpriteClipVO("https://cdn/b.png", 8, loop = true)),
            listOf(SpriteClipVO("https://cdn/a.png", 9), SpriteClipVO("https://cdn/c.png", 13)),
        ),
        lines = listOf("쉬자"),
    )

    @Test
    fun `고른 동작 묶음을 재생 모델로 바꾸고 상태의 시트 주소를 모두 모은다`() {
        val motion = animation.toHomeCatMotion(group = 1, playId = 7, restMillis = 6_000L)

        assertEquals(
            listOf(NyummySpriteClip("https://cdn/a.png", 9), NyummySpriteClip("https://cdn/c.png", 13)),
            motion.clips,
        )
        assertEquals(NyummySpriteFrame(width = 136, height = 120, framesPerRow = 4, durationMs = 90), motion.frame)
        assertEquals(listOf("https://cdn/a.png", "https://cdn/b.png", "https://cdn/c.png"), motion.sheetUrls)
        assertEquals(7, motion.playId)
        assertEquals(6_000L, motion.restMillis)
    }

    @Test
    fun `묶음 번호가 범위를 벗어나면 첫 묶음을 재생한다`() {
        val motion = animation.toHomeCatMotion(group = 5, playId = 1, restMillis = 6_000L)

        assertEquals(listOf("https://cdn/a.png", "https://cdn/b.png"), motion.clips.map { it.url })
        assertEquals(true, motion.clips.last().loop)
    }
}
