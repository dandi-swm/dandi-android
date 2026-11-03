package com.dandi.nyummy.shop.data.dto

import com.dandi.nyummy.shop.entity.CoinChargeGrantVO
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
          "notice": {
            "badge": "상점 오픈",
            "imageUrl": "https://cdn.nyummy.co.kr/notices/2026-11-shop-open.webp",
            "title": "냐미 상점이 문을 열었어요!",
            "body": "코인을 충전하고 냐미 방을 꾸밀\n가구와 배경을 골라 보세요.",
            "cta": "코인 충전하기"
          }
        }
    """.trimIndent()

    @Test
    fun `운영 문서 예시 JSON을 그룹과 공지로 읽고, 묶음은 앱에 고정된 A 그룹 표를 쓴다`() {
        val event = parse(groupA)!!

        assertEquals("2026-11-coin-charge", event.eventId)
        assertEquals("A", event.group)
        assertEquals("코인 충전하기", event.notice?.ctaText)
        assertEquals(
            listOf(
                CoinChargePackageVO(coinPackageId = 1, coins = 300, price = 1000, imageId = 1),
                CoinChargePackageVO(coinPackageId = 2, coins = 1500, price = 5000, imageId = 2),
                CoinChargePackageVO(coinPackageId = 3, coins = 3000, price = 10000, imageId = 3),
            ),
            event.packages,
        )
    }

    @Test
    fun `B 그룹은 코인 수와 묶음 번호는 같고 가격만 1,500 · 7,500 · 15,000원이다`() {
        val event = parse(groupA.replace("\"group\": \"A\"", "\"group\": \"B\""))!!

        assertEquals("B", event.group)
        assertEquals(listOf(1L, 2L, 3L), event.packages.map { it.coinPackageId })
        assertEquals(listOf(300, 1500, 3000), event.packages.map { it.coins })
        assertEquals(listOf(1500, 7500, 15000), event.packages.map { it.price })
    }

    @Test
    fun `그룹은 앞뒤 공백과 대소문자를 맞춰 읽는다`() {
        assertEquals("B", parse(groupA.replace("\"group\": \"A\"", "\"group\": \" b \""))?.group)
    }

    @Test
    fun `그룹이 없거나 A·B 밖이면 서버가 거부하므로 이벤트를 버린다`() {
        assertNull(parse(groupA.replace("\"group\": \"A\",", "")))
        assertNull(parse(groupA.replace("\"group\": \"A\"", "\"group\": \"C\"")))
    }

    @Test
    fun `회차 구분값이 없으면 이벤트를 버린다`() {
        assertNull(parse(groupA.replace("\"eventId\": \"2026-11-coin-charge\",", "")))
    }

    @Test
    fun `예전 형식의 grantCoins와 packages가 남아 있어도 읽지 않고 고정 표를 쓴다`() {
        val old = groupA.replace(
            "\"group\": \"A\",",
            "\"group\": \"A\", \"grantCoins\": 10, \"packages\": [ { \"id\": \"coin_1\", \"coins\": 1, \"price\": 1 } ],",
        )

        assertEquals(listOf(1000, 5000, 10000), parse(old)!!.packages.map { it.price })
    }

    @Test
    fun `버튼 글자가 없는 공지는 버리고 충전 화면만 남긴다`() {
        val event = parse(groupA.replace("\"cta\": \"코인 충전하기\"", "\"cta\": \"\""))

        assertNotNull(event)
        assertNull(event!!.notice)
    }

    @Test
    fun `선물 코인 받기 응답의 코인 수와 잔액을 읽는다`() {
        val dto = json.decodeFromString(CoinGiftDTO.serializer(), """{ "coinQuantity": 3000, "balance": 3860 }""")

        assertEquals(CoinChargeGrantVO(grantedCoins = 3000, balance = 3860), dto.toVO())
        assertEquals("""{"coinPackageId":2}""", json.encodeToString(CoinGiftRequestDTO.serializer(), CoinGiftRequestDTO(2)))
    }
}
