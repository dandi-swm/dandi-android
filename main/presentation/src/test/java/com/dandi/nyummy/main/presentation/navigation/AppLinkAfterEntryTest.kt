package com.dandi.nyummy.main.presentation.navigation

import com.dandi.nyummy.auth.domain.LoginPage
import com.dandi.nyummy.home.domain.HomePage
import com.dandi.nyummy.home.domain.HomePreparePage
import com.dandi.nyummy.intro.domain.IntroPage
import com.dandi.nyummy.mailbox.domain.MailboxPage
import com.dandi.nyummy.onboarding.domain.OnboardingPage
import com.dandi.nyummy.shop.domain.ShopCoinHistoryPage
import com.dandi.nyummy.shop.domain.ShopPage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** 진입 흐름이 끝난 뒤에 여는 앱 링크를 언제 여는지 확인합니다. 화면 판정은 실제 라우트 등록 정보를 씁니다. */
class AppLinkAfterEntryTest {

    private val intro = listOf(GenericNavKey(IntroPage.PATH))
    private val login = listOf(GenericNavKey(LoginPage.PATH))
    private val prepare = listOf(GenericNavKey(HomePreparePage.PATH))
    private val home = listOf(GenericNavKey(HomePage.PATH))
    private val homeMailbox = listOf(GenericNavKey(HomePage.PATH), GenericNavKey(MailboxPage.PATH))
    private val shop = listOf(GenericNavKey(ShopPage.PATH))
    private val shopCoins = listOf(GenericNavKey(ShopPage.PATH), GenericNavKey(ShopCoinHistoryPage.PATH))
    private val link = "https://link.nyummy.co.kr/shop/cart"

    // region 누른 링크

    @Test
    fun `진입 흐름 화면이 없으면 어느 화면에서든 바로 연다`() {
        listOf(home, homeMailbox, shop, shopCoins).forEach { stack ->
            val sut = AppLinkAfterEntry()
            assertEquals(link, sut.offer(link, waitForTabRoot = false, stack))
            assertNull(sut.pending)
        }
    }

    @Test
    fun `인트로나 로그인 중이면 맡아 두었다가 진입 흐름이 끝나면 한 번 연다`() {
        listOf(intro, login, prepare).forEach { stack ->
            val sut = AppLinkAfterEntry()
            assertNull(sut.offer(link, waitForTabRoot = false, stack))
            assertNull(sut.takeIfReady(stack))
            assertEquals(link, sut.takeIfReady(shop))
            assertNull(sut.takeIfReady(shop))
        }
    }

    @Test
    fun `진입 흐름 화면이 아래에 남아 있으면 아직 열지 않는다`() {
        val sut = AppLinkAfterEntry()
        sut.offer(link, waitForTabRoot = false, login)
        assertNull(sut.takeIfReady(login + home))
        assertEquals(link, sut.takeIfReady(home))
    }

    @Test
    fun `나중에 온 링크가 앞의 것을 바꾼다`() {
        val sut = AppLinkAfterEntry()
        sut.offer("https://link.nyummy.co.kr/mailbox", waitForTabRoot = false, intro)
        sut.offer(link, waitForTabRoot = false, intro)
        assertEquals(link, sut.takeIfReady(home))
    }

    @Test
    fun `바로 연 링크는 맡아 둔 링크를 지운다`() {
        val sut = AppLinkAfterEntry()
        sut.offer("https://link.nyummy.co.kr/mailbox", waitForTabRoot = false, intro)
        assertEquals(link, sut.offer(link, waitForTabRoot = false, shopCoins))
        assertNull(sut.takeIfReady(home))
    }

    // endregion

    // region 이어받은 링크

    @Test
    fun `이어받은 링크는 맨 위가 탭 루트면 바로 연다`() {
        listOf(home, shop).forEach { stack ->
            assertEquals(link, AppLinkAfterEntry().offer(link, waitForTabRoot = true, stack))
        }
    }

    @Test
    fun `이어받은 링크가 탭 루트가 아닌 화면을 볼 때 오면 탭 루트가 맨 위가 될 때 연다`() {
        val sut = AppLinkAfterEntry()
        assertNull(sut.offer(link, waitForTabRoot = true, homeMailbox))
        assertNull(sut.takeIfReady(shopCoins))
        assertEquals(link, sut.takeIfReady(shop))
    }

    @Test
    fun `이어받은 링크가 인트로 중에 오면 진입 흐름이 끝나고 탭 루트가 맨 위일 때 연다`() {
        val sut = AppLinkAfterEntry()
        assertNull(sut.offer(link, waitForTabRoot = true, intro))
        assertNull(sut.takeIfReady(login))
        assertEquals(link, sut.takeIfReady(home))
    }

    // endregion

    @Test
    fun `맡아 둔 링크와 종류를 꺼내 다시 만들면 이어서 연다`() {
        val before = AppLinkAfterEntry()
        before.offer(link, waitForTabRoot = true, intro)

        val after = AppLinkAfterEntry(initial = before.pending)
        assertNull(after.takeIfReady(homeMailbox))
        assertEquals(link, after.takeIfReady(home))
    }

    // region 세션이 바뀌면 버리기

    @Test
    fun `진입 흐름을 마친 뒤 로그인 화면만 남으면 맡아 둔 링크를 버린다`() {
        val sut = AppLinkAfterEntry()
        sut.takeIfReady(homeMailbox)
        assertNull(sut.offer(link, waitForTabRoot = true, homeMailbox))
        assertNull(sut.takeIfReady(login))
        assertNull(sut.pending)
        assertNull(sut.takeIfReady(home))
    }

    @Test
    fun `처음 켜서 인트로에서 로그인으로 가는 것은 버리지 않고 마친 뒤에 연다`() {
        val sut = AppLinkAfterEntry()
        sut.takeIfReady(intro)
        assertNull(sut.offer(link, waitForTabRoot = false, intro))
        assertNull(sut.takeIfReady(login))
        assertNull(sut.takeIfReady(listOf(GenericNavKey(OnboardingPage.PATH))))
        assertNull(sut.takeIfReady(prepare))
        assertEquals(link, sut.takeIfReady(home))
    }

    @Test
    fun `로그아웃 뒤 로그인 화면에서 새로 맡은 링크는 다시 로그인한 뒤 연다`() {
        val sut = AppLinkAfterEntry()
        sut.takeIfReady(home)
        sut.takeIfReady(login)
        assertNull(sut.offer(link, waitForTabRoot = false, login))
        assertEquals(link, sut.takeIfReady(home))
    }

    @Test
    fun `진입 흐름을 마쳤는지도 다시 만들 때 이어진다`() {
        val before = AppLinkAfterEntry()
        before.takeIfReady(home)
        before.offer(link, waitForTabRoot = true, homeMailbox)

        val after = AppLinkAfterEntry(initial = before.pending, initialHasFinishedEntry = before.hasFinishedEntry)
        assertNull(after.takeIfReady(login))
        assertNull(after.pending)
    }

    // endregion
}
