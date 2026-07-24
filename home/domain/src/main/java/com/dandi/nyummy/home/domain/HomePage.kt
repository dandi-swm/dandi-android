package com.dandi.nyummy.home.domain

import com.dandi.nyummy.common.domain.navigation.NavRoute
import com.dandi.nyummy.common.domain.navigation.Page

/**
 * 홈 화면으로 이동하기 위한 네비게이션 정보입니다.
 *
 * [PATH]는 앱 내 이동, 백스택, 화면 등록, 딥링크에서 공통으로 사용하는 홈 화면의 식별자입니다.
 */
object HomePage : Page {

    const val PATH = "/home"

    override fun toRoute(): NavRoute = NavRoute(PATH)
}
