package com.dandi.nyummy.cat.domain.rig

import com.dandi.nyummy.cat.domain.rig.RigMath.clamp
import com.dandi.nyummy.cat.domain.rig.RigMath.lerp
import com.dandi.nyummy.cat.domain.rig.RigMath.smooth
import com.dandi.nyummy.cat.entity.rig.AttachmentFollow
import com.dandi.nyummy.cat.entity.rig.AttachmentItemVO
import com.dandi.nyummy.cat.entity.rig.AttachmentSlot
import com.dandi.nyummy.cat.entity.rig.CatBone
import com.dandi.nyummy.cat.entity.rig.CatRigCharacterVO
import com.dandi.nyummy.cat.entity.rig.CatZOrder
import com.dandi.nyummy.cat.entity.rig.EarLayerMode
import com.dandi.nyummy.cat.entity.rig.PartMaterial
import com.dandi.nyummy.cat.entity.rig.RigChannel
import kotlin.math.max
import kotlin.math.min

/**
 * 고양이 하나(스킨 + 체형 + 끼운 아이템)를 그릴 준비. 메시 격자와 가중치를 한 번 만들어 두고,
 * [update]가 프레임마다 포즈로 꼭짓점과 행렬만 바꾼다.
 *
 * 그리는 순서는 [CatZOrder] 표 하나로 정한다. 목 장식(또는 반다나를 숨기는 옷)을 끼우면 기본 반다나를 그리지 않는다.
 */
class CatRigModel(val character: CatRigCharacterVO) {

    val skeleton = CatSkeleton(character.bodyRig, character.spec)
    val solver = CatPoseSolver(skeleton)
    private val rules = CatWeightRules(skeleton)
    private val rig = character.bodyRig

    private val meshes = ArrayList<Pair<CatMeshOp, Skin>>()
    private val rigids = ArrayList<Pair<CatRigidOp, (CatRigidOp) -> Unit>>()
    private val ops = ArrayList<CatDrawOp>()
    private var order = 0

    /** 그릴 목록. [update] 뒤에 읽는다. */
    val frame: CatRigFrame

    /** 기본 반다나를 그리는지 */
    val showsBandana: Boolean =
        character.spec.showDefaultBandana && character.equipped.values.none { it.replacesBandana }

    private val hat: AttachmentItemVO? = character.equipped[AttachmentSlot.HEAD_HAT]

    // 얼굴 모양(코드 도형)
    private val closedL = CatShapeOp(CatZOrder.EYES, next(), CatShape.CLOSED_EYE)
    private val closedR = CatShapeOp(CatZOrder.EYES, next(), CatShape.CLOSED_EYE)
    private val smileL = CatShapeOp(CatZOrder.EYES, next(), CatShape.SMILE_EYE)
    private val smileR = CatShapeOp(CatZOrder.EYES, next(), CatShape.SMILE_EYE)
    private val blush = CatShapeOp(CatZOrder.EYES, next(), CatShape.BLUSH)
    private val openMouth = CatShapeOp(CatZOrder.MOUTH, next(), CatShape.OPEN_MOUTH)
    private val pout = CatShapeOp(CatZOrder.MOUTH, next(), CatShape.POUT_MOUTH)
    private val shadow = CatShapeOp(CatZOrder.SHADOW, next(), CatShape.SHADOW)
    private val zzz = CatShapeOp(CatZOrder.EFFECT, next(), CatShape.ZZZ)

    private lateinit var eyeL: CatRigidOp
    private lateinit var eyeR: CatRigidOp
    private lateinit var mouthFull: CatRigidOp
    private lateinit var mouthNose: CatRigidOp
    private var earOps = ArrayList<CatMeshOp>()

    /** 착용 아이템 소켓의 지금 위치(테스트, 측정용). 슬롯 -> (x, y) */
    val socketWorld = HashMap<AttachmentSlot, FloatArray>()

    private val headWarp: (Float, Float, FloatArray) -> Unit = { x, y, o ->
        val d = skeleton.dome(x, y)
        o[0] = x + yaw * CatPoseSolver.YAW_SHIFT * d
        o[1] = y + pitch * CatPoseSolver.PITCH_SHIFT * d
    }
    private var yaw = 0f
    private var pitch = 0f

