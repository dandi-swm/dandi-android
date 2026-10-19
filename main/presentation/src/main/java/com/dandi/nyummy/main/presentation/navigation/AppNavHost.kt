package com.dandi.nyummy.main.presentation.navigation

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.dandi.nyummy.auth.domain.LoginPage
import com.dandi.nyummy.common.domain.navigation.NavRoute
import com.dandi.nyummy.common.domain.navigation.NavSignal
import com.dandi.nyummy.common.presentation.helper.LocalNavigationHelper
import com.dandi.nyummy.common.presentation.jank.JankPageEffect

@Composable
fun AppNavHost(
    backStack: NavBackStack<NavKey>,
    modifier: Modifier = Modifier,
) {
    val navigationHelper = LocalNavigationHelper.current
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        navigationHelper.navigationFlow.collect { signal ->
            when (signal) {
                is NavSignal.GoToDestPage -> handleNavRoute(signal.route, backStack)
                is NavSignal.DeepLink -> handleDeepLink(signal.route, backStack)
                NavSignal.Back -> backStack.popUnlessRoot()
                is NavSignal.ResetToPage -> handleResetTo(signal.route, backStack)
                NavSignal.BackToInitialPage -> navigateToInitialStack(backStack)
                is NavSignal.GoToExternalLink -> openExternalLink(signal.url, context)
            }
        }
    }

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        modifier = modifier,
        transitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
        popTransitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
        predictivePopTransitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        // 단일 GenericNavKey 디스패처. 실제 화면 결정은 [appRouteByPath] 가 담당한다.
        entryProvider = entryProvider {
            entry<GenericNavKey> { navKey ->
                val route = appRouteByPath[navKey.path]
                if (route == null) {
                    Log.w(TAG, "Unknown path on render: ${navKey.path}")
                    return@entry
                }
                // 페이지 식별자를 JankStats state 로 등록하고, 백스택 이탈 시 PAGE_EXIT flush.
                JankPageEffect(navKey.path)
                route.render(navKey.args)
            }
        }
    )
}

private const val TAG = "[Navigation]"

/**
 * 외부 앱(브라우저/스토어)으로 [url] 을 연다. 앱 내 백스택과 무관한 외부 이동이다.
 * Application/Service context 에서도 안전하도록 [Intent.FLAG_ACTIVITY_NEW_TASK] 를 부여하고,
 * 링크가 비었거나 처리할 앱이 없으면 조용히 무시한다.
 */
fun openExternalLink(url: String, context: Context) {
    if (url.isBlank()) return
    runCatching {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }.onFailure { Log.w(TAG, "openExternalLink failed: $url", it) }
}

/**
 * NavRoute 한 건을 받아 백스택에 push(앱 내 전진 이동).
 * 동일 키가 이미 스택에 있으면 중복을 만들지 않고 최전면으로 끌어올린다 — Navigation3 의
 * contentKey 는 키별 1회만 유효하므로, 같은 (path,args) 키가 스택에 둘 이상 존재해선 안 된다.
 * 탭 화면은 인자와 상관없이 path 하나에 엔트리 하나만 둔다([bringTabToFront]).
 * 미등록 path 는 무시 + 경고 로그.
 */
fun handleNavRoute(route: NavRoute, backStack: NavBackStack<NavKey>) {
    val appRoute = appRouteByPath[route.path]
    if (appRoute == null) {
        Log.w(TAG, "Unhandled NavRoute: ${route.path}")
        return
    }
    val navKey = GenericNavKey.of(route)
    if (appRoute.isBottomTab) {
        backStack.bringTabToFront(navKey)
        return
    }
    if (backStack.lastOrNull() != navKey) {
        backStack.bringToFront(navKey)
        Log.d(TAG, "navigateTo: $navKey")
    }
}

/**
 * 웜 스타트 deep-link 처리.
 *
 * - 대상이 **bottom-tab 루트**면 콜드/in-app 과 동일한 탭 루트 시맨틱으로 처리한다
 *   ([handleNavRoute] 위임). 탭을 단순 bring-to-front 하면 탭이 루트에서 밀려나
 *   Back 동작/탭 루트가 깨지므로, 이 분기로 세 경로(콜드·웜·in-app)의 탭 백스택 형태를 일치시킨다.
 * - 그 외(leaf 화면)는 **bring-to-front**: 콜드 스타트의 synthetic 부모 체인
 *   ([com.dandi.nyummy.main.presentation.deeplink.resolveStartStack])과 달리, 이미 떠 있는
 *   사용자의 스택은 보존하고 대상 키만 최전면으로 올린다(동일 키는 중복 없이 최상단으로).
 *
 * 예) 스택이 `[Home]` 인 상태에서 leaf deep-link 가 도착하면, 기존 맥락을 유지한 채
 * 대상만 전면에 노출된다.
 */
