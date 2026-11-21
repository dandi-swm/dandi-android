package com.dandi.nyummy.cat.domain.rig

import com.dandi.nyummy.cat.entity.rig.AttachmentSlot
import com.dandi.nyummy.cat.entity.rig.CatBodyRigVO
import com.dandi.nyummy.cat.entity.rig.CatBone
import com.dandi.nyummy.cat.entity.rig.CatCharacterSpec
import com.dandi.nyummy.cat.entity.rig.RigPointVO
import kotlin.math.hypot

/**
 * 한 체형, 한 스킨의 휴지 자세 스켈레톤. 본 계층은 [CatBone] 하나로 공통이고, 본 위치만 체형 원화의 기준점에서 계산한다.
 * 스킨의 피벗 오프셋은 본과 그 본에 붙은 파츠를 함께 옮기는 고정 이동으로 들어간다.
 *
 * @property rig 체형 파츠 묶음
 */
class CatSkeleton(val rig: CatBodyRigVO, spec: CatCharacterSpec = CatCharacterSpec()) {

    /** 본별 휴지 피벗(x, y). [CatBone.ordinal] * 2 위치에 있다. */
    val pivots = FloatArray(CatBone.count * 2)

    /** 본별 스킨 오프셋(x, y). 자세와 상관없이 그 본의 부모 기준 이동에 더한다. */
    val offsets = FloatArray(CatBone.count * 2)

    val ground: Float = rig.ground
    val centerX: Float = rig.neck.x
    val bodyHeight: Float = rig.ground - rig.bodyBox.top
    val bodyWidth: Float = rig.bodyBox.width

    /** 귀 끝에서 발바닥까지 높이. 연속성 기준(프레임당 이동 상한)의 단위로 쓴다. */
    val height: Float = rig.ground - minOf(rig.earL.tip.y, rig.earR.tip.y)

    /** 앞다리, 뒷다리 영역(몸통 메시 가중치에 쓴다). */
    val frontLegHalfWidth: Float = bodyWidth * FRONT_LEG_HALF_WIDTH
    val hindLegHalfWidth: Float = bodyWidth * HIND_LEG_HALF_WIDTH
    val shoulderY: Float = rig.bandanaBox.bottom - 2f
    val haunchY: Float = rig.ground - bodyHeight * HAUNCH_HEIGHT

    /** 꼬리 방향(단위 벡터)과 길이 */
    val tailDirX: Float
    val tailDirY: Float
    val tailLength: Float

    /** 머리 돔(고개 돌림 부풂) 중심과 반지름 */
    val headCenterX: Float = rig.headBox.centerX
    val headCenterY: Float = rig.headBox.centerY
    val headRadiusX: Float = rig.headBox.width / 2f
    val headRadiusY: Float = rig.headBox.height / 2f

    init {
        val r = rig
        set(CatBone.ROOT, r.neck.x, r.ground)
        set(CatBone.HIP, r.neck.x, r.ground)
        set(CatBone.TORSO, r.neck.x, r.ground)
        set(CatBone.NECK, r.neck.x, r.neck.y + NECK_BELOW_CHIN)
        set(CatBone.HEAD, r.neck.x, r.neck.y - HEAD_ABOVE_NECK)
        set(CatBone.EAR_L, r.earL.root)
        set(CatBone.EAR_R, r.earR.root)
        set(CatBone.FACE, (r.eyeL.center.x + r.eyeR.center.x) / 2f, (r.eyeL.center.y + r.eyeR.center.y) / 2f)
        set(CatBone.EYE_L, r.eyeL.center)
        set(CatBone.EYE_R, r.eyeR.center)
        set(CatBone.MOUTH, r.mouth.center)
        val frontOffset = bodyWidth * FRONT_LEG_OFFSET
        val paw = r.ground - PAW_ABOVE_GROUND
        set(CatBone.LEG_FL, r.neck.x - frontOffset, paw)
        set(CatBone.LEG_FR, r.neck.x + frontOffset, paw)
        val hindOffset = bodyWidth * HIND_LEG_OFFSET
        set(CatBone.LEG_BL, r.neck.x - hindOffset, paw)
        set(CatBone.LEG_BR, r.neck.x + hindOffset, paw)
        val dx = r.tailTip.x - r.tailBase.x
        val dy = r.tailTip.y - r.tailBase.y
        tailLength = hypot(dx, dy)
        tailDirX = dx / tailLength
        tailDirY = dy / tailLength
        set(CatBone.TAIL_1, r.tailBase)
        set(CatBone.TAIL_2, r.tailBase.x + dx * TAIL_2_AT, r.tailBase.y + dy * TAIL_2_AT)
        set(CatBone.TAIL_3, r.tailBase.x + dx * TAIL_3_AT, r.tailBase.y + dy * TAIL_3_AT)
        set(CatBone.KNOT, r.knot.x - KNOT_PIVOT_LEFT, r.knot.y)
        for (bone in CatBone.entries) {
            val o = spec.pivotOffset(bone)
            offsets[bone.ordinal * 2] = o.x
            offsets[bone.ordinal * 2 + 1] = o.y
        }
    }

