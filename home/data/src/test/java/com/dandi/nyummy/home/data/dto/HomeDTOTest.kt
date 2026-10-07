package com.dandi.nyummy.home.data.dto

import com.dandi.nyummy.home.entity.HomeSummaryVO
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
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
}
