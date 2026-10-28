# 네비게이션 (Navigation3)

## 왜

- **앱 내 이동과 딥링크가 같은 path를 공유**한다 — `/history` 는 탭 이동에서도, `https://link.nyummy.co.kr/history` 딥링크에서도 동일하게 동작. 화면 추가 시 딥링크가 공짜로 따라온다.
- domain 레이어는 `NavigationHelper` 인터페이스만 알고 Navigation3를 모른다 — UseCase가 화면 이동을 트리거해도 플랫폼 의존이 생기지 않는다.

실제 등록 라우트 목록은 `AppRouteRegistry.kt`가 기준이다.

## 계약 (common/domain — 순수 JVM)

골든 예제: [NavRoute.kt](../../common/domain/src/main/java/com/dandi/nyummy/common/domain/navigation/NavRoute.kt), [NavSignal.kt](../../common/domain/src/main/java/com/dandi/nyummy/common/domain/navigation/NavSignal.kt), [NavigationHelper.kt](../../common/domain/src/main/java/com/dandi/nyummy/common/domain/helper/NavigationHelper.kt)

```kotlin
data class NavRoute(val path: String, val args: Map<String, String> = emptyMap())
interface Page { fun toRoute(): NavRoute }            // feature/domain이 구현
sealed interface NavSignal {
    GoToDestPage(route)      // 앱 내 전진 이동(push)
    DeepLink(route)          // 웜 딥링크 — bring-to-front
    Back                     // 한 단계 뒤로
    ResetToPage(route)       // 스택을 비우고 대상만 루트로 (로그인/가입 완료 → 홈)
    BackToInitialPage        // 세션 만료(401) — 초기 화면 단독으로 교체, 목적지는 NavHost가 정한다
    GoToExternalLink(url)    // 외부 앱(브라우저/스토어) 열기, 백스택은 그대로
}
interface NavigationHelper {
    val navigationFlow: Flow<NavSignal>
    fun navigateTo(page: Page)              // 권장 진입점 (앱 내 push)
    fun navigateByRoute(route: NavRoute)    // NavRoute 직접 구성해서 push
    fun navigateDeepLink(route: NavRoute)   // 웜 딥링크 — bring-to-front (탭 대상은 탭 루트 시맨틱)
    fun navigateToBack()
    fun navigateToAsRoot(page: Page)        // → ResetToPage
    fun navigateToInitial()                 // → BackToInitialPage
    fun navigateToExternalLink(url: String) // → GoToExternalLink
}
```

- 외부 링크처럼 화면 이동 성격의 사이드이펙트도 별도 Helper를 만들지 않고 이 단일 채널(`NavSignal`)로 보낸다.
- args는 전부 String — 숫자 id 등은 `toString()`으로 싣고 render에서 파싱한다(아래 `InquiryDetailPage` 예). 복잡한 객체는 넘기지 않고 id만 넘겨 화면에서 다시 읽는다.
- Page 객체는 **feature/domain** 모듈에 둔다. 인자 없는 화면은 `object HistoryPage : Page { const val PATH = "/history" }`, 인자가 있는 화면은 data class로 인자를 받아 `toRoute()`에서 args에 싣는다 — [InquiryDetailPage](../../settings/domain/src/main/java/com/dandi/nyummy/settings/domain/SettingsPages.kt):

```kotlin
data class InquiryDetailPage(val inquiryId: Long) : Page {
    override fun toRoute() = NavRoute(PATH, mapOf(ARG_INQUIRY_ID to inquiryId.toString()))
    companion object {
        const val ARG_INQUIRY_ID = "inquiryId"
        const val PATH = "/mailbox/inquiry/{$ARG_INQUIRY_ID}"
    }
}
```

## 호스트 (main/presentation)

