package com.dandi.nyummy.cat.domain.rig

import com.dandi.nyummy.cat.domain.rig.RigMath.smooth
import com.dandi.nyummy.cat.entity.rig.CatBone
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max

/**
 * 격자 메시 하나와 꼭짓점별 본 가중치(선형 블렌드 스키닝). 꼭짓점마다 본 최대 [MAX_INFLUENCE]개, 가중치 합 1.
 * 가중치는 경계마다 smoothstep 폭을 두어 한 꼭짓점 줄에서 꺾이지 않게 한다(파츠가 접혀 보이지 않게).
 *
 * @property cols 가로 칸 수
 * @property rows 세로 칸 수
 */
class SkinnedGrid(
    left: Float,
    top: Float,
    width: Float,
    height: Float,
    cell: Float,
    weightOf: (x: Float, y: Float, out: BoneWeights) -> Unit,
) {
    val cols: Int = max(1, ceil(width / cell).toInt())
    val rows: Int = max(1, ceil(height / cell).toInt())
    val count: Int = (cols + 1) * (rows + 1)

    /** 휴지 꼭짓점(x, y) */
    val rest = FloatArray(count * 2)
    val bones = IntArray(count * MAX_INFLUENCE)
    val weights = FloatArray(count * MAX_INFLUENCE)

    init {
        val w = BoneWeights()
        var k = 0
        for (j in 0..rows) {
            for (i in 0..cols) {
                val x = left + i * width / cols
                val y = top + j * height / rows
                rest[k * 2] = x
                rest[k * 2 + 1] = y
                w.clear()
                weightOf(x, y, w)
                w.normalize()
                for (n in 0 until MAX_INFLUENCE) {
                    bones[k * MAX_INFLUENCE + n] = w.bone[n]
                    weights[k * MAX_INFLUENCE + n] = w.weight[n]
                }
                k++
            }
        }
    }

    /**
     * 꼭짓점을 스키닝해 [out]에 화면 좌표로 쓴다. [preWarp]가 있으면 스키닝 전에 휴지 좌표를 옮긴다(머리 돔 변형).
     */
    fun skin(solver: CatPoseSolver, out: FloatArray, preWarp: ((x: Float, y: Float, out: FloatArray) -> Unit)? = null) {
        val m = solver.skin
        val tmp = scratch
        for (k in 0 until count) {
            var x = rest[k * 2]
            var y = rest[k * 2 + 1]
            if (preWarp != null) {
                preWarp(x, y, tmp)
                x = tmp[0]
                y = tmp[1]
            }
            var ox = 0f
            var oy = 0f
            for (n in 0 until MAX_INFLUENCE) {
                val wt = weights[k * MAX_INFLUENCE + n]
                if (wt == 0f) continue
                val b = bones[k * MAX_INFLUENCE + n] * 6
                ox += wt * (m[b] * x + m[b + 2] * y + m[b + 4])
                oy += wt * (m[b + 1] * x + m[b + 3] * y + m[b + 5])
            }
            out[k * 2] = ox
            out[k * 2 + 1] = oy
        }
    }

    private val scratch = FloatArray(2)

    companion object {
        const val MAX_INFLUENCE = 4
    }
}

/** 가중치를 모으는 작은 버퍼. 같은 본을 두 번 넣으면 더한다. */
class BoneWeights {
    val bone = IntArray(SkinnedGrid.MAX_INFLUENCE)
    val weight = FloatArray(SkinnedGrid.MAX_INFLUENCE)
    private var size = 0

    fun clear() {
        size = 0
        bone.fill(0)
        weight.fill(0f)
    }

    fun add(b: CatBone, w: Float) {
        if (w <= 0f) return
        for (n in 0 until size) {
            if (bone[n] == b.ordinal) {
                weight[n] += w
                return
            }
        }
        if (size < SkinnedGrid.MAX_INFLUENCE) {
            bone[size] = b.ordinal
            weight[size] = w
            size++
        } else {
            // 가장 작은 칸을 바꾼다
            var min = 0
            for (n in 1 until size) if (weight[n] < weight[min]) min = n
            if (weight[min] < w) {
                bone[min] = b.ordinal
                weight[min] = w
            }
        }
    }

    fun normalize() {
        var sum = 0f
        for (n in 0 until size) sum += weight[n]
        if (sum <= 0f) {
            clear()
            bone[0] = CatBone.ROOT.ordinal
            weight[0] = 1f
            size = 1
            return
        }
        for (n in 0 until size) weight[n] /= sum
    }
}

/**
 * 파츠별 가중치 규칙. 본 위치는 [CatSkeleton]의 휴지 피벗을 쓴다.
 * 이 규칙이 이음새 방지의 절반이다(나머지 절반은 앞 레이어 밑으로 털을 늘려 둔 원화).
 */
