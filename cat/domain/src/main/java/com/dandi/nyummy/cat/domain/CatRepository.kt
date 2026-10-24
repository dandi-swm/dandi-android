package com.dandi.nyummy.cat.domain

import com.dandi.nyummy.cat.entity.CatAnimationSetVO

interface CatRepository {
    /** 내 고양이 체형에 맞는 상태별 애니메이션을 조회한다. [prefetchAnimations]로 받아 둔 결과가 있으면 그것을 넘겨준다. */
    suspend fun getAnimations(): CatAnimationSetVO

    /**
     * 애니메이션을 미리 받아 둔다. 받은 결과는 다음 [getAnimations] 한 번만 이어받고, 그 뒤 조회는 다시 새로 받는다.
     * 받기는 앱 전역에서 돌아, 기다리던 쪽이 먼저 끝나도 끊기지 않는다. 이미 받는 중이면 그 요청을 기다리고,
     * 이전 미리 받기가 끝나 있으면 새로 받는다. 받은 결과를 돌려주며, 로그인 전이거나 받지 못했으면 null이다.
     */
    suspend fun prefetchAnimations(): CatAnimationSetVO?
}