    init {
        ops += shadow
        mesh("tail", CatZOrder.TAIL, PartMaterial.FUR) { x, y, w -> rules.tail(x, y, w) }
        mesh("body", CatZOrder.BODY, PartMaterial.FUR) { x, y, w -> rules.body(x, y, w) }
        if (showsBandana) mesh("bandana", CatZOrder.NECK, PartMaterial.ACCESSORY) { x, y, w -> rules.bandana(x, y, w) }
        earOps += mesh("earL", CatZOrder.EARS, PartMaterial.FUR) { x, y, w -> rules.ear(CatBone.EAR_L, x, y, w) }
        earOps += mesh("earR", CatZOrder.EARS, PartMaterial.FUR) { x, y, w -> rules.ear(CatBone.EAR_R, x, y, w) }
        mesh("head", CatZOrder.HEAD, PartMaterial.FUR, warp = true) { _, _, w -> w.add(CatBone.HEAD, 1f) }
        eyeL = rigidPart("eyeL", CatZOrder.EYES, PartMaterial.FACE, CatBone.EYE_L, skin = true)
        eyeR = rigidPart("eyeR", CatZOrder.EYES, PartMaterial.FACE, CatBone.EYE_R, skin = true)
        ops += listOf(closedL, closedR, smileL, smileR, blush)
        mouthNose = rigidPart("mouth", CatZOrder.MOUTH, PartMaterial.FACE, CatBone.MOUTH, skin = true)
        mouthFull = rigidPart("mouth", CatZOrder.MOUTH, PartMaterial.FACE, CatBone.MOUTH, skin = true)
        ops += listOf(openMouth, pout, zzz)
        for ((_, item) in character.equipped) attach(item)
        ops.sortWith(compareBy<CatDrawOp>({ it.z }, { it.order }))
        frame = CatRigFrame(ops)
    }

    private fun next() = order++

    private class Skin(val grid: SkinnedGrid, val warp: Boolean, val clipToHat: Boolean)

    private fun mesh(
        id: String,
        z: Int,
        material: PartMaterial,
        warp: Boolean = false,
        weight: (Float, Float, BoneWeights) -> Unit,
    ): CatMeshOp {
        val layer = rig.layer(id) ?: error("파츠 없음: $id")
        val grid = SkinnedGrid(layer.x, layer.y, layer.width, layer.height, CELL, weight)
        val op = CatMeshOp(z, next(), "part:$id", material, grid.cols, grid.rows)
        meshes += op to Skin(grid, warp, clipToHat = false)
        ops += op
        return op
    }

    private fun rigidPart(id: String, z: Int, material: PartMaterial, bone: CatBone, skin: Boolean): CatRigidOp {
        val layer = rig.layer(id) ?: error("파츠 없음: $id")
        val op = CatRigidOp(z, next(), "part:$id", material, layer.x, layer.y, layer.width, layer.height)
        rigids += op to { o -> copy(if (skin) solver.skin else solver.rigid, bone, o.matrix) }
        ops += op
        return op
    }

    /** 아이템 하나를 슬롯 규칙대로 붙인다. */
    private fun attach(item: AttachmentItemVO) {
        val slot = item.slot
        val socket = skeleton.socket(slot)
        val sx = when (slot.follow) {
            AttachmentFollow.RIGID -> 1f
            AttachmentFollow.NECK_WIDTH -> skeleton.neckWidthScale
            AttachmentFollow.BODY_SKIN -> skeleton.bodyWidthScale
        }
        val w = item.width * sx
        val h = item.height
        val left = socket.x - item.anchorX * w
        val top = socket.y - item.anchorY * h
        val bone = slot.bone
        // 뒤판은 BETWEEN_EARS 모자, 목걸이 뒤끈, 옷 뒤판에만 쓴다
        val backArt = item.backArt?.takeIf { slot != AttachmentSlot.HEAD_HAT || item.earLayerMode == EarLayerMode.BETWEEN_EARS }
        if (slot.follow == AttachmentFollow.BODY_SKIN) {
            if (backArt != null) bodySkinned(backArt.key, slot.backZ ?: slot.z, left, top, w, h)
            bodySkinned(item.art.key, slot.z, left, top, w, h)
        } else {
            if (backArt != null) rigidItem(backArt.key, slot.backZ ?: slot.z, left, top, w, h, bone)
            rigidItem(item.art.key, slot.z, left, top, w, h, bone)
        }
        socketWorld[slot] = FloatArray(2)
        if (slot == AttachmentSlot.HEAD_HAT && item.earLayerMode == EarLayerMode.OVER_EARS) {
            for (ear in earOps) ear.clip = FloatArray(8)
        }
    }

