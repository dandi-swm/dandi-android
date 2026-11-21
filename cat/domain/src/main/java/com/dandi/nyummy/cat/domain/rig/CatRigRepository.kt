package com.dandi.nyummy.cat.domain.rig

import com.dandi.nyummy.cat.entity.rig.AttachmentItemVO
import com.dandi.nyummy.cat.entity.rig.CatRigCharacterVO
import com.dandi.nyummy.cat.entity.rig.CatSkinId

/**
 * 리그 고양이 데이터. 스프라이트 냐미([com.dandi.nyummy.cat.domain.CatRepository])와 따로 둬서
 * 리그를 끈 동안에는 기존 화면이 이 저장소를 전혀 쓰지 않는다.
 */
interface CatRigRepository {
    /** 홈, 마이룸의 냐미를 리그로 그리는지. 원격 구성 `android_cat_rig_enabled`(기본 false)이고, debug 빌드는 기기에서 덮어쓸 수 있다. */
    fun isEnabled(): Boolean

    /** 지금 체형(기억해 둔 값, 없으면 NORMAL)으로 [skin] 고양이를 읽는다. 스킨이나 파츠가 없으면 null이다. */
    suspend fun getCharacter(skin: CatSkinId, body: String? = null): CatRigCharacterVO?

    /** 끼울 수 있는 아이템 전부. */
    suspend fun getItems(): List<AttachmentItemVO>

    /** 화면이 그릴 스킨과 끼운 아이템 id. 착용 서버 API가 생기기 전까지는 debug 빌드 설정에서만 바뀐다. */
    fun selectedSkin(): CatSkinId

    fun equippedItemIds(): List<String>
}
