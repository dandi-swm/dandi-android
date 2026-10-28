package com.dandi.nyummy.cat.data.dto

import com.dandi.nyummy.cat.entity.CatState
import com.dandi.nyummy.cat.entity.SpriteClipVO
import com.dandi.nyummy.cat.entity.SpriteFrameVO
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CatAnimationDTOTest {

    /** 앱 네트워크 설정과 같은 Json 설정 */
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
    }

    private val baseUrl = "file:///android_asset/cats/NORMAL/"

    /** 앱 에셋 `animations.json` 형식을 줄인 예시. loop가 false면 값이 빠질 수 있다. */
    private val sample = """
        {
          "animations": [
            {
              "state": "HOME_DAY",
              "frame": {"width": 136, "height": 120, "framesPerRow": 4, "durationMs": 90},
              "motions": [
                {"name": "lie_down", "weight": 2.0, "clips": [
                  {"src": "home_day/lie_in.png", "frames": 9},
                  {"src": "/home_day/lie_loop.png", "frames": 8, "loop": true, "loopTimes": 3}
                ]},
                {"name": "zero", "weight": 0, "clips": [{"src": "zero.png", "frames": 4}]}
              ]
            },
            {"state": "SOMETHING_NEW", "frame": {"width": 1, "height": 1, "framesPerRow": 1}, "motions": [{"name": "x", "weight": 1, "clips": [{"src": "x.png", "frames": 1}]}]}
          ]
        }
    """.trimIndent()

    private fun parse(raw: String) = json.decodeFromString<CatAnimationSetDTO>(raw).toVO("NORMAL", baseUrl)

    @Test
    fun `동작 목록을 상황별 애니메이션으로 바꾸고 시트 주소를 체형 폴더 기준으로 완성한다`() {
        val set = parse(sample)

        assertEquals("NORMAL", set.weight)
        assertEquals(setOf(CatState.HOME_DAY), set.animations.keys)
        val day = set.animations.getValue(CatState.HOME_DAY)
        assertEquals(SpriteFrameVO(width = 136, height = 120, framesPerRow = 4, durationMs = 90), day.frame)
        val motion = day.motions.single()
        assertEquals("lie_down", motion.name)
        assertEquals(2.0, motion.weight, 0.0)
        assertEquals(
            listOf(
                SpriteClipVO(url = "${baseUrl}home_day/lie_in.png", frames = 9),
                SpriteClipVO(url = "${baseUrl}home_day/lie_loop.png", frames = 8, loop = true, loopTimes = 3),
            ),
            motion.clips,
        )
        assertEquals(3, motion.loopTimes)
    }

    @Test
    fun `값이 모두 비어 있으면 빈 애니메이션이다`() {
        assertTrue(parse("{}").animations.isEmpty())
    }

    @Test
    fun `프레임 규격이 없거나 0 이하이면 그 상황을 빼고, 프레임 시간이 없으면 100ms로 둔다`() {
        assertEquals(null, SpriteFrameDTO(width = 0, height = 10, framesPerRow = 1).toVO())
        assertEquals(null, SpriteFrameDTO(width = 10, height = 10).toVO())
        assertEquals(100, SpriteFrameDTO(width = 10, height = 10, framesPerRow = 1).toVO()?.durationMs)
        assertEquals(null, CatAnimationDTO(state = "HOME_DAY", motions = listOf(motionDTO("a"))).toVO(baseUrl))
    }

    @Test
    fun `주소나 프레임 수가 없는 클립은 빼고, 클립이 없거나 이름이 없거나 가중치가 0 이하인 동작은 뺀다`() {
        assertEquals(null, SpriteClipDTO(src = "", frames = 4).toVO(baseUrl))
        assertEquals(null, SpriteClipDTO(src = "a.png", frames = 0).toVO(baseUrl))
        assertEquals(null, CatMotionDTO(name = "a", weight = 1.0, clips = listOf(SpriteClipDTO(src = "a.png"))).toVO(baseUrl))
        assertEquals(null, motionDTO("").toVO(baseUrl))
        assertEquals(null, motionDTO("a", weight = -1.0).toVO(baseUrl))
    }

    @Test
    fun `고를 동작이 없는 상황은 빼고, 같은 상황이 두 번 오면 앞의 것을 쓰고, 같은 이름의 동작도 앞의 것을 쓴다`() {
        val frame = SpriteFrameDTO(width = 10, height = 10, framesPerRow = 1)
        val set = CatAnimationSetDTO(
            animations = listOf(
                CatAnimationDTO(state = "HOME_TAP", frame = frame, motions = listOf(motionDTO("a", weight = 0.0))),
                CatAnimationDTO(state = "HOME_DAY", frame = frame, motions = listOf(motionDTO("first"), motionDTO("first", weight = 5.0))),
                CatAnimationDTO(state = "HOME_DAY", frame = frame, motions = listOf(motionDTO("second"))),
            ),
        ).toVO("NORMAL", baseUrl)

        assertEquals(setOf(CatState.HOME_DAY), set.animations.keys)
        val motions = set.animations.getValue(CatState.HOME_DAY).motions
        assertEquals(listOf("first"), motions.map { it.name })
        assertEquals(1.0, motions.single().weight, 0.0)
    }

    @Test
    fun `주소는 슬래시가 겹치거나 빠지지 않게 잇는다`() {
        assertEquals("a/b.png", joinUrl("a/", "/b.png"))
        assertEquals("a/b.png", joinUrl("a", "b.png"))
        assertEquals("b.png", joinUrl("", "b.png"))
    }

    private fun motionDTO(name: String, weight: Double = 1.0) =
        CatMotionDTO(name = name, weight = weight, clips = listOf(SpriteClipDTO(src = "$name.png", frames = 4)))
}
