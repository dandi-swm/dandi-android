package com.dandi.nyummy.main.domain.deeplink

import com.dandi.nyummy.common.domain.navigation.NavRoute

/**
 * URI 의 path segment + query 를 등록 라우트로 매칭하는 **순수 로직**(Android 비의존 → 단위 테스트 대상).
 *
 * 1. [literalPaths] **exact** 매칭 우선 (예: "/users/me").
 * 2. [templates] **순차** 매칭 (예: "/users/{id}") — path 구간 값을 추출.
 * 3. [aliases] 앱 라우트와 경로가 다른 외부 링크를 앱 라우트로 바꾼다(예: 서버 푸시의 "/meals/{mealId}" → "/history").
 *    앱 라우트를 모두 본 뒤에만 시도한다.
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
    aliases: List<LinkAlias> = emptyList(),
): NavRoute? {
    // 1) 정적 path exact 매칭.
    val literalPath = "/" + segments.joinToString("/")
    if (literalPath in literalPaths) return NavRoute(literalPath, query)

    // 2) 동적 템플릿 매칭. 리터럴 우선이므로 exact 매칭 실패 후에만 시도한다.
    for (pattern in templates) {
        val pathParams = pattern.match(segments) ?: continue
        return NavRoute(pattern.template, query + pathParams)
    }

    // 3) 외부 링크 별칭. 링크의 path 인자가 그대로 대상 라우트의 인자가 된다.
    for (alias in aliases) {
        val pathParams = alias.pattern.match(segments) ?: continue
        return NavRoute(alias.targetPath, query + pathParams)
    }
    return null
}

/**
 * 앱 라우트와 경로가 다른 외부 링크(서버 푸시 등)를 앱 라우트로 바꾸는 별칭.
 *
 * 링크의 path 인자는 이름 그대로 [targetPath] 라우트의 args 가 된다. 대상 화면이 그 이름으로 인자를 읽어야 한다.
 * 대상이 탭이어도 탭은 하나로 유지된 채 인자만 바뀐다(호스트의 탭 이동 규칙).
 *
 * @param linkTemplate 외부 링크 경로 템플릿 (예: "/meals/{mealId}")
 * @param targetPath 열 앱 라우트 path (예: "/history")
 */
class LinkAlias(linkTemplate: String, val targetPath: String) {
    val pattern: RoutePattern = RoutePattern(linkTemplate)
}
