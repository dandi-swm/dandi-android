package com.dandi.nyummy.cat.domain.rig

import com.dandi.nyummy.cat.entity.rig.Interp
import com.dandi.nyummy.cat.entity.rig.RigChannel
import com.dandi.nyummy.cat.entity.rig.RigClip
import com.dandi.nyummy.cat.entity.rig.RigKey
import com.dandi.nyummy.cat.entity.rig.RigSegment
import com.dandi.nyummy.cat.entity.rig.RigTrack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/** 클립 계약: 휴지 자세에서 시작해 휴지 자세로 끝나고, 구간 이음매와 되풀이 이음매가 값과 기울기까지 이어진다(기준서 4-3). */
class RigClipContractTest {

    private val clips = CatClipLibrary.clips.values

    private fun value(segment: RigSegment, ch: RigChannel, t: Float): Float =
        segment.tracks[ch]?.let { RigMath.evaluate(it, t) } ?: ch.rest

    private fun slope(segment: RigSegment, ch: RigChannel, t: Float, dir: Int): Float {
        val h = 2e-4f
        return if (dir > 0) (value(segment, ch, t + h) - value(segment, ch, t)) / h else (value(segment, ch, t) - value(segment, ch, t - h)) / h
    }

    private fun tolerance(ch: RigChannel): Float = when {
        ch.angle -> ANGLE_TOLERANCE
        ch.rest == 1f -> SCALE_TOLERANCE
        else -> POSITION_TOLERANCE
    }

    @Test
    fun `모든 클립은 휴지 자세에서 시작해 휴지 자세로 끝난다`() {
        for (clip in clips) for (ch in RigChannel.entries) {
            val start = value(clip.intro, ch, 0f)
            assertEquals("${clip.name}.$ch 시작", ch.rest, start, tolerance(ch))
            val last = clip.outro ?: if (clip.loop == null) clip.intro else null
            if (last != null) assertEquals("${clip.name}.$ch 끝", ch.rest, value(last, ch, last.duration), tolerance(ch))
        }
    }

    @Test
    fun `구간 이음매(들어가기, 되풀이, 돌아오기)는 값과 기울기가 이어진다`() {
        for (clip in clips) {
            val segments = listOfNotNull(clip.intro, clip.loop, clip.outro)
            val joints = segments.zipWithNext().toMutableList()
            clip.loop?.let { joints += it to it } // 되풀이 이음매
            for ((a, b) in joints) for (ch in RigChannel.entries) {
                val end = value(a, ch, a.duration)
                val begin = value(b, ch, 0f)
                assertEquals("${clip.name}.$ch 이음매 값", end, begin, tolerance(ch))
                val sa = slope(a, ch, a.duration, -1)
                val sb = slope(b, ch, 0f, +1)
                assertTrue("${clip.name}.$ch 이음매 기울기 $sa -> $sb", abs(sa - sb) <= SLOPE_TOLERANCE)
            }
        }
    }

    @Test
    fun `클립에 계단형 보간은 없고 키는 시각 순서다`() {
        for (clip in clips) for (seg in listOfNotNull(clip.intro, clip.loop, clip.outro)) for ((ch, track) in seg.tracks) {
            track.keys.zipWithNext().forEach { (a, b) -> assertTrue("${clip.name}.$ch 키 순서", b.time >= a.time) }
            assertTrue("${clip.name}.$ch 키가 구간 밖", track.keys.last().time <= seg.duration + 1e-4f)
        }
    }

    @Test
    fun `Hermite 보간은 가운데 키에서 기울기가 이어지고 끝 키에서 멈춘다`() {
        val track = RigTrack(listOf(RigKey(0f, 0f), RigKey(1f, 1f, Interp.HERMITE), RigKey(2f, 0f, Interp.HERMITE)))
        val h = 1e-3f
        val left = (RigMath.evaluate(track, 1f) - RigMath.evaluate(track, 1f - h)) / h
        val right = (RigMath.evaluate(track, 1f + h) - RigMath.evaluate(track, 1f)) / h
        assertEquals(left, right, 0.01f)
        val end = (RigMath.evaluate(track, 2f) - RigMath.evaluate(track, 2f - h)) / h
        assertEquals(0f, end, 0.01f)
    }

    @Test
    fun `각도는 최단 호로 접는다`() {
        assertEquals(-0.2f, RigMath.wrapAngle((2 * Math.PI).toFloat() - 0.2f), 1e-4f)
        assertEquals(0.3f, RigMath.wrapAngle(0.3f), 1e-6f)
    }

    @Test
    fun `근사 클립은 설계서의 11종이다`() {
        val approximate = clips.filter(RigClip::approximate).map { it.name }.toSet()
        val expected = CatClipLibrary.approximateMotions.map { if (it == "lie_down") CatClipLibrary.SLEEP_LOOP else it }.toSet()
        assertEquals(expected, approximate)
        assertEquals(11, CatClipLibrary.approximateMotions.size)
    }

    private companion object {
        // 기준서 4-3: 위치 0.1u, 각도 0.2도, 배율 0.002
        const val POSITION_TOLERANCE = 0.1f
        const val ANGLE_TOLERANCE = 0.0035f
        const val SCALE_TOLERANCE = 0.002f
        const val SLOPE_TOLERANCE = 0.05f
    }
}
