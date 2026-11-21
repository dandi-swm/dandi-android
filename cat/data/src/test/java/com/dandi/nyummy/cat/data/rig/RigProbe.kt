package com.dandi.nyummy.cat.data.rig

import com.dandi.nyummy.cat.domain.rig.BoneWeights
import com.dandi.nyummy.cat.domain.rig.CatMeshOp
import com.dandi.nyummy.cat.domain.rig.CatRigAnimator
import com.dandi.nyummy.cat.domain.rig.CatRigidOp
import com.dandi.nyummy.cat.domain.rig.CatRigModel
import com.dandi.nyummy.cat.domain.rig.CatWeightRules
import com.dandi.nyummy.cat.entity.rig.AttachmentSlot
import com.dandi.nyummy.cat.entity.rig.CatBone
import com.dandi.nyummy.cat.entity.rig.CatRigCharacterVO
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign

/**
 * 리그를 60fps로 돌리며 프레임마다 그려지는 모든 점(메시 꼭짓점, 강체 파츠, 아이템 모서리)을 재는 측정기.
 *
 * 잰 값:
 * - maxStep: 연속 프레임 사이 한 점이 움직인 최대 거리(u). 급격한 점프 검사.
 * - fastReversals: 6Hz보다 빠르게, 1px 이상 진폭으로 방향이 뒤집힌 횟수. vibrations는 그것이 연달아(왕복) 일어난 횟수로 미세 진동(떨림) 검사.
 * - seam: 관절별 이음새 여유 사용률(틈/밑그림 폭). 1을 넘으면 밑그림 밖으로 나가 틈이 보일 수 있다.
 * - groundDrift: 발바닥(가장 낮은 점)의 y 변화(u).
 * - hatTop: 모자 위 끝 y의 최소값(u). 안전 영역 검사.
 * - itemDrift: 모자, 안경의 머리(얼굴) 기준 상대 위치 변화(u). 강체 추종 검사.
 */
class RigProbe(val character: CatRigCharacterVO, seed: Int = 7) {
    val model = CatRigModel(character)
    val animator = CatRigAnimator(model.skeleton, kotlin.random.Random(seed))
    private val rules = CatWeightRules(model.skeleton)
    private val rig = character.bodyRig

    var frames = 0
        private set
    var maxStep = 0f
        private set
    /** 6Hz보다 빠르고 진폭이 보이는(>= 0.1u) 방향 뒤집힘 */
    var fastReversals = 0
        private set

    /** 그런 뒤집힘이 연달아 일어난 횟수(왕복이 이어지는 진동). 진폭 1px(0.25u) 이상 */
    var vibrations = 0
        private set

    /** 1px 이상 빠른 뒤집힘이 세 번 넘게 이어진 횟수(바들바들 떨림). 두 번(한 번 꺾였다 감)은 [vibrations]에만 든다. */
    var tremors = 0
        private set

    /** 진폭 0.1u(약 0.4px) 이상으로 낮춰 센 진동(참고 값) */
    var subtleVibrations = 0
        private set
    var seamHead = 0f
        private set
    var seamEar = 0f
        private set
    var seamTail = 0f
        private set
    var groundDrift = 0f
        private set
    var hatTop = Float.MAX_VALUE
        private set
    var itemDrift = 0f
        private set

    val vibrationLog = ArrayList<String>()

    /** 프레임 사이 최대 이동의 경계(이 프레임 바로 앞 몇 프레임의 최대). 전환 첫 프레임 검사에 쓴다. */
    val stepHistory = ArrayList<Float>()

    private var prev: FloatArray? = null
    val lastPoints: FloatArray? get() = prev
    private var prevDelta: FloatArray? = null
    private var lastReversal: IntArray? = null
    private var travel: FloatArray? = null
    private var lastFast: BooleanArray? = null
    private var lastSubtle: BooleanArray? = null
    private var fastRun: IntArray? = null
    private val restLowest: Float
    private val hatRef = HashMap<AttachmentSlot, FloatArray>()

    private val restBottom: FloatArray

    init {
        model.update(animator.pose, animator.time)
        restLowest = lowest()
        restBottom = bottomRow()
    }

    /** 몸통 메시 맨 아래 줄(발바닥) 꼭짓점의 y */
    private fun bottomRow(): FloatArray {
        val body = model.frame.ops.first { it.texture == "part:body" } as CatMeshOp
        val start = body.rows * (body.cols + 1)
        return FloatArray(body.cols + 1) { body.vertices[(start + it) * 2 + 1] }
    }

    /** 발바닥 줄 꼭짓점이 휴지 자세에서 위아래로 벗어난 최대값 */
    private fun bottomDrift(): Float {
        val now = bottomRow()
        var d = 0f
        for (i in now.indices) d = max(d, abs(now[i] - restBottom[i]))
        return d
    }

