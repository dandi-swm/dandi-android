package com.dandi.nyummy.main.presentation.navigation

import com.dandi.nyummy.achievement.domain.AchievementPage
import com.dandi.nyummy.collection.domain.CollectionPage
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyMainTabs
import com.dandi.nyummy.history.domain.HistoryPage
import com.dandi.nyummy.home.domain.HomePage
import com.dandi.nyummy.meal.domain.MealRecordPage
import com.dandi.nyummy.quest.domain.QuestPage
import com.dandi.nyummy.shop.domain.ShopPage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MainTabsTest {

    @Test
    fun `탭 화면은 내비 아이콘 순서와 같다`() {
        assertEquals(NyummyMainTabs.size, mainTabPages.size)
        assertEquals(
            listOf(HomePage.PATH, HistoryPage.PATH, QuestPage.PATH, AchievementPage.PATH, ShopPage.PATH),
            mainTabPages.map { it.toRoute().path },
        )
    }

    @Test
    fun `탭 화면 경로로 몇 번째 탭인지 찾는다`() {
        assertEquals(0, mainTabIndexOf(HomePage.PATH))
        assertEquals(2, mainTabIndexOf(QuestPage.PATH))
        assertEquals(4, mainTabIndexOf(ShopPage.PATH))
    }

    @Test
    fun `탭이 아닌 화면은 -1이다`() {
        assertEquals(-1, mainTabIndexOf(MealRecordPage.PATH))
        assertEquals(-1, mainTabIndexOf(CollectionPage.PATH))
        assertEquals(-1, mainTabIndexOf(null))
    }

    @Test
    fun `탭 화면은 모두 탭 루트로 등록돼 있다`() {
        mainTabPages.forEach { page ->
            val route = appRouteByPath[page.toRoute().path]
            assertTrue("${page.toRoute().path} 등록 확인", route?.isBottomTab == true)
        }
    }

    @Test
    fun `도감은 탭에서 빠져 일반 화면으로 열린다`() {
        assertFalse(appRouteByPath.getValue(CollectionPage.PATH).isBottomTab)
    }
}
