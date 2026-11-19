package com.dandi.nyummy.link

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import com.dandi.nyummy.common.data.analytics.AirbridgeSdk
import com.dandi.nyummy.common.data.analytics.handleDeeplink
import com.dandi.nyummy.common.data.analytics.handleDeferredDeeplink
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.main.presentation.MainActivity

/**
 * Airbridge 트래킹 링크로 앱이 열렸을 때 목적지 화면으로 보낸다.
 *
 * 화면 모듈은 SDK 를 모르므로(presentation 은 data 를 보지 않는다) 앱 셸이 [MainActivity]의 생명주기를 지켜보다 처리한다.
 * SDK 가 돌려준 링크는 앱을 켜면서 왔든 켜져 있을 때 왔든 모두 [NavigationHelper.navigateToAppLinkAfterEntry]로 넘긴다.
 * 진입 흐름(인트로, 로그인, 가입, 온보딩, 방 준비)이 끝났으면 바로 열리고, 아직이면 끝난 뒤에 열린다.
 * 설치 뒤 첫 실행에 이어받는 링크는 누른 순간과 떨어져 오므로 맨 위 화면이 하단 탭 루트일 때만 열린다.
 *
 * `link.nyummy.co.kr` 앱 링크와 푸시는 SDK 가 트래킹 링크가 아니라고 답하므로 [MainActivity]의 기존 처리 그대로다.
 */
class AirbridgeDeeplinkCallbacks(
    private val navigationHelper: NavigationHelper,
) : Application.ActivityLifecycleCallbacks {

    /** 이미 SDK 에 넘긴 intent. 화면이 다시 보일 때마다 같은 링크를 또 열지 않게 한다. */
    private var handledIntent: Intent? = null

    private var isDeferredDeeplinkChecked = false

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        if (activity !is MainActivity) return
        // 시스템이 종료한 앱을 되살렸거나 최근 앱 목록에서 다시 연 것이면 예전 링크를 그대로 들고 온다. 이미 연 링크라 다시 열지 않는다.
        if (savedInstanceState != null || activity.intent.isLaunchedFromHistory()) {
            handledIntent = activity.intent
        }
    }

    // 새 링크는 onNewIntent 로 오는데 여기서는 그 순간을 볼 수 없다. MainActivity 가 setIntent 로 바꿔 두므로,
    // 그 직후에 오는 onResume 에서 intent 가 바뀌었는지로 알아본다.
    override fun onActivityResumed(activity: Activity) {
        if (activity !is MainActivity) return

        val intent = activity.intent
        if (intent != null && intent !== handledIntent) {
            handledIntent = intent
            AirbridgeSdk.handleDeeplink(intent) { navigationHelper.navigateToAppLinkAfterEntry(it) }
        }

        if (!isDeferredDeeplinkChecked) {
            isDeferredDeeplinkChecked = true
            // 이어받는 링크는 설치하고 한참 뒤에 올 수 있어, 보던 화면 위에 열리지 않게 하단 탭 루트가 맨 위일 때만 연다.
            AirbridgeSdk.handleDeferredDeeplink { navigationHelper.navigateToAppLinkAfterEntry(it, waitForTabRoot = true) }
        }
    }

    override fun onActivityDestroyed(activity: Activity) {
        if (activity !is MainActivity) return
        // 닫힌 화면의 intent 를 붙들고 있지 않는다. 새 화면이 먼저 만들어졌을 수 있으니 닫힌 화면의 것일 때만 비운다.
        if (activity.intent === handledIntent) handledIntent = null
    }

    override fun onActivityStarted(activity: Activity) = Unit
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
}

private fun Intent?.isLaunchedFromHistory(): Boolean =
    this != null && flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY != 0
