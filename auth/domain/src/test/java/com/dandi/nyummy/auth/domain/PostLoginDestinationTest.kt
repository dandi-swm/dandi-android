package com.dandi.nyummy.auth.domain

import com.dandi.nyummy.home.domain.HomePage
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
    fun `홈이나 빈 값이나 알 수 없는 값은 홈으로 보낸다`() {
        assertEquals(HomePage, PostLoginDestination.from("/home"))
        assertEquals(HomePage, PostLoginDestination.from("https://link.nyummy.co.kr/home"))
        assertEquals(HomePage, PostLoginDestination.from(""))
        assertEquals(HomePage, PostLoginDestination.from("dandi://"))
        assertEquals(HomePage, PostLoginDestination.from("/onboarding-old"))
    }
}
