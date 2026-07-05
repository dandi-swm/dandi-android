package com.swm.dandi.main.presentation.deeplink

import android.net.Uri
import android.util.Log
import androidx.navigation3.runtime.NavKey
import com.swm.dandi.common.domain.navigation.NavRoute
import com.swm.dandi.main.domain.deeplink.matchRoute
import com.swm.dandi.main.presentation.navigation.GenericNavKey
import com.swm.dandi.main.presentation.navigation.appRouteByPath
import com.swm.dandi.main.presentation.navigation.appRoutePatterns

private const val TAG = "[DeepLink]"

/**
 * App Link [Uri] 를 등록된 라우트로 해석한다.
 *
 * 1. 정적 path **exact** 매칭 (예: "/home") — 항상 우선.
 * 2. 동적 **템플릿** 매칭 (예: "/articleList/articlePage/{articleId}") — path 구간 값을 args 로 추출.
 *
 * 해석된 [NavRoute.path] 는 항상 등록된 "템플릿" path 다(= 백스택 키 [GenericNavKey] 의 식별자이자
 * in-app path). path 파라미터와 query parameter 가 모두 [NavRoute.args] 에 담기며, 이름이 충돌하면
 * 구조적인 path 파라미터를 우선한다. 어떤 라우트와도 매칭되지 않으면 null.
 */
fun Uri.resolveRoute(): NavRoute? {
    val segments = pathSegments?.filter { it.isNotEmpty() }?.takeIf { it.isNotEmpty() }
        ?: return null
    val query = queryParameterNames
        .filter { it.isNotEmpty() }
        .associateWith { (getQueryParameter(it) ?: "") }
    // 순수 매칭 로직은 main/domain 에 있다. 여기서 실제 레지스트리를 주입한다.
    return matchRoute(
        segments = segments,
        query = query,
        literalPaths = appRouteByPath.keys,
        templates = appRoutePatterns.map { it.first },
    )
}

/**
 * App Link 콜드 스타트의 시작 백스택을 구성한다.
 * - URI 없음 / 미매칭 → 빈 시작 스택으로 fallback.
 * - 매칭 → 해당 [com.swm.dandi.main.presentation.navigation.AppRoute.syntheticStack]
 *   (부모 체인을 포함한 정식 스택). 계층형 path 도 동일하게 동작한다.
 *
 * 예) "/articleList/articlePage/{articleId}" 가 syntheticStack 으로
 *     `[Home, ArticleList, ArticlePage(args)]` 를 선언했다면, 그 전체 스택이 깔린 채 진입한다.
 */
fun resolveStartStack(uri: Uri?): List<NavKey> {
    val route = uri?.resolveRoute()
    if (route == null) {
        if (uri != null) Log.w(TAG, "No matching route for uri=$uri")
        return emptyList()
    }
    val appRoute = appRouteByPath[route.path]
        ?: return emptyList()
    return appRoute.syntheticStack(route.args)
}

/**
 * 앱 실행 중(웜 스타트) 도착한 deep-link 를 [NavRoute] 로 해석한다.
 *
 * 콜드 스타트와 달리 synthetic 부모 체인을 만들지 않는다 — 실제 dispatch 는 호스트의
 * bring-to-front 정책([com.swm.dandi.main.presentation.navigation.handleDeepLink])이
 * 담당하여 사용자의 현재 스택을 보존한다. 미매칭이면 null(호출부가 무시).
 */
fun resolveNewIntentRoute(uri: Uri): NavRoute? {
    val route = uri.resolveRoute()
    if (route == null) {
        Log.w(TAG, "onNewIntent: unhandled uri=$uri")
        return null
    }
    return route
}
