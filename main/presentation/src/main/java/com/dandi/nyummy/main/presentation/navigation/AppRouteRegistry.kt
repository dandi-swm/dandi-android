package com.dandi.nyummy.main.presentation.navigation

import com.dandi.nyummy.achievement.domain.AchievementPage
import com.dandi.nyummy.achievement.presentation.AchievementPage
import com.dandi.nyummy.auth.domain.EmailLoginPage
import com.dandi.nyummy.auth.domain.LoginPage
import com.dandi.nyummy.auth.domain.SignUpPage
import com.dandi.nyummy.auth.domain.SocialSignUpPage
import com.dandi.nyummy.auth.presentation.EmailLoginPage
import com.dandi.nyummy.auth.presentation.LoginPage
import com.dandi.nyummy.auth.presentation.SignUpPage
import com.dandi.nyummy.collection.domain.CollectionPage
import com.dandi.nyummy.collection.presentation.CollectionPage
import com.dandi.nyummy.common.presentation.helper.LocalNavigationHelper
import com.dandi.nyummy.common.domain.navigation.LinkArgs
import com.dandi.nyummy.history.domain.HistoryPage
import com.dandi.nyummy.history.presentation.HistoryPage
import com.dandi.nyummy.home.domain.HomePage
import com.dandi.nyummy.home.presentation.HomePage
import com.dandi.nyummy.intro.domain.IntroPage
import com.dandi.nyummy.intro.presentation.IntroPage
import com.dandi.nyummy.mailbox.domain.InquiryDetailPage as InquiryDetailRoute
import com.dandi.nyummy.mailbox.domain.InquiryWritePage
import com.dandi.nyummy.mailbox.domain.MailboxPage
import com.dandi.nyummy.mailbox.presentation.detail.InquiryDetailPage
import com.dandi.nyummy.mailbox.presentation.list.MailboxPage
import com.dandi.nyummy.mailbox.presentation.write.InquiryWritePage
import com.dandi.nyummy.main.domain.deeplink.LinkAlias
import com.dandi.nyummy.main.domain.deeplink.RoutePattern
import com.dandi.nyummy.meal.domain.MealRecordPage
import com.dandi.nyummy.meal.presentation.MealRecordPage
import com.dandi.nyummy.onboarding.domain.OnboardingPage
import com.dandi.nyummy.onboarding.presentation.OnboardingPage
import com.dandi.nyummy.quest.domain.QuestPage
import com.dandi.nyummy.quest.presentation.QuestPage
import com.dandi.nyummy.settings.domain.MealTimeSettingPage
import com.dandi.nyummy.settings.domain.SettingsPage
import com.dandi.nyummy.settings.presentation.main.SettingsPage
import com.dandi.nyummy.settings.presentation.mealtime.MealTimeSettingPage
import com.dandi.nyummy.shop.domain.ShopPage
import com.dandi.nyummy.shop.presentation.ShopPage

/**
 * 앱의 모든 페이지 메타데이터 + 렌더러 모음.
 * 새 화면 추가 시 본 리스트에 한 줄을 더한다.
 */