class CatWeightRules(private val s: CatSkeleton) {

    /** 몸통(앞발, 뒷발 포함). 높이로 골반과 몸통, 목 근처는 목, 다리 영역은 다리. 옷도 같은 규칙을 쓴다. */
    fun body(x: Float, y: Float, out: BoneWeights) {
        val up = s.ground - y
        val h = up / s.bodyHeight
        val torso = smooth(0.15f, 0.55f, h)
        // 반다나와 머리 밑(목 근처)은 목을 따라가 머리가 움직여도 밑그림이 드러나지 않는다
        val neck = smooth(s.rig.bandanaBox.bottom, s.rig.neck.y, y) * NECK_SHARE_OF_BODY
        val fl = legWeight(x, y, CatBone.LEG_FL, s.frontLegHalfWidth, s.shoulderY)
        val fr = legWeight(x, y, CatBone.LEG_FR, s.frontLegHalfWidth, s.shoulderY)
        val bl = legWeight(x, y, CatBone.LEG_BL, s.hindLegHalfWidth, s.haunchY)
        val br = legWeight(x, y, CatBone.LEG_BR, s.hindLegHalfWidth, s.haunchY)
        val leg = fl + fr + bl + br
        val rest = (1f - leg).coerceAtLeast(0f)
        out.add(CatBone.HIP, rest * (1f - torso) * (1f - neck))
        out.add(CatBone.TORSO, rest * torso * (1f - neck))
        out.add(CatBone.NECK, rest * neck)
        out.add(CatBone.LEG_FL, fl)
        out.add(CatBone.LEG_FR, fr)
        out.add(CatBone.LEG_BL, bl)
        out.add(CatBone.LEG_BR, br)
    }

    /** 다리 영역 가중치: 다리 가운데 줄에서 멀어지면 0, 어깨(엉덩이)에서 발끝으로 갈수록 커진다. */
    private fun legWeight(x: Float, y: Float, leg: CatBone, halfWidth: Float, topY: Float): Float {
        val lat = abs(x - s.pivotX(leg))
        val across = 1f - smooth(halfWidth * 0.55f, halfWidth, lat)
        if (across <= 0f) return 0f
        val along = smooth(topY, s.ground, y)
        return across * along * LEG_MAX
    }

    /** 기본 반다나: 윗단은 목, 아랫단은 가슴(몸통), 매듭 끝은 매듭 본. */
    fun bandana(x: Float, y: Float, out: BoneWeights) {
        val box = s.rig.bandanaBox
        val neck = (1f - smooth(box.top + box.height * 0.18f, box.top + box.height * 0.86f, y)) * 0.92f
        val knot = smooth(s.rig.knot.x - 2.4f, s.rig.knot.x + 5.6f, x)
        out.add(CatBone.KNOT, knot)
        out.add(CatBone.NECK, (1f - knot) * neck)
        out.add(CatBone.TORSO, (1f - knot) * (1f - neck))
    }

    /** 귀: 절단선 근처는 머리, 귀 끝으로 갈수록 귀 본. */
    fun ear(ear: CatBone, x: Float, y: Float, out: BoneWeights) {
        val e = if (ear == CatBone.EAR_L) s.rig.earL else s.rig.earR
        val rx = e.root.x
        val ry = e.root.y
        val ax = e.tip.x - rx
        val ay = e.tip.y - ry
        val len = kotlin.math.hypot(ax, ay)
        val d = ((x - rx) * ax + (y - ry) * ay) / len
        val w = smooth(len * 0.04f, len * 0.8f, d)
        out.add(ear, w)
        out.add(CatBone.HEAD, 1f - w)
    }

    /** 꼬리: 뿌리에서 끝으로 골반 -> 마디 1 -> 2 -> 3, 마디 경계는 부드럽게 넘긴다. */
    fun tail(x: Float, y: Float, out: BoneWeights) {
        val u = ((x - s.rig.tailBase.x) * s.tailDirX + (y - s.rig.tailBase.y) * s.tailDirY) / s.tailLength
        val r = smooth(0f, 0.15f, u)
        val a = smooth(0.3f, 0.5f, u)
        val b = smooth(0.65f, 0.85f, u)
        out.add(CatBone.HIP, 1f - r)
        out.add(CatBone.TAIL_1, r * (1f - a))
        out.add(CatBone.TAIL_2, r * a * (1f - b))
        out.add(CatBone.TAIL_3, r * a * b)
    }

    private companion object {
        const val NECK_SHARE_OF_BODY = 0.85f
        const val LEG_MAX = 0.95f
    }
}
