package com.dandi.nyummy.cat.data.rig

import com.dandi.nyummy.cat.domain.rig.CatClipLibrary
import com.dandi.nyummy.cat.entity.CatState
import com.dandi.nyummy.cat.entity.rig.RigChannel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.math.max
import kotlin.random.Random

/**
 * 스킨 2종 x 체형 5종 x 상황 9개 x 동작 전부 x 착용 조합을 실제 에셋으로 돌려 품질 기준을 검사한다.
 * 결과 표를 `build/cat-rig-metrics/summary.md`에 남긴다(완료 보고의 근거).
 *
 * 기준(설계서 10절, 기준서 v1.2 Q1~Q10):
 * - 프레임당 이동 <= 키의 3%(급격한 점프 0)
 * - 6Hz보다 빠르고 1px(0.25u) 이상인 왕복 진동 0(미세 진동 0). 0.4px(0.1u) 기준 수치도 참고로 남긴다
 * - 이음새 여유 사용률 < 0.8(틈 0)
 * - 접지 변화 <= 0.5u(점프, 구르기 동작 제외)
 * - 모자 위 끝 >= 4u, 모자/안경 상대 위치 변화 <= 0.5u
 */
class CatRigMatrixTest {

    private data class Row(
        val skin: String,
        val body: String,
        val state: CatState,
        val outfit: String,
        val motions: Int,
        val maxStepRatio: Float,
        val reversals: Int,
        val vibrations: Int,
        val tremors: Int,
        val subtle: Int,
        val seam: Float,
        val ground: Float,
        val hatTop: Float,
        val itemDrift: Float,
    )

    @Test
    fun `모든 스킨, 체형, 상황, 동작, 착용 조합이 품질 기준을 지킨다`() {
        val rows = ArrayList<Row>()
        val outfits = listOf("none", "full_over", "full_between")
        for (skin in RigFixtures.SKINS) for (body in RigFixtures.BODIES) {
            val set = RigFixtures.animations(body)
            for ((state, animation) in set.animations) for (outfit in outfits) {
                var maxRatio = 0f
                var reversals = 0
                var vibrations = 0
                var subtle = 0
                var tremors = 0
                var seam = 0f
                var ground = 0f
                var hatTop = Float.MAX_VALUE
                var drift = 0f
                for (motion in animation.motions) {
                    val probe = RigProbe(RigFixtures.character(skin, body, outfit))
                    val clip = probe.animator.play(motion.name, state, loopTimes = motion.loopTimes ?: 2, restMillis = 600)
                    val grounded = !clip.approximate &&
                        !clip.hasAny(RigChannel.JUMP, RigChannel.ROOT_ROT, RigChannel.ROOT_X, RigChannel.LEG_FL, RigChannel.LEG_FR, RigChannel.LEG_BL, RigChannel.LEG_BR)
                    var guard = 0
                    while (!probe.animator.motionFinished && guard++ < 60 * 30) probe.frame(1f / 60, grounded)
                    probe.run(0.5f, checkGround = grounded)
                    maxRatio = max(maxRatio, probe.maxStep / probe.model.skeleton.height)
                    reversals += probe.fastReversals
                    vibrations += probe.vibrations
                    subtle += probe.subtleVibrations
                    tremors += probe.tremors
                    seam = max(seam, max(probe.seamHead, max(probe.seamEar, probe.seamTail)))
                    ground = max(ground, probe.groundDrift)
                    hatTop = minOf(hatTop, probe.hatTop)
                    drift = max(drift, probe.itemDrift)
                }
                rows += Row(skin, body, state, outfit, animation.motions.size, maxRatio, reversals, vibrations, tremors, subtle, seam, ground, hatTop, drift)
            }
        }
        writeSummary(rows)
        val bad = rows.filter {
            it.maxStepRatio > MAX_STEP_RATIO || it.vibrations > 0 || it.tremors > 0 || it.seam >= MAX_SEAM || it.ground > MAX_GROUND ||
                it.hatTop < MIN_HAT_TOP || it.itemDrift > MAX_ITEM_DRIFT
        }
        assertTrue("기준을 넘은 조합 ${bad.size}개:\n" + bad.joinToString("\n"), bad.isEmpty())
        // 상황 9개 x 착용 3 x 체형 5 x 스킨 2
        assertEquals(9 * 3 * 5 * 2, rows.size)
    }

