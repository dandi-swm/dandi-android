package com.dandi.nyummy.auth.domain

import com.dandi.nyummy.common.domain.navigation.Page
import com.dandi.nyummy.home.domain.HomePage
import com.dandi.nyummy.onboarding.domain.OnboardingPage

/**
 * 로그인 응답의 redirectUrl 로 로그인 직후 이동할 화면을 정한다.
 *
 * 서버는 앱 라우트 path(`"/onboarding"`)나 앱 링크 URL(`"https://link.nyummy.co.kr/onboarding"`)을 줄 수 있어
 * scheme·host·query 를 떼고 path 만 비교한다. 온보딩이 아니거나 비어 있으면 홈으로 보낸다.
 */
object PostLoginDestination {

    fun from(redirectUrl: String): Page =
        when (redirectUrl.toPath()) {
            OnboardingPage.PATH -> OnboardingPage
            else -> HomePage
        }

    private fun String.toPath(): String {
        val withoutScheme = trim().substringAfter("://", missingDelimiterValue = trim())
        // 절대 URL 이면 host 뒤부터가 path. 상대 path 면 그대로 쓴다.
        val path = if (withoutScheme == trim() || withoutScheme.startsWith("/")) {
            withoutScheme
        } else {
            "/" + withoutScheme.substringAfter("/", missingDelimiterValue = "")
        }
        return path.substringBefore("?").substringBefore("#").trimEnd('/').ifEmpty { "/" }
    }
}
