package com.dandi.nyummy.cat.domain.rig

import com.dandi.nyummy.cat.entity.rig.CatBone
import com.dandi.nyummy.cat.entity.rig.RigChannel
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.sin

/**
 * 채널 값(포즈)을 본 변환으로 푼다(정방향 운동학).
 *
 * 본마다 두 가지 변환을 낸다. 둘 다 휴지 좌표를 화면(리그) 좌표로 옮기는 2D 아핀 변환이다.
 * - 스킨 변환: 위치 + 회전 + 그 본만의 변형(숨쉬기 배율, 기울임, 눈 크기). 메시 꼭짓점에 쓴다.
 * - 강체 변환: 위치 + 회전만. 모자, 안경처럼 찌그러지면 안 되는 아이템에 쓴다.
 * 자식은 부모 스킨 변환으로 옮겨진 자기 피벗 위치와 부모 회전만 물려받는다(배율은 물려받지 않음).
 *
 * 아핀 변환 6개 값(a, b, c, d, tx, ty): x' = a*x + c*y + tx, y' = b*x + d*y + ty. 배열을 재사용해 프레임마다 할당하지 않는다.
 */
class CatPoseSolver(val skeleton: CatSkeleton) {

    /** 본별 스킨 변환. [CatBone.ordinal] * 6 위치. */
    val skin = FloatArray(CatBone.count * 6)

    /** 본별 강체 변환. [CatBone.ordinal] * 6 위치. */
    val rigid = FloatArray(CatBone.count * 6)

    /** 본별 누적 회전(라디안) */
    val rotation = FloatArray(CatBone.count)

    private val worldX = FloatArray(CatBone.count)
    private val worldY = FloatArray(CatBone.count)

    /** 본 피벗의 지금 위치 */
    fun worldX(bone: CatBone): Float = worldX[bone.ordinal]
    fun worldY(bone: CatBone): Float = worldY[bone.ordinal]

    /** [pose](채널 값, [RigChannel.ordinal] 순서)를 풀어 [skin], [rigid]를 채운다. [time]은 꼬리 파동의 위상에 쓴다. */
    fun solve(pose: FloatArray, time: Double) {
        val s = skeleton
        fun ch(c: RigChannel) = pose[c.ordinal]
        val lean = ch(RigChannel.LEAN)
        val leanRot = atan(lean) * LEAN_TO_HEAD
        val headRot = ch(RigChannel.HEAD_ROT)
        val headX = ch(RigChannel.HEAD_X)
        val headY = ch(RigChannel.HEAD_Y)
        val yaw = ch(RigChannel.YAW)
        val pitch = ch(RigChannel.PITCH)
        val breath = ch(RigChannel.BREATH)
        val stretch = ch(RigChannel.STRETCH)

        // 바닥 앵커: 이동, 점프, 회전, 스쿼시
        root(
            x = s.pivotX(CatBone.ROOT) + ch(RigChannel.ROOT_X),
            y = s.pivotY(CatBone.ROOT) - ch(RigChannel.JUMP),
            rot = ch(RigChannel.ROOT_ROT),
            sx = ch(RigChannel.SQUASH_X),
            sy = ch(RigChannel.SQUASH_Y),
        )
        // 골반, 몸통: 발바닥 고정 기울임(x가 높이에 비례해 밀림), 몸통은 숨쉬기 배율(면적 보존)까지
        child(CatBone.HIP, rot = 0f, a = 1f, b = 0f, c = -lean, d = 1f)
        child(
            CatBone.TORSO, rot = 0f,
            a = 1f - BREATH_X * breath, b = 0f,
            c = -lean * stretch * (1f + BREATH_Y * breath), d = stretch * (1f + BREATH_Y * breath),
        )
        // 목과 머리: 머리 이동과 회전을 목 40%, 머리 60%로 나눈다(목 장식이 턱과 가슴 사이에 머문다)
        child(CatBone.NECK, rot = NECK_SHARE * headRot + leanRot * NECK_SHARE, tx = headX * NECK_MOVE, ty = headY * NECK_MOVE)
        child(CatBone.HEAD, rot = (1f - NECK_SHARE) * headRot + leanRot * (1f - NECK_SHARE), tx = headX * (1f - NECK_MOVE), ty = headY * (1f - NECK_MOVE))
        child(CatBone.EAR_L, rot = ch(RigChannel.EAR_L), tx = -yaw * EAR_YAW, ty = pitch * EAR_PITCH)
        child(CatBone.EAR_R, rot = ch(RigChannel.EAR_R), tx = -yaw * EAR_YAW, ty = pitch * EAR_PITCH)
        val faceDome = s.dome(s.pivotX(CatBone.FACE), s.pivotY(CatBone.FACE)) * FACE_PARALLAX
        child(
            CatBone.FACE, rot = 0f,
            tx = yaw * YAW_SHIFT * faceDome + ch(RigChannel.LOOK_X),
            ty = pitch * PITCH_SHIFT * faceDome + ch(RigChannel.LOOK_Y),
        )
        val eyeScale = ch(RigChannel.EYE_SCALE)
        child(CatBone.EYE_L, rot = 0f, a = eyeScale, d = eyeScale)
        child(CatBone.EYE_R, rot = 0f, a = eyeScale, d = eyeScale)
        child(CatBone.MOUTH, rot = 0f)
        // 다리: 들어 올리면 위로 조금, 바깥으로 조금 돈다
        val frontLift = s.bodyHeight * FRONT_LEG_LIFT
        child(CatBone.LEG_FL, rot = -ch(RigChannel.LEG_FL) * LEG_TURN, ty = -ch(RigChannel.LEG_FL) * frontLift)
        child(CatBone.LEG_FR, rot = ch(RigChannel.LEG_FR) * LEG_TURN, ty = -ch(RigChannel.LEG_FR) * frontLift)
        val hindLift = s.bodyHeight * HIND_LEG_LIFT
        child(CatBone.LEG_BL, rot = -ch(RigChannel.LEG_BL) * LEG_TURN, ty = -ch(RigChannel.LEG_BL) * hindLift)
        child(CatBone.LEG_BR, rot = ch(RigChannel.LEG_BR) * LEG_TURN, ty = -ch(RigChannel.LEG_BR) * hindLift)
        // 꼬리: 굽힘은 마디마다 나눠 끝으로 갈수록 크게, 파동은 마디마다 늦게
        val tail = ch(RigChannel.TAIL)
        val wag = ch(RigChannel.TAIL_WAG)
        val phase = time * TAIL_WAG_SPEED
        child(CatBone.TAIL_1, rot = -(tail * 0.25f + wag * sin(phase).toFloat() * 0.25f))
        child(CatBone.TAIL_2, rot = -(tail * 0.35f + wag * sin(phase - 0.96).toFloat() * 0.35f))
        child(CatBone.TAIL_3, rot = -(tail * 0.4f + wag * sin(phase - 1.8).toFloat() * 0.4f))
        child(CatBone.KNOT, rot = ch(RigChannel.KNOT))
    }

