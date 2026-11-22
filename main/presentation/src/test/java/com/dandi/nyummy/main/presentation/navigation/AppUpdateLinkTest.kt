package com.dandi.nyummy.main.presentation.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.dandi.nyummy.home.domain.HomePage
import com.dandi.nyummy.intro.domain.IntroPage
import com.dandi.nyummy.main.presentation.deeplink.shouldOpenStoreOnStart
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 앱 업데이트 링크(`https://link.nyummy.co.kr/app-update`)가 진입 경로마다 Play 스토어를 정확히 한 번 열고,
 * 앱 백스택(인트로, 로그인, 홈 등 평소 흐름)은 건드리지 않는지 확인합니다.
 */
class AppUpdateLinkTest {

    private val link = "https://link.nyummy.co.kr/app-update"
    private val pushLink = "https://link.nyummy.co.kr/App-Update/?notificationId=n-1"

    private var storeOpened = 0
    private val openStore: () -> Unit = { storeOpened++ }

    // 콜드 스타트(MainActivity.onCreate) ----------------------------------------------------------

    @Test
    fun `콜드 스타트 - 링크를 눌러 처음 켰으면 스토어를 연다`() {
        assertTrue(shouldOpenStoreOnStart(link, isRestored = false, isLaunchedFromHistory = false))
        assertTrue(shouldOpenStoreOnStart(pushLink, isRestored = false, isLaunchedFromHistory = false))
    }

    @Test
    fun `콜드 스타트 - 화면 회전이나 시스템 복원으로 다시 만들어지면 스토어를 다시 열지 않는다`() {
        assertFalse(shouldOpenStoreOnStart(link, isRestored = true, isLaunchedFromHistory = false))
    }

    @Test
    fun `콜드 스타트 - 최근 앱 목록에서 다시 열면 예전 링크로 스토어를 다시 열지 않는다`() {
        assertFalse(shouldOpenStoreOnStart(link, isRestored = false, isLaunchedFromHistory = true))
    }

    @Test
    fun `콜드 스타트 - 화면 링크면 스토어를 열지 않는다`() {
        assertFalse(shouldOpenStoreOnStart("https://link.nyummy.co.kr/home", isRestored = false, isLaunchedFromHistory = false))
    }

    // 웜 스타트·앱 안 링크(NavSignal.AppLink → handleAppLink) -----------------------------------------

    @Test
    fun `웜 스타트 - 보던 화면은 그대로 두고 스토어를 한 번 연다`() {
        val backStack = NavBackStack<NavKey>(GenericNavKey(HomePage.PATH))

        handleAppLink(pushLink, asRoot = false, backStack = backStack, openStore = openStore)

        assertEquals(1, storeOpened)
        assertEquals(listOf<NavKey>(GenericNavKey(HomePage.PATH)), backStack.toList())
    }

    @Test
    fun `로그인 전 - 인트로나 로그인 화면에서도 화면을 바꾸지 않고 스토어를 연다`() {
        val backStack = NavBackStack<NavKey>(GenericNavKey(IntroPage.PATH))

        handleAppLink(link, asRoot = false, backStack = backStack, openStore = openStore)

        assertEquals(1, storeOpened)
        assertEquals(listOf<NavKey>(GenericNavKey(IntroPage.PATH)), backStack.toList())
    }

    @Test
    fun `링크 신호 한 건에 스토어는 한 번만 열린다`() {
        // 신호는 채널로 한 번만 오므로, 링크가 두 번 와야 두 번 열린다(같은 신호를 다시 처리하지 않는다).
        val backStack = NavBackStack<NavKey>(GenericNavKey(HomePage.PATH))

        handleAppLink(link, asRoot = false, backStack = backStack, openStore = openStore)
        assertEquals(1, storeOpened)

        handleAppLink(link, asRoot = false, backStack = backStack, openStore = openStore)
        assertEquals(2, storeOpened)
    }

    // 광고 트래킹 링크(NavSignal.AppLinkAfterEntry) ------------------------------------------------

    @Test
    fun `광고 링크 - 진입 흐름을 기다리지 않고 바로 스토어를 연다`() {
        assertTrue(handleAppUpdateLinkAfterEntry(link, waitForTabRoot = false, openStore = openStore))
        assertEquals(1, storeOpened)
    }

    @Test
    fun `광고 링크 - 설치 뒤 첫 실행에 이어받은 링크는 스토어를 다시 열지 않고 버린다`() {
        assertTrue(handleAppUpdateLinkAfterEntry(link, waitForTabRoot = true, openStore = openStore))
        assertEquals(0, storeOpened)
    }

    @Test
    fun `광고 링크 - 화면 링크는 원래대로 진입 흐름 뒤에 연다`() {
        assertFalse(handleAppUpdateLinkAfterEntry("https://link.nyummy.co.kr/shop", waitForTabRoot = false, openStore = openStore))
        assertEquals(0, storeOpened)
    }
}
