package com.dandi.nyummy.home.domain

import com.dandi.nyummy.home.entity.HomeNoticeVO
import com.dandi.nyummy.home.entity.HomeSummaryVO
import java.time.LocalDate

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

    /** 원격 구성에 넣은 새 소식 공지를 JSON 순서대로 읽는다. 받아 둔 값만 읽고, 비었거나 형식이 맞지 않으면 빈 목록이다. */
    fun getNotices(): List<HomeNoticeVO>

    /** 새 소식을 "오늘 하루 보지 않기"로 닫은 날(기기 날짜). 그런 적이 없으면 null. */
    suspend fun getNoticeHiddenDate(): LocalDate?

    /** 새 소식을 [date] 하루 동안 띄우지 않게 기기에 남긴다. */
    suspend fun setNoticeHiddenDate(date: LocalDate)

    /** 이번 실행에서 새 소식을 이미 보였거나 띄우지 않기로 했는지. 앱을 다시 실행하면 false로 돌아간다. */
    fun isNoticeHandled(): Boolean

    /** 이번 실행에서는 새 소식을 다시 띄우지 않게 표시한다. */
    fun markNoticeHandled()
}
