package com.dandi.nyummy.common.presentation.image

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NyummyCdnPngFallbackUrlTest {

    @Test
    fun `앱이 만든 webp 주소는 같은 경로의 png 주소로 바꾼다`() {
        assertEquals("https://cdn.nyummy.co.kr/items/12.png", NyummyCdn.pngFallbackUrlOf(NyummyCdn.itemUrl(12L)))
        assertEquals(
            "https://cdn.nyummy.co.kr/reward-boxes/closed/3.png",
            NyummyCdn.pngFallbackUrlOf(NyummyCdn.rewardBoxClosedUrl(3L)),
        )
        assertEquals(
            "https://cdn.nyummy.co.kr/reward-boxes/opened/3.png",
            NyummyCdn.pngFallbackUrlOf(NyummyCdn.rewardBoxOpenedUrl(3L)),
        )
        assertEquals("https://cdn.nyummy.co.kr/coins/1.png", NyummyCdn.pngFallbackUrlOf(NyummyCdn.coinUrl(1L)))
    }

    @Test
    fun `다른 호스트, png, 쿼리가 붙은 주소는 건드리지 않는다`() {
        assertNull(NyummyCdn.pngFallbackUrlOf("https://cdn.example.com/banner.webp"))
        assertNull(NyummyCdn.pngFallbackUrlOf("https://cdn.nyummy.co.kr/items/12.png"))
        assertNull(NyummyCdn.pngFallbackUrlOf("https://cdn.nyummy.co.kr/items/12.webp?v=2"))
        assertNull(NyummyCdn.pngFallbackUrlOf("https://cdn.nyummy.co.kr.evil.com/items/12.webp"))
        assertNull(NyummyCdn.pngFallbackUrlOf(""))
    }
}
