package com.dandi.nyummy.room.presentation.component

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.dandi.nyummy.common.presentation.room.RoomCardLayout
import com.dandi.nyummy.room.presentation.model.RoomGeometry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomFrameTest {

    private val density = Density(density = 1f)

    @Test
    fun `9대16 카드면 방 그림이 꼭 맞아 전부 보인다`() {
        val frame = RoomFrame.of(360.dp, 640.dp, coveredBottom = 0.dp, density = density)

        assertEquals(360f, frame.roomRect.width, 0.5f)
        assertEquals(0f, frame.visibleLeft, 0.001f)
        assertEquals(1f, frame.visibleRight, 0.001f)
        assertEquals(0f, frame.visibleTop, 0.001f)
        assertEquals(1f, frame.visibleBottom, 0.001f)
    }

    @Test
    fun `아이템은 보이는 곳 중 화면 요소에 가리지 않는 곳에만 놓는다`() {
        // 371×780은 좌우가 약 8%씩만 잘려서, 놓을 수 있는 곳은 정해 둔 범위(좌우 12% 안쪽, 위 33%, 아래 80%)가 정한다.
        val phone = RoomFrame.of(371.dp, 780.dp, coveredBottom = 0.dp, density = density)
        assertTrue("좌우가 조금 잘린 무대다", phone.visibleLeft in 0.01f..RoomCardLayout.BleedFraction)
        assertEquals(RoomCardLayout.PlaceMinX, phone.placeLeft, 0.001f)
        assertEquals(RoomCardLayout.PlaceMaxX, phone.placeRight, 0.001f)
        assertEquals(RoomCardLayout.PlaceMinY, phone.placeTop, 0.001f)
        assertEquals(RoomCardLayout.PlaceMaxY, phone.placeBottom, 0.001f)

        // 정해 둔 범위보다 많이 잘리는 무대에서는 보이는 곳까지만 놓는다.
        val tall = RoomFrame.of(300.dp, 800.dp, coveredBottom = 0.dp, density = density)
        assertTrue("좌우가 12%보다 많이 잘리는 무대다", tall.visibleLeft > RoomCardLayout.BleedFraction)
        assertEquals(tall.visibleLeft, tall.placeLeft, 0.001f)
        assertEquals(tall.visibleRight, tall.placeRight, 0.001f)
    }

    @Test
    fun `시트가 덮은 만큼 보이는 아래 끝이 올라간다`() {
        val frame = RoomFrame.of(360.dp, 640.dp, coveredBottom = 200.dp, density = density)

        assertEquals((640f - 200f) / 640f, frame.visibleBottom, 0.001f)
    }

    @Test
    fun `새 아이템은 무대 가운데 바닥에 놓고 시트에 가리면 시트 바로 위로 올린다`() {
        val open = RoomFrame.of(344.dp, 516.dp, coveredBottom = 0.dp, density = density).newItemPoint()
        assertEquals(0.5f, open.x, 0.001f)
        assertEquals(RoomGeometry.NEW_ITEM_Y, open.y, 0.001f)

        val covered = RoomFrame.of(344.dp, 516.dp, coveredBottom = 300.dp, density = density)
        val point = covered.newItemPoint()
        assertTrue("시트 위에 있어야 한다: $point", point.y < covered.visibleBottom)
        assertTrue("너무 위로 가지 않는다: $point", point.y > covered.visibleBottom - 0.05f)
    }

    @Test
    fun `냐미 자리는 홈 카드와 같다`() {
        val frame = RoomFrame.of(344.dp, 516.dp, coveredBottom = 0.dp, density = density)
        val home = RoomCardLayout.nyummyRect(344.dp, 516.dp)

        assertEquals(home.left.value, frame.catRect.left, 0.01f)
        assertEquals(home.top.value, frame.catRect.top, 0.01f)
        assertEquals(home.right.value, frame.catRect.right, 0.01f)
        assertEquals(home.bottom.value, frame.catRect.bottom, 0.01f)
    }
}
