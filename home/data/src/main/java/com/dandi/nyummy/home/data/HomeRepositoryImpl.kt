package com.dandi.nyummy.home.data

import com.dandi.nyummy.home.domain.HomeRepository
import com.dandi.nyummy.home.entity.HomeSummaryVO
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async

/**
 * 미리 받은 요약은 조회 한 번만 넘겨준다. 그 뒤 조회는 매번 새로 받아, 식사를 기록하고 돌아오면 바뀐 숫자를 읽는다.
 * 미리 받기는 [ioScope]에서 돌려, 부른 화면(인트로, 방 준비)이 먼저 닫혀도 끊기지 않는다.
 */
class HomeRepositoryImpl(
    private val dataSource: HomeDataSource,
    private val ioScope: CoroutineScope,
) : HomeRepository {

    /**
     * 미리 받는 중이거나 받아 둔 요약. 조회 한 번이 가져가면 비운다.
     * 냐미 미리 받기처럼 화면(ViewModel)이 메인 스레드에서 차례로 읽고 쓰고, 받는 코루틴은 이 값을 건드리지 않아 잠그지 않는다.
     */
    private var prefetched: Deferred<HomeSummaryVO>? = null

    override suspend fun getHomeSummary(): HomeSummaryVO {
        val pending = prefetched ?: return fetch()
        prefetched = null
        // 미리 받기가 실패했으면 다시 받지 않고 그 실패를 넘긴다. 홈은 다음에 화면이 보일 때 다시 받는다.
        return pending.await()
    }

    override fun prefetchHomeSummary() {
        if (prefetched?.isActive == true) return
        prefetched = ioScope.async { fetch() }
    }

    private suspend fun fetch(): HomeSummaryVO = dataSource.getHome().toVO()
}
