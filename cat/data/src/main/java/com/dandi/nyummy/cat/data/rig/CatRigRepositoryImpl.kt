package com.dandi.nyummy.cat.data.rig

import com.dandi.nyummy.cat.domain.rig.CatRigRepository
import com.dandi.nyummy.cat.entity.rig.AttachmentItemVO
import com.dandi.nyummy.cat.entity.rig.CatRigCharacterVO
import com.dandi.nyummy.cat.entity.rig.CatSkinId
import com.dandi.nyummy.common.domain.helper.CompanionMemoryHelper
import com.dandi.nyummy.common.domain.remoteconfig.RemoteConfigHelper
import com.dandi.nyummy.common.domain.remoteconfig.RemoteConfigKey

/**
 * 리그 고양이를 앱 에셋에서 읽는다. 체형은 스프라이트 냐미와 같이 기기에 기억한 값(없으면 NORMAL)을 쓴다.
 * 그 체형의 파츠 묶음이 없으면 NORMAL로, 스킨이 없으면 기본 냐미로 내려간다.
 */
class CatRigRepositoryImpl(
    private val dataSource: CatRigAssetDataSource,
    private val companionMemory: CompanionMemoryHelper,
    private val remoteConfig: RemoteConfigHelper,
    private val debugStore: CatRigDebugStore,
) : CatRigRepository {

    override fun isEnabled(): Boolean = debugStore.enabledOverride ?: remoteConfig.get(RemoteConfigKey.CatRigEnabled)

    override suspend fun getCharacter(skin: CatSkinId, body: String?): CatRigCharacterVO? {
        val spec = dataSource.getSkin(skin.value) ?: dataSource.getSkin(CatSkinId.NYAMI.value) ?: return null
        val weight = body ?: companionMemory.lastCatWeight() ?: DEFAULT_BODY
        val rig = dataSource.getBodyRig(spec.partSet, weight) ?: dataSource.getBodyRig(spec.partSet, DEFAULT_BODY) ?: return null
        return CatRigCharacterVO(spec = spec, bodyRig = rig)
    }

    override suspend fun getItems(): List<AttachmentItemVO> = dataSource.getItems()

    override fun selectedSkin(): CatSkinId = debugStore.skin?.let { CatSkinId(it) } ?: CatSkinId.NYAMI

    override fun equippedItemIds(): List<String> = debugStore.equipped

    private companion object {
        const val DEFAULT_BODY = "NORMAL"
    }
}
