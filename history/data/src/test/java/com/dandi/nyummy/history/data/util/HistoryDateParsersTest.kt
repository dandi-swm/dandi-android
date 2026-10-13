package com.dandi.nyummy.history.data.util

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.TimeZone

class HistoryDateParsersTest {

    private val originalTimeZone: TimeZone = TimeZone.getDefault()

    /** 표시 시각은 기기 타임존을 따른다. 기본은 서울로 두고, 다른 지역은 테스트에서 바꾼다. */
    @Before
    fun setUp() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"))
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(originalTimeZone)
    }

    @Test
    fun `오프셋 표기가 달라도 같은 순간이면 같은 시각을 보여준다`() {
        assertEquals("12:10", "2026-09-17T03:10:00Z".toDisplayTime())
        assertEquals("12:10", "2026-09-17T12:10:00+09:00".toDisplayTime())
        assertEquals("12:10", "2026-09-17T03:10:00".toDisplayTime())
    }

    @Test
    fun `자정을 넘기는 UTC 시각도 기기 타임존 기준으로 보여준다`() {
        assertEquals("01:00", "2026-09-16T16:00:00Z".toDisplayTime())
    }

    @Test
    fun `기기 타임존이 바뀌면 그 지역 시각과 날짜로 보여준다`() {
        TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"))

        assertEquals("20:10", "2026-09-17T03:10:00Z".toDisplayTime())
        assertEquals(16, "2026-09-17T03:10:00Z".toDeviceHistoryDate().day)
    }

    @Test
    fun `표시 시각을 파싱하지 못하면 빈 문자열이다`() {
        assertEquals("", null.toDisplayTime())
        assertEquals("", "".toDisplayTime())
        assertEquals("", "20260917".toDisplayTime())
    }

    @Test
    fun `날짜 문자열은 그대로 파싱한다`() {
        assertEquals(2026, "2026-09-17".toHistoryDateVO().year)
        assertEquals(9, "2026-09-17".toHistoryDateVO().month)
        assertEquals(17, "2026-09-17".toHistoryDateVO().day)
    }

    @Test
    fun `날짜 형식이 어긋나면 빈 값이다`() {
        assertEquals(0, null.toHistoryDateVO().year)
        assertEquals(0, "2026/09/17".toHistoryDateVO().year)
    }

    @Test
    fun `정렬용 epoch 은 실제 시간순과 일치한다`() {
        val earlier = "2026-09-17T00:10:00Z".toEpochMillisOrNull()
        val later = "2026-09-17T12:30:00+09:00".toEpochMillisOrNull()

        assertTrue(earlier != null && later != null)
        assertTrue(earlier!! < later!!)
    }

    @Test
    fun `정렬용 epoch 은 파싱 실패 시 null 이다`() {
        assertNull("nope".toEpochMillisOrNull())
    }
}
