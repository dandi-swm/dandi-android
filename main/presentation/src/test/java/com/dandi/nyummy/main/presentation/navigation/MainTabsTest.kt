package com.dandi.nyummy.main.presentation.navigation

import com.dandi.nyummy.common.presentation.designsystem.component.NyummyMainTabs
import com.dandi.nyummy.history.domain.HistoryPage
import com.dandi.nyummy.home.domain.HomePage
import com.dandi.nyummy.meal.domain.MealRecordPage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MainTabsTest {

    @Test
    fun `탭 화면은 내비 아이콘 순서와 같다`() {
        val expectedTabs = listOf(
            "홈" to HomePage.PATH,
            "기록" to HistoryPage.PATH,
        )
        assertEquals(expectedTabs.map { it.first }, NyummyMainTabs.map { it.label })
        assertEquals(expectedTabs.map { it.second }, mainTabPages.map { it.toRoute().path })
    }

    @Test
    fun `탭 화면 경로로 몇 번째 탭인지 찾는다`() {
        assertEquals(0, mainTabIndexOf(HomePage.PATH))
        assertEquals(1, mainTabIndexOf(HistoryPage.PATH))
    }

    @Test
    fun `탭이 아닌 화면은 -1이다`() {
        assertEquals(-1, mainTabIndexOf(MealRecordPage.PATH))
        assertEquals(-1, mainTabIndexOf(null))
    }

    @Test
    fun `탭 화면은 모두 탭 루트로 등록돼 있다`() {
        mainTabPages.forEach { page ->
            val route = appRouteByPath[page.toRoute().path]
            assertTrue("${page.toRoute().path} 등록 확인", route?.isBottomTab == true)
        }
    }
}
