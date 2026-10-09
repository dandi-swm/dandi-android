package com.dandi.nyummy.common.presentation.jank

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FirebaseJankTraceValuesTest {

    private fun snapshot(
        page: String = "/history",
        reason: JankSnapshot.Reason = JankSnapshot.Reason.PAGE_EXIT,
        totalFrames: Int = 200,
        jankFrames: Int = 12,
        states: Map<String, String> = emptyMap(),
    ) = JankSnapshot(
        page = page,
        reason = reason,
        totalFrames = totalFrames,
        jankFrames = jankFrames,
        frozenFrames = 1,
        maxFrameDurationMs = 820L,
        sumFrameDurationMs = 4_000L,
        states = states,
    )

    @Test
    fun `화면 통계를 정수 metric 과 분류 attribute 로 바꾼다`() {
        val values = FirebaseJankTraceValues.from(snapshot())!!

        assertEquals("jank_history", values.traceName)
        assertEquals(
            mapOf(
                "total_frames" to 200L,
                "jank_frames" to 12L,
                "frozen_frames" to 1L,
                "jank_permille" to 60L,
                "avg_frame_ms" to 20L,
                "max_frame_ms" to 820L,
            ),
            values.metrics,
        )
        assertEquals(mapOf("reason" to "page_exit", "during_scroll" to "false"), values.attributes)
    }

    @Test
    fun `버벅임 천분율은 실수 오차 없이 정수로 계산한다`() {
        // 7 / 10 을 실수로 곱하면 699.999... 가 되어 699 로 내려간다.
        val values = FirebaseJankTraceValues.from(snapshot(totalFrames = 10, jankFrames = 7))!!

        assertEquals(700L, values.metrics["jank_permille"])
    }

    @Test
    fun `스크롤 중 멈춘 프레임은 during_scroll 로 표시한다`() {
        val values = FirebaseJankTraceValues.from(
            snapshot(reason = JankSnapshot.Reason.FROZEN_FRAME, states = mapOf("scrolling" to "true")),
        )!!

        assertEquals(mapOf("reason" to "frozen_frame", "during_scroll" to "true"), values.attributes)
    }

    @Test
    fun `스크롤 구간 통계와 빈 통계는 보내지 않는다`() {
        assertNull(FirebaseJankTraceValues.from(snapshot(reason = JankSnapshot.Reason.SCROLL_END)))
        assertNull(FirebaseJankTraceValues.from(snapshot(totalFrames = 0)))
    }

    @Test
    fun `화면 경로를 트레이스 이름으로 쓸 수 있게 바꾼다`() {
        assertEquals("jank_meal_record", FirebaseJankTraceValues.traceNameOf("/meal/record"))
        assertEquals("jank_signup_social", FirebaseJankTraceValues.traceNameOf("/signup/social"))
        assertEquals("jank_intro", FirebaseJankTraceValues.traceNameOf(""))
        assertEquals("jank_article_articleId", FirebaseJankTraceValues.traceNameOf("/article/{articleId}"))
    }
}
