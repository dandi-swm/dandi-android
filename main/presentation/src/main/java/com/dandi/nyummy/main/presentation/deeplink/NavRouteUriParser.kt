package com.dandi.nyummy.main.presentation.deeplink

import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import androidx.navigation3.runtime.NavKey
import com.dandi.nyummy.common.domain.navigation.AppLinks
import com.dandi.nyummy.common.domain.navigation.NavRoute
import com.dandi.nyummy.intro.domain.IntroPage
import com.dandi.nyummy.main.domain.deeplink.matchRoute
import com.dandi.nyummy.main.presentation.navigation.GenericNavKey
import com.dandi.nyummy.main.presentation.navigation.appLinkAliases
import com.dandi.nyummy.main.presentation.navigation.appRouteByPath
import com.dandi.nyummy.main.presentation.navigation.appRoutePatterns

private const val TAG = "[AppLink]"

/**
 * App Link [Uri] 를 등록된 라우트로 해석한다.
 *
 * 1. 정적 path **exact** 매칭 (예: "/home") — 항상 우선.
 * 2. 동적 **템플릿** 매칭 (예: "/articleList/articlePage/{articleId}") — path 구간 값을 args 로 추출.
 * 3. 외부 링크 **별칭**([appLinkAliases], 예: 서버 푸시의 "/meals/{mealId}" → 식사 상세) — path 인자가 대상 라우트의 args 가 된다.
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
        aliases = appLinkAliases,
    )
}

/**
 * App Link 콜드 스타트의 시작 백스택을 구성한다.
 * - URI 없음 / 미매칭 → 인트로 단일 스택으로 fallback (인트로가 홈/로그인 분기를 담당).
 * - 매칭 → 해당 [com.dandi.nyummy.main.presentation.navigation.AppRoute.syntheticStack]
 *   (부모 체인을 포함한 정식 스택). 계층형 path 도 동일하게 동작한다.
 *
 * 예) "/articleList/articlePage/{articleId}" 가 syntheticStack 으로
 *     `[Home, ArticleList, ArticlePage(args)]` 를 선언했다면, 그 전체 스택이 깔린 채 진입한다.
 */
fun resolveStartStack(uri: Uri?, notificationId: String? = null): List<NavKey> {
    val route = uri?.resolveRoute()?.withNotificationId(notificationId)
    if (route == null) {
        if (uri != null) Log.w(TAG, "No matching route for uri=$uri")
        return listOf(GenericNavKey(IntroPage.PATH))
    }
    val appRoute = appRouteByPath[route.path]
        ?: return listOf(GenericNavKey(IntroPage.PATH))
    return appRoute.syntheticStack(route.args)
}

/**
 * 앱 실행 중 여는 앱 링크([com.dandi.nyummy.common.domain.navigation.NavSignal.AppLink])를 [NavRoute] 로 해석한다.
 *
 * 푸시·외부 앱 링크(웜 스타트)와 앱 안 링크(서버 SDUI 액션, 홈 공지 CTA)가 모두 여기를 지나며, 콜드 스타트와
 * 같은 [resolveRoute] 로 푼다. 앱 링크 호스트([AppLinks.HOST])의 http(s) 링크만 받고, 아니거나 미매칭이면 null.
 */
fun appLinkToRoute(link: String): NavRoute? {
    val uri = link.trim().toUri()
    val isAppLink = (uri.scheme.equals("https", ignoreCase = true) || uri.scheme.equals("http", ignoreCase = true)) &&
        uri.host.equals(AppLinks.HOST, ignoreCase = true)
    if (!isAppLink) {
        Log.w(TAG, "Not an app link: $link")
        return null
    }
    return uri.resolveRoute() ?: run {
        Log.w(TAG, "No matching route for app link: $link")
        null
    }
}