| 파일 | 역할 |
|---|---|
| [AppRoute.kt](../../main/presentation/src/main/java/com/dandi/nyummy/main/presentation/navigation/AppRoute.kt) | path / isBottomTab / drawsBehindSystemBars / drawsBehindBottomBar / usesLightSystemBarIcons / syntheticStack / render(Composable) |
| [AppRouteRegistry.kt](../../main/presentation/src/main/java/com/dandi/nyummy/main/presentation/navigation/AppRouteRegistry.kt) | **모든 라우트의 단일 등록처** — 새 화면은 여기에 항목 추가. 외부 링크 별칭 `appLinkAliases`도 여기 |
| [AppNavHost.kt](../../main/presentation/src/main/java/com/dandi/nyummy/main/presentation/navigation/AppNavHost.kt) | `navigationFlow` collect → 신호별 처리(`handleNavRoute` / `handleDeepLink` / `handleResetTo` / `navigateToInitialStack` / `openExternalLink`), `NavDisplay` + `entry<GenericNavKey>` 렌더 |
| [GenericNavKey.kt](../../main/presentation/src/main/java/com/dandi/nyummy/main/presentation/navigation/GenericNavKey.kt) | `@Serializable (path, args)` — 백스택 엔트리 단일 타입 |
| [RootComposable.kt](../../main/presentation/src/main/java/com/dandi/nyummy/main/presentation/navigation/RootComposable.kt) | Scaffold + 하단 내비(`NyummyBottomNav`) + SnackbarHost/ToastHost |

`AppRoute`의 화면 표시 플래그:

- `drawsBehindSystemBars` — 화면이 상태 표시줄·시스템 내비 뒤까지 그리고 인셋을 스스로 처리한다(인트로, 온보딩, 로그인·가입 계열, 홈, 마이룸, 식사 기록).
- `drawsBehindBottomBar` — 하단 내비 뒤까지 그린다(홈의 방).
- `usesLightSystemBarIcons` — 어두운 화면 위에서 시스템 바 아이콘을 밝게(온보딩).

### 하단 내비

- 탭은 홈·마이룸·기록·상점 순서(`mainTabPages`)이고, 가운데(마이룸과 기록 사이, `MealRecordActionIndex = 2`)에 식사 기록 버튼이 솟아 있다.
- `isBottomTab = true`인 라우트(홈·기록·상점)가 탭 루트다. **마이룸은 탭에 있지만 탭 루트가 아니다** — 하단 내비 없는 마이룸 화면으로 가는 지름길이라 뒤로 가면 보던 탭으로 돌아온다.
- 마이룸·상점 탭은 `MainTabFlags`(Remote Config)가 꺼져 있으면 이동하지 않고 "준비 중" 토스트를 띄운다(`isMainTabReady`).

새 화면 등록 예 (args 없는 기본형 — Page Composable이 `viewModel = hiltViewModel()`을 기본 파라미터로 가진다):

```kotlin
AppRoute(
    path = HistoryPage.PATH,
    isBottomTab = true,
    render = { HistoryPage() },
),
```

## Synthetic backstack (콜드 스타트 단일 출처)

딥링크/탭으로 "중간 화면"에 바로 진입해도 백 키가 자연스럽도록, 라우트가 자기 진입 시 스택 전체를 합성한다. **콜드 스타트 백스택은 오직 `syntheticStack` 한 곳에서만 정의된다** — 웜 스타트(아래)는 이 스택을 재정의하지 않고 별도 정책으로 동작한다.

```kotlin
// 회원가입: 링크로 바로 들어와도 뒤로 가면 이메일 로그인 → 로그인 순서로 돌아간다.
AppRoute(
    path = SignUpPage.PATH,
    syntheticStack = { args -> listOf(
        GenericNavKey(LoginPage.PATH),
        GenericNavKey(EmailLoginPage.PATH),
        GenericNavKey(SignUpPage.PATH, args),   // 자기 자신
    ) },
    render = { SignUpPage() },
),
```

계층형 동적 path도 동일하게 표현한다 — 문의 답장 알림으로 문의 상세에 바로 들어와도 뒤로 가면 설정의 문의 목록으로 간다:

```kotlin
AppRoute(
    path = InquiryDetailPage.PATH,   // "/mailbox/inquiry/{inquiryId}"
    syntheticStack = { args -> listOf(
        GenericNavKey(HomePage.PATH),
        GenericNavKey(SettingsPage.PATH),
        GenericNavKey(InquiryListPage.PATH),
        GenericNavKey(InquiryDetailPage.PATH, args),   // 자기 자신(inquiryId 포함)
    ) },
    render = { args -> InquiryDetailPage(inquiryId = args[InquiryDetailPage.ARG_INQUIRY_ID]?.toLongOrNull() ?: 0L) },
),
```

## 딥링크 (App Links)