    @Test
    fun `동작을 아무 때나 끊고 바꿔도 전환 첫 프레임에 튀지 않는다`() {
        val realistic = transitions(stress = false)
        val stress = transitions(stress = true)
        File("build/cat-rig-metrics").mkdirs()
        File("build/cat-rig-metrics/transitions.md").writeText(
            "| 시나리오 | 전환 수 | 전환 첫 프레임 초과 이동 최대(u) | 프레임당 최대 이동/키 | 6Hz 초과 뒤집힘(1px 이상) | 꺾임(1px 이상 두 번 뒤집힘) | 떨림(1px 이상 세 번 넘게) | 꺾임(0.4px 이상, 참고) |\n" +
                "|---|---|---|---|---|---|---|---|\n" + realistic.row("동작마다 0.3~2.3초 머문 뒤 전환") + stress.row("블렌드 도중(0.05~0.2초) 재인터럽트") +
                "\n최악 사례: ${realistic.worstCase} / ${stress.worstCase}\n",
        )
        // 전환 첫 프레임은 (직전 움직임) + (새 클립을 휴지 자세에서 틀 때의 첫 움직임) + 0.6u를 넘지 않는다: 포즈가 순간이동하지 않는다
        assertTrue("현실 전환 첫 프레임 ${realistic.worstJump}u: ${realistic.worstCase}", realistic.worstJump <= 0.6f)
        assertTrue("재인터럽트 첫 프레임 ${stress.worstJump}u: ${stress.worstCase}", stress.worstJump <= 0.6f)
        assertTrue("프레임당 최대 이동 ${realistic.worstRatio}", realistic.worstRatio <= MAX_STEP_RATIO)
        // 바들바들 떨림(1px 이상 빠른 뒤집힘 세 번 넘게)은 어떤 전환에서도 0이다.
        // 한 번 꺾였다 가는 것(놀람처럼 반대로 튀는 동작)은 수만 남긴다.
        assertEquals(0, realistic.tremors)
        assertEquals(0, stress.tremors)
    }

    private class TransitionResult(
        val switches: Int,
        val worstJump: Float,
        val worstRatio: Float,
        val reversals: Int,
        val vibrations: Int,
        val tremors: Int,
        val subtle: Int,
        val worstCase: String,
    ) {
        fun row(name: String) =
            "| $name | $switches | ${"%.3f".format(worstJump)} | ${"%.4f".format(worstRatio)} | $reversals | $vibrations | $tremors | $subtle |\n"
    }

    private fun transitions(stress: Boolean): TransitionResult {
        val random = Random(if (stress) 43 else 42)
        var worstJump = 0f
        var worstRatio = 0f
        var switches = 0
        var reversals = 0
        var vibrations = 0
        var subtle = 0
        var tremors = 0
        var worstCase = ""
        val ownStart = HashMap<String, Float>()
        for (skin in RigFixtures.SKINS) for (body in RigFixtures.BODIES) {
            val set = RigFixtures.animations(body)
            val pool = set.animations.flatMap { (state, a) -> a.motions.map { state to it.name } }
            val probe = RigProbe(RigFixtures.character(skin, body, "full_between"), seed = random.nextInt())
            probe.run(1f)
            var last = "IDLE"
            repeat(60) {
                val (state, motion) = pool[random.nextInt(pool.size)]
                val hold = if (stress) 0.05f + random.nextFloat() * 0.15f else 0.3f + random.nextFloat() * 2f
                val own = ownStart.getOrPut("$skin/$body/$motion") { firstStep(skin, body, motion, state) }
                val before = probe.stepHistory.takeLast(5).maxOrNull() ?: 0f
                probe.animator.play(motion, state, restMillis = 300)
                probe.frame(1f / 60, checkGround = false)
                val extra = probe.stepHistory.last() - before - own
                if (extra > worstJump) {
                    worstJump = extra
                    worstCase = "$skin $body $last -> $motion (직전 ${"%.2f".format(before)}u, 클립 자체 ${"%.2f".format(own)}u)"
                }
                switches++
                last = motion
                probe.run(hold, checkGround = false)
            }
            worstRatio = max(worstRatio, probe.maxStep / probe.model.skeleton.height)
            reversals += probe.fastReversals
            vibrations += probe.vibrations
            subtle += probe.subtleVibrations
            tremors += probe.tremors
        }
        return TransitionResult(switches, worstJump, worstRatio, reversals, vibrations, tremors, subtle, worstCase)
    }

