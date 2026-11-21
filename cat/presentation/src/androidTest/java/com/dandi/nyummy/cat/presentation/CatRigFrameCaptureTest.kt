package com.dandi.nyummy.cat.presentation

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dandi.nyummy.cat.domain.rig.CatClipLibrary
import com.dandi.nyummy.cat.entity.CatState
import org.junit.Assert.assertTrue
import org.junit.runner.RunWith
import org.junit.Test

/**
 * 연속 프레임 캡처(기준서 v1.2 5-1 표준 캡처 + 상태별, 전환별, 착용별, 스킨별). 그림은 실제 화면과 같은 렌더러로 그린다.
 * 결과: 앱 외부 파일 폴더 `cat-rigging-frames/`(adb pull로 가져간다).
 *
 * 파일 이름 규칙
 * - std1_bodies_strip: 체형 5종 정렬(발바닥 y=118, 눈 y=57, 대칭축 x=64 보조선)
 * - std2_slots_<체형>: 착용 4슬롯 x OVER_EARS/BETWEEN_EARS
 * - std3_seq_<클립>: 100ms 간격 연속 프레임
 * - std4_xfade_<from>_to_<to>: 블렌드 0/25/50/75/100%
 * - state_<스킨>_<체형>_<상황>: 상황별 대표 동작 100ms 간격 8컷
 * - wear_<스킨>_<체형>_<착용>_<동작>: 착용 추종 연속 프레임
 * - motion_<스킨>_<동작>: NORMAL 체형 동작 48종 전부
 */
@RunWith(AndroidJUnit4::class)
class CatRigFrameCaptureTest {

    private val full = listOf("sample_beanie", "sample_vest", "sample_bell_collar", "sample_glasses")
    private val fullBetween = listOf("sample_crown", "sample_vest", "sample_bell_collar", "sample_glasses")

    @Test
    fun std1_체형5종_정렬() {
        val sheet = RigCapture.Sheet(columns = 5, rows = 1, cell = 300)
        RigCapture.BODIES.forEachIndexed { i, body ->
            val rig = RigCapture.Rig(RigCapture.character("nyami", body))
            sheet.put(i, 0, rig, body, guides = true)
        }
        assertTrue(sheet.save("std1_bodies_strip").exists())
    }

    @Test
    fun std2_착용4슬롯_극단체형() {
        for (body in listOf("LEAN", "NORMAL", "PLUMP")) {
            val sheet = RigCapture.Sheet(columns = 3, rows = 1, cell = 320)
            sheet.put(0, 0, RigCapture.Rig(RigCapture.character("nyami", body)), "$body 기본")
            sheet.put(1, 0, RigCapture.Rig(RigCapture.character("nyami", body, full)), "OVER_EARS 전부")
            sheet.put(2, 0, RigCapture.Rig(RigCapture.character("nyami", body, fullBetween)), "BETWEEN_EARS 전부")
            sheet.save("std2_slots_$body")
        }
    }

    @Test
    fun std3_핵심동작_연속프레임() {
        val clips = listOf(
            Triple(CatClipLibrary.IDLE, CatState.HOME_DAY, 8),
            Triple(CatClipLibrary.TAP_REACT, CatState.HOME_TAP, 7),
            Triple(CatClipLibrary.GREET_WAVE, CatState.HOME_GREET, 8),
            Triple(CatClipLibrary.SLEEP_LOOP, CatState.HOME_NIGHT, 8),
            Triple(CatClipLibrary.TAP_IRRITATED, CatState.HOME_TAP_BURST, 8),
            Triple(CatClipLibrary.EAT_HAPPY, CatState.HOME_AFTER_MEAL, 8),
            Triple(CatClipLibrary.DROWSY_DOZE, CatState.HOME_DROWSY, 8),
        )
        for ((clip, state, count) in clips) {
            val rig = RigCapture.Rig(RigCapture.character("nyami", "NORMAL"))
            rig.advance(1f)
            val c = CatClipLibrary.clips.getValue(clip)
            rig.animator.playClip(c, loopTimes = 2)
            // 자는 동작은 되풀이 구간(숨)을 찍는다
            if (clip == CatClipLibrary.SLEEP_LOOP) rig.advance(c.intro.duration + 0.4f)
            val sheet = RigCapture.Sheet(columns = count, rows = 1, cell = 272)
            for (i in 0 until count) {
                sheet.put(i, 0, rig, "${i * 100}ms")
                rig.advance(0.1f)
            }
            sheet.save("std3_seq_$clip")
        }
    }

