package com.dandi.nyummy.intro.domain

import com.dandi.nyummy.common.domain.navigation.NavRoute
import com.dandi.nyummy.common.domain.navigation.Page

object IntroPage : Page {
    /** 빈 문자열 = 앱 시작 라우트 컨벤션. 시작 화면이 아닌 페이지는 "/<feature>" 를 쓴다. */
    const val PATH = ""

    override fun toRoute(): NavRoute = NavRoute(PATH)
}
