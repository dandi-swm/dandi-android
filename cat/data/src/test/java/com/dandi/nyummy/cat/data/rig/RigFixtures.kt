package com.dandi.nyummy.cat.data.rig

import com.dandi.nyummy.cat.data.dto.CatAnimationSetDTO
import com.dandi.nyummy.cat.entity.CatAnimationSetVO
import com.dandi.nyummy.cat.entity.rig.AttachmentItemVO
import com.dandi.nyummy.cat.entity.rig.AttachmentSlot
import com.dandi.nyummy.cat.entity.rig.CatBodyRigVO
import com.dandi.nyummy.cat.entity.rig.CatCharacterSpec
import com.dandi.nyummy.cat.entity.rig.CatRigCharacterVO
import kotlinx.serialization.json.Json
import java.io.File

/** 실제 앱 에셋(cat/data/src/main/assets)을 읽는 테스트 도우미. 테스트 작업 폴더는 모듈 폴더다. */
object RigFixtures {
    val BODIES = listOf("LEAN", "SLIM", "NORMAL", "CHUBBY", "PLUMP")
    val SKINS = listOf("nyami", "mackerel")

    private val assets = File("src/main/assets")
    private val json = Json { ignoreUnknownKeys = true }

    fun read(path: String): String = File(assets, path).readText()

    fun bodyRig(body: String, partSet: String = "nyami"): CatBodyRigVO {
        val dir = CatRigParser.partDir(partSet, body)
        return requireNotNull(CatRigParser.bodyRig(read(dir + CatRigParser.RIG_FILE), dir)) { "rig.json 없음: $body" }
    }

    fun skin(id: String): CatCharacterSpec = requireNotNull(CatRigParser.skin(read(CatRigParser.skinPath(id)))) { "스킨 없음: $id" }

    val items: List<AttachmentItemVO> by lazy { CatRigParser.items(read(CatRigParser.ITEMS_PATH)) }

    fun item(id: String): AttachmentItemVO = items.first { it.id == id }

    /** 착용 조합 이름 -> 슬롯별 아이템 */
    val OUTFITS: Map<String, Map<AttachmentSlot, AttachmentItemVO>> by lazy {
        mapOf(
            "none" to emptyMap(),
            "beanie" to listOf(item("sample_beanie")).bySlot(),
            "crown" to listOf(item("sample_crown")).bySlot(),
            "full_over" to listOf("sample_beanie", "sample_vest", "sample_bell_collar", "sample_glasses").map(::item).bySlot(),
            "full_between" to listOf("sample_crown", "sample_vest", "sample_bell_collar", "sample_glasses").map(::item).bySlot(),
        )
    }

    private fun List<AttachmentItemVO>.bySlot() = associateBy { it.slot }

    fun character(skin: String, body: String, outfit: String = "none"): CatRigCharacterVO {
        val spec = skin(skin)
        return CatRigCharacterVO(spec = spec, bodyRig = bodyRig(body, spec.partSet), equipped = OUTFITS.getValue(outfit))
    }

    /** 스프라이트 동작 목록(체형별 animations.json). */
    fun animations(body: String): CatAnimationSetVO =
        json.decodeFromString<CatAnimationSetDTO>(read("cats/$body/animations.json")).toVO(body, "")
}