    fun pivotX(bone: CatBone): Float = pivots[bone.ordinal * 2]
    fun pivotY(bone: CatBone): Float = pivots[bone.ordinal * 2 + 1]

    /** 머리 돔 값(0..1). 머리 가운데가 1, 윤곽에서 0이다. */
    fun dome(x: Float, y: Float): Float {
        val nx = (x - headCenterX) / headRadiusX
        val ny = (y - headCenterY) / headRadiusY
        val f = 1f - (nx * nx + ny * ny)
        return if (f > 0f) f else 0f
    }

    /** 슬롯 소켓의 휴지 위치. */
    fun socket(slot: AttachmentSlot): RigPointVO = when (slot) {
        AttachmentSlot.HEAD_HAT -> RigPointVO(rig.neck.x, rig.headBox.top + HAT_SOCKET_BELOW_TOP)
        AttachmentSlot.FACE_ITEM -> RigPointVO(pivotX(CatBone.FACE), pivotY(CatBone.FACE))
        // 목 기준점 바로 아래와 턱 끝 바로 위 중 낮은 쪽. 머리가 목을 덮는 PLUMP에서도 목 장식이 턱 밑으로 보인다.
        AttachmentSlot.NECK_ACCESSORY -> RigPointVO(
            rig.neck.x,
            maxOf(rig.neck.y + NECK_SOCKET_BELOW, rig.headBox.bottom - NECK_SOCKET_UNDER_CHIN),
        )
        AttachmentSlot.TORSO_OUTFIT -> RigPointVO(rig.neck.x, rig.bandanaBox.bottom + OUTFIT_SOCKET_BELOW)
        AttachmentSlot.BACK_ITEM -> RigPointVO(rig.neck.x, rig.bodyBox.top + bodyHeight * BACK_SOCKET_AT)
        AttachmentSlot.TAIL_ACCESSORY -> RigPointVO(
            rig.tailBase.x + tailDirX * tailLength * TAIL_ITEM_AT,
            rig.tailBase.y + tailDirY * tailLength * TAIL_ITEM_AT,
        )
    }

    /** 목 장식 가로 배율(이 체형 목 폭 / NORMAL 목 폭). */
    val neckWidthScale: Float get() = rig.bandanaBox.width / REFERENCE_NECK_WIDTH

    /** 옷 가로 배율(이 체형 몸 폭 / NORMAL 몸 폭). */
    val bodyWidthScale: Float get() = bodyWidth / REFERENCE_BODY_WIDTH

    private fun set(bone: CatBone, p: RigPointVO) = set(bone, p.x, p.y)

    private fun set(bone: CatBone, x: Float, y: Float) {
        pivots[bone.ordinal * 2] = x
        pivots[bone.ordinal * 2 + 1] = y
    }

    companion object {
        /** NORMAL 체형 원화 실측(rig.json)의 반다나 폭, 몸 폭(u). 체형 배율의 기준이다. */
        const val REFERENCE_NECK_WIDTH = 55.065f
        const val REFERENCE_BODY_WIDTH = 56.821f

        private const val NECK_BELOW_CHIN = 3f
        private const val PAW_ABOVE_GROUND = 2f
        private const val HEAD_ABOVE_NECK = 1.5f
        private const val FRONT_LEG_OFFSET = 0.115f
        private const val FRONT_LEG_HALF_WIDTH = 0.1f
        private const val HIND_LEG_OFFSET = 0.36f
        private const val HIND_LEG_HALF_WIDTH = 0.14f
        private const val HAUNCH_HEIGHT = 0.42f
        private const val TAIL_2_AT = 0.4f
        private const val TAIL_3_AT = 0.75f
        private const val TAIL_ITEM_AT = 0.55f
        private const val KNOT_PIVOT_LEFT = 2.2f
        private const val HAT_SOCKET_BELOW_TOP = 2f
        private const val NECK_SOCKET_BELOW = 3.8f
        private const val NECK_SOCKET_UNDER_CHIN = 1.5f
        private const val OUTFIT_SOCKET_BELOW = 4f
        private const val BACK_SOCKET_AT = 0.4f
    }
}
