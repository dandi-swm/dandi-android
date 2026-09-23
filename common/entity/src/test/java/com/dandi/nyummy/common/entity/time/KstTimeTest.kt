package com.dandi.nyummy.common.entity.time

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.TimeZone

class KstTimeTest {

    private val originalTimeZone: TimeZone = TimeZone.getDefault()

    /** 기기(JVM) 기본 타임존이 무엇이든 결과가 같아야 하므로, 일부러 KST 가 아닌 곳으로 바꿔 둔다. */
    @Before
    fun setUp() {
        TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"))
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(originalTimeZone)
    }

    @Test
    fun `UTC 시각은 9시간을 더해 KST 로 환산한다`() {
        val parsed = KstTime.parseIsoToKstOrNull("2026-09-17T03:10:00Z")

        assertEquals(KstDateTime(2026, 9, 17, 12, 10), parsed)
    }

    @Test
    fun `UTC 15시는 KST 로 다음 날 자정이다`() {
        val parsed = KstTime.parseIsoToKstOrNull("2026-09-16T15:00:00Z")

        assertEquals(KstDateTime(2026, 9, 17, 0, 0), parsed)
    }

    @Test
    fun `UTC 14시 59분은 아직 같은 날 23시 59분이다`() {
        val parsed = KstTime.parseIsoToKstOrNull("2026-09-17T14:59:59Z")

        assertEquals(KstDateTime(2026, 9, 17, 23, 59, 59), parsed)
    }

    @Test
    fun `연 경계에서도 날짜가 올바르게 넘어간다`() {
        val parsed = KstTime.parseIsoToKstOrNull("2026-12-31T15:00:00Z")

        assertEquals(KstDateTime(2027, 1, 1, 0, 0), parsed)
    }

    @Test
    fun `KST 오프셋이 붙은 시각은 그대로 유지된다`() {
        val parsed = KstTime.parseIsoToKstOrNull("2026-09-17T12:10:00+09:00")

        assertEquals(KstDateTime(2026, 9, 17, 12, 10), parsed)
    }

    @Test
    fun `콜론 없는 오프셋도 해석한다`() {
        val parsed = KstTime.parseIsoToKstOrNull("2026-09-17T12:10:00+0900")

        assertEquals(KstDateTime(2026, 9, 17, 12, 10), parsed)
    }

    @Test
    fun `음수 오프셋도 KST 로 환산한다`() {
        val parsed = KstTime.parseIsoToKstOrNull("2026-09-16T23:10:00-04:00")

        assertEquals(KstDateTime(2026, 9, 17, 12, 10), parsed)
    }

    @Test
    fun `오프셋이 없으면 이미 KST 로컬 시각으로 본다`() {
        val parsed = KstTime.parseIsoToKstOrNull("2026-09-17T12:10:00")

        assertEquals(KstDateTime(2026, 9, 17, 12, 10), parsed)
    }

    @Test
    fun `초와 소수점 이하가 있거나 없어도 같은 분을 돌려준다`() {
        val withFraction = KstTime.parseIsoToKstOrNull("2026-09-17T03:10:00.123456Z")
        val withoutSecond = KstTime.parseIsoToKstOrNull("2026-09-17T03:10Z")
        val spaceSeparated = KstTime.parseIsoToKstOrNull("2026-09-17 03:10:00Z")

        assertEquals("12:10", withFraction?.hourMinuteLabel)
        assertEquals("12:10", withoutSecond?.hourMinuteLabel)
        assertEquals("12:10", spaceSeparated?.hourMinuteLabel)
    }

    @Test
    fun `형식이 어긋난 값은 null 이다`() {
        assertNull(KstTime.parseIsoToKstOrNull(null))
        assertNull(KstTime.parseIsoToKstOrNull(""))
        assertNull(KstTime.parseIsoToKstOrNull("not-a-date"))
        assertNull(KstTime.parseIsoToKstOrNull("2026-13-45T00:00:00Z"))
        assertNull(KstTime.parseIsoToKstOrNull("2026-02-30T00:00:00Z"))
        assertNull(KstTime.parseIsoToKstOrNull("2026-09-17T25:00:00Z"))
    }

    @Test
    fun `윤일도 정상 파싱한다`() {
        val parsed = KstTime.parseIsoToKstOrNull("2028-02-29T00:00:00+09:00")

        assertEquals(KstDateTime(2028, 2, 29, 0, 0), parsed)
    }

    @Test
    fun `epoch 변환은 왕복해도 같은 값이다`() {
        val millis = KstTime.epochMillisOf(2026, 9, 17, 12, 10)

        assertEquals(KstDateTime(2026, 9, 17, 12, 10), KstTime.atKst(millis))
    }

    @Test
    fun `now 는 넘겨준 시각을 KST 로 해석한다`() {
        val millis = KstTime.epochMillisOf(2026, 9, 17, 0, 30)

        assertEquals(KstDateTime(2026, 9, 17, 0, 30), KstTime.now(millis))
    }

    @Test
    fun `KST 는 서머타임이 없어 하루를 더해도 시각이 그대로다`() {
        var millis = KstTime.epochMillisOf(2026, 1, 1, 9, 15)
        repeat(365) {
            millis += 24L * 60L * 60L * 1000L
            val moment = KstTime.atKst(millis)
            assertEquals(9, moment.hour)
            assertEquals(15, moment.minute)
        }
    }

    @Test
    fun `요일은 일요일 기준 0부터 센다`() {
        assertEquals(4, KstTime.sundayBasedWeekdayOf(1970, 1, 1)) // 목요일
        assertEquals(3, KstTime.sundayBasedWeekdayOf(2026, 7, 1)) // 수요일
        assertEquals(0, KstTime.sundayBasedWeekdayOf(2026, 11, 1)) // 일요일
    }

    @Test
    fun `말일은 윤년 규칙을 따른다`() {
        assertEquals(28, KstTime.lengthOfMonth(2026, 2))
        assertEquals(29, KstTime.lengthOfMonth(2028, 2))
        assertEquals(28, KstTime.lengthOfMonth(1900, 2))
        assertEquals(29, KstTime.lengthOfMonth(2000, 2))
        assertEquals(31, KstTime.lengthOfMonth(2026, 12))
        assertEquals(30, KstTime.lengthOfMonth(2026, 11))
    }

    @Test
    fun `epoch day 와 달력 날짜는 서로 왕복한다`() {
        var epochDay = KstTime.epochDayOf(1970, 1, 1)
        val lastEpochDay = KstTime.epochDayOf(2100, 12, 31)
        while (epochDay <= lastEpochDay) {
            val (year, month, day) = KstTime.civilOf(epochDay)
            assertEquals(epochDay, KstTime.epochDayOf(year, month, day))
            epochDay += 397 // 하루씩 돌면 느리므로 윤년·월말을 고루 훑는 간격으로 건너뛴다.
        }
    }

    @Test
    fun `표시 문자열은 두 자리로 맞춘다`() {
        val moment = KstDateTime(2026, 9, 7, 8, 5)

        assertEquals("08:05", moment.hourMinuteLabel)
        assertEquals("2026-09-07", moment.isoDate)
    }

    @Test
    fun `기기 타임존을 바꿔도 결과는 같다`() {
        val expected = KstTime.parseIsoToKstOrNull("2026-09-17T03:10:00Z")

        TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Kiritimati"))
        assertEquals(expected, KstTime.parseIsoToKstOrNull("2026-09-17T03:10:00Z"))

        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        assertEquals(expected, KstTime.parseIsoToKstOrNull("2026-09-17T03:10:00Z"))

        assertTrue(expected != null)
    }
}
