package com.dandi.nyummy.room.presentation.model

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.dandi.nyummy.common.presentation.room.RoomCardLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomGeometryTest {

    @Test
    fun `겹친 곳을 누르면 나중에 그린 아이템을 고르고 빈 곳이면 없다`() {
        val rects = listOf(
            1L to Rect(0f, 0f, 100f, 100f),
            2L to Rect(50f, 50f, 150f, 150f),
        )

        assertEquals(2L, RoomGeometry.topmostAt(rects, Offset(75f, 75f)))
        assertEquals(1L, RoomGeometry.topmostAt(rects, Offset(10f, 10f)))
        assertNull(RoomGeometry.topmostAt(rects, Offset(200f, 200f)))
    }

    @Test
    fun `자리는 보이는 영역 안으로 자른다`() {
        assertEquals(
            Offset(0.9f, 0.3f),
            RoomGeometry.clampPosition(x = 1.4f, y = 0.1f, minX = 0.1f, maxX = 0.9f, minY = 0.3f, maxY = 0.9f),
        )
        assertEquals(
            Offset(0.1f, 0.9f),
            RoomGeometry.clampPosition(x = -0.2f, y = 1.2f, minX = 0.1f, maxX = 0.9f, minY = 0.3f, maxY = 0.9f),
        )
    }

    @Test
    fun `보이는 영역이 그림 밖까지 넓어도 그림 안으로 자른다`() {
        assertEquals(
            Offset(1f, 1f),
            RoomGeometry.clampPosition(x = 2f, y = 2f, minX = -0.5f, maxX = 1.5f, minY = -0.5f, maxY = 1.5f),
        )
    }

    @Test
    fun `보이는 영역이 뒤집힌 아주 작은 화면이면 아래 끝에 둔다`() {
        assertEquals(
            Offset(0.5f, 0.6f),
            RoomGeometry.clampPosition(x = 0.5f, y = 0.2f, minX = 0f, maxX = 1f, minY = 0.7f, maxY = 0.6f),
        )
    }

    @Test
    fun `위쪽이 잘리지 않는 폰에서도 위쪽이 가장 많이 잘리는 화면 기준으로 아이템 절반 아래까지만 올린다`() {
        // 세로로 긴 폰은 그림 위쪽이 잘리지 않지만(visibleTop 0), 넓은 화면은 위쪽이 12%까지 잘린다.
        val minY = RoomGeometry.minBaseY(placeTop = RoomCardLayout.PlaceMinY, visibleTop = 0f, itemHeight = 0.2f)

        assertEquals(RoomCardLayout.BleedFraction + 0.1f, minY, 0.0001f)
    }

    @Test
    fun `이 기기에서 위쪽이 더 많이 보이지 않으면 이 기기 기준을 따른다`() {
        val minY = RoomGeometry.minBaseY(placeTop = RoomCardLayout.PlaceMinY, visibleTop = 0.2f, itemHeight = 0.1f)

        assertEquals(0.25f, minY, 0.0001f)
    }

    @Test
    fun `아주 낮은 아이템도 놓을 수 있는 위 끝보다 올리지 않는다`() {
        val minY = RoomGeometry.minBaseY(placeTop = 0.3f, visibleTop = 0f, itemHeight = 0f)

        assertEquals(0.3f, minY, 0.0001f)
    }

    @Test
    fun `놓을 수 있는 위 끝은 그림 위쪽이 가장 많이 잘리는 선보다 위로 가지 않는다`() {
        assertTrue(RoomCardLayout.PlaceMinY >= RoomCardLayout.BleedFraction)
    }
}
