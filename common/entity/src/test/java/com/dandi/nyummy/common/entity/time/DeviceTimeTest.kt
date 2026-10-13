package com.dandi.nyummy.common.entity.time

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class DeviceTimeTest {

    private val seoul = ZoneId.of("Asia/Seoul")
    private val newYork = ZoneId.of("America/New_York")

    @Test
    fun `UTC 시각을 넘긴 타임존의 벽시계로 바꾼다`() {
        assertEquals(LocalDateTime.of(2026, 9, 17, 12, 10), DeviceTime.parseIsoOrNull("2026-09-17T03:10:00Z", seoul)?.toLocalDateTime())
        assertEquals(LocalDateTime.of(2026, 9, 16, 23, 10), DeviceTime.parseIsoOrNull("2026-09-17T03:10:00Z", newYork)?.toLocalDateTime())
    }

    @Test
    fun `오프셋 표기가 달라도 같은 순간이면 같은 epoch 이다`() {
        val expected = DeviceTime.parseIsoToEpochMillisOrNull("2026-09-17T03:10:00Z")

        assertEquals(expected, DeviceTime.parseIsoToEpochMillisOrNull("2026-09-17T12:10:00+09:00"))
        assertEquals(expected, DeviceTime.parseIsoToEpochMillisOrNull("2026-09-16T23:10:00-04:00"))
    }

    @Test
    fun `오프셋이 없으면 UTC 로 본다`() {
        assertEquals(
            DeviceTime.parseIsoToEpochMillisOrNull("2026-09-17T03:10:00Z"),
            DeviceTime.parseIsoToEpochMillisOrNull("2026-09-17T03:10:00"),
        )
    }

    @Test
    fun `소수점 초, 초 생략, 공백 구분자를 허용한다`() {
        val expected = DeviceTime.parseIsoToEpochMillisOrNull("2026-09-17T03:10:00Z")

        assertEquals(expected!! + 123, DeviceTime.parseIsoToEpochMillisOrNull("2026-09-17T03:10:00.123456Z"))
        assertEquals(expected, DeviceTime.parseIsoToEpochMillisOrNull("2026-09-17T03:10Z"))
        assertEquals(expected, DeviceTime.parseIsoToEpochMillisOrNull("2026-09-17 03:10:00Z"))
    }

    @Test
    fun `형식이 어긋나면 null 이다`() {
        assertNull(DeviceTime.parseIsoToEpochMillisOrNull(null))
        assertNull(DeviceTime.parseIsoToEpochMillisOrNull(""))
        assertNull(DeviceTime.parseIsoToEpochMillisOrNull("not-a-date"))
        assertNull(DeviceTime.parseIsoToEpochMillisOrNull("2026-13-45T00:00:00Z"))
        assertNull(DeviceTime.parseIsoToEpochMillisOrNull("2026-02-30T00:00:00Z"))
        assertNull(DeviceTime.parseIsoToEpochMillisOrNull("2026-09-17T25:00:00Z"))
    }

    @Test
    fun `오늘은 넘긴 타임존의 자정 기준으로 바뀐다`() {
        // 2026-09-16T15:30Z = 서울 9/17 00:30, 뉴욕 9/16 11:30
        val millis = DeviceTime.parseIsoToEpochMillisOrNull("2026-09-16T15:30:00Z")!!

        assertEquals(LocalDate.of(2026, 9, 17), DeviceTime.today(millis, seoul))
        assertEquals(LocalDate.of(2026, 9, 16), DeviceTime.today(millis, newYork))
    }

    @Test
    fun `서머타임 구간은 타임존 규칙대로 오프셋이 바뀐다`() {
        val summer = DeviceTime.parseIsoOrNull("2026-07-01T12:00:00Z", newYork)!!
        val winter = DeviceTime.parseIsoOrNull("2026-12-01T12:00:00Z", newYork)!!

        assertEquals(8, summer.hour) // EDT, UTC-4
        assertEquals(7, winter.hour) // EST, UTC-5
    }

    @Test
    fun `요일은 일요일을 0 으로 센다`() {
        assertEquals(4, LocalDate.of(1970, 1, 1).sundayBasedWeekday) // 목요일
        assertEquals(3, LocalDate.of(2026, 7, 1).sundayBasedWeekday) // 수요일
        assertEquals(0, LocalDate.of(2026, 11, 1).sundayBasedWeekday) // 일요일
        assertEquals(6, LocalDate.of(2026, 10, 31).sundayBasedWeekday) // 토요일
    }
}
