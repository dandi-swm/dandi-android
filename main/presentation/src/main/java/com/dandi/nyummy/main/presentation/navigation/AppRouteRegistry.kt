package com.dandi.nyummy.main.presentation.navigation

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.dandi.nyummy.auth.domain.EmailLoginPage
import com.dandi.nyummy.auth.domain.LoginPage
import com.dandi.nyummy.auth.presentation.EmailLoginPage
import com.dandi.nyummy.auth.presentation.LoginPage
import com.dandi.nyummy.auth.presentation.LoginViewModel
import com.dandi.nyummy.common.presentation.helper.LocalNavigationHelper
import com.dandi.nyummy.history.domain.HistoryPage
import com.dandi.nyummy.history.presentation.HistoryPage
import com.dandi.nyummy.home.domain.HomePage
import com.dandi.nyummy.home.presentation.HomePage
import com.dandi.nyummy.main.domain.deeplink.RoutePattern
import com.dandi.nyummy.meal.domain.MealRecordPage
import com.dandi.nyummy.meal.presentation.MealRecordPage

/**
 * 앱의 모든 페이지 메타데이터 + 렌더러 모음.
 * 새 화면 추가 시 본 리스트에 한 줄을 더한다.
 */
val appRoutes: List<AppRoute> = listOf(
    AppRoute(
        path = LoginPage.PATH,
        isBottomTab = false,
        render = {
            LoginPage(viewModel = hiltViewModel<LoginViewModel>())
        }
    ),
    AppRoute(
        path = EmailLoginPage.PATH,
        isBottomTab = false,
        syntheticStack = { args ->
            listOf(
                GenericNavKey(LoginPage.PATH),
                GenericNavKey(EmailLoginPage.PATH, args),
            )
        },
        render = { EmailLoginPage() },
    ),
    AppRoute(
        path = HomePage.PATH,
        isBottomTab = true,
        render = {
            val navigationHelper = LocalNavigationHelper.current
            HomePage(
                onFeedClick = { navigationHelper.navigateTo(MealRecordPage) },
            )
        },
    ),
    AppRoute(
        path = MealRecordPage.PATH,
        isBottomTab = false,
        syntheticStack = { args ->
            listOf(
                GenericNavKey(HomePage.PATH),
                GenericNavKey(MealRecordPage.PATH, args),
            )
        },
        render = { MealRecordPage() },
    ),
    AppRoute(
        path = HistoryPage.PATH,
        isBottomTab = true,
        render = { HistoryPage() },
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
val appRoutePatterns: List<Pair<RoutePattern, AppRoute>> =
    appRoutes.map { route -> RoutePattern(route.path) to route }
        .filter { (pattern, _) -> pattern.hasParams }
