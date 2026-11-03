package com.dandi.nyummy.room.presentation.model

import com.dandi.nyummy.common.presentation.room.RoomCardLayout
import com.dandi.nyummy.room.entity.RoomItemCategory
import com.dandi.nyummy.room.entity.RoomPlacementVO
import com.dandi.nyummy.room.presentation.RoomBackgroundUiModel
import com.dandi.nyummy.room.presentation.RoomDraft
import com.dandi.nyummy.room.presentation.RoomPlacementUiModel
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class RoomDraftEditsTest {

    /** 뒤에서 앞 순서로 방석(1), 캣타워(2). 배경은 햇살 방(100). */
    private val room = RoomDraft(
        background = RoomBackgroundUiModel(roomItemId = 100, imageUrl = "bg"),
        items = persistentListOf(item(1, x = 0.3f), item(2, x = 0.7f)),
    )

    @Test
    fun `놓기는 바닥 점을 방 안으로 맞춰 기본 크기로 맨 앞에 놓는다`() {
        val placed = room.withItemPlaced(3, imageUrl = "ball", x = 1.4f, y = -0.2f)!!

        val added = placed.items.last()
        assertEquals(listOf(1L, 2L, 3L), placed.items.map { it.roomItemId })
        assertEquals(1f, added.x)
        assertEquals(0f, added.y)
        assertEquals(1f, added.scale)
        assertEquals("ball", added.imageUrl)
    }

    @Test
    fun `이미 놓인 아이템은 다시 놓지 않는다`() {
        assertNull(room.withItemPlaced(1, imageUrl = "cushion", x = 0.5f, y = 0.5f))
    }

    @Test
    fun `옮긴 아이템은 맨 앞으로 오고 크기는 허용 범위로 자른다`() {
        val moved = room.withItemMoved(1, x = 0.4f, y = 0.6f, scale = 99f)!!

        assertEquals(listOf(2L, 1L), moved.items.map { it.roomItemId })
        val cushion = moved.items.last()
        assertEquals(0.4f, cushion.x)
        assertEquals(0.6f, cushion.y)
        assertEquals(RoomCardLayout.MaxItemScale, cushion.scale)
    }

    @Test
    fun `제자리면 같은 방을 돌려주고 없는 아이템이면 null이다`() {
        val same = room.withItemMoved(1, x = 0.3f, y = 0.8f, scale = 1f)

        assertSame(room, same)
        assertNull(room.withItemMoved(9, x = 0.3f, y = 0.8f, scale = 1f))
    }

    @Test
    fun `맨 앞으로는 이미 맨 앞이거나 없는 아이템이면 null이다`() {
        assertEquals(listOf(2L, 1L), room.withItemInFront(1)!!.items.map { it.roomItemId })
        assertNull(room.withItemInFront(2))
        assertNull(room.withItemInFront(9))
    }

    @Test
    fun `빼기는 그 아이템만 빼고 없는 아이템이면 null이다`() {
        assertEquals(listOf(2L), room.withItemRemoved(1)!!.items.map { it.roomItemId })
        assertNull(room.withItemRemoved(9))
    }

    @Test
    fun `배경은 다른 배경일 때만 바꾸고 null이면 기본 빈 방이다`() {
        val mint = RoomBackgroundUiModel(roomItemId = 101, imageUrl = "mint")

        assertEquals(mint, room.withBackground(mint)!!.background)
        assertNull(room.withBackground(RoomBackgroundUiModel(roomItemId = 100, imageUrl = "다른 주소")))
        assertNull(room.withBackground(null)!!.background)
    }

    @Test
    fun `서버 배치는 그리는 순서와 좌표로 바꾼다`() {
        val draft = listOf(
            RoomPlacementVO(roomItemId = 1, itemId = 11, x = 50f, y = 20f, zIndex = 2),
            RoomPlacementVO(roomItemId = 100, category = RoomItemCategory.BACKGROUND, itemId = 100),
            RoomPlacementVO(roomItemId = 2, itemId = 12, x = 10f, y = 10f, zIndex = 1),
        ).toRoomDraft()

        assertEquals(100L, draft.background?.roomItemId)
        assertEquals(listOf(2L, 1L), draft.items.map { it.roomItemId })
        assertEquals(0.5f, draft.items.last().x, 0.001f)
        assertEquals(0.8f, draft.items.last().y, 0.001f)
    }

    private fun item(id: Long, x: Float) = RoomPlacementUiModel(roomItemId = id, imageUrl = "$id", x = x, y = 0.8f, scale = 1f)
}
