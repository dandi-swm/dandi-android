package com.dandi.nyummy.cat.data.rig

import com.dandi.nyummy.cat.domain.rig.CatClipLibrary
import com.dandi.nyummy.cat.domain.rig.CatMeshOp
import com.dandi.nyummy.cat.domain.rig.CatRigAnimator
import com.dandi.nyummy.cat.domain.rig.CatRigModel
import com.dandi.nyummy.cat.domain.rig.CatSkeleton
import com.dandi.nyummy.cat.entity.CatState
import com.dandi.nyummy.cat.entity.rig.AttachmentSlot
import com.dandi.nyummy.cat.entity.rig.CatBone
import com.dandi.nyummy.cat.entity.rig.CatSkinId
import com.dandi.nyummy.cat.entity.rig.CatZOrder
import com.dandi.nyummy.cat.entity.rig.PartMaterial
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.random.Random

/** 에셋, 동작 전수, 착용 슬롯, z 순서, 체형 비율, 스킨 데이터 교체, 프레임률 독립을 실제 에셋으로 검사한다. */
class CatRigSlotAndSkinTest {

    @Test
    fun `체형 5종 파츠 묶음과 스킨 2종, 아이템 5종을 읽는다`() {
        for (body in RigFixtures.BODIES) {
            val rig = RigFixtures.bodyRig(body)
            assertEquals(body, rig.body)
            assertEquals(listOf("tail", "body", "bandana", "earL", "earR", "head", "eyeL", "eyeR", "mouth"), rig.layers.map { it.id })
            assertEquals(118f, rig.ground, 0f)
            assertEquals(64f, rig.neck.x, 0.001f)
        }
        assertEquals(CatSkinId.NYAMI, RigFixtures.skin("nyami").id)
        assertEquals(CatSkinId("mackerel"), RigFixtures.skin("mackerel").id)
        assertEquals(5, RigFixtures.items.size)
    }

    @Test
    fun `체형 5종의 모든 상황, 모든 동작이 이름으로 리그 클립을 찾는다(미재생 0)`() {
        var count = 0
        for (body in RigFixtures.BODIES) for ((state, animation) in RigFixtures.animations(body).animations) {
            for (motion in animation.motions) {
                assertTrue("$body $state ${motion.name}", CatClipLibrary.hasClip(motion.name))
                val clip = CatClipLibrary.clipFor(motion.name, state)
                assertFalse("$body $state ${motion.name} 대기 클립으로 떨어짐", clip.name == CatClipLibrary.IDLE)
                count++
            }
        }
        // 체형 5 x (상황별 동작 합 74)
        assertEquals(5 * 74, count)
    }

    @Test
    fun `상황 기본 클립과 모르는 동작은 대기로 내려간다`() {
        assertEquals(CatClipLibrary.TAP_REACT, CatClipLibrary.clipFor("새_동작", CatState.HOME_TAP).name)
        assertEquals(CatClipLibrary.SLEEP_LOOP, CatClipLibrary.clipFor("새_동작", CatState.HOME_NIGHT).name)
        assertEquals(CatClipLibrary.IDLE, CatClipLibrary.clipFor("새_동작").name)
    }

    @Test
    fun `프레임률이 달라도 같은 시각의 포즈는 같다`() {
        val model = CatRigModel(RigFixtures.character("nyami", "NORMAL"))
        val rates = listOf(listOf(1f / 120), listOf(1f / 60), listOf(1f / 30), listOf(1f / 60, 1f / 30, 1f / 120, 1f / 40))
        val results = rates.map { pattern ->
            val animator = CatRigAnimator(model.skeleton, Random(1))
            animator.play("pop", CatState.HOME_TAP)
            var t = 0f
            var i = 0
            while (t < 1.2f - 1e-4f) {
                val dt = minOf(pattern[i++ % pattern.size], 1.2f - t)
                animator.advance(dt)
                t += dt
            }
            animator.pose.copyOf() to animator.time
        }
        for ((pose, time) in results.drop(1)) {
            assertEquals(results[0].second, time, 1e-6)
            for (i in pose.indices) assertEquals(results[0].first[i], pose[i], 1e-4f)
        }
    }

