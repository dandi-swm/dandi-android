package com.dandi.nyummy.shop.domain

import com.dandi.nyummy.common.domain.analytics.AnalyticsEvent
import com.dandi.nyummy.common.domain.analytics.AnalyticsHelper
import com.dandi.nyummy.shop.entity.PurchaseResultVO
import com.dandi.nyummy.shop.entity.SduiComponentVO
import com.dandi.nyummy.shop.entity.ShopRecommendPageVO
import com.dandi.nyummy.shop.entity.ShopTab
import javax.inject.Inject

/**
 * 상점 분석 이벤트(추천 탭 배치 A/B). 추천 탭 노출·칸별 누름·담기·구매를 같은 이름과 매개변수로 남긴다.
 * 배치 그룹은 추천 탭을 읽을 때마다 사용자 속성(shop_layout_group)으로도 남겨, 담기·구매를 그룹별로 나눠 볼 수 있게 한다.
 */
class ShopTracker @Inject constructor(
    private val analyticsHelper: AnalyticsHelper,
) {
    fun rememberLayoutGroup(layoutVariant: String) {
        analyticsHelper.setUserProperty(AnalyticsHelper.UserProperty.SHOP_LAYOUT_GROUP, layoutVariant)
    }

    fun recommendView(page: ShopRecommendPageVO) = analyticsHelper.logEvent(
        AnalyticsEvent.ShopRecommendView(
            layoutGroup = page.layoutVariant,
            sections = page.components.filterNot { it is SduiComponentVO.SectionHeader }.joinToString(",") { it.sectionName },
        ),
    )

    /** 추천 탭 [componentIndex]번째 칸의 [position]번째(0부터)를 눌렀다. 상품이면 [productId], 배너면 [bannerId]. */
    fun recommendItemClick(page: ShopRecommendPageVO, componentIndex: Int, position: Int, productId: Long?, bannerId: Long?) {
        val component = page.components.getOrNull(componentIndex) ?: return
        analyticsHelper.logEvent(
            AnalyticsEvent.ShopRecommendItemClick(
                layoutGroup = page.layoutVariant,
                section = component.sectionName,
                sectionOrder = page.components.take(componentIndex + 1).count { it !is SduiComponentVO.SectionHeader },
                position = position + 1,
                productId = productId,
                bannerId = bannerId,
            ),
        )
    }

    fun addToCart(source: String, productId: Long) =
        analyticsHelper.logEvent(AnalyticsEvent.ShopAddToCart(source, productId))

    fun purchase(result: PurchaseResultVO) =
        analyticsHelper.logEvent(AnalyticsEvent.ShopPurchase(itemCount = result.quantity, coins = result.purchasePrice))

    /** 분석 이벤트의 담은 곳 값. 철자를 이 파일에서만 정한다. */
    object Source {
        const val PROMOTION = "promotion"
        const val CART = "cart"

        /** 상점 탭에서 담았다(recommend, interior, background). */
        fun of(tab: ShopTab): String = tab.linkSegment
    }
}

/** 분석 이벤트에 남기는 칸 종류 이름. */
private val SduiComponentVO.sectionName: String
    get() = when (this) {
        is SduiComponentVO.HeroBanner -> "hero_banner"
        is SduiComponentVO.SectionHeader -> "section_header"
        is SduiComponentVO.ItemGrid -> "item_grid"
        is SduiComponentVO.Ranking -> "ranking"
        is SduiComponentVO.ItemList -> "item_list"
    }

/** 추천 탭 배치 그룹. 원격 구성 값이 비었거나 모르는 값이면 A다(서버도 헤더가 없거나 모르는 값이면 A로 준다). */
object ShopLayoutVariant {
    const val A = "A"
    const val B = "B"

    fun of(raw: String): String = when (raw.trim().uppercase()) {
        B -> B
        else -> A
    }
}
