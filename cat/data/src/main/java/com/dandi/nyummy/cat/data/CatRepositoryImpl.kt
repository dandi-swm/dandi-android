package com.dandi.nyummy.cat.data

import com.dandi.nyummy.cat.domain.CatRepository
import com.dandi.nyummy.cat.entity.CatAnimationSetVO

class CatRepositoryImpl(
    private val dataSource: CatDataSource,
) : CatRepository {
    override suspend fun getAnimations(): CatAnimationSetVO =
        dataSource.getAnimations().toVO()
}
