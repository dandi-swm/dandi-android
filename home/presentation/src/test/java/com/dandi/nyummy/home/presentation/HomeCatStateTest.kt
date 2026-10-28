package com.dandi.nyummy.home.presentation

import com.dandi.nyummy.cat.domain.CatPick
import com.dandi.nyummy.cat.entity.CatMotionVO
import com.dandi.nyummy.cat.entity.CatState
import com.dandi.nyummy.cat.entity.SpriteClipVO
import com.dandi.nyummy.cat.entity.SpriteFrameVO
import com.dandi.nyummy.common.presentation.designsystem.component.NyummySpriteClip
import com.dandi.nyummy.common.presentation.designsystem.component.NyummySpriteSheet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HomeCatStateTest {

    private val pick = CatPick(
        state = CatState.HOME_DAY,
        frame = SpriteFrameVO(width = 136, height = 120, framesPerRow = 4, durationMs = 90),
        motion = CatMotionVO(
            name = "lie_down",
            weight = 1.0,
            clips = listOf(
                SpriteClipVO("https://cdn/a.png", 9),
                SpriteClipVO("https://cdn/b.png", 8, loop = true, loopTimes = 4),
            ),
        ),
    )

    /** 칸 규격(136x120, 한 줄 4칸, 90ms)을 담은 시트. */
    private fun sheet(url: String, frames: Int) = NyummySpriteSheet(
        url = url,
        frameWidth = 136,
        frameHeight = 120,
        totalFrames = frames,
        framesPerRow = 4,
        frameDurationMillis = 90,
    )

    @Test
    fun `고른 동작을 재생 모델로 바꾸고 되풀이 횟수는 동작이 정한 값을 쓴다`() {
        val motion = pick.toHomeCatMotion(playId = 7, restMillis = 6_000L)

        assertEquals(
            listOf(NyummySpriteClip(sheet("https://cdn/a.png", 9)), NyummySpriteClip(sheet("https://cdn/b.png", 8), loop = true)),
            motion.clips,
        )
        assertEquals(listOf("https://cdn/a.png", "https://cdn/b.png"), motion.sheetUrls)
        assertEquals(7, motion.playId)
        assertEquals(6_000L, motion.restMillis)
        assertEquals(4, motion.loopTimes)
    }

    @Test
    fun `다음 동작의 시트 주소도 겹치지 않게 함께 모은다`() {
        val motion = pick.toHomeCatMotion(playId = 1, restMillis = 0L, prefetchUrls = listOf("https://cdn/b.png", "https://cdn/c.png"))

        assertEquals(listOf("https://cdn/a.png", "https://cdn/b.png", "https://cdn/c.png"), motion.sheetUrls)
    }

    @Test
    fun `되풀이 횟수를 정하지 않은 동작은 재생 기본값을 쓴다`() {
        val motion = pick.copy(motion = pick.motion.copy(clips = listOf(SpriteClipVO("https://cdn/a.png", 9))))
            .toHomeCatMotion(playId = 1, restMillis = 0L)

        assertNull(motion.loopTimes)
    }
}
