package com.dandi.nyummy.settings.data.dto

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InquiryDTOTest {

    /** 앱 Retrofit과 같은 설정(NetworkModule). */
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
    }

    @Test
    fun `상세 응답의 첨부 사진 주소를 옮긴다`() {
        val inquiry = json.decodeFromString<InquiryDTO>(
            """{"inquiryId": 3, "questionContent": "사진이 안 보여요", "imageUrl": "https://s3/inquiries/1/a.jpg?sig=1"}""",
        ).toVO()

        assertEquals("https://s3/inquiries/1/a.jpg?sig=1", inquiry.imageUrl)
    }

    @Test
    fun `첨부가 없으면 사진 주소는 빈 문자열이다`() {
        val inquiry = json.decodeFromString<InquiryDTO>("""{"inquiryId": 3, "imageUrl": null}""").toVO()

        assertEquals("", inquiry.imageUrl)
    }

    @Test
    fun `사진을 붙였으면 요청에 이미지 키를 싣는다`() {
        val body = json.encodeToString(
            CreateInquiryRequestDTO(
                category = "BUG_REPORT",
                questionTitle = "제목",
                questionContent = "내용",
                imageKey = "inquiries/1/2026/10/22/a.jpg",
            ),
        )

        assertTrue(body, body.contains(""""imageKey":"inquiries/1/2026/10/22/a.jpg""""))
    }

    @Test
    fun `사진이 없으면 요청에 이미지 키를 싣지 않는다`() {
        val body = json.encodeToString(
            CreateInquiryRequestDTO(category = "QUESTION", questionTitle = "제목", questionContent = "내용"),
        )

        assertFalse(body, body.contains("imageKey"))
    }
}