    /** [seconds]초 동안 [fps]로 진행하며 잰다. [checkGround]가 false면 접지는 재지 않는다(점프, 구르기 동작). */
    fun run(seconds: Float, fps: Int = 60, checkGround: Boolean = true, onFrame: (Int) -> Unit = {}) {
        val n = (seconds * fps).toInt()
        repeat(n) { i ->
            onFrame(i)
            frame(1f / fps, checkGround)
        }
    }

    fun frame(dt: Float, checkGround: Boolean = true) {
        animator.advance(dt)
        model.update(animator.pose, animator.time)
        val pts = collect()
        val p = prev
        var step = 0f
        if (p != null && p.size == pts.size) {
            val d = prevDelta ?: FloatArray(pts.size).also { prevDelta = it }
            val last = lastReversal ?: IntArray(pts.size) { -100 }.also { lastReversal = it }
            val moved = travel ?: FloatArray(pts.size).also { travel = it }
            val wasFast = lastFast ?: BooleanArray(pts.size).also { lastFast = it }
            val wasSubtle = lastSubtle ?: BooleanArray(pts.size).also { lastSubtle = it }
            val run = fastRun ?: IntArray(pts.size).also { fastRun = it }
            for (i in pts.indices) {
                val delta = pts[i] - p[i]
                if (i % 2 == 0) step = max(step, hypot(delta, pts[i + 1] - p[i + 1]))
                // 6Hz보다 빠른 방향 뒤집힘: 반주기 5프레임(60fps) 미만에, 보이는 진폭(0.1u)으로 다시 뒤집히면 센다.
                // 한 번 뒤집힘(예비 동작)은 진동이 아니고, 그것이 연달아 이어지면 진동으로 센다.
                if (abs(delta) > REVERSAL_EPS && abs(d[i]) > REVERSAL_EPS && sign(delta) != sign(d[i])) {
                    val quick = frames - last[i] < MIN_HALF_PERIOD_FRAMES
                    val fast = quick && moved[i] >= VISIBLE_AMPLITUDE
                    val subtle = quick && moved[i] >= SUBTLE_AMPLITUDE
                    if (fast) fastReversals++
                    if (fast && wasFast[i]) { vibrations++; vibrationLog += "f=$frames i=$i" }
                    if (subtle && wasSubtle[i]) subtleVibrations++
                    run[i] = if (fast) run[i] + 1 else 0
                    if (run[i] == TREMOR_RUN) tremors++
                    wasFast[i] = fast
                    wasSubtle[i] = subtle
                    last[i] = frames
                    moved[i] = 0f
                }
                moved[i] += abs(delta)
                if (abs(delta) > REVERSAL_EPS) d[i] = delta
            }
        }
        prev = pts
        maxStep = max(maxStep, step)
        stepHistory += step
        frames++
        measureSeams()
        if (checkGround) groundDrift = max(groundDrift, bottomDrift())
        measureItems()
    }

    /** 그려지는 모든 점(불투명도와 상관없이). 순서가 프레임마다 같다. */
    private fun collect(): FloatArray {
        var size = 0
        for (op in model.frame.ops) size += when (op) {
            is CatMeshOp -> op.vertices.size
            is CatRigidOp -> 8
            else -> 0
        }
        val out = FloatArray(size)
        var k = 0
        for (op in model.frame.ops) when (op) {
            is CatMeshOp -> {
                System.arraycopy(op.vertices, 0, out, k, op.vertices.size)
                k += op.vertices.size
            }
            is CatRigidOp -> {
                val m = op.matrix
                val xs = floatArrayOf(op.left, op.left + op.width, op.left + op.width, op.left)
                val ys = floatArrayOf(op.top, op.top, op.top + op.height, op.top + op.height)
                for (c in 0 until 4) {
                    out[k++] = m[0] * xs[c] + m[2] * ys[c] + m[4]
                    out[k++] = m[1] * xs[c] + m[3] * ys[c] + m[5]
                }
            }
            else -> Unit
        }
        return out
    }

    /** 몸통 메시 맨 아래 줄 꼭짓점 중 가장 낮은 y */
    private fun lowest(): Float {
        val body = model.frame.ops.first { it.texture == "part:body" } as CatMeshOp
        val v = body.vertices
        val start = body.rows * (body.cols + 1)
        var low = -Float.MAX_VALUE
        for (i in start until start + body.cols + 1) low = max(low, v[i * 2 + 1])
        return low
    }

    private val w = BoneWeights()
    private val tmp = FloatArray(2)

