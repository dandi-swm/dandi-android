package com.dandi.nyummy.room.presentation.component

import org.junit.Assert.assertEquals
import org.junit.Test

class RoomSheetStateTest {

    /** 손잡이와 탭 100, 칸 영역 400인 시트를 다 펼친 채로 잰 상태. */
    private fun measuredSheet(): RoomSheetState = RoomSheetState(expanded = true).apply {
        bodyHeightPx = 400
        onMeasured(heightPx = 500)
    }

    @Test
    fun 잰_높이에서_보이던_칸_영역을_빼서_다_접은_높이를_얻는다() {
        val sheet = measuredSheet()

        assertEquals(100, sheet.collapsedHeightPx)
        assertEquals(500, sheet.heightPx)
    }

    @Test
    fun 끄는_동안에는_다시_재기_전에도_펼친_만큼의_높이를_바로_돌려준다() {
        val sheet = measuredSheet()

        sheet.dragBy(200f)

        assertEquals(0.5f, sheet.expansion)
        assertEquals("다 접은 높이는 펼친 정도와 상관없다", 100, sheet.collapsedHeightPx)
        assertEquals(300, sheet.heightPx)
    }

    @Test
    fun 재기_전에는_높이가_0이다() {
        val sheet = RoomSheetState(expanded = true)

        assertEquals(0, sheet.collapsedHeightPx)
        assertEquals(0, sheet.heightPx)
    }
}
