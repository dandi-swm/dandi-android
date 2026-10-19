package com.dandi.nyummy.common.presentation.image

import org.junit.Assert.assertEquals
import org.junit.Test

class NyummyCdnTest {

    @Test
    fun `종류별로 정해진 CDN 경로에 id를 붙인다`() {
        assertEquals("https://cdn.nyummy.co.kr/icons/rice.png", NyummyCdn.foodIconUrl("rice"))
        assertEquals("https://cdn.nyummy.co.kr/items/12.png", NyummyCdn.itemUrl(12L))
        assertEquals("https://cdn.nyummy.co.kr/reward-boxes/3.png", NyummyCdn.rewardBoxUrl(3L))
        assertEquals("https://cdn.nyummy.co.kr/coins/1.png", NyummyCdn.coinUrl(1L))
    }
}