- 매니페스트: `https://link.nyummy.co.kr` autoVerify intent-filter ([AndroidManifest.xml](../../app/src/main/AndroidManifest.xml))
- [NavRouteUriParser.kt](../../main/presentation/src/main/java/com/dandi/nyummy/main/presentation/deeplink/NavRouteUriParser.kt) `Uri.resolveRoute()`: URI를 등록 라우트로 해석한다.
  1. **정적 path exact 매칭** (`/home`) — 항상 우선.
  2. **동적 템플릿 매칭** ([RoutePattern.kt](../../main/domain/src/main/java/com/dandi/nyummy/main/domain/deeplink/RoutePattern.kt)) — `{param}` 구간을 가진 다중 세그먼트 path(`/mailbox/inquiry/{inquiryId}`)를 매칭하고 path 구간 값을 args로 추출. path 파라미터는 query 와 충돌 시 우선. 템플릿 목록 `appRoutePatterns`는 `appRoutes` 중 `{param}`이 있는 것만 자동으로 모은다.
  3. **외부 링크 별칭** (`appLinkAliases`, [RouteMatcher.kt](../../main/domain/src/main/java/com/dandi/nyummy/main/domain/deeplink/RouteMatcher.kt)의 `LinkAlias`) — 서버 푸시처럼 앱 라우트와 경로가 다른 외부 링크를 앱 라우트로 바꾼다. path 인자는 대상 라우트의 args가 된다. 지금 등록된 별칭: `/meals/{mealId}` → 식사 상세 `/history/meals/{mealId}`(뒤로 가면 기록 탭), `/inquiries/{inquiryId}` → 문의 상세.
  - 순수 매칭 로직(`RoutePattern` / `matchRoute` / `LinkAlias`)은 **`main/domain`**(kotlin-jvm)에 있고 단위 테스트도 거기 둔다. `main/presentation`의 `Uri.resolveRoute()`는 레지스트리(`appRouteByPath`/`appRoutePatterns`/`appLinkAliases`)를 주입만 한다.
  - 해석된 `NavRoute.path`는 항상 **템플릿 path**(= `GenericNavKey` 식별자, in-app path와 동일). 동적 값은 args에 들어가므로 렌더 dispatch(`appRouteByPath[path]`)와 직렬화/복원은 그대로 O(1)·안전하게 동작.
- **푸시 알림 진입**: [PushAppLink.kt](../../main/presentation/src/main/java/com/dandi/nyummy/main/presentation/deeplink/PushAppLink.kt). 링크는 App Link면 `Intent.data`로, OS가 띄운 푸시면 extras의 `appLink`로 들어온다. 알림 식별자는 `LinkArgs.NOTIFICATION_ID`로 라우트 args에 실어서, 같은 링크라도 알림마다 다른 키가 되고 화면은 같은 알림을 한 번만 처리한다.
- **콜드 스타트** `resolveStartStack`: 매칭 라우트의 `syntheticStack` 전체를 깐다 → 부모 체인 포함. 미매칭 → 인트로 폴백.
- **웜 스타트** `resolveNewIntentRoute` → `navigateDeepLink(route)` → `NavSignal.DeepLink` → [handleDeepLink](../../main/presentation/src/main/java/com/dandi/nyummy/main/presentation/navigation/AppNavHost.kt). 미매칭이면 무시한다(인트로로 가지 않는다).
  - **leaf 화면**: **bring-to-front** — 기존 스택은 보존하고 대상 키(path+args)만 최전면으로(동일 키는 `removeAll` 후 최상단 → 중복 키 금지). 사용자의 현재 맥락 유지가 의도된 동작.
  - **탭 루트**(`isBottomTab`): 단순 bring-to-front 하면 탭이 루트에서 밀려 Back 동작이 깨지므로, 콜드/in-app 과 동일한 **탭 루트 시맨틱**(`handleNavRoute` 위임)으로 처리.
  - in-app 전진 이동(`GoToDestPage`, push)과는 신호 자체가 분리되어 서로 영향을 주지 않는다.

| 진입 | 처리 | 예: `/mailbox/inquiry/7`, 보던 스택 `[Home, History]` |
|---|---|---|
| 콜드 스타트 | `syntheticStack` 전체 | `[Home, Settings, InquiryList, InquiryDetail(7)]` |
| 웜 스타트 (leaf) | bring-to-front | `[Home, History, InquiryDetail(7)]` (기존 스택 보존) |
| 웜 스타트 (탭 path) | 탭 루트 시맨틱 | in-app 탭 전환과 동일 (탭이 루트에서 밀려나지 않음) |

- 테스트: `adb shell 'am start -W -a android.intent.action.VIEW -d "https://link.nyummy.co.kr/<path>" com.dandi.nyummy'` (`<path>`는 `AppRouteRegistry.kt`에 등록된 라우트나 `appLinkAliases`의 링크)
