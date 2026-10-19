package com.dandi.nyummy.mailbox.data.dto

import com.dandi.nyummy.mailbox.entity.MailCategory
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MailDTOTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `목록 우편은 그림 주소 대신 그림 번호를 받는다`() {
        val body = """
            {"mails": [
              {"mailId": 3, "category": "REWARD_BOX", "title": "골드 상자", "content": "식사 기록 보상", "imageId": 3,
               "isClaimed": false, "createdAt": "2026-10-19T03:00:00Z"},
              {"mailId": 2, "category": "COIN", "title": "코인 10개", "content": "", "imageId": null,
               "isClaimed": true, "createdAt": "2026-10-18T03:00:00Z"}
            ], "nextCursor": null, "hasNext": false}
        """.trimIndent()

        val mails = json.decodeFromString<MailListResponseDTO>(body).toVO().mails

        assertEquals(MailCategory.REWARD_BOX, mails[0].category)
        assertEquals(3L, mails[0].imageId)
        assertNull(mails[1].imageId)
    }
}