    private fun skinAt(x: Float, y: Float, rule: (Float, Float, BoneWeights) -> Unit, out: FloatArray) {
        w.clear()
        rule(x, y, w)
        w.normalize()
        val m = model.solver.skin
        var ox = 0f
        var oy = 0f
        for (n in w.bone.indices) {
            val wt = w.weight[n]
            if (wt == 0f) continue
            val b = w.bone[n] * 6
            ox += wt * (m[b] * x + m[b + 2] * y + m[b + 4])
            oy += wt * (m[b + 1] * x + m[b + 3] * y + m[b + 5])
        }
        out[0] = ox
        out[1] = oy
    }

    private fun gap(x: Float, y: Float, front: (Float, Float, BoneWeights) -> Unit, back: (Float, Float, BoneWeights) -> Unit): Float {
        skinAt(x, y, front, tmp)
        val fx = tmp[0]
        val fy = tmp[1]
        skinAt(x, y, back, tmp)
        return hypot(fx - tmp[0], fy - tmp[1])
    }

    private fun measureSeams() {
        val s = model.skeleton
        // 턱 아래: 머리 가장자리가 몸통(반다나) 밑그림 위에 머무는지. 밑그림은 반다나 윗단보다 원화 20px(1.6u) 위까지 있다.
        val headBottom = rig.headBox.bottom - 1f
        val headDepth = headBottom - (rig.bandanaBox.top - UNDERLAY_ABOVE_BANDANA)
        val head: (Float, Float, BoneWeights) -> Unit = { _, _, o -> o.add(CatBone.HEAD, 1f) }
        val body: (Float, Float, BoneWeights) -> Unit = { x, y, o -> rules.body(x, y, o) }
        for (dx in CHIN_OFFSETS) {
            seamHead = max(seamHead, gap(s.centerX + dx, headBottom, head, body) / headDepth)
        }
        // 귀 절단선: 귀 그림은 머리 안쪽으로 원화 30px(2.4u) 늘려 두었다
        for ((bone, ear) in listOf(CatBone.EAR_L to rig.earL, CatBone.EAR_R to rig.earR)) {
            val earRule: (Float, Float, BoneWeights) -> Unit = { x, y, o -> rules.ear(bone, x, y, o) }
            for (t in floatArrayOf(0f, 0.5f, 1f)) {
                val x = ear.base.x + (ear.top.x - ear.base.x) * t
                val y = ear.base.y + (ear.top.y - ear.base.y) * t
                seamEar = max(seamEar, gap(x, y, earRule, head) / EAR_UNDERLAY)
            }
        }
        // 꼬리 뿌리: 꼬리 그림은 몸 밑으로 원화 40px(3.2u) 늘려 두었다
        val tail: (Float, Float, BoneWeights) -> Unit = { x, y, o -> rules.tail(x, y, o) }
        seamTail = max(seamTail, gap(rig.tailBase.x, rig.tailBase.y, tail, body) / TAIL_UNDERLAY)
    }

    private fun measureItems() {
        for (slot in listOf(AttachmentSlot.HEAD_HAT, AttachmentSlot.FACE_ITEM)) {
            val corners = model.itemCorners(slot) ?: continue
            if (slot == AttachmentSlot.HEAD_HAT) {
                for (i in 0 until 4) hatTop = min(hatTop, corners[i * 2 + 1])
            }
            // 아이템 모서리를 그 슬롯 본의 강체 좌표로 되돌려, 프레임마다 같은 자리에 있는지 본다
            val k = slot.bone.ordinal * 6
            val m = model.solver.rigid
            val local = FloatArray(8)
            for (i in 0 until 4) {
                val x = corners[i * 2] - m[k + 4]
                val y = corners[i * 2 + 1] - m[k + 5]
                // 강체 변환의 역(회전만 있으므로 전치)
                local[i * 2] = m[k] * x + m[k + 1] * y
                local[i * 2 + 1] = m[k + 2] * x + m[k + 3] * y
            }
            val ref = hatRef.getOrPut(slot) { local }
            for (i in local.indices) itemDrift = max(itemDrift, abs(local[i] - ref[i]))
        }
    }

    companion object {
        const val REVERSAL_EPS = 0.01f
        const val MIN_HALF_PERIOD_FRAMES = 5
        /** 1px: 냐미 칸 130dp를 xxxhdpi(4x)로 그릴 때 1u = 3.8px라 0.25u가 약 1px다(가장 크게 그려지는 경우 기준). */
        const val VISIBLE_AMPLITUDE = 0.25f
        const val SUBTLE_AMPLITUDE = 0.1f
        const val TREMOR_RUN = 3
        const val UNDERLAY_ABOVE_BANDANA = 1.6f
        const val EAR_UNDERLAY = 2.4f
        const val TAIL_UNDERLAY = 3.2f
        val CHIN_OFFSETS = floatArrayOf(-12f, -6f, 0f, 6f, 12f)
    }
}
