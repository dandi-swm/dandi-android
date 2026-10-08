package com.dandi.nyummy.tti

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TTIInfoTest {

    @Test
    fun `측정하기로 한 구간이 빠지면 is_bounced 로 표시한다`() {
        val info = TTIInfo(TwoTimelinePage)

        info.recordStartTime(TimelineCategory.API_RESPONSE_TIME)
        info.recordEndTime(TimelineCategory.API_RESPONSE_TIME)

        assertEquals(true, info.getTTIInfo()["tti.is_bounced"])
    }

    @Test
    fun `끝만 있고 시작이 없는 구간은 -1 로 두고 is_bounced 로 표시한다`() {
        val info = TTIInfo(TwoTimelinePage)

        info.recordEndTime(TimelineCategory.VIEW_BINDING_TIME)

        val result = info.getTTIInfo()
        assertEquals(-1, result["tti.view_binding_time"])
        assertEquals(true, result["tti.is_bounced"])
    }

    @Test
    fun `시작만 있고 끝이 없는 구간도 -1 로 둔다`() {
        val info = TTIInfo(TwoTimelinePage)

        info.recordStartTime(TimelineCategory.API_RESPONSE_TIME)

        assertEquals(-1, info.getTTIInfo()["tti.api_response_time"])
    }

    @Test
    fun `먼저 찍은 시작 시각을 유지한다`() {
        val info = TTIInfo(TwoTimelinePage)

        info.recordStartTime(TimelineCategory.API_RESPONSE_TIME)
        Thread.sleep(5)
        info.recordStartTime(TimelineCategory.API_RESPONSE_TIME)
        info.recordEndTime(TimelineCategory.API_RESPONSE_TIME)

        // 두 번째 시작으로 덮어쓰지 않으므로 구간은 최소 5ms 이상이다.
        assertTrue((info.getTTIInfo()["tti.api_response_time"] as Long) >= 5_000_000L)
    }

    @Test
    fun `결과에 인스턴스 번호와 기본 메타데이터가 들어간다`() {
        val info = TTIInfo(TwoTimelinePage, instanceNo = 3L)

        val result = info.getTTIInfo()

        assertEquals("two_timeline", result["tti.page_name"])
        assertEquals(3L, result["tti.instance_no"])
        assertEquals(false, result["tti.is_timeout"])
        assertEquals(TTI_LOG_VERSION, result["tti.tti_log_version"])
        assertTrue(info.ttiKey.startsWith("two_timeline#3_"))
    }

    private object TwoTimelinePage : TTIPage {
        override val pageName = "two_timeline"
        override val timelines = listOf(TimelineCategory.API_RESPONSE_TIME, TimelineCategory.VIEW_BINDING_TIME)
    }
}
