---
name: "navigation-conventions"
description: "How this project's Navigation3 model works and the invariants every screen / route / deep link must follow \u2014 NavRoute / Page objects, GenericNavKey, AppRouteRegistry, syntheticStack, the deep-link cold/warm policy, RoutePattern templates (nested paths), and NavigationHelper. Trigger when adding or changing a screen, route, deep link, back-stack behavior, AppRouteRegistry entry, Page navigation object, or NavigationHelper usage; or when reasoning about why a back stack / deep link behaves a certain way. Pairs with `make-new-feature-module` (scaffolding) and the canonical [docs/architecture/navigation.md](docs/architecture/navigation.md)."
---

# Navigation conventions (Navigation3)

This project uses **Jetpack Navigation3**. The core idea: **the app owns the back stack** (a `NavBackStack<NavKey>`), and a single host renders it. Navigation3 deliberately has **no built-in deep-link parser / NavController** — routing and deep-link → back-stack mapping are app code, centralized here.

Canonical deep dive: [docs/architecture/navigation.md](docs/architecture/navigation.md). This skill is the **enforcement layer** — the rules to follow when touching navigation, and the shapes a route can take. For scaffolding a brand-new feature, use `make-new-feature-module` (its §2.8 / §5 hold the copy-paste templates).

## Nyummy 현재 시작 화면 규칙 (우선 적용)

현재 Nyummy는 `HomePage`가 앱의 시작 화면이다. 앱을 처음 실행할 때의 fallback은
`RootComposable`이나 `MainActivity`에서 만들지 않고, 반드시
`deeplink/NavRouteUriParser.kt`의 `resolveStartStack()`에서 결정한다.

AndroidArchi와 같은 형태를 유지한다. `IntroPage`만 `HomePage`로 바꾼 아래 구조가 기준이다.

```kotlin
fun resolveStartStack(uri: Uri?): List<NavKey> {
    val route = uri?.resolveRoute()
    if (route == null) {
        if (uri != null) Log.w(TAG, "No matching route for uri=$uri")
        return listOf(GenericNavKey(HomePage.PATH))
    }
    val appRoute = appRouteByPath[route.path]
        ?: return listOf(GenericNavKey(HomePage.PATH))
    return appRoute.syntheticStack(route.args)
}
```

- URI가 없거나 등록되지 않은 URI면 홈 한 화면으로 시작한다.
- 등록된 딥링크면 해당 `AppRoute.syntheticStack(route.args)`을 사용한다.
- `MainActivity`는 `resolveStartStack(intent.data)`의 결과를 그대로 `RootComposable`에 전달한다.
- `RootComposable`의 `startStack` 기본값, 별도 `startRoute`, 빈 스택을 조건부로 치환하는 로직을 추가하지 않는다.
- 이 시작 fallback은 등록된 딥링크의 부모 스택 규칙과 별개인 앱 진입점 예외다. 이 규칙이 아래의 일반적인 `syntheticStack` 설명보다 우선한다.

Nyummy의 공통 쉘은 `Scaffold(bottomBar = …)`로 바텀 네비게이션을 붙인다. `isBottomTab`은 현재 화면에서 바텀 바를 표시할지 판단하는 메타데이터이며, 화면 이동은 항상 `navigationHelper.navigateTo(Page)`를 사용한다.

## The model — one key type, path-based dispatch

| Layer | Type / file | Role |
|---|---|---|
| domain (pure JVM) | `NavRoute(path, args: Map<String,String>)` | The single navigation unit. `args` are **all String**. |
| domain | `interface Page { fun toRoute(): NavRoute }` | Each screen's typed entry point (in `<feature>/domain`). |
| domain | `sealed interface NavSignal { GoToDestPage / DeepLink / Back }` | What flows through the nav channel. |
| domain | `interface NavigationHelper` | `navigateTo(Page)` (recommended) / `navigateByRoute` / `navigateDeepLink` / `navigateToBack`. The only thing domain knows. |
| **main/domain** (pure JVM) | `deeplink/RoutePattern.kt`, `deeplink/RouteMatcher.kt` (`matchRoute`) | Pure route matching — template `{param}` extraction + exact/template resolution. **Unit-tested here** (project convention: tests live in `domain`). |
| host | `GenericNavKey(@Serializable path, args)` : `NavKey` | **The one and only back-stack entry type.** Every destination is this; the screen is chosen by `path`. |
| host | `AppRoute(path, isBottomTab, syntheticStack, render)` | Per-route host metadata. |
| host | `AppRouteRegistry.kt` | **Single registration point** — `appRoutes` list + derived `appRouteByPath`, `appRoutePatterns`, `bottomTabRoutes`. |
| host | `AppNavHost.kt` | Collects `navigationFlow`, mutates the back stack, renders `NavDisplay` + `entry<GenericNavKey>`. |
| host | `deeplink/NavRouteUriParser.kt` | `Uri.resolveRoute()` → injects the registry into `matchRoute`; cold/warm deep-link entry points. |

