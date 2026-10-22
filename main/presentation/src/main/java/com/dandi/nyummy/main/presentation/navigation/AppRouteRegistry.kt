package com.dandi.nyummy.main.presentation.navigation

import com.dandi.nyummy.auth.domain.EmailLoginPage
import com.dandi.nyummy.auth.domain.LoginPage
import com.dandi.nyummy.auth.domain.PasswordResetPage
import com.dandi.nyummy.auth.domain.SignUpPage
import com.dandi.nyummy.auth.domain.SocialSignUpPage
import com.dandi.nyummy.auth.presentation.EmailLoginPage
import com.dandi.nyummy.auth.presentation.LoginPage
import com.dandi.nyummy.auth.presentation.PasswordResetPage
import com.dandi.nyummy.auth.presentation.SignUpPage
import com.dandi.nyummy.common.presentation.helper.LocalNavigationHelper
import com.dandi.nyummy.common.domain.navigation.LinkArgs
import com.dandi.nyummy.history.domain.HistoryPage
import com.dandi.nyummy.history.presentation.HistoryPage
import com.dandi.nyummy.home.domain.HomePage
import com.dandi.nyummy.home.presentation.HomePage
import com.dandi.nyummy.intro.domain.IntroPage
import com.dandi.nyummy.intro.presentation.IntroPage
import com.dandi.nyummy.legal.domain.LegalPage as LegalRoute
import com.dandi.nyummy.legal.entity.LegalDocument
import com.dandi.nyummy.legal.presentation.LegalPage
import com.dandi.nyummy.mailbox.domain.InquiryDetailPage as InquiryDetailRoute
import com.dandi.nyummy.mailbox.domain.InquiryListPage as InquiryListRoute
import com.dandi.nyummy.mailbox.domain.InquiryWritePage
import com.dandi.nyummy.mailbox.domain.MailboxPage
import com.dandi.nyummy.mailbox.presentation.detail.InquiryDetailPage
import com.dandi.nyummy.mailbox.presentation.inquiry.InquiryListPage
import com.dandi.nyummy.mailbox.presentation.list.MailboxPage
import com.dandi.nyummy.mailbox.presentation.write.InquiryWritePage
import com.dandi.nyummy.main.domain.deeplink.LinkAlias
import com.dandi.nyummy.main.domain.deeplink.RoutePattern
import com.dandi.nyummy.meal.domain.MealRecordPage
import com.dandi.nyummy.meal.presentation.MealRecordPage
import com.dandi.nyummy.onboarding.domain.OnboardingPage
import com.dandi.nyummy.onboarding.presentation.OnboardingPage
import com.dandi.nyummy.room.domain.MyRoomPage
import com.dandi.nyummy.room.presentation.MyRoomPage
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
        path = PasswordResetPage.PATH,
        isBottomTab = false,
        drawsBehindSystemBars = true,
        syntheticStack = { args ->
            listOf(
                GenericNavKey(LoginPage.PATH),
                GenericNavKey(EmailLoginPage.PATH),
                GenericNavKey(PasswordResetPage.PATH, args),
            )
        },
        render = { PasswordResetPage() },
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
        // 방이 상태 표시줄과 하단 내비 뒤까지 화면 전체를 채운다.
        drawsBehindSystemBars = true,
        drawsBehindBottomBar = true,
        render = {
            HomePage()
        },
    ),
    AppRoute(
        path = MealRecordPage.PATH,
        isBottomTab = false,
        // 식사 기록 연출은 홈처럼 방이 시스템 바 뒤까지 화면 전체를 채운다. 촬영과 확인 단계는 화면이 시스템 바를 피한다.
        drawsBehindSystemBars = true,
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
        // 상점 탭. 기능이 꺼져 있으면 하단 내비에 탭이 없고, 링크로 들어오면 내비 없이 이 화면만 보인다.
        path = ShopPage.PATH,
        isBottomTab = true,
        render = { ShopPage() },
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
        // 설정의 문의하기에서 들어오는 내가 보낸 문의 목록.
        path = InquiryListRoute.PATH,
        syntheticStack = { args ->
            listOf(
                GenericNavKey(HomePage.PATH),
                GenericNavKey(SettingsPage.PATH),
                GenericNavKey(InquiryListRoute.PATH, args),
            )
        },
        render = { InquiryListPage() },
    ),
    AppRoute(
        // 경로는 서버 푸시 링크와 맞춰 둔 우편함 아래 그대로이고, 뒤로 가면 설정의 문의 목록으로 간다.
        path = InquiryWritePage.PATH,
        syntheticStack = { args ->
            listOf(
                GenericNavKey(HomePage.PATH),
                GenericNavKey(SettingsPage.PATH),
                GenericNavKey(InquiryListRoute.PATH),
                GenericNavKey(InquiryWritePage.PATH, args),
            )
        },
        render = { InquiryWritePage() },
    ),
    AppRoute(
        // 답장 알림 링크로 바로 들어와도 뒤로 가면 설정의 문의 목록으로 간다.
        path = InquiryDetailRoute.PATH,
        syntheticStack = { args ->
            listOf(
                GenericNavKey(HomePage.PATH),
                GenericNavKey(SettingsPage.PATH),
                GenericNavKey(InquiryListRoute.PATH),
                GenericNavKey(InquiryDetailRoute.PATH, args),
            )
        },
        render = { args ->
            InquiryDetailPage(inquiryId = args[InquiryDetailRoute.ARG_INQUIRY_ID]?.toLongOrNull() ?: 0L)
        },
    ),
    AppRoute(
        path = MyRoomPage.PATH,
        // 방이 상태 표시줄 뒤까지 화면 전체를 채운다(홈과 같다).
        drawsBehindSystemBars = true,
        syntheticStack = { args ->
            listOf(
                GenericNavKey(HomePage.PATH),
                GenericNavKey(MyRoomPage.PATH, args),
            )
        },
        render = { MyRoomPage() },
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
        // 로그인 화면과 설정에서 연다. 로그인하지 않아도 열린다.
        path = LegalRoute.TERMS_PATH,
        render = { LegalPage(document = LegalDocument.TERMS) },
    ),
    AppRoute(
        path = LegalRoute.PRIVACY_PATH,
        render = { LegalPage(document = LegalDocument.PRIVACY) },
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
    // 문의 답장 푸시의 문의 링크 → 그 문의 상세(뒤로 가면 설정의 문의 목록)
    LinkAlias("/inquiries/{${InquiryDetailRoute.ARG_INQUIRY_ID}}", targetPath = InquiryDetailRoute.PATH),
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
