package com.dandi.nyummy.auth.domain

import com.dandi.nyummy.home.domain.HomePreparePage
import com.dandi.nyummy.onboarding.domain.OnboardingPage
import org.junit.Assert.assertEquals
import org.junit.Test

class PostLoginDestinationTest {

    @Test
    fun `앱 라우트 path 로 온 온보딩은 온보딩으로 보낸다`() {
        assertEquals(OnboardingPage, PostLoginDestination.from("/onboarding"))
    }

    @Test
    fun `앱 링크 URL 로 온 온보딩도 path 만 보고 온보딩으로 보낸다`() {
        assertEquals(OnboardingPage, PostLoginDestination.from("https://link.nyummy.co.kr/onboarding"))
        assertEquals(OnboardingPage, PostLoginDestination.from("https://link.nyummy.co.kr/onboarding/?from=login"))
    }

    @Test
    fun `홈이나 빈 값이나 알 수 없는 값은 냐미 방 준비 화면을 거쳐 홈으로 보낸다`() {
        assertEquals(HomePreparePage, PostLoginDestination.from("/home"))
        assertEquals(HomePreparePage, PostLoginDestination.from("https://link.nyummy.co.kr/home"))
        assertEquals(HomePreparePage, PostLoginDestination.from(""))
        assertEquals(HomePreparePage, PostLoginDestination.from("dandi://"))
        assertEquals(HomePreparePage, PostLoginDestination.from("/onboarding-old"))
    }
}
