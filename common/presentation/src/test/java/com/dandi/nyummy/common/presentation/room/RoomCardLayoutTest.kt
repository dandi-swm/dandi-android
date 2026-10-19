package com.dandi.nyummy.common.presentation.room

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomCardLayoutTest {

    @Test
    fun `카드는 자리를 채우고 너무 길거나 넓을 때만 줄인다`() {
        // 픽셀 9 홈 자리(371×636)는 그대로 채운다.
        assertEquals(DpSize(371.dp, 636.dp), RoomCardLayout.cardSize(371.dp, 636.dp))

        // 태블릿 세로처럼 너무 길면 높이를 가장 긴 비율까지 줄인다.
        val tall = RoomCardLayout.cardSize(440.dp, 1000.dp)
        assertEquals(440f, tall.width.value, 0.01f)
        assertEquals(440f / RoomCardLayout.MinCardAspect, tall.height.value, 0.01f)

        // 아주 작은 폰처럼 너무 넓으면 폭을 가장 넓은 비율까지 줄인다.
        val wide = RoomCardLayout.cardSize(320.dp, 335.dp)
        assertEquals(335f * RoomCardLayout.MaxCardAspect, wide.width.value, 0.01f)
        assertEquals(335f, wide.height.value, 0.01f)
    }

    @Test
    fun `세로로 긴 카드는 방 그림 좌우가 잘리지만 안전 지대 밖까지만 잘린다`() {
        val rect = RoomCardLayout.roomRect(371.dp, 636.dp)
        val roomWidth = rect.right - rect.left
        val sideCut = -rect.left / roomWidth

        assertEquals(636f, rect.bottom.value, 0.01f)
        assertTrue("좌우가 잘린다: $sideCut", sideCut > 0f)
        assertTrue("잘려도 되는 몫 안이다: $sideCut", sideCut <= RoomCardLayout.BleedFraction)
    }

    @Test
    fun `가장 긴 카드와 가장 넓은 카드에서는 잘리는 몫이 딱 15퍼센트다`() {
        val width = 440.dp
        val tall = RoomCardLayout.roomRect(width, width / RoomCardLayout.MinCardAspect)
        assertEquals(RoomCardLayout.BleedFraction, -tall.left / (tall.right - tall.left), 0.001f)

        val height = 400.dp
        val wide = RoomCardLayout.roomRect(height * RoomCardLayout.MaxCardAspect, height)
        assertEquals(RoomCardLayout.BleedFraction, -wide.top / (wide.bottom - wide.top), 0.001f)
    }

    @Test
    fun `2대3 카드에서는 방 그림이 꼭 맞아 잘리는 곳이 없다`() {
        val rect = RoomCardLayout.roomRect(344.dp, 516.dp)

        assertEquals(0f, rect.left.value, 0.01f)
        assertEquals(0f, rect.top.value, 0.01f)
        assertEquals(344f, rect.right.value, 0.01f)
        assertEquals(516f, rect.bottom.value, 0.01f)
    }

    @Test
    fun `냐미는 방 그림 배율대로 커지고 가운데 바닥 위에 앉는다`() {
        // 371×636 카드: 높이에 맞춰 그림이 커진다(배율 636/645). 냐미 155 × 배율, 카드 바닥에서 133 × 배율 위
        val scale = 636f / 645f
        val rect = RoomCardLayout.nyummyRect(371.dp, 636.dp)

        assertEquals(155f * scale, (rect.right - rect.left).value, 0.01f)
        assertEquals(185.5f, ((rect.left + rect.right) / 2).value, 0.01f)
        assertEquals(636f - 133f * scale, rect.bottom.value, 0.01f)
    }

    @Test
    fun `카드가 작아지면 냐미도 같은 비율로 작아진다`() {
        // 200×300 카드는 냐미도 카드 폭의 36%(72)다. 최소 크기로 버티지 않는다.
        val rect = RoomCardLayout.nyummyRect(200.dp, 300.dp)

        assertEquals(200f * 155f / 430f, (rect.right - rect.left).value, 0.01f)
    }
}
