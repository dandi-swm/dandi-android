package com.dandi.nyummy.common.presentation.room

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class RoomCardLayoutTest {

    @Test
    fun `카드는 2대3으로 폭을 먼저 채우고 높이가 모자라면 같은 비율로 줄인다`() {
        // S24 울트라 홈: 폭 344, 남는 높이 517이면 폭을 다 쓴다.
        assertEquals(DpSize(344.dp, 516.dp), RoomCardLayout.cardSize(344.dp, 517.dp))
        // 높이가 480밖에 없으면 폭을 320으로 줄인다.
        assertEquals(DpSize(320.dp, 480.dp), RoomCardLayout.cardSize(344.dp, 480.dp))
    }

    @Test
    fun `2대3 카드에서는 방 그림이 카드에 꼭 맞아 잘리는 곳이 없다`() {
        val rect = RoomCardLayout.roomRect(344.dp, 516.dp)

        assertEquals(0f, rect.left.value, 0.01f)
        assertEquals(0f, rect.top.value, 0.01f)
        assertEquals(344f, rect.right.value, 0.01f)
        assertEquals(516f, rect.bottom.value, 0.01f)
    }

    @Test
    fun `2대3이 아닌 칸에서는 넘치는 쪽이 칸 밖으로 나가고 바닥은 맞춘다`() {
        val tall = RoomCardLayout.roomRect(300.dp, 600.dp)
        assertEquals(600f, tall.bottom.value, 0.01f)
        assertEquals(-tall.left.value, (tall.right - 300.dp).value, 0.01f)

        val wide = RoomCardLayout.roomRect(600.dp, 600.dp)
        assertEquals(0f, wide.left.value, 0.01f)
        assertEquals(600f, wide.bottom.value, 0.01f)
        assertEquals(600f - 900f, wide.top.value, 0.01f)
    }

    @Test
    fun `냐미는 카드 폭의 36퍼센트이고 가운데 바닥 위에 앉는다`() {
        // 344×516 카드(배율 344/430 = 0.8): 냐미 155 × 0.8 = 124, 카드 바닥에서 133 × 0.8 위
        val rect = RoomCardLayout.nyummyRect(344.dp, 516.dp)

        assertEquals(124f, (rect.right - rect.left).value, 0.01f)
        assertEquals(172f, ((rect.left + rect.right) / 2).value, 0.01f)
        assertEquals(516f - 133f * 0.8f, rect.bottom.value, 0.01f)
    }

    @Test
    fun `아주 작은 카드에서도 냐미는 120 아래로 줄이지 않는다`() {
        val rect = RoomCardLayout.nyummyRect(200.dp, 300.dp)

        assertEquals(120f, (rect.right - rect.left).value, 0.01f)
    }
}
