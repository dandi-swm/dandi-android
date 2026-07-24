package com.dandi.nyummy.main.presentation.navigation

import com.dandi.nyummy.main.domain.deeplink.RoutePattern
import com.dandi.nyummy.history.domain.HistoryPage as HistoryNavigationPage
import com.dandi.nyummy.history.presentation.HistoryPage as HistoryScreen
import com.dandi.nyummy.home.domain.HomePage as HomeNavigationPage
import com.dandi.nyummy.home.presentation.HomePage as HomeScreen

/**
 * 앱의 모든 페이지 메타데이터 + 렌더러 모음.
 * 새 화면 추가 시 본 리스트에 한 줄을 더한다.
 */
val appRoutes: List<AppRoute> = listOf(
    AppRoute(
        path = HomeNavigationPage.PATH,
        isBottomTab = true,
        render = { HomeScreen() },
    ),
    AppRoute(
        path = HistoryNavigationPage.PATH,
        isBottomTab = true,
        render = { HistoryScreen() },
    ),
)

val appRouteByPath: Map<String, AppRoute> = appRoutes.associateBy { it.path }

/**
 * 동적 구간(`{param}`)을 가진 계층형 라우트의 (패턴, 라우트) 목록.
 *
 * 정적 path 는 [appRouteByPath] 가 O(1) 로 처리하므로, 여기에는 다중 세그먼트 템플릿
 * (예: "/articleList/articlePage/{articleId}")만 보관한다. deep-link URI 해석 시
 * exact 매칭이 실패한 경우에만 이 목록을 순차 매칭한다.
 */
val appRoutePatterns: List<Pair<RoutePattern, AppRoute>> = appRoutes
    .map { route -> RoutePattern(route.path) to route }
    .filter { (pattern, _) -> pattern.hasParams }