    @Test
    fun `전부 끼우면 그리는 순서가 z 표와 같고 목 장식은 기본 반다나를 숨긴다`() {
        val model = CatRigModel(RigFixtures.character("nyami", "NORMAL", "full_between"))
        val order = model.frame.ops.map { it.texture }
        fun at(key: String) = order.indexOf(key).also { assertTrue("$key 없음", it >= 0) }
        assertFalse("기본 반다나가 남음", "part:bandana" in order)
        val chain = listOf(
            "shape:SHADOW", "part:tail", "part:body", "sample:vest", "sample:bell_collar", "sample:crown_back",
            "part:earL", "part:head", "part:eyeL", "part:mouth", "sample:glasses", "sample:crown",
        )
        chain.zipWithNext().forEach { (a, b) -> assertTrue("$a 가 $b 보다 앞", at(a) < at(b)) }
        val z = model.frame.ops.map { it.z }
        assertEquals(z.sorted(), z)
        assertEquals(CatZOrder.HAT_BACK, model.frame.ops.first { it.texture == "sample:crown_back" }.z)
    }

    @Test
    fun `OVER_EARS 모자는 귀를 챙 선 위에서 자르고 BETWEEN_EARS 모자는 자르지 않는다`() {
        val over = CatRigModel(RigFixtures.character("nyami", "NORMAL", "beanie"))
        val between = CatRigModel(RigFixtures.character("nyami", "NORMAL", "crown"))
        over.update(CatRigAnimator(over.skeleton).pose, 0.0)
        between.update(CatRigAnimator(between.skeleton).pose, 0.0)
        val overEars = over.frame.ops.filter { it.texture == "part:earL" || it.texture == "part:earR" }
        val betweenEars = between.frame.ops.filter { it.texture == "part:earL" || it.texture == "part:earR" }
        assertTrue(overEars.all { it.clip != null })
        assertTrue(betweenEars.all { it.clip == null })
        // 챙 선은 모자 그림 안에 있다(모자 아래 끝보다 위)
        val clip = overEars.first().clip!!
        val hat = over.itemCorners(AttachmentSlot.HEAD_HAT)!!
        assertTrue(clip[1] < hat[5])
        // 기본 반다나는 모자만 쓰면 그대로 있다
        assertTrue(over.frame.ops.any { it.texture == "part:bandana" })
    }

    @Test
    fun `모자와 안경은 체형 5종에서 크기가 같고 목 장식과 옷은 체형 폭을 따른다`() {
        fun width(c: FloatArray) = hypot(c[2] - c[0], c[3] - c[1])
        val hat = HashMap<String, Float>()
        val glasses = HashMap<String, Float>()
        val collar = HashMap<String, Float>()
        val vest = HashMap<String, Float>()
        for (body in RigFixtures.BODIES) {
            val model = CatRigModel(RigFixtures.character("nyami", body, "full_over"))
            model.update(CatRigAnimator(model.skeleton).pose, 0.0)
            hat[body] = width(model.itemCorners(AttachmentSlot.HEAD_HAT)!!)
            glasses[body] = width(model.itemCorners(AttachmentSlot.FACE_ITEM)!!)
            collar[body] = width(model.itemCorners(AttachmentSlot.NECK_ACCESSORY)!!)
            vest[body] = width(model.itemCorners(AttachmentSlot.TORSO_OUTFIT)!!)
            val s = model.skeleton
            assertEquals(s.neckWidthScale, collar.getValue(body) / 40f, 0.03f)
            assertEquals(s.bodyWidthScale, vest.getValue(body) / 44f, 0.05f)
        }
        val h = hat.getValue("NORMAL")
        val g = glasses.getValue("NORMAL")
        for (body in RigFixtures.BODIES) {
            assertEquals("$body 모자 폭", 0f, abs(hat.getValue(body) / h - 1f), 0.001f)
            assertEquals("$body 안경 폭", 0f, abs(glasses.getValue(body) / g - 1f), 0.001f)
        }
        // 목 장식과 옷은 체형이 클수록 넓다
        assertTrue(collar.getValue("PLUMP") > collar.getValue("NORMAL") && collar.getValue("NORMAL") > collar.getValue("LEAN"))
        assertTrue(vest.getValue("PLUMP") > vest.getValue("NORMAL"))
    }

