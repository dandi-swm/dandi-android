package com.dandi.nyummy.common.presentation.room

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class RoomCardLayoutTest {

    @Test
    fun `세로로 긴 칸은 높이에 맞추고 좌우를 똑같이 자른다`() {
        val rect = RoomCardLayout.roomRect(350.dp, 567.dp)

        assertEquals(567f, rect.bottom.value, 0.01f)
        assertEquals(567f, (rect.bottom - rect.top).value, 0.01f)
        assertEquals(-rect.left.value, (rect.right - 350.dp).value, 0.01f)
    }

    @Test
    fun `가로로 넓은 칸은 폭에 맞추고 위쪽을 자른다`() {
        val rect = RoomCardLayout.roomRect(600.dp, 500.dp)

        assertEquals(0f, rect.left.value, 0.01f)
        assertEquals(600f, rect.right.value, 0.01f)
        assertEquals(500f, rect.bottom.value, 0.01f)
        assertEquals(500f - 600f / 430f * 564f, rect.top.value, 0.01f)
    }

    @Test
    fun `냐미는 방 그림 배율대로 커지고 가운데 바닥 위에 앉는다`() {
        // 350×567 카드(배율 567/564): 냐미 155 × 배율, 카드 바닥에서 116 × 배율 위
        val scale = 567f / 564f
        val rect = RoomCardLayout.nyummyRect(350.dp, 567.dp)

        assertEquals(155f * scale, (rect.right - rect.left).value, 0.01f)
        assertEquals(175f, ((rect.left + rect.right) / 2).value, 0.01f)
        assertEquals(567f - 116f * scale, rect.bottom.value, 0.01f)
    }

    @Test
    fun `가장 긴 카드 비율에서는 방 그림 좌우가 잘려도 되는 몫만큼만 잘린다`() {
        val width = 440.dp
        val rect = RoomCardLayout.roomRect(width, width / RoomCardLayout.MinCardAspect)
        val roomWidth = rect.right - rect.left

        assertEquals(-RoomCardLayout.BleedFraction, rect.left / roomWidth, 0.001f)
    }

    @Test
    fun `아주 작은 칸에서도 냐미는 120 아래로 줄이지 않는다`() {
        val rect = RoomCardLayout.nyummyRect(200.dp, 260.dp)

        assertEquals(120f, (rect.right - rect.left).value, 0.01f)
    }
}
