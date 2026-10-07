package com.dandi.nyummy.home.domain

import com.dandi.nyummy.home.entity.HomeSummaryVO

interface HomeRepository {
    /** 홈 요약(보유 코인, 연속 기록, 오늘 식사 현황)을 조회한다. */
    suspend fun getHomeSummary(): HomeSummaryVO
}
