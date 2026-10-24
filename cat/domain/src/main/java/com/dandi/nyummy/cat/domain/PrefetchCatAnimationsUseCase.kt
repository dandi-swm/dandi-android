package com.dandi.nyummy.cat.domain

import com.dandi.nyummy.cat.entity.CatState
import com.dandi.nyummy.common.domain.helper.ImagePrefetchHelper
import javax.inject.Inject

/**
 * 냐미가 화면에 나오자마자 움직이도록 애니메이션과 시트 그림을 미리 받는다.
 * 앱을 켤 때 인트로가, 로그인 직후에는 방 준비 화면이 부르고, 끝날 때까지 기다린다. 로그인 전이면 받지 않는다.
 * 앱이 재생하는 상태의 시트를 모든 동작 묶음까지 디스크에 받아 둔다. 화면은 묶음을 무작위로 고르기 때문이다.
 * 받은 애니메이션은 홈의 첫 조회([GetCatAnimationsUseCase])가 이어받는다.
 * 실패해도 알리지 않는다. 받지 못한 것은 화면이 그릴 때 다시 받는다.
 */
class PrefetchCatAnimationsUseCase @Inject constructor(
    private val repository: CatRepository,
    private val imagePrefetchHelper: ImagePrefetchHelper,
) {

    suspend operator fun invoke() {
        val animations = repository.prefetchAnimations() ?: return
        // 응답에 없는 상태는 화면처럼 기본 인사(FRIENDLY)로 대신한다.
        val urls = PlayedStates
            .mapNotNull(animations::animationFor)
            .flatMap { animation -> animation.groups.flatten() }
            .map { clip -> clip.url }
            .distinct()
        imagePrefetchHelper.prefetchSpriteSheets(urls)
    }

    private companion object {
        /**
         * 앱이 재생하는 상태. 홈은 기록 전 기다림, 기록 후 여유, 방금 기록하면 먹기(homeCatState)를,
         * 마이룸은 놀이를 재생한다. 재생하는 상태가 늘면 여기에도 더한다.
         */
        val PlayedStates = listOf(CatState.CONFLICTED_CAUTIOUS, CatState.RELAXED, CatState.CONTENT, CatState.PLAYFUL)
    }
}
