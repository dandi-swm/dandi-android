package com.dandi.nyummy.shop.data.dto

import com.dandi.nyummy.shop.entity.CoinChargePackageVO
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class CoinChargeEventDTOTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun parse(raw: String) = json.decodeFromString(CoinChargeEventDTO.serializer(), raw).toVO()

    private val groupA = """
        {
          "eventId": "2026-11-coin-charge",
          "group": "A",
          "grantCoins": 3000,
          "notice": {
            "badge": "상점 오픈",
            "imageUrl": "https://cdn.nyummy.co.kr/notices/2026-11-shop-open.webp",
            "title": "냐미 상점이 문을 열었어요!",
            "body": "코인을 충전하고 냐미 방을 꾸밀\n가구와 배경을 골라 보세요.",
            "cta": "코인 충전하기"
          },
          "packages": [
            { "id": "coin_300", "coins": 300, "price": 1000 },
            { "id": "coin_1500", "coins": 1500, "price": 5000 },
            { "id": "coin_3000", "coins": 3000, "price": 10000 }
          ],
          "unknownField": true
        }
    """.trimIndent()

    @Test
    fun `운영 문서 예시 JSON을 그룹, 지급 코인, 공지, 묶음으로 읽는다`() {
        val event = parse(groupA)!!

        assertEquals("2026-11-coin-charge", event.eventId)
        assertEquals("A", event.group)
        assertEquals(3000, event.grantCoins)
        assertEquals("코인 충전하기", event.notice?.ctaText)
        assertEquals(
            listOf(
                CoinChargePackageVO("coin_300", 300, 1000, imageId = 1),
                CoinChargePackageVO("coin_1500", 1500, 5000, imageId = 2),
                CoinChargePackageVO("coin_3000", 3000, 10000, imageId = 3),
            ),
            event.packages,
        )
    }

    @Test
    fun `그룹이 없으면 분석에서 나눌 수 없어 이벤트를 버린다`() {
        assertNull(parse(groupA.replace("\"group\": \"A\",", "")))
    }

    @Test
    fun `지급 코인이 0 이하면 이벤트를 버린다`() {
        assertNull(parse(groupA.replace("\"grantCoins\": 3000", "\"grantCoins\": 0")))
    }

    @Test
    fun `형식이 틀린 묶음만 빼고, 맞는 묶음이 하나도 없으면 이벤트를 버린다`() {
        val oneBroken = parse(groupA.replace("{ \"id\": \"coin_1500\", \"coins\": 1500, \"price\": 5000 }", "{ \"id\": \"coin_1500\", \"coins\": 1500 }"))!!
        assertEquals(listOf("coin_300", "coin_3000"), oneBroken.packages.map { it.id })

        val noPackages = """{ "eventId": "e", "group": "B", "grantCoins": 3000, "packages": [ { "id": "", "coins": 1, "price": 1 } ] }"""
        assertNull(parse(noPackages))
    }

    @Test
    fun `버튼 글자가 없는 공지는 버리고 충전 화면만 남긴다`() {
        val event = parse(groupA.replace("\"cta\": \"코인 충전하기\"", "\"cta\": \"\""))

        assertNotNull(event)
        assertNull(event!!.notice)
    }

    @Test
    fun `그림 번호를 적은 묶음은 그 그림을 쓰고, 넷째 묶음부터는 마지막 그림을 쓴다`() {
        val raw = """
            { "eventId": "e", "group": "B", "grantCoins": 3000, "packages": [
              { "id": "a", "coins": 1, "price": 1, "imageId": 3 },
              { "id": "b", "coins": 1, "price": 1 },
              { "id": "c", "coins": 1, "price": 1 },
              { "id": "d", "coins": 1, "price": 1 }
            ] }
        """.trimIndent()

        assertEquals(listOf(3L, 2L, 3L, 3L), parse(raw)!!.packages.map { it.imageId })
    }

    @Test
    fun `지급 응답에 코인 수가 없으면 이벤트의 지급 코인을 쓴다`() {
        assertEquals(3000, CoinChargeGrantDTO().toVO(fallbackCoins = 3000).grantedCoins)
        assertEquals(500, CoinChargeGrantDTO(grantedCoins = 500, balance = 1740).toVO(fallbackCoins = 3000).grantedCoins)
    }
}
