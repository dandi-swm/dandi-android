package com.dandi.nyummy.main.presentation.navigation

import androidx.navigation3.runtime.NavKey
import com.dandi.nyummy.history.domain.HistoryPage
import com.dandi.nyummy.home.domain.HomePage
import com.dandi.nyummy.meal.domain.MealRecordPage
import com.dandi.nyummy.shop.domain.ShopCartPage
import com.dandi.nyummy.shop.domain.ShopCoinHistoryPage
import com.dandi.nyummy.shop.domain.ShopPage
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 식사 기록을 마치면 기록 화면(MealRecordViewModel)이 보내는 "뒤로 + 홈 탭" 두 신호로 기록 화면이 닫히고 홈이 맨 위에 온다.
 * 호스트가 두 신호를 처리하는 그대로([popUnlessRoot], [bringTabToFront]) 진입점마다 백스택을 확인합니다.
 */
class MealRecordCloseToHomeTest {

    private val home = GenericNavKey(HomePage.PATH)
    private val history = GenericNavKey(HistoryPage.PATH)
    private val shop = GenericNavKey(ShopPage.PATH)
    private val cart = GenericNavKey(ShopCartPage.PATH)
    private val coinHistory = GenericNavKey(ShopCoinHistoryPage.PATH)
    private val mealRecord = GenericNavKey(MealRecordPage.PATH)

    /** NavSignal.Back → GoToDestPage(HomePage) 순서로 처리한다. */
    private fun MutableList<NavKey>.closeToHome() {
        popUnlessRoot()
        bringTabToFront(home)
    }

    @Test
    fun `홈에서 열었으면 뒤로 간 것과 같다`() {
        val stack = mutableListOf<NavKey>(home, mealRecord)

        stack.closeToHome()

        assertEquals(listOf(home), stack)
    }

    @Test
    fun `기록 탭에서 열었으면 기록 화면은 닫히고 홈이 맨 위에 온다`() {
        val stack = mutableListOf<NavKey>(home, history, mealRecord)

        stack.closeToHome()

        assertEquals(listOf(history, home), stack)
    }

    @Test
    fun `장바구니에서 열었으면 기록 화면은 닫히고 홈이 맨 위에 온다`() {
        val stack = mutableListOf<NavKey>(home, shop, cart, mealRecord)

        stack.closeToHome()

        assertEquals(listOf(shop, cart, home), stack)
    }

    @Test
    fun `코인 내역에서 열었으면 기록 화면은 닫히고 홈이 맨 위에 온다`() {
        val stack = mutableListOf<NavKey>(home, shop, coinHistory, mealRecord)

        stack.closeToHome()

        assertEquals(listOf(shop, coinHistory, home), stack)
    }
}
