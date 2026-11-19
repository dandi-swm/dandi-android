package com.dandi.nyummy.main.presentation.deeplink

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 앱 링크 호스트의 주소만 라우트로 해석하는지 확인합니다. */
class AppLinkHostTest {

    @Test
    fun `앱 링크 호스트의 http(s) 주소는 앱 링크다`() {
        assertTrue(isAppLinkAddress("https", "link.nyummy.co.kr"))
        assertTrue(isAppLinkAddress("HTTP", "LINK.nyummy.co.kr"))
    }

    @Test
    fun `광고 트래킹 링크는 앱 링크가 아니다`() {
        assertFalse(isAppLinkAddress("nyummy", "shop"))
        assertFalse(isAppLinkAddress("nyummy", null))
        assertFalse(isAppLinkAddress("https", "nyummy.abr.ge"))
        assertFalse(isAppLinkAddress("https", "nyummy.airbridge.io"))
    }

    @Test
    fun `스킴이나 호스트가 없으면 앱 링크가 아니다`() {
        assertFalse(isAppLinkAddress(null, "link.nyummy.co.kr"))
        assertFalse(isAppLinkAddress("ftp", "link.nyummy.co.kr"))
    }
}
