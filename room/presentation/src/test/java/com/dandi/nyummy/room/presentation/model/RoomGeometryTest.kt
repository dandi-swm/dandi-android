package com.dandi.nyummy.room.presentation.model

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RoomGeometryTest {

    @Test
    fun `가로로 긴 그림은 폭이 기본 크기이고 바닥 가운데가 좌표에 닿는다`() {
        val rect = RoomGeometry.itemRect(roomWidth = 400f, roomHeight = 500f, x = 0.5f, y = 0.8f, scale = 1f, imageAspect = 2f)

        // 긴 변 = 400 * 0.3 = 120, 높이 = 120 / 2 = 60
        assertRect(Rect(left = 140f, top = 340f, right = 260f, bottom = 400f), rect)
    }

    @Test
    fun `세로로 긴 그림은 높이가 기본 크기이고 배율만큼 커진다`() {
        val rect = RoomGeometry.itemRect(roomWidth = 400f, roomHeight = 500f, x = 0.25f, y = 1f, scale = 2f, imageAspect = 0.5f)

        // 긴 변 = 400 * 0.3 * 2 = 240, 폭 = 120
        assertRect(Rect(left = 40f, top = 260f, right = 160f, bottom = 500f), rect)
    }

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
    fun `배율과 자리는 보이는 영역 안으로 자른다`() {
        assertEquals(RoomGeometry.MIN_SCALE, RoomGeometry.clampScale(0.1f))
        assertEquals(RoomGeometry.MAX_SCALE, RoomGeometry.clampScale(9f))
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
    fun `저장 좌표는 그림 왼쪽 아래 기준 퍼센트라서 그리는 좌표로 바꾸면 위아래가 뒤집힌다`() {
        assertOffset(Offset(0.25f, 0.9f), RoomGeometry.fromSavedPosition(x = 25f, y = 10f))
        assertOffset(Offset(25f, 10f), RoomGeometry.toSavedPosition(x = 0.25f, y = 0.9f))
        // 그림 왼쪽 아래 끝과 오른쪽 위 끝
        assertOffset(Offset(0f, 1f), RoomGeometry.fromSavedPosition(x = 0f, y = 0f))
        assertOffset(Offset(1f, 0f), RoomGeometry.fromSavedPosition(x = 100f, y = 100f))
    }

    @Test
    fun `저장 좌표가 0에서 100을 벗어나면 그 안으로 맞춘다`() {
        assertOffset(Offset(1f, 0f), RoomGeometry.fromSavedPosition(x = 120f, y = 480f))
        assertOffset(Offset(0f, 1f), RoomGeometry.fromSavedPosition(x = -5f, y = -1f))
        assertOffset(Offset(100f, 0f), RoomGeometry.toSavedPosition(x = 1.2f, y = 1.5f))
    }

    private fun assertOffset(expected: Offset, actual: Offset) {
        assertEquals(expected.x, actual.x, 0.001f)
        assertEquals(expected.y, actual.y, 0.001f)
    }

    private fun assertRect(expected: Rect, actual: Rect) {
        assertEquals(expected.left, actual.left, 0.01f)
        assertEquals(expected.top, actual.top, 0.01f)
        assertEquals(expected.right, actual.right, 0.01f)
        assertEquals(expected.bottom, actual.bottom, 0.01f)
    }
}
