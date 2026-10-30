package com.dandi.nyummy.cat.domain

import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * 홈에 가기 전에 내 냐미 체형과 이름을 조회해 기억해 둔다. 애니메이션은 앱에 들어 있어 받을 것이 없고, 체형과 이름만 서버에서 읽는다.
 * 앱을 켜서 홈으로 갈 때는 인트로가, 로그인 직후에는 방 준비 화면이 부르고, 끝날 때까지 기다린다. 로그인 전이면 조회하지 않는다.
 * 그래서 홈의 첫 화면부터 내 냐미 체형으로 그린다. 실패해도 알리지 않는다. 홈은 기억해 둔 체형(없으면 NORMAL)으로 그리고 다시 조회한다.
 */
class PrefetchCatAnimationsUseCase @Inject constructor(
    private val repository: CatRepository,
) {

    suspend operator fun invoke() {
        try {
            repository.refreshWeight()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // 실패는 알리지 않는다. 홈이 화면에 보일 때 다시 조회한다.
        }
    }
}
