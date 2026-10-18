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
    fun `세로로 긴 홈 카드 크기면 방 그림 좌우가 잘린 만큼 보이는 영역도 좁다`() {
        // 362×597: 높이에 맞춰 그림 폭이 455가 되어 좌우가 46씩 잘린다.
        val frame = RoomFrame.of(362.dp, 597.dp, coveredBottom = 0.dp, density = density)

        val roomWidth = frame.roomRect.width
        assertEquals(597f / 564f * 430f, roomWidth, 0.5f)
        assertEquals((roomWidth - 362f) / 2f / roomWidth, frame.visibleLeft, 0.001f)
        assertEquals(1f - frame.visibleLeft, frame.visibleRight, 0.001f)
        assertEquals(0f, frame.visibleTop, 0.001f)
        assertEquals(1f, frame.visibleBottom, 0.001f)
    }

    @Test
    fun `시트가 덮은 만큼 보이는 아래 끝이 올라간다`() {
        val frame = RoomFrame.of(362.dp, 597.dp, coveredBottom = 200.dp, density = density)

        assertEquals((597f - 200f) / 597f, frame.visibleBottom, 0.001f)
    }

    @Test
    fun `새 아이템은 무대 가운데 바닥에 놓고 시트에 가리면 시트 바로 위로 올린다`() {
        val open = RoomFrame.of(362.dp, 597.dp, coveredBottom = 0.dp, density = density).newItemPoint()
        assertEquals(0.5f, open.x, 0.001f)
        assertEquals(RoomGeometry.NEW_ITEM_Y, open.y, 0.001f)

        val covered = RoomFrame.of(362.dp, 597.dp, coveredBottom = 300.dp, density = density)
        val point = covered.newItemPoint()
        assertTrue("시트 위에 있어야 한다: $point", point.y < covered.visibleBottom)
        assertTrue("너무 위로 가지 않는다: $point", point.y > covered.visibleBottom - 0.05f)
    }

    @Test
    fun `냐미 자리는 홈 카드와 같다`() {
        val frame = RoomFrame.of(362.dp, 597.dp, coveredBottom = 0.dp, density = density)
        val home = RoomCardLayout.nyummyRect(362.dp, 597.dp)

        assertEquals(home.left.value, frame.catRect.left, 0.01f)
        assertEquals(home.top.value, frame.catRect.top, 0.01f)
        assertEquals(home.right.value, frame.catRect.right, 0.01f)
        assertEquals(home.bottom.value, frame.catRect.bottom, 0.01f)
    }
}
