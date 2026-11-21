package com.dandi.nyummy.cat.domain.rig

import com.dandi.nyummy.cat.entity.rig.AttachmentItemVO
import com.dandi.nyummy.cat.entity.rig.CatRigCharacterVO
import com.dandi.nyummy.cat.entity.rig.CatSkinId

/**
 * 리그 고양이 데이터(홈, 마이룸 냐미). 동작 이름과 상황은 스프라이트 냐미([com.dandi.nyummy.cat.domain.CatRepository])의
 * 애니메이션 정보를 그대로 쓰고, 그림만 이 저장소에서 읽는다.
 */
interface CatRigRepository {
    /** 지금 체형(기억해 둔 값, 없으면 NORMAL)으로 [skin] 고양이를 읽는다. 스킨이나 파츠가 없으면 null이다. */
    suspend fun getCharacter(skin: CatSkinId, body: String? = null): CatRigCharacterVO?

    /** 끼울 수 있는 아이템 전부. */
    suspend fun getItems(): List<AttachmentItemVO>

    /** 화면이 그릴 스킨과 끼운 아이템 id. 착용 서버 API가 생기기 전까지는 debug 빌드 설정에서만 바뀐다. */
    fun selectedSkin(): CatSkinId

    fun equippedItemIds(): List<String>
}