    private fun bodySkinned(key: String, z: Int, left: Float, top: Float, w: Float, h: Float) {
        val grid = SkinnedGrid(left, top, w, h, CELL) { x, y, out -> rules.body(x, y, out) }
        val op = CatMeshOp(z, next(), key, null, grid.cols, grid.rows)
        meshes += op to Skin(grid, warp = false, clipToHat = false)
        ops += op
    }

    private fun rigidItem(key: String, z: Int, left: Float, top: Float, w: Float, h: Float, bone: CatBone) {
        val op = CatRigidOp(z, next(), key, null, left, top, w, h)
        rigids += op to { o -> copy(solver.rigid, bone, o.matrix) }
        ops += op
    }

    /**
     * [pose](채널 값)와 [time]으로 이번 프레임을 계산한다. 결과는 [frame]에 있다.
     */
    fun update(pose: FloatArray, time: Double) {
        solver.solve(pose, time)
        yaw = pose[RigChannel.YAW.ordinal]
        pitch = pose[RigChannel.PITCH.ordinal]
        for ((op, skin) in meshes) skin.grid.skin(solver, op.vertices, if (skin.warp) headWarp else null)
        for ((op, fill) in rigids) fill(op)
        updateFace(pose)
        updateEffects(pose, time)
        updateSockets()
        updateHatClip()
    }

    private fun updateFace(pose: FloatArray) {
        fun ch(c: RigChannel) = pose[c.ordinal]
        val sm = clamp(ch(RigChannel.SMILE), 0f, 1f)
        val open = clamp(ch(RigChannel.EYE_OPEN) * (1f - smooth(0f, 0.6f, sm)), 0f, 1.3f)
        // 눈은 불투명한 채 위에서 눈꺼풀에 가려지다가, 거의 감기면 감은 눈 선으로 바뀐다(납작하게 누르지 않는다)
        val eyeA = smooth(0.18f, 0.3f, open) * (1f - smooth(0.42f, 0.6f, sm))
        val lidA = (1f - smooth(0.2f, 0.32f, open)) * (1f - smooth(0.25f, 0.5f, sm))
        val arcA = smooth(0.3f, 0.55f, sm)
        val cut = max(ch(RigChannel.LID), min((1f - clamp(open, 0f, 1f)) * 0.88f, 0.62f))
        val slant = ch(RigChannel.LID_SLANT)
        eyeL.alpha = eyeA
        eyeR.alpha = eyeA
        eyeL.clip = lidClip(eyeL, cut, slant, eyeL.clip)
        eyeR.clip = lidClip(eyeR, cut, -slant, eyeR.clip)
        eyeShape(closedL, rig.eyeL, CatBone.EYE_L, lidA, closed = true)
        eyeShape(closedR, rig.eyeR, CatBone.EYE_R, lidA, closed = true)
        eyeShape(smileL, rig.eyeL, CatBone.EYE_L, arcA, closed = false)
        eyeShape(smileR, rig.eyeR, CatBone.EYE_R, arcA, closed = false)

        // 입: 코(입 그림 위쪽)는 늘 남기고, 아래 입 모양만 ω, 벌린 입, 삐진 입 사이를 넘긴다
        val mouth = ch(RigChannel.MOUTH)
        val mo = smooth(0.04f, 0.3f, mouth)
        val po = clamp(ch(RigChannel.POUT), 0f, 1f) * (1f - mo)
        val full = (1f - mo) * (1f - clamp(ch(RigChannel.POUT), 0f, 1f))
        mouthFull.alpha = full
        mouthNose.alpha = if (full < 0.999f) 1f else 0f
        mouthNose.clip = mouthNose.clip ?: keepTopClip(mouthNose, NOSE_PART)
        val m = rig.mouth
        val ms = lerp(0.55f, 1f, clamp(mouth, 0f, 1f)) + max(0f, mouth - 1f) * 0.5f
        copy(solver.skin, CatBone.MOUTH, openMouth.matrix)
        openMouth.alpha = mo
        openMouth.params[0] = m.center.x
        openMouth.params[1] = mouthNose.top + mouthNose.height * NOSE_PART * 0.9f
        openMouth.params[2] = m.width * 0.42f * (0.85f + 0.15f * ms)
        openMouth.params[3] = m.height * 0.62f * ms
        copy(solver.skin, CatBone.MOUTH, pout.matrix)
        pout.alpha = po
        pout.params[0] = m.center.x
        pout.params[1] = m.center.y + m.height * 0.16f
        pout.params[2] = m.width * 0.16f
        pout.params[3] = m.height * 0.2f
        pout.params[4] = FACE_LINE
        copy(solver.skin, CatBone.FACE, blush.matrix)
        blush.alpha = clamp(ch(RigChannel.BLUSH), 0f, 1f) * 0.45f
        blush.params[0] = rig.eyeL.center.x - rig.eyeL.radiusX * 0.6f
        blush.params[1] = rig.eyeL.center.y + rig.eyeL.radiusY * 1.35f
        blush.params[2] = rig.eyeR.center.x + rig.eyeR.radiusX * 0.6f
        blush.params[3] = rig.eyeR.center.y + rig.eyeR.radiusY * 1.35f
        blush.params[4] = rig.eyeL.radiusX * 0.95f
    }

