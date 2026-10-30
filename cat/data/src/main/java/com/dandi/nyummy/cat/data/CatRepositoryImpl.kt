package com.dandi.nyummy.cat.data

import com.dandi.nyummy.cat.domain.CatRepository
import com.dandi.nyummy.cat.entity.CatAnimationSetVO
import com.dandi.nyummy.common.data.token.TokenProvider
import com.dandi.nyummy.common.domain.helper.CompanionMemoryHelper

/**
 * 애니메이션은 앱 에셋에서, 체형은 고양이 조회에서 읽는다. 체형은 기기에 기억해 두어 다음 실행에도 네트워크 없이 바로 그린다.
 * 같은 조회로 받은 이름도 기억해 두어 화면 문구에 쓴다.
 * 읽은 체형별 애니메이션은 메모리에 둔다. 화면(ViewModel)이 메인 스레드에서 차례로 읽고 써서 잠그지 않는다.
 */
class CatRepositoryImpl(
    private val dataSource: CatDataSource,
    private val assetDataSource: CatAssetDataSource,
    private val companionMemory: CompanionMemoryHelper,
    private val tokenProvider: TokenProvider,
) : CatRepository {

    private val cached = mutableMapOf<String, CatAnimationSetVO>()

    override suspend fun getAnimations(): CatAnimationSetVO {
        val weight = companionMemory.lastCatWeight() ?: DEFAULT_WEIGHT
        return load(weight) ?: load(DEFAULT_WEIGHT) ?: CatAnimationSetVO.empty
    }

    override suspend fun refreshWeight(): Boolean {
        if (tokenProvider.refreshToken.isNullOrBlank()) return false
        val cat = dataSource.getCat()
        cat.name?.trim()?.takeIf { it.isNotEmpty() }?.let { companionMemory.setCatName(it) }
        val weight = cat.weight?.takeIf { it.isNotBlank() } ?: return false
        val previous = companionMemory.lastCatWeight()
        if (weight == previous) return false
        companionMemory.setLastCatWeight(weight)
        return weight != (previous ?: DEFAULT_WEIGHT)
    }

    private suspend fun load(weight: String): CatAnimationSetVO? = cached[weight]
        ?: assetDataSource.getAnimations(weight)
            ?.toVO(weight, assetDataSource.baseUrl(weight))
            ?.takeIf { it.animations.isNotEmpty() }
            ?.also { cached[weight] = it }

    private companion object {
        const val DEFAULT_WEIGHT = "NORMAL"
    }
}