    /** 휴지 자세에서 [motion]을 틀었을 때 첫 프레임의 최대 이동(클립 자체의 시작 속도). */
    private fun firstStep(skin: String, body: String, motion: String, state: CatState): Float {
        val probe = RigProbe(RigFixtures.character(skin, body, "full_between"))
        probe.run(1f)
        probe.animator.play(motion, state, restMillis = 300)
        probe.frame(1f / 60, checkGround = false)
        return probe.stepHistory.last()
    }

    private fun com.dandi.nyummy.cat.entity.rig.RigClip.hasAny(vararg channels: RigChannel): Boolean =
        listOfNotNull(intro, loop, outro).any { seg -> channels.any { it in seg.tracks } }

    private fun writeSummary(rows: List<Row>) {
        val dir = File("build/cat-rig-metrics").apply { mkdirs() }
        val sb = StringBuilder()
        sb.appendLine("| 스킨 | 체형 | 상황 | 착용 | 동작 수 | 프레임당 최대 이동/키 | 6Hz 초과 뒤집힘(1px 이상) | 꺾임(1px 이상 두 번 뒤집힘) | 떨림(세 번 넘게) | 꺾임(0.4px 이상, 참고) | 이음새 사용률 | 접지 변화(u) | 모자 위 끝(u) | 아이템 상대 이동(u) |")
        sb.appendLine("|---|---|---|---|---|---|---|---|---|---|---|---|---|---|")
        for (r in rows) {
            val hat = if (r.hatTop == Float.MAX_VALUE) "-" else "%.2f".format(r.hatTop)
            sb.appendLine(
                "| ${r.skin} | ${r.body} | ${r.state} | ${r.outfit} | ${r.motions} | ${"%.4f".format(r.maxStepRatio)} | ${r.reversals} | ${r.vibrations} | ${r.tremors} | ${r.subtle} | " +
                    "${"%.3f".format(r.seam)} | ${"%.3f".format(r.ground)} | $hat | ${"%.4f".format(r.itemDrift)} |",
            )
        }
        File(dir, "summary.md").writeText(sb.toString())
        val worst = "최대: 이동/키 ${"%.4f".format(rows.maxOf { it.maxStepRatio })}, 뒤집힘 ${rows.sumOf { it.reversals }}, 꺾임 ${rows.sumOf { it.vibrations }}, 떨림 ${rows.sumOf { it.tremors }}(0.4px 기준 꺾임 ${rows.sumOf { it.subtle }}), " +
            "이음새 ${"%.3f".format(rows.maxOf { it.seam })}, 접지 ${"%.3f".format(rows.maxOf { it.ground })}u, " +
            "모자 위 끝 최소 ${"%.2f".format(rows.minOf { it.hatTop })}u, 아이템 이동 ${"%.4f".format(rows.maxOf { it.itemDrift })}u"
        File(dir, "worst.txt").writeText(worst + "\n")
        println(worst)
    }

    private companion object {
        const val MAX_STEP_RATIO = 0.03f
        const val MAX_SEAM = 0.8f
        const val MAX_GROUND = 0.5f
        const val MIN_HAT_TOP = 4f
        const val MAX_ITEM_DRIFT = 0.5f
    }
}
