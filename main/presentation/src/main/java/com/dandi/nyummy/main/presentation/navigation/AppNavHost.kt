package com.dandi.nyummy.main.presentation.navigation

import android.util.Log
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
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

    LaunchedEffect(Unit) {
        navigationHelper.navigationFlow.collect { signal ->
            when (signal) {
                is NavSignal.GoToDestPage -> handleNavRoute(signal.route, backStack)
                is NavSignal.DeepLink -> handleDeepLink(signal.route, backStack)
                NavSignal.Back -> backStack.removeLastOrNull()
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
 * NavRoute 한 건을 받아 백스택에 push(앱 내 전진 이동).
 * 동일 키가 이미 스택에 있으면 중복을 만들지 않고 최전면으로 끌어올린다 — Navigation3 의
 * contentKey 는 키별 1회만 유효하므로, 같은 (path,args) 키가 스택에 둘 이상 존재해선 안 된다.
 * 미등록 path 는 무시 + 경고 로그.
 */
fun handleNavRoute(route: NavRoute, backStack: NavBackStack<NavKey>) {
    if (appRouteByPath[route.path] == null) {
        Log.w(TAG, "Unhandled NavRoute: ${route.path}")
        return
    }
    val navKey = GenericNavKey.of(route)
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
 * 동일 키(path+args)의 **모든** 출현을 제거한 뒤 최상단에 추가한다.
 * 백스택에 같은 키가 둘 이상 남지 않도록 보장한다(Navigation3 의 contentKey 중복 방지).
 * MutableList.remove 가 첫 출현만 지우는 것과 달리 removeAll 로 잔존 중복까지 제거한다.
 */
private fun NavBackStack<NavKey>.bringToFront(key: NavKey) {
    removeAll { it == key }
    add(key)
}
