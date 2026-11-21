package com.dandi.nyummy.main.presentation.navigation

import androidx.navigation3.runtime.NavKey
import com.dandi.nyummy.history.domain.HistoryPage
import com.dandi.nyummy.home.domain.HomePage
import com.dandi.nyummy.shop.domain.ShopCoinChargePage
import com.dandi.nyummy.shop.domain.ShopPage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * 코인을 받은 뒤 화면의 "홈으로"는 ShopCoinChargeViewModel이 "뒤로 + 홈 탭" 두 신호를 보낸다.
 * 호스트가 두 신호를 처리하는 그대로([popUnlessRoot], [bringTabToFront]) 진입 경로마다 백스택을 확인하고,
 * 충전 화면이 홈 아래에 남지 않는지(홈에서 뒤로 가도 충전 화면으로 돌아오지 않는지) 본다.
 */
class CoinChargeCloseToHomeTest {

    private val home = GenericNavKey(HomePage.PATH)
    private val history = GenericNavKey(HistoryPage.PATH)
    private val shop = GenericNavKey(ShopPage.PATH)
    private val chargeFromShopTab = GenericNavKey.of(ShopCoinChargePage.fromShopTab.toRoute())
    private val chargeFromLink = GenericNavKey(ShopCoinChargePage.PATH)

    /** NavSignal.Back → GoToDestPage(HomePage) 순서로 처리한다. */
    private fun MutableList<NavKey>.closeToHome() {
        popUnlessRoot()
        bringTabToFront(home)
    }

    private fun List<NavKey>.hasCoinCharge() = any { (it as? GenericNavKey)?.path == ShopCoinChargePage.PATH }

    @Test
    fun `상점 코인 탭에서 들어왔으면 충전 화면은 닫히고 상점 위에 홈이 온다`() {
        // 홈 공지의 코인 충전 장도 상점을 열고, 그 코인 탭으로 들어오므로 같은 스택이다.
        val stack = mutableListOf<NavKey>(home, shop, chargeFromShopTab)

        stack.closeToHome()

        assertEquals(listOf(shop, home), stack)
        assertFalse(stack.hasCoinCharge())
    }

    @Test
    fun `링크로 앱을 처음 열었으면 홈만 남는다`() {
        // 콜드 스타트는 라우트의 syntheticStack([홈, 충전])으로 시작한다.
        val stack = mutableListOf<NavKey>(home, chargeFromLink)

        stack.closeToHome()

        assertEquals(listOf(home), stack)
    }

    @Test
    fun `앱이 켜진 채 다른 탭에서 링크로 열었으면 충전 화면은 닫히고 그 탭 위에 홈이 온다`() {
        val stack = mutableListOf<NavKey>(home, history, chargeFromLink)

        stack.closeToHome()

        assertEquals(listOf(history, home), stack)
        assertFalse(stack.hasCoinCharge())
    }

    @Test
    fun `홈에서 뒤로 가도 충전 화면으로 돌아오지 않는다`() {
        val stack = mutableListOf<NavKey>(home, shop, chargeFromShopTab)
        stack.closeToHome()

        // 시스템 뒤로 가기는 맨 위 하나를 닫는다.
        stack.removeAt(stack.lastIndex)

        assertEquals(listOf(shop), stack)
    }
}
