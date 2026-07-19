package com.dandi.nyummy.main.domain.deeplink

import com.dandi.nyummy.common.domain.navigation.NavRoute

/**
 * URI 의 path segment + query 를 등록 라우트로 매칭하는 **순수 로직**(Android 비의존 → 단위 테스트 대상).
 *
 * 1. [literalPaths] **exact** 매칭 우선 (예: "/users/me").
 * 2. [templates] **순차** 매칭 (예: "/users/{id}") — path 구간 값을 추출.
 *
 * 동일 이름의 path 파라미터와 query 가 충돌하면 구조적인 path 파라미터를 우선한다(`query + pathParams`).
 * 호출부(main/presentation 의 `Uri.resolveRoute`)가 실제 레지스트리(appRouteByPath / appRoutePatterns)를 주입한다.
 *
 * @return 매칭된 [NavRoute](path 는 항상 등록된 "템플릿" path), 미매칭이면 null.
 */
fun matchRoute(
    segments: List<String>,
    query: Map<String, String>,
    literalPaths: Set<String>,
    templates: List<RoutePattern>,
): NavRoute? {
    // 1) 정적 path exact 매칭.
    val literalPath = "/" + segments.joinToString("/")
    if (literalPath in literalPaths) return NavRoute(literalPath, query)

    // 2) 동적 템플릿 매칭. 리터럴 우선이므로 exact 매칭 실패 후에만 시도한다.
    for (pattern in templates) {
        val pathParams = pattern.match(segments) ?: continue
        return NavRoute(pattern.template, query + pathParams)
    }
    return null
}
