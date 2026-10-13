package com.dandi.nyummy.meal.data.util

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.TimeZone

class MealPhotoExifTimeTest {

    private val seoul = ZoneId.of("Asia/Seoul")
    private val originalTimeZone: TimeZone = TimeZone.getDefault()

    /** 넘긴 타임존([seoul])으로 해석돼야 하므로, 기기 기본값은 일부러 다른 지역으로 바꿔 둔다. */
    @Before
    fun setUp() {
        TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"))
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(originalTimeZone)
    }

    @Test
    fun `오프셋이 없으면 넘긴 타임존의 로컬 시각으로 본다`() {
        assertEquals(
            LocalDateTime.of(2026, 9, 30, 12, 34, 56),
            parseExifDateTimeOrNull("2026:09:30 12:34:56", null, seoul),
        )
    }

    @Test
    fun `오프셋이 있으면 그 순간을 넘긴 타임존으로 옮긴다`() {
        // 미국 서부 9월 29일 저녁은 넘긴 타임존(서울)으로 9월 30일 오전이다.
        assertEquals(
            LocalDateTime.of(2026, 9, 30, 11, 0, 0),
            parseExifDateTimeOrNull("2026:09:29 19:00:00", "-07:00", seoul),
        )
        assertEquals(
            LocalDateTime.of(2026, 9, 30, 0, 30, 0),
            parseExifDateTimeOrNull("2026:09:30 00:30:00", "+09:00", seoul),
        )
    }

    @Test
    fun `오프셋이 깨져 있으면 없는 것으로 본다`() {
        assertEquals(
            LocalDateTime.of(2026, 9, 30, 8, 0, 0),
            parseExifDateTimeOrNull("2026:09:30 08:00:00", "   ", seoul),
        )
    }

    @Test
    fun `날짜 구분자가 하이픈이어도 읽는다`() {
        assertEquals(
            LocalDateTime.of(2026, 9, 30, 8, 0, 0),
            parseExifDateTimeOrNull("2026-09-30 08:00:00", null, seoul),
        )
    }

    @Test
    fun `촬영 시각이 없거나 형식이 어긋나면 null 이다`() {
        assertNull(parseExifDateTimeOrNull(null, null, seoul))
        assertNull(parseExifDateTimeOrNull("", "+09:00", seoul))
        assertNull(parseExifDateTimeOrNull("0000:00:00 00:00:00", null, seoul))
        assertNull(parseExifDateTimeOrNull("2026:09:30", null, seoul))
    }
}