val appRoutes: List<AppRoute> = listOf(
    AppRoute(
        path = IntroPage.PATH,
        isBottomTab = false,
        drawsBehindSystemBars = true,
        render = { IntroPage() },
    ),
    AppRoute(
        path = OnboardingPage.PATH,
        isBottomTab = false,
        drawsBehindSystemBars = true,
        usesLightSystemBarIcons = true,
        render = { OnboardingPage() },
    ),
    AppRoute(
        path = LoginPage.PATH,
        isBottomTab = false,
        drawsBehindSystemBars = true,
        render = { LoginPage() },
    ),
    AppRoute(
        path = EmailLoginPage.PATH,
        isBottomTab = false,
        drawsBehindSystemBars = true,
        syntheticStack = { args ->
            listOf(
                GenericNavKey(LoginPage.PATH),
                GenericNavKey(EmailLoginPage.PATH, args),
            )
        },
        render = { EmailLoginPage() },
    ),
    AppRoute(
        path = SignUpPage.PATH,
        isBottomTab = false,
        drawsBehindSystemBars = true,
        syntheticStack = { args ->
            listOf(
                GenericNavKey(LoginPage.PATH),
                GenericNavKey(EmailLoginPage.PATH),
                GenericNavKey(SignUpPage.PATH, args),
            )
        },
        render = { SignUpPage() },
    ),
    AppRoute(
        // 소셜 로그인 신규 회원의 프로필 입력. 가입 토큰은 메모리 세션으로만 전달되므로
        // 외부 링크로 직접 열면 안내 후 로그인 화면으로 돌아간다.
        path = SocialSignUpPage.PATH,
        isBottomTab = false,
        drawsBehindSystemBars = true,
        syntheticStack = { args ->
            listOf(
                GenericNavKey(LoginPage.PATH),
                GenericNavKey(SocialSignUpPage.PATH, args),
            )
        },
        render = { SignUpPage(isSocialSignUp = true) },
    ),
    AppRoute(
        path = HomePage.PATH,
        isBottomTab = true,
        render = {
            HomePage()
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
        render = { args ->
            HistoryPage(
                openMealId = args[HistoryPage.ARG_MEAL_ID]?.toLongOrNull(),
                linkEventId = args[LinkArgs.NOTIFICATION_ID],
            )
        },
    ),
    AppRoute(
        path = MailboxPage.PATH,
        syntheticStack = { args ->
            listOf(
                GenericNavKey(HomePage.PATH),
                GenericNavKey(MailboxPage.PATH, args),
            )
        },
        render = { MailboxPage() },
    ),
    AppRoute(
        path = InquiryWritePage.PATH,
        syntheticStack = { args ->
            listOf(
                GenericNavKey(HomePage.PATH),
                GenericNavKey(MailboxPage.PATH),
                GenericNavKey(InquiryWritePage.PATH, args),
            )
        },
        render = { InquiryWritePage() },
    ),
    AppRoute(
        path = InquiryDetailRoute.PATH,
        syntheticStack = { args ->
            listOf(
                GenericNavKey(HomePage.PATH),
                GenericNavKey(MailboxPage.PATH),
                GenericNavKey(InquiryDetailRoute.PATH, args),
            )
        },
        render = { args ->
            InquiryDetailPage(inquiryId = args[InquiryDetailRoute.ARG_INQUIRY_ID]?.toLongOrNull() ?: 0L)
        },
    ),
    AppRoute(
        path = SettingsPage.PATH,
        syntheticStack = { args ->
            listOf(
                GenericNavKey(HomePage.PATH),
                GenericNavKey(SettingsPage.PATH, args),
            )
        },
        render = { SettingsPage() },
    ),
    AppRoute(
        path = MealTimeSettingPage.PATH,
        syntheticStack = { args ->
            listOf(
                GenericNavKey(HomePage.PATH),
                GenericNavKey(SettingsPage.PATH),
                GenericNavKey(MealTimeSettingPage.PATH, args),
            )
        },
        render = { MealTimeSettingPage() },
    ),
    AppRoute(
        // 하단 탭에서 빠져 링크로만 열린다. 기능을 열 때 진입 위치를 다시 정한다.
        path = CollectionPage.PATH,
        render = { CollectionPage() },
    ),
    AppRoute(
        path = QuestPage.PATH,
        isBottomTab = true,
        render = { QuestPage() },
    ),
    AppRoute(
        path = AchievementPage.PATH,
        isBottomTab = true,
        render = { AchievementPage() },
    ),
    AppRoute(
        path = ShopPage.PATH,
        isBottomTab = true,
        render = { ShopPage() },
    ),
)

val appRouteByPath: Map<String, AppRoute> = appRoutes.associateBy { it.path }

/**
 * 앱 라우트와 경로가 다른 외부 링크(서버 푸시 등)를 앱 라우트로 바꾸는 별칭 목록.
 *
 * 외부에서 주는 딥링크 경로가 실제로 타켓팅 되어야하는 우리 앱에서의 관심사로 설계된 링크와 다르다면, 이 별칭을 등록한다.
 */
val appLinkAliases: List<LinkAlias> = listOf(
    // 분석 결과 푸시의 식사 상세 링크(서버 app-link meal-detail-url) → 기록 탭에서 그 식사 상세 열기
    LinkAlias("/meals/{${HistoryPage.ARG_MEAL_ID}}", targetPath = HistoryPage.PATH),
)

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
