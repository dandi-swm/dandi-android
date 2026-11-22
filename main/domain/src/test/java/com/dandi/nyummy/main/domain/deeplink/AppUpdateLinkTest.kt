package com.dandi.nyummy.main.domain.deeplink

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 서버 앱 업데이트 푸시가 주는 링크(`https://link.nyummy.co.kr/app-update`)를 알아보는지 확인합니다. */
class AppUpdateLinkTest {

    @Test
    fun `서버가 주는 앱 업데이트 링크를 알아본다`() {
        assertTrue(isAppUpdateLink("https://link.nyummy.co.kr/app-update"))
        assertTrue(isAppUpdateLink("http://link.nyummy.co.kr/app-update"))
    }

    @Test
    fun `대소문자가 달라도 알아본다`() {
        assertTrue(isAppUpdateLink("HTTPS://LINK.NYUMMY.CO.KR/APP-UPDATE"))
        assertTrue(isAppUpdateLink("https://link.nyummy.co.kr/App-Update"))
    }

    @Test
    fun `앞뒤 슬래시가 붙어도 알아본다`() {
        assertTrue(isAppUpdateLink("https://link.nyummy.co.kr/app-update/"))
        assertTrue(isAppUpdateLink("https://link.nyummy.co.kr//app-update//"))
    }

    @Test
    fun `쿼리나 프래그먼트가 붙어도 알아본다`() {
        // 알림에서 열리면 알림 식별자가 쿼리로 붙는다(Uri.withNotificationId).
        assertTrue(isAppUpdateLink("https://link.nyummy.co.kr/app-update?notificationId=n-1"))
        assertTrue(isAppUpdateLink("https://link.nyummy.co.kr/app-update/?utm_source=push&x=1#top"))
        assertTrue(isAppUpdateLink("  https://link.nyummy.co.kr/app-update  "))
    }

    @Test
    fun `광고 트래킹 스킴 링크는 앱 셸이 바꾼 앱 링크로 알아본다`() {
        // nyummy://app-update 는 Airbridge 처리(toAppLinkOrNull)가 아래 앱 링크로 바꿔 넘긴다.
        assertTrue(isAppUpdateLink("https://link.nyummy.co.kr/app-update"))
        assertFalse(isAppUpdateLink("nyummy://app-update"))
    }

    @Test
    fun `다른 호스트나 스킴의 주소는 앱 업데이트 링크가 아니다`() {
        assertFalse(isAppUpdateLink("https://nyummy.abr.ge/app-update"))
        assertFalse(isAppUpdateLink("https://www.nyummy.co.kr/app-update"))
        assertFalse(isAppUpdateLink("ftp://link.nyummy.co.kr/app-update"))
        assertFalse(isAppUpdateLink("market://details?id=com.dandi.nyummy"))
    }

    @Test
    fun `경로가 다르면 앱 업데이트 링크가 아니다`() {
        assertFalse(isAppUpdateLink("https://link.nyummy.co.kr/"))
        assertFalse(isAppUpdateLink("https://link.nyummy.co.kr/home"))
        assertFalse(isAppUpdateLink("https://link.nyummy.co.kr/app-updates"))
        assertFalse(isAppUpdateLink("https://link.nyummy.co.kr/app-update/now"))
        assertFalse(isAppUpdateLink("https://link.nyummy.co.kr/settings/app-update"))
        assertFalse(isAppUpdateLink("https://link.nyummy.co.kr/home?next=/app-update"))
    }

    @Test
    fun `주소로 읽을 수 없으면 앱 업데이트 링크가 아니다`() {
        assertFalse(isAppUpdateLink(""))
        assertFalse(isAppUpdateLink("app-update"))
        assertFalse(isAppUpdateLink("https://link.nyummy.co.kr/app update"))
    }
}
