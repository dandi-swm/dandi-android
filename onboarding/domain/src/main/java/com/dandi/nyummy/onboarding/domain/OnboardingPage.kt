package com.dandi.nyummy.onboarding.domain

import com.dandi.nyummy.common.domain.navigation.NavRoute
import com.dandi.nyummy.common.domain.navigation.Page

/** 냥줍 온보딩(첫 고양이 이름 짓기). 로그인 응답 redirectUrl 이 이 PATH 이거나 회원가입 직후 진입한다. */
object OnboardingPage : Page {
    const val PATH = "/onboarding"

    override fun toRoute(): NavRoute = NavRoute(PATH)
}
