package com.dandi.nyummy.home.domain

import javax.inject.Inject

/**
 * 홈 요약을 미리 받기 시작한다. 기다리지 않으며, 결과는 홈의 첫 조회([GetHomeSummaryUseCase])가 이어받는다.
 * 인트로(자동 로그인으로 홈에 갈 때)와 방 준비 화면이 냐미 미리 받기와 함께 불러, 냐미를 기다린 뒤에야 홈이 요약을 부르기 시작하지 않게 한다.
 * 실패를 알리는 것도 홈의 첫 조회가 한다.
 */
class PrefetchHomeSummaryUseCase @Inject constructor(
    private val repository: HomeRepository,
) {
    operator fun invoke() = repository.prefetchHomeSummary()
}