    @Test
    fun `두 번째 스킨은 코드 수정 없이 JSON만으로 색, 귀 피벗, 반다나가 바뀐다`() {
        val nyami = CatRigModel(RigFixtures.character("nyami", "NORMAL"))
        val mackerel = CatRigModel(RigFixtures.character("mackerel", "NORMAL"))
        val spec = mackerel.character.spec
        assertFalse(spec.colorOf(PartMaterial.FUR).isIdentity)
        assertTrue(spec.colorOf(PartMaterial.FACE).isIdentity)
        assertTrue(nyami.showsBandana)
        assertFalse(mackerel.showsBandana)
        val rest = CatRigAnimator(nyami.skeleton).pose
        nyami.update(rest, 0.0)
        mackerel.update(rest, 0.0)
        // 귀 본이 JSON의 오프셋만큼 옮겨져 그려진다
        assertEquals(-0.8f, mackerel.solver.worldX(CatBone.EAR_L) - nyami.solver.worldX(CatBone.EAR_L), 1e-3f)
        assertEquals(0.6f, mackerel.solver.worldY(CatBone.EAR_L) - nyami.solver.worldY(CatBone.EAR_L), 1e-3f)
        val earN = nyami.frame.ops.first { it.texture == "part:earL" } as CatMeshOp
        val earM = mackerel.frame.ops.first { it.texture == "part:earL" } as CatMeshOp
        // 귀 끝 쪽 꼭짓점은 오프셋만큼 옮겨지고, 머리에 붙은 뿌리 쪽은 머리를 따라 그대로다(이음새가 벌어지지 않음)
        val shifts = (0 until earN.vertices.size / 2).map { earM.vertices[it * 2] - earN.vertices[it * 2] }
        assertEquals(-0.8f, shifts.min(), 0.05f)
        assertEquals(0f, shifts.minOf { abs(it) }, 0.05f)
        // 같은 클립 전부와 착용 슬롯 전부가 그대로 돈다(전수 품질은 CatRigMatrixTest)
        val dressed = CatRigModel(RigFixtures.character("mackerel", "PLUMP", "full_between"))
        assertNotNull(dressed.itemCorners(AttachmentSlot.HEAD_HAT))
        assertNull(dressed.frame.ops.firstOrNull { it.texture == "part:bandana" })
    }

    @Test
    fun `스켈레톤은 체형마다 원화 기준점으로 만들고 발바닥 앵커는 모두 (64, 118)이다`() {
        for (body in RigFixtures.BODIES) {
            val s = CatSkeleton(RigFixtures.bodyRig(body))
            assertEquals(64f, s.pivotX(CatBone.ROOT), 0f)
            assertEquals(118f, s.pivotY(CatBone.ROOT), 0f)
            assertTrue("$body 머리가 목 위", s.pivotY(CatBone.HEAD) < s.pivotY(CatBone.NECK))
            assertTrue("$body 귀가 머리 위", s.pivotY(CatBone.EAR_L) < s.pivotY(CatBone.HEAD))
            assertTrue("$body 꼬리 마디 순서", s.pivotY(CatBone.TAIL_3) < s.pivotY(CatBone.TAIL_1))
        }
    }
}
