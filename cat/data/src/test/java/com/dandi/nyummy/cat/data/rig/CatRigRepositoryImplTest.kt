package com.dandi.nyummy.cat.data.rig

import com.dandi.nyummy.cat.entity.rig.AttachmentSlot
import com.dandi.nyummy.cat.entity.rig.CatSkinId
import com.dandi.nyummy.common.domain.helper.CompanionMemoryHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** 기본 리깅 경로의 저장소: 실제 앱 에셋에서 체형, 스킨, 아이템을 읽고 debug 설정으로 스킨과 착용을 바꾼다. */
class CatRigRepositoryImplTest {

    private val assets = File("src/main/assets")

    private class Memory(var weight: String?) : CompanionMemoryHelper {
        override suspend fun lastSeenAtMillis(): Long? = null
        override suspend fun setLastSeenAtMillis(millis: Long) = Unit
        override suspend fun lastCatWeight(): String? = weight
        override suspend fun setLastCatWeight(weight: String) {
            this.weight = weight
        }
        override fun catName(): Flow<String?> = flowOf(null)
        override suspend fun setCatName(name: String) = Unit
        override suspend fun clear() = Unit
    }

    private class Settings(override var skin: String? = null, override var equipped: List<String> = emptyList()) : CatRigDebugSettings

    private fun repository(
        weight: String? = null,
        settings: CatRigDebugSettings = Settings(),
        read: (String) -> String? = { path -> File(assets, path).takeIf { it.exists() }?.readText() },
    ) = CatRigRepositoryImpl(CatRigAssetDataSource(read), Memory(weight), settings)

    @Test
    fun `기억한 체형이 없으면 기본 냐미 NORMAL이다`() = runBlocking {
        val repo = repository()

        val character = repo.getCharacter(repo.selectedSkin())!!

        assertEquals(CatSkinId.NYAMI, character.spec.id)
        assertEquals("NORMAL", character.bodyRig.body)
        assertTrue(character.bodyRig.layers.isNotEmpty())
        assertTrue(repo.equippedItemIds().isEmpty())
    }

    @Test
    fun `기억한 체형의 파츠를 읽는다`() = runBlocking {
        for (body in RigFixtures.BODIES) {
            assertEquals(body, repository(weight = body).getCharacter(CatSkinId.NYAMI)!!.bodyRig.body)
        }
    }

    @Test
    fun `모르는 체형은 NORMAL, 모르는 스킨은 기본 냐미로 내려간다`() = runBlocking {
        val character = repository(weight = "HUGE").getCharacter(CatSkinId("tiger"))!!

        assertEquals(CatSkinId.NYAMI, character.spec.id)
        assertEquals("NORMAL", character.bodyRig.body)
    }

    @Test
    fun `에셋을 하나도 읽지 못하면 null이다`() = runBlocking {
        assertNull(repository(read = { null }).getCharacter(CatSkinId.NYAMI))
    }

    @Test
    fun `debug 설정의 스킨과 끼운 아이템을 따른다`() = runBlocking {
        val repo = repository(weight = "PLUMP", settings = Settings(skin = "mackerel", equipped = listOf("sample_crown", "sample_vest")))

        assertEquals(CatSkinId("mackerel"), repo.selectedSkin())
        assertEquals(listOf("sample_crown", "sample_vest"), repo.equippedItemIds())
        val character = repo.getCharacter(repo.selectedSkin())!!
        assertEquals(CatSkinId("mackerel"), character.spec.id)
        assertFalse("고등어 스킨은 기본 반다나를 숨긴다", character.spec.showDefaultBandana)
        assertEquals("PLUMP", character.bodyRig.body)
    }

    @Test
    fun `샘플 아이템은 공식 원화 외곽선 색을 데이터로 갖는다`() = runBlocking {
        val items = repository().getItems()

        assertEquals(5, items.size)
        assertTrue(items.all { it.lineColor == OFFICIAL_LINE })
        val vest = items.first { it.slot == AttachmentSlot.TORSO_OUTFIT }
        // 디자이너 폴리싱: 조끼 윗변을 가슴 쪽으로 4u 올리고, 아랫변은 전과 같은 자리(앞발을 가리지 않는다). 전: 높이 17u, anchorY 0.1
        assertEquals("소켓 위 길이", 17f * 0.1f + 4f, vest.height * vest.anchorY, 0.05f)
        assertEquals("소켓 아래 길이", 17f * 0.9f, vest.height * (1f - vest.anchorY), 0.05f)
    }

    private companion object {
        /** 기준서 C9 공식 외곽선 #764829 */
        const val OFFICIAL_LINE = 0xFF764829.toInt()
    }
}
