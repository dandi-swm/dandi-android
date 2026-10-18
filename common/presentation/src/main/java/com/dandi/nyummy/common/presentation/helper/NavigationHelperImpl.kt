package com.dandi.nyummy.common.presentation.helper

import android.util.Log
import androidx.compose.runtime.compositionLocalOf
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.message.MessageEffect
import com.dandi.nyummy.common.domain.navigation.NavRoute
import com.dandi.nyummy.common.domain.navigation.NavSignal
import com.dandi.nyummy.common.domain.navigation.Page
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

class NavigationHelperImpl : NavigationHelper {
    private val _navigationFlow = Channel<NavSignal>(capacity = Channel.BUFFERED)
    override val navigationFlow: Flow<NavSignal> = _navigationFlow.receiveAsFlow()

    override fun navigateTo(page: Page) {
        navigateByRoute(page.toRoute())
    }

    override fun navigateByRoute(route: NavRoute) {
        emit(NavSignal.GoToDestPage(route))
    }

    override fun navigateDeepLink(route: NavRoute) {
        emit(NavSignal.DeepLink(route))
    }

    override fun navigateToBack() {
        emit(NavSignal.Back)
    }

    override fun navigateToAsRoot(page: Page) {
        emit(NavSignal.ResetToPage(page.toRoute()))
    }

    override fun navigateToInitial() {
        emit(NavSignal.BackToInitialPage)
    }

    override fun navigateToExternalLink(url: String) {
        emit(NavSignal.GoToExternalLink(url))
    }

    private fun emit(navSignal: NavSignal) {
        val result = _navigationFlow.trySend(navSignal)
        if (result.isFailure) Log.w("NavigationHelper", "dropped: $navSignal")
    }
}

val LocalNavigationHelper = compositionLocalOf<NavigationHelper> { error("No user found!") }

/**
 * 이 화면이 지금 사용자가 보고 있는 맨 위 화면인지. 다른 화면으로 넘어가는 순간 false가 된다.
 * 넘어가는 동안 나가는 화면이 한 번 더 다시 그려질 수 있어서, 그때 잰 값을 버려야 할 때 쓴다.
 * 내비게이션 밖(미리보기, 테스트)에서는 true다.
 */
val LocalIsTopScreen = compositionLocalOf { true }