A screen's identity is its **`path`**. The same path drives in-app navigation, the back-stack key, render dispatch (`appRouteByPath[path]`), and the deep-link URL — that's why "adding a screen gives you its deep link for free."

## Golden rules (invariants — violating these breaks routing or crashes Compose)

1. **`Page.PATH` is the canonical identity.** It is the in-app path, the `GenericNavKey.path`, the render-dispatch key, and the deep-link template — all the same string. Register every path in `AppRouteRegistry.appRoutes`; nowhere else.
2. **For nested/parameterized routes, `PATH` is the TEMPLATE string** (e.g. `"/articleList/articlePage/{articleId}"`). **Never** splice the concrete value into the path — the value goes in `args` only. (The key stored in the back stack literally contains `{articleId}`; the value `123` lives in `args`.) This keeps `appRouteByPath[path]` O(1), keeps serialization/process-death restore safe, and lets `RoutePattern` extract the value.
3. **Template param name == Args key == `Args.from` lookup key**, char-for-char. `{articleId}` ↔ `KEY_ARTICLE_ID = "articleId"` ↔ `args["articleId"]`. Mismatch silently drops the value.
4. **등록된 딥링크의 cold-start back stack은 `AppRoute.syntheticStack`에서 정의한다.** 단, URI 없음·미매칭의 앱 시작 fallback은 위의 **Nyummy 현재 시작 화면 규칙**을 따른다. 웜 딥링크 경로는 라우트별 스택을 다시 정의하지 않는다.
5. **No duplicate keys in the back stack.** Navigation3's `contentKey` defaults to `key.toString()`, which must be unique per back-stack entry. Use the `bringToFront` helper (`removeAll { it == key }; add(key)`) when re-surfacing an existing key — never a bare `add` that can duplicate, and never `remove` (removes only the first occurrence).
6. **In-app navigation goes through `navigationHelper.navigateTo(Page)`** (AGENTS.md rule 7). Construct the `Page`/`Args` and call `navigateTo`; don't poke the back stack from a screen.
7. **`args` are String-only.** Complex types serialize to a JSON string (`NavRouteJson`) — see `FullScreenMediaPage` for the typed-Args golden example.

## In-app navigation vs deep links — two separate signals

- **In-app** (tab click, list-item click): `navigateTo(Page)` → `NavSignal.GoToDestPage` → `handleNavRoute` → **push** (with `bringToFront` dedup). Navigating to a bottom-tab root (currently only `Home`) is handled by `handleNavRoute`, which brings the tab root to front instead of duplicating it.
- **Deep link, cold start** (`MainActivity.onCreate`): `resolveStartStack(intent.data)` → 매칭된 라우트의 `syntheticStack`이 **전체 부모 체인**을 구성하고, URI 없음·미매칭이면 Nyummy에서는 `HomePage` 단일 스택으로 fallback한다. `rememberNavBackStack` 전에 구성한다.
- **Deep link, warm start** (`MainActivity.onNewIntent`, `singleTop`): `resolveNewIntentRoute` → `navigateDeepLink` → `NavSignal.DeepLink` → `handleDeepLink`:
  - **leaf screen** → **bring-to-front**: keep the user's current stack, surface only the target key. (Preserving context is intended.)
  - **tab root** (`isBottomTab` — Nyummy에서는 `RootComposable`의 `Scaffold(bottomBar = …)`가 바텀 네비게이션을 표시한다) → `handleNavRoute`에 위임해 탭 루트 시맨틱을 유지한다.

> ⚠️ **Illustrative example.** `/articleList/articlePage/{articleId}` (and the `123` rows below) is a **hypothetical** route used to show the nested/`{param}` mechanism. There is currently **no dynamic `{param}` route registered** in `appRoutes` (so `appRoutePatterns` is empty at runtime); the **only registered path is `/home`** (`HomePage`, `isBottomTab = true`). The `""`(Intro)/`/search`/`/favorite`/`/fullScreenMedia` paths mentioned elsewhere in this skill are **legacy/hypothetical examples from the AndroidArchi template — none are registered in this project.** The nested example is validated only by the pure-logic unit tests (`RouteMatcherTest`/`RoutePatternTest`), which use a **fake** registry.

| Entry | Back stack for `/articleList/articlePage/123` (current `[Home]`) |
|---|---|
| Cold start | `[Home, ArticleList, ArticlePage(123)]` (full `syntheticStack`) |
| Warm, leaf | `[Home, ArticlePage(123)]` (context preserved) |
| Warm, tab path | in-app tab semantics (tab stays root) |

## Deep-link resolution (RoutePattern)

`Uri.resolveRoute()` (host) extracts segments + query, then delegates to the pure `matchRoute(segments, query, literalPaths, templates)` in **`main/domain`**:
1. **Exact literal match** first (currently just `/home`; `/search`/`/fullScreenMedia` are legacy examples).
2. **Template match** (`RoutePattern`) for paths with `{param}` — extracts path-segment values into `args`. Path params win over query params on name collision.