    private fun eyeShape(op: CatShapeOp, eye: com.dandi.nyummy.cat.entity.rig.RigEyeVO, bone: CatBone, alpha: Float, closed: Boolean) {
        copy(solver.skin, bone, op.matrix)
        op.alpha = alpha
        op.params[0] = eye.center.x
        op.params[1] = if (closed) eye.center.y + eye.radiusY * 0.16f else eye.center.y - eye.radiusY * 0.05f
        op.params[2] = eye.radiusX * if (closed) 1.15f else 1.1f
        op.params[3] = eye.radiusY * if (closed) 0.42f else 0.55f
        op.params[4] = FACE_LINE
    }

    /** 눈꺼풀: 눈 그림 위에서 [cut]만큼(기울기 [slant]) 가린다. 휴지 좌표 다각형. */
    private fun lidClip(op: CatRigidOp, cut: Float, slant: Float, reuse: FloatArray?): FloatArray? {
        if (cut <= 0.01f && slant == 0f) return null
        val c = reuse ?: FloatArray(8)
        val l = op.left - 1f
        val r = op.left + op.width + 1f
        val yl = op.top + op.height * (cut + slant * -0.5f)
        val yr = op.top + op.height * (cut + slant * 0.5f)
        val b = op.top + op.height + 1f
        c[0] = l; c[1] = yl; c[2] = r; c[3] = yr; c[4] = r; c[5] = b; c[6] = l; c[7] = b
        return c
    }

    private fun keepTopClip(op: CatRigidOp, part: Float): FloatArray {
        val l = op.left - 1f
        val r = op.left + op.width + 1f
        val t = op.top - 1f
        val b = op.top + op.height * part
        return floatArrayOf(l, t, r, t, r, b, l, b)
    }

    private fun updateEffects(pose: FloatArray, time: Double) {
        val jump = pose[RigChannel.JUMP.ordinal]
        val rootX = pose[RigChannel.ROOT_X.ordinal]
        identity(shadow.matrix)
        val lift = clamp(jump / 20f, 0f, 0.5f)
        shadow.alpha = SHADOW_ALPHA * (1f - lift)
        shadow.params[0] = skeleton.centerX + rootX
        shadow.params[1] = skeleton.ground + 0.3f
        shadow.params[2] = skeleton.bodyWidth * 0.5f * (1f - lift * 0.3f)
        shadow.params[3] = 2.4f * (1f - lift * 0.3f)
        val z = clamp(pose[RigChannel.ZZZ.ordinal], 0f, 1f)
        zzz.alpha = z
        copy(solver.rigid, CatBone.HEAD, zzz.matrix)
        zzz.params[0] = rig.headBox.right - 4f
        zzz.params[1] = rig.headBox.top + 6f
        zzz.params[2] = 5f
        zzz.params[3] = ((time / ZZZ_PERIOD) % 1.0).toFloat()
    }

