package com.dandi.nyummy.room.data.dto

import com.dandi.nyummy.room.entity.RoomItemCategory
import com.dandi.nyummy.room.entity.RoomPlacementVO
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomDTOTest {

    // 앱 네트워크 모듈과 같은 설정
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
    }

    @Test
    fun `마이룸 응답을 종류별 묶음과 놓인 아이템으로 바꾼다`() {
        val room = json.decodeFromString<RoomResponseDTO>(
            """
            {
              "categories": [
                {"category":"BACKGROUND","categoryName":"배경","ownedRoomItems":[{"roomItemId":108,"name":"햇살 방","imageUrl":"bg.png","isEquipped":true}],"nextCursor":null,"hasNext":false},
                {"category":"BED","categoryName":"침대","ownedRoomItems":[{"roomItemId":101,"name":"구름 방석","imageUrl":"bed.png","isEquipped":true}],"nextCursor":101,"hasNext":true}
              ],
              "equippedRoomItems": [
                {"roomItemId":101,"category":"BED","imageUrl":"bed.png","x":50.0,"y":20.0,"zIndex":2,"scale":1.2},
                {"roomItemId":108,"category":"BACKGROUND","imageUrl":"bg.png","x":0.0,"y":0.0,"zIndex":0,"scale":1.0}
              ]
            }
            """.trimIndent(),
        ).toVO()

        assertEquals(listOf(RoomItemCategory.BACKGROUND, RoomItemCategory.BED), room.categories.map { it.category })
        assertNull(room.categories[0].nextCursor)
        assertEquals(101L, room.categories[1].nextCursor)
        assertEquals("침대", room.categories[1].categoryName)
        // zIndex 오름차순으로 정렬한다.
        assertEquals(listOf(108L, 101L), room.placements.map { it.roomItemId })
        assertEquals(RoomPlacementVO(101, RoomItemCategory.BED, "bed.png", 50f, 20f, 2, 1.2f), room.placements[1])
    }

    @Test
    fun `zIndex가 소문자 zindex로 와도 읽는다`() {
        val placement = json.decodeFromString<EquippedRoomItemDTO>("""{"roomItemId":1,"zindex":3}""").toVO()

        assertEquals(3, placement.zIndex)
    }

    @Test
    fun `좌표가 0에서 100을 벗어나 오면 그 안으로 맞춘다`() {
        val over = json.decodeFromString<EquippedRoomItemDTO>("""{"roomItemId":1,"x":120.0,"y":480.0}""").toVO()
        val under = json.decodeFromString<EquippedRoomItemDTO>("""{"roomItemId":2,"x":-5.0,"y":-0.5}""").toVO()

        assertEquals(100f, over.x)
        assertEquals(100f, over.y)
        assertEquals(0f, under.x)
        assertEquals(0f, under.y)
    }

    @Test
    fun `모르는 종류는 기타로, 0 이하 배율은 1로 본다`() {
        val placement = json.decodeFromString<EquippedRoomItemDTO>("""{"roomItemId":1,"category":"SOFA","scale":0}""").toVO()

        assertEquals(RoomItemCategory.ETC, placement.category)
        assertEquals(1f, placement.scale)
    }

    @Test
    fun `다음 묶음이 없다고 하면 cursor를 버린다`() {
        val page = json.decodeFromString<OwnedRoomItemsResponseDTO>(
            """{"ownedRoomItems":[{"roomItemId":5,"name":"털실 공","imageUrl":"yarn.png","isEquipped":false}],"nextCursor":5,"hasNext":false}""",
        ).toVO(RoomItemCategory.ETC)

        assertFalse(page.hasNext)
        assertEquals(RoomItemCategory.ETC, page.category)
        assertEquals("털실 공", page.items.single().name)
    }

    @Test
    fun `저장 요청은 아이템마다 roomItemId x y zIndex scale 를 보낸다`() {
        val body = json.encodeToString(
            UpdateEquippedRoomItemsRequestDTO.serializer(),
            UpdateEquippedRoomItemsRequestDTO(
                equippedRoomItems = listOf(
                    EquippedRoomItemRequestDTO.from(RoomPlacementVO(roomItemId = 7, x = 25f, y = 10f, zIndex = 0, scale = 1.5f)),
                ),
            ),
        )

        val item = Json.parseToJsonElement(body).jsonObject["equippedRoomItems"]!!.jsonArray.single().jsonObject
        assertEquals(setOf("roomItemId", "x", "y", "zIndex", "scale"), item.keys)
        // 0도 기본값으로 빠지지 않고 보낸다.
        assertEquals("0", item["zIndex"].toString())
        assertTrue(item["scale"].toString().startsWith("1.5"))
        assertEquals("25.0", item["x"].toString())
        assertEquals("10.0", item["y"].toString())
    }

    @Test
    fun `저장 요청 좌표도 0에서 100 안으로 맞춰 보낸다`() {
        val request = EquippedRoomItemRequestDTO.from(RoomPlacementVO(roomItemId = 7, x = 130f, y = -3f))

        assertEquals(100f, request.x)
        assertEquals(0f, request.y)
    }
}
