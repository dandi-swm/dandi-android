package com.dandi.nyummy.main.presentation.navigation

import com.dandi.nyummy.common.domain.navigation.NavRoute
import com.dandi.nyummy.history.domain.HistoryPage
import com.dandi.nyummy.history.domain.MealDetailPage
import com.dandi.nyummy.history.entity.MealAnalysisVersion
import com.dandi.nyummy.home.domain.HomePage
import com.dandi.nyummy.main.domain.deeplink.matchRoute
import org.junit.Assert.assertEquals
import org.junit.Test

class MealDetailRouteTest {

    private fun resolve(vararg segments: String, query: Map<String, String> = emptyMap()): NavRoute? = matchRoute(
        segments = segments.toList(),
        query = query,
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
    fun `알림 링크에는 분석 버전이 없어 상세 화면이 정한다`() {
        val route = requireNotNull(resolve("meals", "42"))

        assertEquals(null, MealDetailPage.versionOf(route.args[MealDetailPage.ARG_VERSION]))
    }

    @Test
    fun `기록 화면이 넘긴 분석 버전은 라우트 인자로 오간다`() {
        listOf(MealAnalysisVersion.V1, MealAnalysisVersion.V2).forEach { version ->
            val args = MealDetailPage(42L, version).toRoute().args

            assertEquals(version, MealDetailPage.versionOf(args[MealDetailPage.ARG_VERSION]))
            assertEquals("42", args[MealDetailPage.ARG_MEAL_ID])
        }
    }

    @Test
    fun `링크의 v 쿼리는 분석 버전으로 읽고 모르는 값은 버린다`() {
        assertEquals(MealDetailPage(42L, MealAnalysisVersion.V2).toRoute(), resolve("history", "meals", "42", query = mapOf("v" to "2")))
        assertEquals(null, MealDetailPage.versionOf("9"))
        assertEquals(null, MealDetailPage.versionOf(null))
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
