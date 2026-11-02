package com.dandi.nyummy.main.presentation.navigation

import com.dandi.nyummy.common.domain.navigation.NavRoute
import com.dandi.nyummy.main.domain.deeplink.matchRoute
import com.dandi.nyummy.shop.domain.ShopCartPage
import com.dandi.nyummy.shop.domain.ShopCoinHistoryPage
import com.dandi.nyummy.shop.domain.ShopPage
import com.dandi.nyummy.shop.domain.ShopPromotionPage
import org.junit.Assert.assertEquals
import org.junit.Test

class ShopRouteTest {

    private fun resolve(vararg segments: String, query: Map<String, String> = emptyMap()): NavRoute? = matchRoute(
        segments = segments.toList(),
        query = query,
        literalPaths = appRouteByPath.keys,
        templates = appRoutePatterns.map { it.first },
        aliases = appLinkAliases,
    )

    @Test
    fun `상점 탭 링크는 별칭으로 상점 탭이 되고 탭과 정렬이 인자로 온다`() {
        assertEquals(
            NavRoute(ShopPage.PATH, mapOf(ShopPage.ARG_TAB to "interior", ShopPage.ARG_SORT to "POPULAR")),
            resolve("shop", "interior", query = mapOf("sort" to "POPULAR")),
        )
        assertEquals(NavRoute(ShopPage.PATH, mapOf(ShopPage.ARG_TAB to "background")), resolve("shop", "background"))
    }

    @Test
    fun `장바구니와 코인 내역은 별칭보다 리터럴 라우트가 먼저다`() {
        assertEquals(ShopCartPage.toRoute(), resolve("shop", "cart"))
        assertEquals(ShopCoinHistoryPage.toRoute(), resolve("shop", "coins"))
    }

    @Test
    fun `프로모션 링크는 템플릿 라우트가 된다`() {
        assertEquals(ShopPromotionPage(12L).toRoute(), resolve("shop", "promotions", "12"))
    }

    @Test
    fun `하위 화면을 링크로 바로 열면 상점 탭 위에 쌓인다`() {
        val args = ShopPromotionPage(12L).toRoute().args
        assertEquals(
            listOf(GenericNavKey(ShopPage.PATH), GenericNavKey(ShopPromotionPage.PATH, args)),
            appRouteByPath.getValue(ShopPromotionPage.PATH).syntheticStack(args),
        )
        assertEquals(
            listOf(GenericNavKey(ShopPage.PATH), GenericNavKey(ShopCartPage.PATH)),
            appRouteByPath.getValue(ShopCartPage.PATH).syntheticStack(emptyMap()),
        )
        assertEquals(
            listOf(GenericNavKey(ShopPage.PATH), GenericNavKey(ShopCoinHistoryPage.PATH)),
            appRouteByPath.getValue(ShopCoinHistoryPage.PATH).syntheticStack(emptyMap()),
        )
    }
}
