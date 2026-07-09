package com.swm.dandi.main.presentation.navigation

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.swm.dandi.meal.domain.NewFoodPage as NewFoodRoute
import com.swm.dandi.meal.domain.PreviousMealPage as PreviousMealRoute
import com.swm.dandi.meal.presentation.NewFoodPage as NewFoodScreen
import com.swm.dandi.meal.presentation.PreviousMealPage as PreviousMealScreen
import com.swm.dandi.main.domain.deeplink.RoutePattern

/**
 * 앱의 모든 페이지 메타데이터 + 렌더러 모음.
 * 새 화면 추가 시 본 리스트에 한 줄을 더한다.
 */
val appRoutes: List<AppRoute> = listOf(
    AppRoute(
        path = PreviousMealRoute.PATH,
        render = {
            PreviousMealScreen(viewModel = hiltViewModel())
        },
    ),
    AppRoute(
        path = NewFoodRoute.PATH,
        render = {
            NewFoodScreen(viewModel = hiltViewModel())
        },
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
