package com.swm.dandi.main.domain.deeplink

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * [RoutePattern] 의 다중 세그먼트 / `{param}` 매칭 로직 검증.
 * 프로덕션에서 동적 라우트가 아직 등록되지 않아(appRoutePatterns 공집합) 이 primitive 의
 * 회귀 안전망 역할을 한다.
 */
class RoutePatternTest {

    @Test
    fun `literal template - exact segments match - returns empty params`() {
        val pattern = RoutePattern("/articleList")
        assertEquals(emptyMap<String, String>(), pattern.match(listOf("articleList")))
    }

    @Test
    fun `literal template - different value - returns null`() {
        val pattern = RoutePattern("/articleList")
        assertNull(pattern.match(listOf("favorite")))
    }

    @Test
    fun `single param - extracts value`() {
        val pattern = RoutePattern("/articleList/articlePage/{articleId}")
        assertEquals(
            mapOf("articleId" to "123"),
            pattern.match(listOf("articleList", "articlePage", "123")),
        )
    }

    @Test
    fun `multiple params - extracts all by name`() {
        val pattern = RoutePattern("/board/{boardId}/post/{postId}")
        assertEquals(
            mapOf("boardId" to "free", "postId" to "42"),
            pattern.match(listOf("board", "free", "post", "42")),
        )
    }

    @Test
    fun `segment count mismatch - too few - returns null`() {
        val pattern = RoutePattern("/articleList/articlePage/{articleId}")
        assertNull(pattern.match(listOf("articleList", "articlePage")))
    }

    @Test
    fun `segment count mismatch - too many - returns null`() {
        val pattern = RoutePattern("/articleList/articlePage/{articleId}")
        assertNull(pattern.match(listOf("articleList", "articlePage", "123", "extra")))
    }

    @Test
    fun `literal prefix mismatch with param - returns null`() {
        val pattern = RoutePattern("/articleList/articlePage/{articleId}")
        assertNull(pattern.match(listOf("articleList", "WRONG", "123")))
    }

    @Test
    fun `hasParams - true only when a brace segment exists`() {
        assertEquals(false, RoutePattern("/articleList").hasParams)
        assertEquals(true, RoutePattern("/articleList/articlePage/{articleId}").hasParams)
    }
}
