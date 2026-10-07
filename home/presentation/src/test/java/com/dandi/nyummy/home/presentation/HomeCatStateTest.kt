package com.dandi.nyummy.home.presentation

import com.dandi.nyummy.cat.entity.CatState
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeCatStateTest {

    @Test
    fun `오늘 기록이 없으면 기다림, 있으면 여유다`() {
        assertEquals(CatState.CONFLICTED_CAUTIOUS, homeCatState(hasRecordedToday = false, recordedJustNow = false, isCelebrating = false))
        assertEquals(CatState.RELAXED, homeCatState(hasRecordedToday = true, recordedJustNow = false, isCelebrating = false))
    }

    @Test
    fun `방금 기록하고 돌아왔거나 먹는 반응 중이면 먹는 반응이다`() {
        assertEquals(CatState.CONTENT, homeCatState(hasRecordedToday = true, recordedJustNow = true, isCelebrating = false))
        assertEquals(CatState.CONTENT, homeCatState(hasRecordedToday = true, recordedJustNow = false, isCelebrating = true))
    }
}
