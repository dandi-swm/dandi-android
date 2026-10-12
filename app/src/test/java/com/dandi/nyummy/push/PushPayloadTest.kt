package com.dandi.nyummy.push

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** 서버 FcmPushClient가 보내는 data 형태 그대로 파싱되는지 확인합니다. */
class PushPayloadTest {

    @Test
    fun `분석 완료 푸시의 키를 서버 이름 그대로 읽는다`() {
        val payload = PushPayload.from(
            mapOf(
                "type" to "ANALYSIS_COMPLETED",
                "v" to "1",
                "notificationId" to "analysis:123:COMPLETED:1760238000123",
                "title" to "냐미 맛있게 먹었다냥 🐾",
                "body" to "비빔밥 분석이 끝났어요. 칼로리 확인해줘!",
                "appLink" to "https://link.nyummy.co.kr/meals/123",
                "sentAt" to "2026-10-12T03:00:00.123Z",
            ),
        )

        assertEquals("analysis:123:COMPLETED:1760238000123", payload.notificationId)
        assertEquals("냐미 맛있게 먹었다냥 🐾", payload.title)
        assertEquals("비빔밥 분석이 끝났어요. 칼로리 확인해줘!", payload.body)
        assertEquals("https://link.nyummy.co.kr/meals/123", payload.appLink)
    }

    @Test
    fun `리마인더 푸시는 홈 링크를 읽는다`() {
        val payload = PushPayload.from(
            mapOf(
                "type" to "MEAL_REMINDER",
                "notificationId" to "meal-reminder:42:2026-10-12:LUNCH",
                "title" to "집사, 점심은 먹었어? 🐾",
                "body" to "냐미 너무 배고프다냥. 사진 한 장만 찍어줘!",
                "appLink" to "https://link.nyummy.co.kr/home",
            ),
        )

        assertEquals("https://link.nyummy.co.kr/home", payload.appLink)
        assertEquals("meal-reminder:42:2026-10-12:LUNCH", payload.notificationId)
    }

    @Test
    fun `빠지거나 빈 값은 null로 둔다`() {
        val payload = PushPayload.from(mapOf("title" to " ", "appLink" to ""))

        assertNull(payload.notificationId)
        assertNull(payload.title)
        assertNull(payload.body)
        assertNull(payload.appLink)
    }
}