    private fun updateSockets() {
        for ((slot, xy) in socketWorld) {
            val p = skeleton.socket(slot)
            val m = if (slot.follow == AttachmentFollow.BODY_SKIN) solver.skin else solver.rigid
            val k = slot.bone.ordinal * 6
            xy[0] = m[k] * p.x + m[k + 2] * p.y + m[k + 4]
            xy[1] = m[k + 1] * p.x + m[k + 3] * p.y + m[k + 5]
        }
    }

    /** OVER_EARS 모자: 귀는 모자 챙 선 아래만 그린다(머리 강체 변환을 따라 돈다). */
    private fun updateHatClip() {
        val item = hat ?: return
        if (item.earLayerMode != EarLayerMode.OVER_EARS) return
        val socket = skeleton.socket(AttachmentSlot.HEAD_HAT)
        val top = socket.y - item.anchorY * item.height
        val brim = top + item.height * item.brimY
        val l = skeleton.centerX - CLIP_REACH
        val r = skeleton.centerX + CLIP_REACH
        val b = brim + CLIP_REACH
        val m = solver.rigid
        val k = CatBone.HEAD.ordinal * 6
        for (ear in earOps) {
            val c = ear.clip ?: continue
            fun put(i: Int, x: Float, y: Float) {
                c[i] = m[k] * x + m[k + 2] * y + m[k + 4]
                c[i + 1] = m[k + 1] * x + m[k + 3] * y + m[k + 5]
            }
            put(0, l, brim); put(2, r, brim); put(4, r, b); put(6, l, b)
        }
    }

    private fun copy(src: FloatArray, bone: CatBone, dst: FloatArray) {
        System.arraycopy(src, bone.ordinal * 6, dst, 0, 6)
    }

    private fun identity(m: FloatArray) {
        m[0] = 1f; m[1] = 0f; m[2] = 0f; m[3] = 1f; m[4] = 0f; m[5] = 0f
    }

    /** 착용 아이템이 그려지는 네 모서리(화면 좌표). 측정, 테스트용. */
    fun itemCorners(slot: AttachmentSlot): FloatArray? {
        val item = character.equipped[slot] ?: return null
        val op = ops.lastOrNull { it.texture == item.art.key } ?: return null
        val out = FloatArray(8)
        when (op) {
            is CatRigidOp -> {
                val m = op.matrix
                val xs = floatArrayOf(op.left, op.left + op.width, op.left + op.width, op.left)
                val ys = floatArrayOf(op.top, op.top, op.top + op.height, op.top + op.height)
                for (i in 0 until 4) {
                    out[i * 2] = m[0] * xs[i] + m[2] * ys[i] + m[4]
                    out[i * 2 + 1] = m[1] * xs[i] + m[3] * ys[i] + m[5]
                }
            }
            is CatMeshOp -> {
                val v = op.vertices
                val last = (op.cols + 1) * (op.rows + 1) - 1
                val idx = intArrayOf(0, op.cols, last, last - op.cols)
                for (i in 0 until 4) {
                    out[i * 2] = v[idx[i] * 2]
                    out[i * 2 + 1] = v[idx[i] * 2 + 1]
                }
            }
            is CatShapeOp -> return null
        }
        return out
    }

    companion object {
        /** 격자 한 칸(u). 원화 34px 정도. */
        const val CELL = 2.8f

        /** 입 그림에서 코가 차지하는 위쪽 비율 */
        const val NOSE_PART = 0.37f
        const val FACE_LINE = 0.9f
        const val SHADOW_ALPHA = 0.14f
        const val ZZZ_PERIOD = 2.4
        private const val CLIP_REACH = 80f
    }
}
