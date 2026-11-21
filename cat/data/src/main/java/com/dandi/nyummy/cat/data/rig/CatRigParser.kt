package com.dandi.nyummy.cat.data.rig

import com.dandi.nyummy.cat.entity.rig.AttachmentItemVO
import com.dandi.nyummy.cat.entity.rig.CatBodyRigVO
import com.dandi.nyummy.cat.entity.rig.CatCharacterSpec
import kotlinx.serialization.json.Json

/** 리그 에셋 JSON 해석과 경로 규칙. Android에 의존하지 않아 JVM 테스트가 실제 에셋 파일로 그대로 쓴다. */
object CatRigParser {
    const val ROOT = "cats/rig"
    const val RIG_FILE = "rig.json"
    const val ITEMS_PATH = "$ROOT/items.json"

    private val json = Json { ignoreUnknownKeys = true }

    /** 파츠 묶음 폴더(끝에 /) */
    fun partDir(partSet: String, body: String): String = "$ROOT/parts/$partSet/$body/"

    fun skinPath(id: String): String = "$ROOT/skins/$id.json"

    fun bodyRig(raw: String, assetDir: String): CatBodyRigVO? =
        runCatching { json.decodeFromString<CatBodyRigDTO>(raw) }.getOrNull()?.toVO(assetDir)

    fun skin(raw: String): CatCharacterSpec? =
        runCatching { json.decodeFromString<CatSkinDTO>(raw) }.getOrNull()?.toVO()

    fun items(raw: String): List<AttachmentItemVO> =
        runCatching { json.decodeFromString<CatRigItemsDTO>(raw) }.getOrNull()?.toVO().orEmpty()
}
