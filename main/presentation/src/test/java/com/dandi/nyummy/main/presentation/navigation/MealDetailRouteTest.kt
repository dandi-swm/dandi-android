package com.dandi.nyummy.main.presentation.navigation

import com.dandi.nyummy.common.domain.navigation.NavRoute
import com.dandi.nyummy.history.domain.HistoryPage
import com.dandi.nyummy.history.domain.MealDetailPage
import com.dandi.nyummy.home.domain.HomePage
import com.dandi.nyummy.main.domain.deeplink.matchRoute
import org.junit.Assert.assertEquals
import org.junit.Test

class MealDetailRouteTest {

    private fun resolve(vararg segments: String): NavRoute? = matchRoute(
        segments = segments.toList(),
        query = emptyMap(),
        literalPaths = appRouteByPath.keys,
        templates = appRoutePatterns.map { it.first },
        aliases = appLinkAliases,
    )

    @Test
    fun `알림의 식사 링크는 그 식사의 상세 라우트로 간다`() {
        assertEquals(MealDetailPage(42L).toRoute(), resolve("meals", "42"))
    }

    @Test
    fun `식사 상세 경로를 그대로 열어도 같은 라우트다`() {
        assertEquals(MealDetailPage(42L).toRoute(), resolve("history", "meals", "42"))
    }

    @Test
    fun `식사 상세로 바로 들어오면 홈, 기록, 상세 순으로 쌓아 뒤로 가면 기록이다`() {
        val args = MealDetailPage(42L).toRoute().args
        val stack = appRouteByPath.getValue(MealDetailPage.PATH).syntheticStack(args)

        assertEquals(
            listOf(
                GenericNavKey(HomePage.PATH),
                GenericNavKey(HistoryPage.PATH),
                GenericNavKey(MealDetailPage.PATH, args),
            ),
            stack,
        )
    }
}
