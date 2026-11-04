package com.dandi.nyummy.common.presentation.designsystem.component

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class NyummyBirthDatePickerTest {

    private val today = LocalDate.of(2026, 11, 4)

    @Test
    fun `오늘과 그 전 날짜는 고를 수 있다`() {
        assertFalse(isBirthAfter(2026, 11, 4, latest = today))
        assertFalse(isBirthAfter(2026, 10, 31, latest = today))
        assertFalse(isBirthAfter(2000, 12, 31, latest = today))
    }

    @Test
    fun `오늘 뒤 날짜는 미래로 본다`() {
        assertTrue(isBirthAfter(2026, 11, 5, latest = today))
        assertTrue(isBirthAfter(2026, 12, 1, latest = today))
    }

    @Test
    fun `그달에 없는 일은 마지막 날로 맞춰 본다`() {
        // 2026년 11월 31일은 없어서 30일로 본다. 30일은 오늘(4일)보다 뒤다.
        assertTrue(isBirthAfter(2026, 11, 31, latest = LocalDate.of(2026, 11, 29)))
        assertFalse(isBirthAfter(2026, 11, 31, latest = LocalDate.of(2026, 11, 30)))
    }
}
