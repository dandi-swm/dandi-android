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

    @Test
    fun `음식 목록 방식 식사를 다시 분석하면 음식 인식부터 다시 한다`() {
        val dto = Json.decodeFromString<MealStatusDTO>("""{"id":7,"status":"RECOGNIZING"}""")

        assertEquals(MealAnalysisStatus.RECOGNIZING, dto.toVO())
    }

    @Test
    fun `확인 대기 상태를 옮긴다`() {
        assertEquals(MealAnalysisStatus.CONFIRMING, MealStatusDTO(id = 7, status = "CONFIRMING").toVO())
    }
}