    private fun root(x: Float, y: Float, rot: Float, sx: Float, sy: Float) {
        val i = CatBone.ROOT.ordinal
        val px = skeleton.pivotX(CatBone.ROOT)
        val py = skeleton.pivotY(CatBone.ROOT)
        rotation[i] = rot
        worldX[i] = x
        worldY[i] = y
        write(i, rot, sx, 0f, 0f, sy, px, py, x, y)
    }

    /**
     * 본 하나를 푼다. 부모 스킨 변환으로 자기 피벗(+스킨 오프셋, +[tx],[ty]를 부모 회전으로 돌린 것)을 옮기고,
     * 부모 회전에 [rot]를 더한 뒤, 자기 변형(a, b, c, d: 피벗 기준 2x2)을 곱한다.
     */
    private fun child(
        bone: CatBone,
        rot: Float,
        tx: Float = 0f,
        ty: Float = 0f,
        a: Float = 1f,
        b: Float = 0f,
        c: Float = 0f,
        d: Float = 1f,
    ) {
        val i = bone.ordinal
        val p = bone.parent!!.ordinal
        val px = skeleton.pivotX(bone)
        val py = skeleton.pivotY(bone)
        val pr = rotation[p]
        val cp = cos(pr)
        val sp = sin(pr)
        val ox = tx + skeleton.offsets[i * 2]
        val oy = ty + skeleton.offsets[i * 2 + 1]
        val k = p * 6
        val qx = skin[k] * px + skin[k + 2] * py + skin[k + 4] + (cp * ox - sp * oy)
        val qy = skin[k + 1] * px + skin[k + 3] * py + skin[k + 5] + (sp * ox + cp * oy)
        val r = pr + rot
        rotation[i] = r
        worldX[i] = qx
        worldY[i] = qy
        write(i, r, a, b, c, d, px, py, qx, qy)
    }

    /** M = R(r) * S, 변환 x -> M(x - p) + q 를 스킨에, R(r)(x - p) + q 를 강체에 쓴다. */
    private fun write(i: Int, r: Float, a: Float, b: Float, c: Float, d: Float, px: Float, py: Float, qx: Float, qy: Float) {
        val cr = cos(r)
        val sr = sin(r)
        // R = [cr -sr; sr cr], S = [a c; b d]
        val ma = cr * a - sr * b
        val mb = sr * a + cr * b
        val mc = cr * c - sr * d
        val md = sr * c + cr * d
        val k = i * 6
        skin[k] = ma
        skin[k + 1] = mb
        skin[k + 2] = mc
        skin[k + 3] = md
        skin[k + 4] = qx - (ma * px + mc * py)
        skin[k + 5] = qy - (mb * px + md * py)
        rigid[k] = cr
        rigid[k + 1] = sr
        rigid[k + 2] = -sr
        rigid[k + 3] = cr
        rigid[k + 4] = qx - (cr * px - sr * py)
        rigid[k + 5] = qy - (sr * px + cr * py)
    }

    companion object {
        /** 숨쉬기 1일 때 몸통 세로 +2.8%, 가로 -1.2%(기준서 IDLE_BREATH, 면적 보존) */
        const val BREATH_Y = 0.028f
        const val BREATH_X = 0.012f

        /** 몸 기울기가 머리 회전으로 이어지는 비율 */
        const val LEAN_TO_HEAD = 0.6f

        /** 머리 회전 중 목이 받는 몫(기준서 목 장식 회전 = 몸통 0.6 + 머리 0.4) */
        const val NECK_SHARE = 0.4f

        /** 머리 이동 중 목이 받는 몫. 목 근처 몸통 밑그림이 머리를 따라와 턱 밑이 비지 않는다. */
        const val NECK_MOVE = 0.65f

        /** 고개 돌림 1일 때 머리 가운데가 밀리는 양(u) */
        const val YAW_SHIFT = 2.15f
        const val PITCH_SHIFT = 1.36f
        const val FACE_PARALLAX = 1.12f
        const val EAR_YAW = 0.88f
        const val EAR_PITCH = 0.4f

        const val FRONT_LEG_LIFT = 0.09f
        const val HIND_LEG_LIFT = 0.05f
        const val LEG_TURN = 0.12f
        const val TAIL_WAG_SPEED = 3.1
    }
}