A route whose `PATH` contains `{` is **auto-added to `appRoutePatterns`** (via `hasParams`) — no extra registration. `RoutePattern` / `matchRoute` are pure (Android-free) and live in `main/domain`, unit-tested there ([RoutePatternTest](main/domain/src/test/java/com/jongchan/androidarchi/main/domain/deeplink/RoutePatternTest.kt), [RouteMatcherTest](main/domain/src/test/java/com/jongchan/androidarchi/main/domain/deeplink/RouteMatcherTest.kt)) — add cases there when you add a template shape.

## Adding a route — pick the shape

All four shapes are scaffolded by `make-new-feature-module` (Page object → its §2.8; registry entry → its §5). Summary:

The parenthesized route names below (intro/search/favorite/fullScreenMedia) are **legacy AndroidArchi examples** — only `/home` is registered today (the **Tab** shape). Use them as pattern references, not as active routes.

| Shape | `Page` | Registry `AppRoute` |
|---|---|---|
| **Simple** (*legacy: intro/search*) | `object XPage : Page { const val PATH = "/x" }` | `path`, `render` |
| **Tab** (current `/home`; *legacy: search/favorite*) — flag `isBottomTab` | same | `+ isBottomTab = true` (default `syntheticStack = [X]`, or `[Home, X]` for a sub-tab) |
| **Typed Args** (*legacy: fullScreenMedia*) | `object XPage { const val PATH = "/x"; data class Args(...) : Page { toRoute()/from() } }` | `syntheticStack = [Home, X(args)]`, `render` decodes `Args.from` |
| **Nested / `{param}`** (*hypothetical: articleList → articlePage — not currently registered*) | `PATH = "/parent/x/{id}"`, `Args(id)` (rules 2–3) | `syntheticStack = [Home, Parent, X(args)]`; template auto-registers for deep links |

A single feature module may host **multiple screens** (e.g. a list + its detail) — that's just multiple `Page` objects + multiple Composables + multiple registry entries. The parent referenced in a nested `syntheticStack` must itself be a registered route (today that root is `Home`).

## DO / DON'T

- **DO** register every new path in `AppRouteRegistry.appRoutes`, and add the `:<feature>:domain` + `:<feature>:presentation` deps to `:main:presentation` so the host can import the `Page`/Composable.
- **DO** define the cold stack only in `syntheticStack`; root nested stacks at `Home` (the current bottom-tab root). (`favorite`/`fullScreenMedia` were the AndroidArchi examples that rooted at `Search` — legacy, not registered here.)
- **DO** use `navigateTo(Page)` for in-app moves and typed `Args` for parameters.
- **DON'T** put a path-param value into `PATH` — `PATH` stays the `{param}` template; values go in `args`.
- **DON'T** let the param name, `KEY_*`, and `Args.from` key drift apart.
- **DON'T** `add` a key that may already be in the stack, or use `remove` (first-only) to dedupe — use `bringToFront`.
- **DON'T** redefine a route's cold back stack outside `syntheticStack`, and don't handle deep links via a `NavController` or Nav3 built-in (there is none — it's `RoutePattern` + the registry).
- **DON'T** put the navigation `Page` object in `presentation` — it lives in `<feature>/domain`.

## Canonical references

- Contracts: [NavRoute.kt](common/domain/src/main/java/com/jongchan/androidarchi/common/domain/navigation/NavRoute.kt), [NavSignal.kt](common/domain/src/main/java/com/jongchan/androidarchi/common/domain/navigation/NavSignal.kt), [NavigationHelper.kt](common/domain/src/main/java/com/jongchan/androidarchi/common/domain/helper/NavigationHelper.kt)
- Host: [AppRouteRegistry.kt](main/presentation/src/main/java/com/jongchan/androidarchi/main/presentation/navigation/AppRouteRegistry.kt), [AppRoute.kt](main/presentation/src/main/java/com/jongchan/androidarchi/main/presentation/navigation/AppRoute.kt), [AppNavHost.kt](main/presentation/src/main/java/com/jongchan/androidarchi/main/presentation/navigation/AppNavHost.kt), [GenericNavKey.kt](main/presentation/src/main/java/com/jongchan/androidarchi/main/presentation/navigation/GenericNavKey.kt)
- Deep link — pure matching (main/domain): [RoutePattern.kt](main/domain/src/main/java/com/jongchan/androidarchi/main/domain/deeplink/RoutePattern.kt), [RouteMatcher.kt](main/domain/src/main/java/com/jongchan/androidarchi/main/domain/deeplink/RouteMatcher.kt). Host glue (main/presentation): [NavRouteUriParser.kt](main/presentation/src/main/java/com/jongchan/androidarchi/main/presentation/deeplink/NavRouteUriParser.kt), [MainActivity.kt](main/presentation/src/main/java/com/jongchan/androidarchi/main/presentation/MainActivity.kt)
- Full prose + diagrams: [docs/architecture/navigation.md](docs/architecture/navigation.md)
- Scaffolding a new feature/screen: `make-new-feature-module`

Manual smoke test: `adb shell 'am start -W -a android.intent.action.VIEW -d "https://www.androidarchi.com/<path>" com.jongchan.androidarchi'`
