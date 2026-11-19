package com.dandi.nyummy.main.presentation.navigation

import androidx.navigation3.runtime.NavKey

/**
 * 진입 흐름이 끝난 뒤에 여는 앱 링크([com.dandi.nyummy.common.domain.navigation.NavSignal.AppLinkAfterEntry])를 맡아 둔다.
 *
 * 인트로, 로그인, 가입, 온보딩, 방 준비 중에 링크를 바로 열면 그 흐름이 끝날 때 스택을 비우면서 화면이 사라지거나,
 * 로그인하지 않은 채 화면이 열린다. 그래서 진입 흐름 화면([AppRoute.isEntryFlow])이 백스택에 하나도 없을 때만 열고,
 * 있으면 링크 하나를 맡아 두었다가 모두 사라지는 순간 연다. 나중에 온 링크가 앞의 것을 바꾼다.
 *
 * 진입 흐름이 한 번 끝난 뒤에 다시 진입 흐름 화면이 백스택 맨 아래(루트)가 되면(세션 만료, 로그아웃, 탈퇴로 로그인 화면만 남음)
 * 이전 세션에서 맡아 둔 링크를 버린다. 처음 켜서 인트로에서 로그인으로 가는 것은 진입 흐름이 아직 끝나지 않은 것이라
 * 버리지 않고, 로그인, 가입, 온보딩을 마친 뒤에 연다.
 *
 * `waitForTabRoot` 링크(설치 뒤 첫 실행에 이어받은 링크)는 사용자가 누른 순간과 떨어져 도착하므로, 보던 화면 위에 열리지 않게
 * 진입 흐름이 끝났고 맨 위 화면이 하단 탭 루트([AppRoute.isBottomTab], 지금은 홈, 기록, 상점)일 때만 연다.
 *
 * 맡아 둔 링크는 화면 재생성(설정 변경)과 시스템이 앱을 종료했다 되살린 경우에는 저장 상태로 남고, 사용자가 앱을 끄면 사라진다.
 *
 * 화면 판정은 [isEntryFlow], [isTabRoot]로 받아 단위 테스트에서 바꿔 넣을 수 있게 했다. 맡아 둔 값은 [pending]으로 꺼내
 * 호스트가 Activity 재생성을 넘겨 보존하고, 다시 만들 때 생성자로 돌려준다. [hasFinishedEntry]도 같이 보존한다.
 */
internal class AppLinkAfterEntry(
    initial: Pending? = null,
    initialHasFinishedEntry: Boolean = false,
    private val isEntryFlow: (NavKey) -> Boolean = ::isEntryFlowKey,
    private val isTabRoot: (NavKey) -> Boolean = ::isTabRootKey,
) {

    /** 맡아 둔 링크와 그 종류. */
    data class Pending(val link: String, val waitForTabRoot: Boolean)

    var pending: Pending? = initial
        private set

    /** 이 화면이 뜬 뒤 진입 흐름이 한 번이라도 끝났는지(진입 흐름 화면이 하나도 없는 백스택을 본 적 있는지). */
    var hasFinishedEntry: Boolean = initialHasFinishedEntry
        private set

    /** 새 링크가 왔다. 지금 열 링크를 주고, 아직 열 때가 아니라 맡아 두었으면 null 이다. */
    fun offer(link: String, waitForTabRoot: Boolean, backStack: List<NavKey>): String? {
        val next = Pending(link, waitForTabRoot)
        if (backStack.canOpen(next)) {
            pending = null
            return link
        }
        pending = next
        return null
    }

    /**
     * 백스택이 바뀌었다. 맡아 둔 링크가 있고 이제 열 수 있으면 그 링크를 주고 비운다.
     * 진입 흐름이 끝난 뒤 다시 진입 흐름 화면이 루트가 되면 세션이 바뀐 것으로 보고 맡아 둔 링크를 버린다.
     */
    fun takeIfReady(backStack: List<NavKey>): String? {
        val root = backStack.firstOrNull()
        if (root != null && isEntryFlow(root)) {
            if (hasFinishedEntry) {
                hasFinishedEntry = false
                pending = null
            }
        } else if (backStack.isNotEmpty() && backStack.none(isEntryFlow)) {
            hasFinishedEntry = true
        }
        val held = pending ?: return null
        if (!backStack.canOpen(held)) return null
        pending = null
        return held.link
    }

    private fun List<NavKey>.canOpen(link: Pending): Boolean {
        if (isEmpty() || any(isEntryFlow)) return false
        return !link.waitForTabRoot || isTabRoot(last())
    }
}

private fun isEntryFlowKey(key: NavKey): Boolean =
    (key as? GenericNavKey)?.let { appRouteByPath[it.path]?.isEntryFlow } == true

private fun isTabRootKey(key: NavKey): Boolean =
    (key as? GenericNavKey)?.let { appRouteByPath[it.path]?.isBottomTab } == true
