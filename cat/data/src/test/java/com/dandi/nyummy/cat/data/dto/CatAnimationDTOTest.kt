package com.dandi.nyummy.cat.data.dto

import com.dandi.nyummy.cat.entity.CatState
import com.dandi.nyummy.cat.entity.SpriteClipVO
import com.dandi.nyummy.cat.entity.SpriteFrameVO
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CatAnimationDTOTest {

    /** 앱 네트워크 설정과 같은 Json 설정 */
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
    }

    /** 실제 응답 형식을 줄인 예시. loop가 false면 서버가 값을 빼고 보낼 수 있다. */
    private val sample = """
        {
          "weight": "NORMAL",
          "baseUrl": "https://cdn.nyummy.co.kr/cats/normal/v1/",
          "animations": [
            {
              "state": "CONTENT",
              "moment": "기록 직후 eat 리액션",
              "frame": {"width": 136, "height": 136, "framesPerRow": 4, "durationMs": 100},
              "animation": [
                [{"src": "content/eat/nyami_content_eat_01_eat_grid_136.png", "frames": 13}],
                [{"src": "content/purr/nyami_content_purr_01_purr_grid_136.png", "frames": 9, "loop": true}]
              ],
              "text": ["냠냠… 잘 먹었어!", "골골골… 배부르고 행복해"]
            },
            {
              "state": "SOMETHING_NEW",
              "moment": "앱이 아직 모르는 상태",
              "frame": {"width": 136, "height": 136, "framesPerRow": 4, "durationMs": 100},
              "animation": [[{"src": "new/a.png", "frames": 4}]],
              "text": ["안녕"]
            }
          ]
        }
    """.trimIndent()

    @Test
    fun `응답을 상태별 애니메이션으로 바꾸고 이미지 주소를 완성한다`() {
        val vo = json.decodeFromString<CatAnimationSetDTO>(sample).toVO()

        assertEquals("NORMAL", vo.weight)
        val content = vo.animations.getValue(CatState.CONTENT)
        assertEquals(SpriteFrameVO(width = 136, height = 136, framesPerRow = 4, durationMs = 100), content.frame)
        assertEquals(
            listOf(
                listOf(SpriteClipVO("https://cdn.nyummy.co.kr/cats/normal/v1/content/eat/nyami_content_eat_01_eat_grid_136.png", 13, loop = false)),
                listOf(SpriteClipVO("https://cdn.nyummy.co.kr/cats/normal/v1/content/purr/nyami_content_purr_01_purr_grid_136.png", 9, loop = true)),
            ),
            content.groups,
        )
        assertEquals(listOf("냠냠… 잘 먹었어!", "골골골… 배부르고 행복해"), content.lines)
    }

    @Test
    fun `앱이 모르는 상태는 빼고 받는다`() {
        val vo = json.decodeFromString<CatAnimationSetDTO>(sample).toVO()

        assertEquals(setOf(CatState.CONTENT), vo.animations.keys)
    }

    @Test
    fun `값이 모두 비어 있어도 빈 묶음으로 바꾼다`() {
        val vo = CatAnimationSetDTO().toVO()

        assertEquals("", vo.weight)
        assertTrue(vo.animations.isEmpty())
    }

    @Test
    fun `프레임 규격이 없거나 0 이하이면 그 상태를 뺀다`() {
        val noFrame = CatAnimationDTO(state = "CONTENT", animation = listOf(listOf(SpriteClipDTO("a.png", 4))))
        val zeroWidth = noFrame.copy(frame = SpriteFrameDTO(width = 0, height = 136, framesPerRow = 4, durationMs = 100))

        assertNull(noFrame.toVO("https://cdn/"))
        assertNull(zeroWidth.toVO("https://cdn/"))
    }

    @Test
    fun `프레임 시간이 없으면 100ms로 둔다`() {
        val frame = SpriteFrameDTO(width = 136, height = 136, framesPerRow = 4, durationMs = null).toVO()

        assertEquals(100, frame?.durationMs)
    }

    @Test
    fun `주소나 프레임 수가 없는 클립과 비어 버린 묶음은 빼고, 남은 묶음이 없으면 상태를 뺀다`() {
        val frame = SpriteFrameDTO(width = 136, height = 136, framesPerRow = 4, durationMs = 100)
        val mixed = CatAnimationDTO(
            state = "RELAXED",
            frame = frame,
            animation = listOf(
                listOf(SpriteClipDTO(src = null, frames = 4)),
                listOf(SpriteClipDTO(src = "ok.png", frames = 0), SpriteClipDTO(src = "yawn.png", frames = 13)),
            ),
        )
        val empty = mixed.copy(animation = listOf(listOf(SpriteClipDTO(src = "", frames = 4))))

        assertEquals(listOf(listOf(SpriteClipVO("https://cdn/yawn.png", 13))), mixed.toVO("https://cdn/")?.groups)
        assertNull(empty.toVO("https://cdn/"))
    }

    @Test
    fun `같은 상태가 두 번 오면 앞의 것을 쓴다`() {
        val frame = SpriteFrameDTO(width = 136, height = 136, framesPerRow = 4, durationMs = 100)
        val first = CatAnimationDTO(state = "RELAXED", frame = frame, animation = listOf(listOf(SpriteClipDTO("first.png", 4))))
        val second = first.copy(animation = listOf(listOf(SpriteClipDTO("second.png", 4))))

        val vo = CatAnimationSetDTO(baseUrl = "https://cdn/", animations = listOf(first, second)).toVO()

        assertEquals("https://cdn/first.png", vo.animations.getValue(CatState.RELAXED).groups.single().single().url)
    }

    @Test
    fun `주소는 슬래시가 겹치거나 빠지지 않게 잇고, 전체 주소는 그대로 둔다`() {
        assertEquals("https://cdn/a/b.png", joinUrl("https://cdn/a/", "/b.png"))
        assertEquals("https://cdn/a/b.png", joinUrl("https://cdn/a", "b.png"))
        assertEquals("https://other/b.png", joinUrl("https://cdn/a/", "https://other/b.png"))
        assertEquals("b.png", joinUrl("", "b.png"))
    }
}
