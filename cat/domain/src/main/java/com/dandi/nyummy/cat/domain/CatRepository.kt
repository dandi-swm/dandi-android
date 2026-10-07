package com.dandi.nyummy.cat.domain

import com.dandi.nyummy.cat.entity.CatAnimationSetVO

interface CatRepository {
    /** 내 고양이 체형에 맞는 상태별 애니메이션을 조회한다. */
    suspend fun getAnimations(): CatAnimationSetVO
}
