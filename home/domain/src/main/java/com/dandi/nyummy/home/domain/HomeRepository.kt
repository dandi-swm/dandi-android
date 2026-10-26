package com.dandi.nyummy.home.domain

import com.dandi.nyummy.home.entity.HomeSummaryVO

interface HomeRepository {
    /**
     * 홈 요약(보유 코인, 연속 기록, 오늘 식사 현황)을 조회한다.
     * [prefetchHomeSummary]로 받기 시작한 요청이 있으면 새로 부르지 않고 그 결과를 넘겨주며, 실패했으면 그 실패를 던진다.
     */
    suspend fun getHomeSummary(): HomeSummaryVO

    /**
     * 홈 요약을 미리 받기 시작하고 기다리지 않는다. 결과는 다음 [getHomeSummary] 한 번만 이어받고, 그 뒤 조회는 다시 새로 받는다.
     * 받기는 앱 전역에서 돌아, 부른 화면이 먼저 닫혀도 끊기지 않는다. 이미 받는 중이면 새로 받지 않는다.
     */
    fun prefetchHomeSummary()
}
