package com.dandi.nyummy.cat.domain

import com.dandi.nyummy.cat.entity.CatAnimationSetVO

interface CatRepository {
    /**
     * 마지막으로 알던 체형(모르면 NORMAL)의 상황별 애니메이션. 네트워크 없이 앱 에셋에서 읽고, 체형마다 한 번만 읽는다.
     * 그 체형의 에셋이 없으면 NORMAL을 쓴다.
     */
    suspend fun getAnimations(): CatAnimationSetVO

    /**
     * 고양이 조회로 체형과 이름을 새로 읽어 기억한다. 그리는 체형이 바뀌었으면 true다. 로그인 전이면 조회하지 않고 false다.
     * 조회에 실패하면 그 실패를 던진다.
     */
    suspend fun refreshWeight(): Boolean
}
