package com.dandi.nyummy.common.data.analytics

import com.dandi.nyummy.common.domain.analytics.AnalyticsEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** 앱 분석 이벤트가 Airbridge 표준 이벤트로 바뀌는지 확인합니다. */
class AirbridgeEventMapperTest {

    @Test
    fun `회원가입은 가입 이벤트와 수단으로 보낸다`() {
        assertEquals(
            AirbridgeEvent("airbridge.user.signup", mapOf("action" to "email")),
            AnalyticsEvent.SignUp("email").toAirbridgeEvent(),
        )
    }

    @Test
    fun `로그인은 로그인 이벤트와 수단으로 보낸다`() {
        assertEquals(
            AirbridgeEvent("airbridge.user.signin", mapOf("action" to "kakao")),
            AnalyticsEvent.Login("kakao").toAirbridgeEvent(),
        )
    }

    @Test
    fun `온보딩 완료는 튜토리얼 완료로 보낸다`() {
        assertEquals(AirbridgeEvent("airbridge.completeTutorial"), AnalyticsEvent.OnboardingComplete.toAirbridgeEvent())
    }

    @Test
    fun `식사 기록 저장은 과제 달성으로 보낸다`() {
        assertEquals(
            AirbridgeEvent("airbridge.unlockAchievement", mapOf("achievementID" to "meal_record", "action" to "camera")),
            AnalyticsEvent.MealSubmitSuccess("camera", durationSec = 12).toAirbridgeEvent(),
        )
    }

    @Test
    fun `장바구니 담기는 상품 번호를 실어 보낸다`() {
        assertEquals(
            AirbridgeEvent(
                "airbridge.ecommerce.product.addedToCart",
                mapOf("action" to "recommend", "products" to listOf(mapOf("productID" to "42"))),
            ),
            AnalyticsEvent.ShopAddToCart("recommend", productId = 42).toAirbridgeEvent(),
        )
    }

    @Test
    fun `코인 구매는 크레딧 사용으로 보낸다`() {
        assertEquals(
            AirbridgeEvent("airbridge.spendCredits", mapOf("value" to 300L, "totalQuantity" to 2L)),
            AnalyticsEvent.ShopPurchase(itemCount = 2, coins = 300).toAirbridgeEvent(),
        )
    }

    @Test
    fun `가짜 결제 누름은 결제 시작으로 보낸다`() {
        assertEquals(
            AirbridgeEvent(
                "airbridge.initiateCheckout",
                mapOf("action" to "p1", "label" to "A", "value" to 3000L, "currency" to "KRW"),
            ),
            AnalyticsEvent.CoinChargePayClick(eventId = "e1", group = "A", packageId = "p1", price = 3000, coins = 100)
                .toAirbridgeEvent(),
        )
    }

    @Test
    fun `퍼널 세부 단계는 보내지 않는다`() {
        assertNull(AnalyticsEvent.MealRecordOpen.toAirbridgeEvent())
        assertNull(AnalyticsEvent.SignUpStep("account", "email").toAirbridgeEvent())
    }
}
