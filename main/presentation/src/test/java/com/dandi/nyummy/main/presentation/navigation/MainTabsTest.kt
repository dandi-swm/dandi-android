package com.dandi.nyummy.main.presentation.navigation

import com.dandi.nyummy.common.presentation.designsystem.component.NyummyMainTabs
import com.dandi.nyummy.history.domain.HistoryPage
import com.dandi.nyummy.home.domain.HomePage
import com.dandi.nyummy.meal.domain.MealRecordPage
import com.dandi.nyummy.room.domain.MyRoomPage
import com.dandi.nyummy.shop.domain.ShopPage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MainTabsTest {

    @Test
    fun `탭 화면은 내비 아이콘 순서와 같다`() {
        val expectedTabs = listOf(
            "홈" to HomePage.PATH,
            "마이룸" to MyRoomPage.PATH,
            "기록" to HistoryPage.PATH,
            "상점" to ShopPage.PATH,
        )
        assertEquals(expectedTabs.map { it.first }, NyummyMainTabs.map { it.label })
        assertEquals(expectedTabs.map { it.second }, mainTabPages.map { it.toRoute().path })
    }

    @Test
    fun `탭 화면 경로로 몇 번째 탭인지 찾는다`() {
        assertEquals(0, mainTabIndexOf(HomePage.PATH))
        assertEquals(2, mainTabIndexOf(HistoryPage.PATH))
    }

    @Test
    fun `탭이 아닌 화면은 -1이다`() {
        assertEquals(-1, mainTabIndexOf(MealRecordPage.PATH))
        assertEquals(-1, mainTabIndexOf(null))
    }

    @Test
    fun `마이룸 말고 탭 화면은 모두 탭 루트로 등록돼 있다`() {
        mainTabPages.filter { it != MyRoomPage }.forEach { page ->
            val route = appRouteByPath[page.toRoute().path]
            assertTrue("${page.toRoute().path} 등록 확인", route?.isBottomTab == true)
        }
    }
}