fun handleDeepLink(route: NavRoute, backStack: NavBackStack<NavKey>) {
    val appRoute = appRouteByPath[route.path]
    if (appRoute == null) {
        Log.w(TAG, "Unhandled deep-link route: ${route.path}")
        return
    }
    if (appRoute.isBottomTab) {
        // 루트 탭은 in-app 탭 전환과 동일한 시맨틱으로(탭이 루트에서 밀려나지 않도록).
        handleNavRoute(route, backStack)
        return
    }
    val target = GenericNavKey.of(route)
    if (backStack.lastOrNull() == target) return   // 이미 최전면 — no-op.
    backStack.bringToFront(target)
    Log.d(TAG, "deepLink bringToFront: $target")
}

/**
 * 세션 만료(401) 처리. 스택을 비우고 로그인 화면 단독으로 교체한다.
 *
 * 인증이 필요한 화면들이 백스택에 남아 있으면 뒤로가기 시 만료된 세션 상태로
 * 다시 노출되기 때문이다. 목적지는 domain 이 아닌 여기서 결정한다 —
 * common:domain 이 feature 의 Page 를 참조하면 순환 의존이 된다.
 */
fun navigateToInitialStack(backStack: NavBackStack<NavKey>) {
    backStack.clear()
    backStack.add(GenericNavKey(LoginPage.PATH))
    Log.d(TAG, "backToInitial: reset stack to Login")
}

/**
 * 맨 위 화면 하나를 닫는다. 남은 화면이 하나(루트)뿐이면 아무것도 하지 않는다.
 * 홈처럼 루트로 들어간 화면에서 공통 오류(404) 다이얼로그가 "뒤로"를 보내도 백스택이 비지 않게 한다.
 */
internal fun MutableList<*>.popUnlessRoot() {
    if (size > 1) removeAt(lastIndex)
}

/**
 * 백스택을 전부 비우고 대상 키만 루트로 남긴다(앱 내 리셋 전환).
 * 로그인/회원가입 완료처럼 이전 플로우로 Back 이 되돌아가면 안 되는 경우에 사용한다.
 * 미등록 path 는 무시 + 경고 로그 — 스택을 비우기 전에 검증하므로 빈 스택이 되지 않는다.
 */
fun handleResetTo(route: NavRoute, backStack: NavBackStack<NavKey>) {
    if (appRouteByPath[route.path] == null) {
        Log.w(TAG, "Unhandled reset route: ${route.path}")
        return
    }
    backStack.clear()
    backStack.add(GenericNavKey.of(route))
    Log.d(TAG, "navigateToAsRoot: ${route.path}")
}

/**
 * 탭 화면은 path 하나에 엔트리 하나만 둔다. 인자가 달라도 같은 탭이다.
 * 같은 탭이 둘 생기면 하단 탭 선택과 뒤로 가기가 어긋나기 때문이다.
 *
 * - 인자 없이 오면(하단 탭 누름) 이미 있는 그 탭 엔트리를 인자째 맨 위로 올린다. 화면 상태가 그대로 남는다.
 * - 인자와 함께 오면(알림 링크 등) 같은 탭 엔트리를 지우고 새 인자로 맨 위에 올린다.
 *   키가 바뀌므로 새 엔트리가 되어 화면이 새 인자로 다시 그려진다.
 */
internal fun MutableList<NavKey>.bringTabToFront(key: GenericNavKey) {
    val isSameTab = { entry: NavKey -> (entry as? GenericNavKey)?.path == key.path }
    val target = if (key.args.isEmpty()) lastOrNull(isSameTab) ?: key else key
    // 맨 위가 이미 그 엔트리여도, 앞쪽에 같은 탭이 남아 있으면 정리한다.
    if (lastOrNull() == target && count(isSameTab) == 1) return
    removeAll(isSameTab)
    add(target)
}

/**
 * 동일 키(path+args)의 **모든** 출현을 제거한 뒤 최상단에 추가한다.
 * 백스택에 같은 키가 둘 이상 남지 않도록 보장한다(Navigation3 의 contentKey 중복 방지).
 * MutableList.remove 가 첫 출현만 지우는 것과 달리 removeAll 로 잔존 중복까지 제거한다.
 */
private fun NavBackStack<NavKey>.bringToFront(key: NavKey) {
    removeAll { it == key }
    add(key)
}
