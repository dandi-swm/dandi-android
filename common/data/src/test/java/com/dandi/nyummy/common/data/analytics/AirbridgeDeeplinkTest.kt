package com.dandi.nyummy.common.data.analytics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Airbridge 가 돌려준 목적지를 앱 링크로 바꾸는지 확인합니다. */
class AirbridgeDeeplinkTest {

    @Test
    fun `스킴 링크는 첫 구간부터 앱 링크 경로가 된다`() {
        assertEquals(
            "https://link.nyummy.co.kr/shop/promotions/3?sort=new",
            "nyummy://shop/promotions/3?sort=new&airbridge_referrer=abc".toAppLinkOrNull(),
        )
    }

    @Test
    fun `슬래시 세 개 스킴 링크도 같은 경로가 된다`() {
        assertEquals("https://link.nyummy.co.kr/shop/cart", "nyummy:///shop/cart".toAppLinkOrNull())
    }

    @Test
    fun `경로 없는 스킴 링크는 홈으로 연다`() {
        assertEquals("https://link.nyummy.co.kr/home", "nyummy://".toAppLinkOrNull())
        assertEquals("https://link.nyummy.co.kr/home", "nyummy://?airbridge_referrer=abc".toAppLinkOrNull())
    }

    @Test
    fun `앱 링크는 경로를 두고 Airbridge 쿼리만 뺀다`() {
        assertEquals(
            "https://link.nyummy.co.kr/home",
            "https://link.nyummy.co.kr/home?airbridge_referrer=abc&airbridge_x=1".toAppLinkOrNull(),
        )
        assertEquals(
            "https://link.nyummy.co.kr/shop?tab=interior",
            "https://link.nyummy.co.kr/shop?tab=interior&airbridge_referrer=abc".toAppLinkOrNull(),
        )
    }

    @Test
    fun `http 앱 링크도 앱 링크로 받는다`() {
        assertEquals("https://link.nyummy.co.kr/shop", "http://link.nyummy.co.kr/shop?airbridge_referrer=abc".toAppLinkOrNull())
    }

    @Test
    fun `계층형이 아닌 주소는 예외 없이 열지 않는다`() {
        assertNull("nyummy:shop?airbridge_referrer=abc".toAppLinkOrNull())
    }

    @Test
    fun `다른 주소는 열지 않는다`() {
        assertNull("https://example.com/shop".toAppLinkOrNull())
        assertNull("https://nyummy.abr.ge/shop".toAppLinkOrNull())
        assertNull("not a uri".toAppLinkOrNull())
    }
}
