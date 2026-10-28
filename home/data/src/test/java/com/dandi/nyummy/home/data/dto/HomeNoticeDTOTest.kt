package com.dandi.nyummy.home.data.dto

import com.dandi.nyummy.home.entity.HomeNoticeVO
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeNoticeDTOTest {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
    }

    private fun decode(raw: String): List<HomeNoticeVO> = json.decodeFromString<HomeNoticeListDTO>(raw).toVO()

    @Test
    fun `공지 필드를 순서대로 옮기고 시각은 순간으로 바꾼다`() {
        val notices = decode(
            """
            {
              "notices": [
                {
                  "id": "2026-10-renewal",
                  "type": "update",
                  "badge": "업데이트",
                  "startAt": "2026-10-28T00:00:00+09:00",
                  "endAt": "2026-11-30T23:59:59Z",
                  "minVersionCode": 210,
                  "imageUrl": "https://cdn.example.com/renewal.png",
                  "title": "냐미가 새단장을 했어요!",
                  "body": "화면을 더 보기 편하게 새로 꾸몄어요."
                },
                {
                  "id": "2026-autumn-event",
                  "badge": "이벤트",
                  "meta": "10월 28일 ~ 11월 10일",
                  "title": "가을 한 상 이벤트가 열렸어요",
                  "body": "이벤트 페이지에서 자세히 알아보세요.",
                  "cta": { "text": "자세히 보기", "link": "/shop" }
                }
              ]
            }
            """.trimIndent(),
        )

        assertEquals(
            listOf(
                HomeNoticeVO(
                    id = "2026-10-renewal",
                    badge = "업데이트",
                    startAtMillis = 1_793_113_200_000L, // 2026-10-27T15:00:00Z
                    endAtMillis = 1_796_083_199_000L,
                    minVersionCode = 210,
                    imageUrl = "https://cdn.example.com/renewal.png",
                    title = "냐미가 새단장을 했어요!",
                    body = "화면을 더 보기 편하게 새로 꾸몄어요.",
                ),
                HomeNoticeVO(
                    id = "2026-autumn-event",
                    badge = "이벤트",
                    meta = "10월 28일 ~ 11월 10일",
                    title = "가을 한 상 이벤트가 열렸어요",
                    body = "이벤트 페이지에서 자세히 알아보세요.",
                    ctaText = "자세히 보기",
                    ctaLink = "/shop",
                ),
            ),
            notices,
        )
    }

    @Test
    fun `제목이 없는 공지는 버리고 나머지는 남긴다`() {
        val notices = decode("""{"notices":[{"id":"a","title":"  "},{"id":"b"},{"id":"c","title":"남는 공지"}]}""")

        assertEquals(listOf("c"), notices.map { it.id })
    }

    @Test
    fun `적어 둔 시각을 읽을 수 없으면 그 공지를 버린다`() {
        val notices = decode(
            """{"notices":[{"id":"a","title":"끝이 틀림","endAt":"11월 말"},{"id":"b","title":"시작이 틀림","startAt":"2026-13-01"},{"id":"c","title":"기간 없음"}]}""",
        )

        assertEquals(listOf("c"), notices.map { it.id })
    }

    @Test
    fun `빈 시각 글자는 적지 않은 것으로 본다`() {
        val notice = decode("""{"notices":[{"title":"공지","startAt":"","endAt":" "}]}""").single()

        assertNull(notice.startAtMillis)
        assertNull(notice.endAtMillis)
    }

    @Test
    fun `버튼은 글자와 경로가 모두 있을 때만 둔다`() {
        val notices = decode(
            """
            {"notices":[
              {"id":"a","title":"글자 없음","cta":{"text":" ","link":"/shop"}},
              {"id":"b","title":"경로 없음","cta":{"text":"자세히 보기"}},
              {"id":"c","title":"버튼 없음","cta":null},
              {"id":"d","title":"버튼 있음","cta":{"text":"자세히 보기","link":" /shop "}}
            ]}
            """.trimIndent(),
        )

        assertEquals(listOf(null, null, null, "자세히 보기"), notices.map { it.ctaText })
        assertEquals(listOf(null, null, null, "/shop"), notices.map { it.ctaLink })
    }

    @Test
    fun `비어 있는 사진 주소와 작은 글은 없는 것으로 본다`() {
        val notice = decode("""{"notices":[{"title":"공지","imageUrl":"","meta":"  "}]}""").single()

        assertNull(notice.imageUrl)
        assertNull(notice.meta)
    }

    @Test
    fun `공지 목록이 없으면 빈 목록이다`() {
        assertTrue(decode("""{}""").isEmpty())
        assertTrue(decode("""{"notices":null}""").isEmpty())
    }
}