    @Test
    fun std4_전환_블렌드_5컷() {
        val cases = listOf(
            Triple(CatClipLibrary.IDLE, CatClipLibrary.TAP_REACT, "idle"),
            Triple(CatClipLibrary.IDLE, CatClipLibrary.SLEEP_LOOP, "idle"),
            Triple(CatClipLibrary.SLEEP_LOOP, CatClipLibrary.TAP_REACT, "sleeping"),
            Triple(CatClipLibrary.DROWSY_DOZE, CatClipLibrary.TAP_IRRITATED, "drowsy"),
        )
        for ((from, to, _) in cases) {
            val rig = RigCapture.Rig(RigCapture.character("nyami", "NORMAL", fullBetween))
            rig.advance(0.5f)
            rig.animator.playClip(CatClipLibrary.clips.getValue(from), loopTimes = 3)
            rig.advance(2.2f)
            rig.animator.playClip(CatClipLibrary.clips.getValue(to))
            val blend = rig.animator.blendDurationSeconds
            val sheet = RigCapture.Sheet(columns = 5, rows = 1, cell = 272)
            var t = 0f
            for (i in 0 until 5) {
                val target = blend * i / 4f
                rig.advance(target - t)
                t = target
                sheet.put(i, 0, rig, "${i * 25}% (${(target * 1000).toInt()}ms)")
            }
            sheet.save("std4_xfade_${from}_to_$to")
        }
    }

    @Test
    fun 상황별_스킨별_체형별_연속프레임() {
        for (skin in listOf("nyami", "mackerel")) for (body in RigCapture.BODIES) {
            val set = RigCapture.animations(body)
            for ((state, animation) in set.animations) {
                val motion = animation.motions.first()
                val rig = RigCapture.Rig(RigCapture.character(skin, body))
                rig.advance(0.6f)
                rig.animator.play(motion.name, state, loopTimes = 1)
                val sheet = RigCapture.Sheet(columns = 8, rows = 1, cell = 200)
                for (i in 0 until 8) {
                    sheet.put(i, 0, rig, if (i == 0) "${motion.name} ${i * 150}ms" else "${i * 150}ms")
                    rig.advance(0.15f)
                }
                sheet.save("state_${skin}_${body}_$state")
            }
        }
    }

    @Test
    fun 착용_추종_연속프레임() {
        val motions = listOf("pop" to CatState.HOME_TAP, "head_tilt" to CatState.HOME_TAP, "doze" to CatState.HOME_DROWSY)
        for (skin in listOf("nyami", "mackerel")) for (body in listOf("LEAN", "NORMAL", "PLUMP")) {
            for ((outfit, ids) in listOf("over" to full, "between" to fullBetween)) for ((motion, state) in motions) {
                val rig = RigCapture.Rig(RigCapture.character(skin, body, ids))
                rig.advance(0.6f)
                rig.animator.play(motion, state, loopTimes = 1)
                val sheet = RigCapture.Sheet(columns = 8, rows = 1, cell = 200)
                for (i in 0 until 8) {
                    sheet.put(i, 0, rig, "${i * 100}ms")
                    rig.advance(0.1f)
                }
                sheet.save("wear_${skin}_${body}_${outfit}_$motion")
            }
        }
    }

    @Test
    fun 동작48종_전부() {
        val set = RigCapture.animations("NORMAL")
        val names = set.animations.values.flatMap { a -> a.motions.map { it.name } }.distinct()
        for (skin in listOf("nyami")) for (name in names) {
            val rig = RigCapture.Rig(RigCapture.character(skin, "NORMAL"))
            rig.advance(0.6f)
            rig.animator.play(name, loopTimes = 1)
            val sheet = RigCapture.Sheet(columns = 8, rows = 1, cell = 180)
            for (i in 0 until 8) {
                sheet.put(i, 0, rig, "${i * 200}ms")
                rig.advance(0.2f)
            }
            sheet.save("motion_${skin}_$name")
        }
    }
}
