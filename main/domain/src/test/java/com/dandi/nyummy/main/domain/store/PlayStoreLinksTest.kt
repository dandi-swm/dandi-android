package com.dandi.nyummy.main.domain.store

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** 앱 업데이트 링크가 여는 Play 스토어 주소와, Play 스토어 앱이 없을 때 웹 주소로 넘어가는지 확인합니다. */
class PlayStoreLinksTest {

    @Test
    fun `Play 에 올라간 패키지 이름으로 스토어 주소를 만든다`() {
        assertEquals("market://details?id=com.dandi.nyummy", PlayStoreLinks.MARKET_URL)
        assertEquals("https://play.google.com/store/apps/details?id=com.dandi.nyummy", PlayStoreLinks.WEB_URL)
    }

    @Test
    fun `Play 스토어 앱이 열리면 웹 주소는 열지 않는다`() {
        val tried = mutableListOf<String>()

        val opened = openFirstAvailable(PlayStoreLinks.candidates) { url -> tried += url; true }

        assertEquals(PlayStoreLinks.MARKET_URL, opened)
        assertEquals(listOf(PlayStoreLinks.MARKET_URL), tried)
    }

    @Test
    fun `Play 스토어 앱이 없으면 웹 주소로 연다`() {
        val tried = mutableListOf<String>()

        val opened = openFirstAvailable(PlayStoreLinks.candidates) { url ->
            tried += url
            url != PlayStoreLinks.MARKET_URL
        }

        assertEquals(PlayStoreLinks.WEB_URL, opened)
        assertEquals(listOf(PlayStoreLinks.MARKET_URL, PlayStoreLinks.WEB_URL), tried)
    }

    @Test
    fun `둘 다 열 수 없으면 null 이다`() {
        assertNull(openFirstAvailable(PlayStoreLinks.candidates) { false })
    }
}
