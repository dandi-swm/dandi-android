package com.dandi.nyummy.cat.data.dto

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class UpdateCatRequestDTOTest {

    /** 앱 네트워크 설정과 같은 Json 설정 */
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
    }

    @Test
    fun `바꿀 이름만 담아 보낸다`() {
        assertEquals("""{"name":"달봉이"}""", json.encodeToString(UpdateCatRequestDTO(name = "달봉이")))
    }

    @Test
    fun `바꿀 항목이 없으면 빈 객체로 보낸다`() {
        assertEquals("{}", json.encodeToString(UpdateCatRequestDTO()))
    }
}
