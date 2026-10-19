package com.dandi.nyummy.home.data.dto

import com.dandi.nyummy.home.entity.HomeSummaryVO
import com.dandi.nyummy.room.entity.RoomItemCategory
import com.dandi.nyummy.room.entity.RoomPlacementVO
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeDTOTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `서버 응답 필드를 홈 요약으로 옮긴다`() {
        val body = """
            {
              "user": {"coin": 1240},
              "streak": {"streakDays": 7, "recordsUntilNextReward": 2},
              "todayMealSummary": {"todayRecordedCount": 3, "todayCurrentCalory": 1350, "todayTargetCalory": 1800}
            }
        """.trimIndent()

        val summary = json.decodeFromString<HomeDTO>(body).toVO()

        assertEquals(
            HomeSummaryVO(
                coinBalance = 1240,
                streakDays = 7,
                recordsUntilNextReward = 2,
                todayRecordedCount = 3,
                todayCalorieKcal = 1350,
                goalCalorieKcal = 1800,
            ),
            summary,
        )
    }

    @Test
    fun `빠진 묶음이나 null 필드는 0으로 채운다`() {
        val body = """{"user": null, "streak": {"streakDays": 4}, "todayMealSummary": {}}"""

        val summary = json.decodeFromString<HomeDTO>(body).toVO()

        assertEquals(HomeSummaryVO(streakDays = 4), summary)
    }

    @Test
    fun `모르는 필드가 와도 읽는다`() {
        val body = """{"user": {"coin": 10, "nickname": "냐미"}, "banner": "new"}"""

        assertEquals(10, json.decodeFromString<HomeDTO>(body).toVO().coinBalance)
    }

    @Test
    fun `마이룸에 놓인 아이템을 zIndex 순서로 받고 그림은 아이템 id로 받는다`() {
        val body = """
            {
              "equippedRoomItems": [
                {"ownedRoomItemId": 101, "category": "BED", "itemId": 2, "x": 50.0, "y": 20.0, "zIndex": 2, "scale": 1.2},
                {"ownedRoomItemId": 108, "category": "BACKGROUND", "itemId": 1, "x": 0.0, "y": 0.0, "zIndex": 0, "scale": 1.0},
                {"ownedRoomItemId": 104, "category": "CARPET", "itemId": 3, "x": 120.0, "y": -4.0, "zindex": 1}
              ]
            }
        """.trimIndent()

        val placements = json.decodeFromString<HomeDTO>(body).toVO().roomPlacements

        assertEquals(listOf(108L, 104L, 101L), placements.map { it.roomItemId })
        assertEquals(
            RoomPlacementVO(101, RoomItemCategory.BED, itemId = 2, x = 50f, y = 20f, zIndex = 2, scale = 1.2f),
            placements[2],
        )
        // 소문자 zindex도 읽고, 0~100을 벗어난 좌표는 그 안으로 맞춘다.
        assertEquals(RoomPlacementVO(104, RoomItemCategory.CARPET, itemId = 3, x = 100f, y = 0f, zIndex = 1), placements[1])
    }

    @Test
    fun `놓인 아이템이 없으면 빈 목록이다`() {
        assertTrue(json.decodeFromString<HomeDTO>("""{"user": {"coin": 1}}""").toVO().roomPlacements.isEmpty())
    }
}
