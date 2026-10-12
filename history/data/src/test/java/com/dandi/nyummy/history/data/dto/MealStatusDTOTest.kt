package com.dandi.nyummy.history.data.dto

import com.dandi.nyummy.history.entity.MealAnalysisStatus
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

/** 재분석 응답(MealStatusResponse{id, status})을 분석 상태로 옮기는지 확인합니다. */
class MealStatusDTOTest {

    @Test
    fun `재분석 응답의 status를 분석 상태로 옮긴다`() {
        val dto = Json.decodeFromString<MealStatusDTO>("""{"id":7,"status":"ANALYZING"}""")

        assertEquals(7L, dto.id)
        assertEquals(MealAnalysisStatus.ANALYZING, dto.toVO())
    }

    @Test
    fun `status가 없으면 UNKNOWN이다`() {
        assertEquals(MealAnalysisStatus.UNKNOWN, MealStatusDTO(id = 7).toVO())
    }
}
