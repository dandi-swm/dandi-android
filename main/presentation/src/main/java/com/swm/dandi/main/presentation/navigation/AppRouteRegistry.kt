package com.swm.dandi.main.presentation.navigation

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.swm.dandi.main.domain.deeplink.RoutePattern
import com.swm.dandi.sprite.domain.SpritePage as SpriteRoute
import com.swm.dandi.sprite.presentation.SpritePage
import com.swm.dandi.sprite.presentation.SpriteViewModel

/**
 * 앱의 모든 페이지 메타데이터 + 렌더러 모음.
 * 새 화면 추가 시 본 리스트에 한 줄을 더한다.
 */
val appRoutes: List<AppRoute> = listOf(
    // TODO: 실제 앱의 시작 화면이 생기면 sprite sample route는 제거하거나 debug/demo 전용으로 분리한다.
    AppRoute(
        path = SpriteRoute.PATH,
        render = { SpritePage(viewModel = hiltViewModel<SpriteViewModel>()) },
    ),
)

val appRouteByPath: Map<String, AppRoute> = appRoutes.associateBy { it.path }

val bottomTabRoutes: List<AppRoute> = appRoutes.filter { it.isBottomTab }

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
