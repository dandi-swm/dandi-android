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

    /** 손잡이 40, 탭 60, 칸 영역 400인 시트를 다 펼친 채로 잰 상태. */
    private fun measuredSheetWithTabs(): RoomSheetState = RoomSheetState(expanded = true).apply {
        bodyHeightPx = 400
        tabsHeightPx = 60
        onMeasured(heightPx = 500)
    }

    @Test
    fun 펼치거나_접어도_방을_덮는_높이는_손잡이와_탭이고_칸_영역만큼_방을_올린다() {
        val sheet = measuredSheetWithTabs()

        assertEquals(100, sheet.collapsedHeightPx)
        assertEquals(100, sheet.coveredHeightPx)
        assertEquals(400, sheet.liftPx)

        sheet.dragBy(400f)

        assertEquals(0f, sheet.expansion)
        assertEquals(100, sheet.coveredHeightPx)
        assertEquals(0, sheet.liftPx)
        assertEquals(100, sheet.heightPx)
    }

    @Test
    fun 끝까지_내리면_탭까지_내려가_손잡이만_방을_덮고_방은_움직이지_않는다() {
        val sheet = measuredSheetWithTabs()

        sheet.dragBy(400f + 60f)

        assertEquals(-1f, sheet.expansion)
        assertEquals(40, sheet.coveredHeightPx)
        assertEquals(40, sheet.heightPx)
        assertEquals(0, sheet.liftPx)
        assertEquals("다 접은 높이는 숨겨도 같다", 100, sheet.collapsedHeightPx)
    }

    @Test
    fun 숨기는_쪽은_탭_높이만큼_끌면_한_단계이고_더_내려도_숨김에서_멈춘다() {
        val sheet = measuredSheetWithTabs()
        sheet.dragBy(400f)

        sheet.dragBy(30f)
        assertEquals(-0.5f, sheet.expansion)
        assertEquals(70, sheet.coveredHeightPx)

        sheet.dragBy(500f)
        assertEquals(-1f, sheet.expansion)

        sheet.dragBy(-60f)
        assertEquals(0f, sheet.expansion)
    }

    @Test
    fun 탭_높이를_재기_전에는_접힌_데서_더_내려가지_않는다() {
        val sheet = measuredSheet()
        sheet.dragBy(400f)

        sheet.dragBy(100f)

        assertEquals(0f, sheet.expansion)
    }
}
