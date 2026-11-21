package com.dandi.nyummy.cat.domain.rig

import com.dandi.nyummy.cat.entity.rig.AttachmentItemVO
import com.dandi.nyummy.cat.entity.rig.AttachmentSlot
import com.dandi.nyummy.cat.entity.rig.CatBodyRigVO
import com.dandi.nyummy.cat.entity.rig.CatCharacterSpec
import com.dandi.nyummy.cat.entity.rig.CatRigCharacterVO
import com.dandi.nyummy.cat.entity.rig.CatSkinId
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

/** 기본 리깅 경로: 홈, 마이룸은 플래그 없이 늘 리그 고양이를 받는다. 읽지 못할 때만 null(스프라이트로 되돌아감)이다. */
class GetCatRigUseCaseTest {

    private val hat = AttachmentItemVO(id = "hat", slot = AttachmentSlot.HEAD_HAT, width = 10f, height = 5f)
    private val vest = AttachmentItemVO(id = "vest", slot = AttachmentSlot.TORSO_OUTFIT, width = 10f, height = 5f)
    private val glasses = AttachmentItemVO(id = "glasses", slot = AttachmentSlot.FACE_ITEM, width = 10f, height = 5f)

    private class FakeRepository(
        var character: (CatSkinId) -> CatRigCharacterVO? = { CatRigCharacterVO(CatCharacterSpec(id = it), CatBodyRigVO(body = "NORMAL")) },
        var items: List<AttachmentItemVO> = emptyList(),
        var skin: CatSkinId = CatSkinId.NYAMI,
        var equipped: List<String> = emptyList(),
    ) : CatRigRepository {
        var itemReads = 0
        override suspend fun getCharacter(skin: CatSkinId, body: String?): CatRigCharacterVO? = character(skin)
        override suspend fun getItems(): List<AttachmentItemVO> {
            itemReads++
            return items
        }
        override fun selectedSkin(): CatSkinId = skin
        override fun equippedItemIds(): List<String> = equipped
    }

    @Test
    fun `설정 없이도 기본 냐미를 돌려준다`() = runBlocking {
        val repository = FakeRepository()

        val rig = GetCatRigUseCase(repository)()

        assertEquals(CatSkinId.NYAMI, rig?.spec?.id)
        assertEquals("NORMAL", rig?.bodyRig?.body)
        assertTrue(rig!!.equipped.isEmpty())
        assertEquals("끼운 아이템이 없으면 아이템 목록을 읽지 않는다", 0, repository.itemReads)
    }

    @Test
    fun `고른 스킨으로 읽고 끼운 아이템만 슬롯별로 붙인다`() = runBlocking {
        val repository = FakeRepository(
            items = listOf(hat, vest, glasses),
            skin = CatSkinId("mackerel"),
            equipped = listOf("vest", "hat", "없는_아이템"),
        )

        val rig = GetCatRigUseCase(repository)()!!

        assertEquals(CatSkinId("mackerel"), rig.spec.id)
        assertEquals(mapOf(AttachmentSlot.HEAD_HAT to hat, AttachmentSlot.TORSO_OUTFIT to vest), rig.equipped)
    }

    @Test
    fun `고양이를 읽지 못하면 null이라 스프라이트로 되돌아간다`() = runBlocking {
        assertNull(GetCatRigUseCase(FakeRepository(character = { null }))())
    }

    @Test
    fun `읽다가 실패해도 다이얼로그 없이 null이다`() = runBlocking {
        assertNull(GetCatRigUseCase(FakeRepository(character = { throw IOException("에셋 없음") }))())
    }

    @Test(expected = CancellationException::class)
    fun `취소는 삼키지 않는다`() {
        runBlocking { GetCatRigUseCase(FakeRepository(character = { throw CancellationException("화면 떠남") }))() }
    }
}
