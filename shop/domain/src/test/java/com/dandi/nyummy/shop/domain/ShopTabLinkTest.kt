package com.dandi.nyummy.shop.domain

import com.dandi.nyummy.common.domain.navigation.NavRoute
import com.dandi.nyummy.shop.entity.ProductSort
import com.dandi.nyummy.shop.entity.ShopTab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ShopTabLinkTest {

    private fun parse(link: String) = ShopTabLink.parseLink(link)

    @Test
    fun `서버 인기 칸 전체보기 링크는 인테리어 탭 인기순이다`() {
        assertEquals(
            ShopTabLink.Target(ShopTab.INTERIOR, ProductSort.POPULAR),
            parse("https://link.nyummy.co.kr/shop/interior?sort=POPULAR"),
        )
    }

    @Test
    fun `정렬이 없으면 정렬은 그대로 둔다`() {
        assertEquals(ShopTabLink.Target(ShopTab.BACKGROUND, null), parse("https://link.nyummy.co.kr/shop/background/"))
    }

    @Test
    fun `모르는 정렬 값은 기본 정렬이다`() {
        assertEquals(ShopTabLink.Target(ShopTab.INTERIOR, ProductSort.NEW), parse("https://link.nyummy.co.kr/shop/interior?sort=CHEAPEST"))
    }

    @Test
    fun `별칭으로 바뀐 상점 라우트도 같은 결과다`() {
        val route = NavRoute(ShopPage.PATH, mapOf(ShopPage.ARG_TAB to "interior", ShopPage.ARG_SORT to "PRICE_ASC"))
        assertEquals(ShopTabLink.Target(ShopTab.INTERIOR, ProductSort.PRICE_ASC), ShopTabLink.parse(route))
        assertEquals(route, ShopPage.of(ShopTab.INTERIOR, ProductSort.PRICE_ASC).toRoute())
    }

    @Test
    fun `상점 탭이 아닌 링크는 null이다`() {
        assertNull(parse("https://link.nyummy.co.kr/shop"))
        assertNull(parse("https://link.nyummy.co.kr/shop/recommend"))
        assertNull(parse("https://link.nyummy.co.kr/shop/cart"))
        assertNull(parse("https://link.nyummy.co.kr/shop/promotions/12"))
        assertNull(parse("https://link.nyummy.co.kr/home"))
        assertNull(parse("https://example.com/shop/interior"))
        assertNull(parse("/shop/interior"))
        assertNull(parse("not a link"))
        assertNull(ShopTabLink.parse(NavRoute(ShopPage.PATH, mapOf(ShopPage.ARG_TAB to "unknown"))))
    }
}
