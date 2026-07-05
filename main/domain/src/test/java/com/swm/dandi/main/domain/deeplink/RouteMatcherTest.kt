package com.swm.dandi.main.domain.deeplink

import com.swm.dandi.common.domain.navigation.NavRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * [matchRoute] 의 정적-우선 / 동적-fallback / query·pathParams 병합 우선순위 검증.
 *
 * 임의 레지스트리(literalPaths/templates)를 주입해 — 실제 appRoutes 에 동적 라우트가 없어도 —
 * 계층 path 처리 전체 경로(REQ3)를 골든 케이스로 고정한다.
 */
class RouteMatcherTest {

    private val articleTemplate = RoutePattern("/articleList/articlePage/{articleId}")

    @Test
    fun `static literal beats template when both could match`() {
        // "/users/me" 는 리터럴이자 "/users/{id}" 템플릿에도 매칭 가능 → 리터럴이 우선이어야 한다.
        val route = matchRoute(
            segments = listOf("users", "me"),
            query = emptyMap(),
            literalPaths = setOf("/users/me"),
            templates = listOf(RoutePattern("/users/{id}")),
        )
        assertEquals(NavRoute("/users/me", emptyMap()), route)
    }

    @Test
    fun `template fallback - path param extracted into args, path is template`() {
        val route = matchRoute(
            segments = listOf("articleList", "articlePage", "123"),
            query = emptyMap(),
            literalPaths = emptySet(),
            templates = listOf(articleTemplate),
        )
        assertEquals(
            NavRoute("/articleList/articlePage/{articleId}", mapOf("articleId" to "123")),
            route,
        )
    }

    @Test
    fun `query and path params merged - path param wins on name collision`() {
        val route = matchRoute(
            segments = listOf("articleList", "articlePage", "123"),
            query = mapOf("articleId" to "999", "ref" to "push"),
            literalPaths = emptySet(),
            templates = listOf(articleTemplate),
        )
        assertEquals(
            NavRoute(
                "/articleList/articlePage/{articleId}",
                mapOf("articleId" to "123", "ref" to "push"), // path("123") overrides query("999")
            ),
            route,
        )
    }

    @Test
    fun `static literal carries query params through`() {
        val route = matchRoute(
            segments = listOf("search"),
            query = mapOf("q" to "kotlin"),
            literalPaths = setOf("/search"),
            templates = emptyList(),
        )
        assertEquals(NavRoute("/search", mapOf("q" to "kotlin")), route)
    }

    @Test
    fun `no match - returns null`() {
        val route = matchRoute(
            segments = listOf("unknown", "path"),
            query = emptyMap(),
            literalPaths = setOf("/search", "/favorite"),
            templates = listOf(articleTemplate),
        )
        assertNull(route)
    }
}
