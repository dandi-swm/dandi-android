package com.dandi.nyummy.auth.domain

import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.navigation.AppLinks
import com.dandi.nyummy.home.domain.HomePage
import com.dandi.nyummy.home.domain.HomePreparePage
import com.dandi.nyummy.onboarding.domain.OnboardingPage
import java.net.URI

/**
 * 로그인 응답 redirectUrl(앱 링크)로 로그인 뒤 화면을 연다. 스택을 비워 뒤로 가기로 로그인에 돌아오지 않게 한다.
 *
 * 홈이면(또는 비어 있으면) 앱 링크 대신 냐미 방 준비 화면([HomePreparePage])을 연다. 그 화면이 냐미 애니메이션을
 * 미리 받아 두고 홈을 연다. 그 밖(온보딩 등)은 서버가 준 앱 링크를 그대로 연다.
 */
internal fun NavigationHelper.openAfterLogin(redirectUrl: String) {
    val path = redirectUrl.redirectPath()
    if (path == null || path == HomePage.PATH) {
        navigateToAsRoot(HomePreparePage)
        return
    }
    val link = redirectUrl.trim().let { if (it.startsWith("/")) AppLinks.BASE_URL + it else it }
    navigateToAppLink(link, asRoot = true)
}

/** redirectUrl 이 온보딩을 가리키는지. 온보딩 미완료 표시에 쓴다. */
internal fun String.isOnboardingRedirect(): Boolean = redirectPath() == OnboardingPage.PATH

// "https://link.nyummy.co.kr/onboarding/?from=login" → "/onboarding". 비었거나 경로가 없으면 null.
private fun String.redirectPath(): String? =
    runCatching { URI(trim()).path }.getOrNull()?.trimEnd('/')?.takeIf { it.isNotEmpty() }
