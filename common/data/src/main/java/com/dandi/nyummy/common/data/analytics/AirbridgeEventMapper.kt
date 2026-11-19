package com.dandi.nyummy.common.data.analytics

import com.dandi.nyummy.common.domain.analytics.AnalyticsEvent

/** Airbridge 로 보낼 이벤트 하나. [category]는 표준 이벤트 이름, [semanticAttributes]는 Airbridge 가 정해 둔 속성이다. */
data class AirbridgeEvent(
    val category: String,
    val semanticAttributes: Map<String, Any> = emptyMap(),
)

/**
 * 앱의 분석 이벤트를 Airbridge 표준 이벤트로 바꾼다. 보내지 않는 이벤트는 null 이다.
 *
 * Airbridge 요금제(Core)가 표준 이벤트만 받아서, 광고 성과를 볼 때 쓰는 핵심 전환만 고른다.
 * 퍼널의 세부 단계(가입 단계, 온보딩 장면, 식사 기록 단계 등)는 GA4 에만 남는다.
 * 이름과 속성 키는 SDK 의 AirbridgeCategory, AirbridgeAttribute 값과 같다(단위 테스트가 되게 문자열로 둔다).
 */
internal fun AnalyticsEvent.toAirbridgeEvent(): AirbridgeEvent? = when (this) {
    is AnalyticsEvent.SignUp -> AirbridgeEvent("airbridge.user.signup", attributes(ACTION to params["method"]))

    is AnalyticsEvent.Login -> AirbridgeEvent("airbridge.user.signin", attributes(ACTION to params["method"]))

    AnalyticsEvent.OnboardingComplete -> AirbridgeEvent("airbridge.completeTutorial")

    // 식사 기록에 꼭 맞는 표준 이벤트가 없어 과제 달성으로 보내고, 고정 ID 로 구분한다.
    is AnalyticsEvent.MealSubmitSuccess -> AirbridgeEvent(
        "airbridge.unlockAchievement",
        attributes("achievementID" to "meal_record", ACTION to params["source"]),
    )

    is AnalyticsEvent.ShopAddToCart -> AirbridgeEvent(
        "airbridge.ecommerce.product.addedToCart",
        attributes(
            ACTION to params["source"],
            "products" to numberParams["product_id"]?.let { listOf(mapOf("productID" to it.toString())) },
        ),
    )

    // 코인은 실제 돈이 아니라서 매출로 잡히는 구매 완료 대신 크레딧 사용으로 보낸다.
    is AnalyticsEvent.ShopPurchase -> AirbridgeEvent(
        "airbridge.spendCredits",
        attributes(VALUE to numberParams["coins"], "totalQuantity" to numberParams["item_count"]),
    )

    // 가짜 결제의 "결제하기" 누름. 실제 결제가 아니라서 결제 시작까지만 남긴다.
    is AnalyticsEvent.CoinChargePayClick -> AirbridgeEvent(
        "airbridge.initiateCheckout",
        attributes(
            ACTION to params["package_id"],
            LABEL to params["ab_group"],
            VALUE to numberParams["price"],
            "currency" to "KRW",
        ),
    )

    else -> null
}

private const val ACTION = "action"
private const val LABEL = "label"
private const val VALUE = "value"

/** 값이 없는 속성은 뺀다. */
private fun attributes(vararg pairs: Pair<String, Any?>): Map<String, Any> =
    pairs.mapNotNull { (key, value) -> value?.let { key to it } }.toMap()
