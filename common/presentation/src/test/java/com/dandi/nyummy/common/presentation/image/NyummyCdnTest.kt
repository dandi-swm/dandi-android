package com.dandi.nyummy.common.presentation.image

import org.junit.Assert.assertEquals
import org.junit.Test

class NyummyCdnTest {

    @Test
    fun `종류별로 정해진 CDN 경로에 id를 붙인다`() {
        assertEquals("https://cdn.nyummy.co.kr/items/12.webp", NyummyCdn.itemUrl(12L))
        assertEquals("https://cdn.nyummy.co.kr/reward-boxes/closed/3.webp", NyummyCdn.rewardBoxClosedUrl(3L))
        assertEquals("https://cdn.nyummy.co.kr/reward-boxes/opened/3.webp", NyummyCdn.rewardBoxOpenedUrl(3L))
        assertEquals("https://cdn.nyummy.co.kr/coins/1.webp", NyummyCdn.coinUrl(1L))
    }
}
