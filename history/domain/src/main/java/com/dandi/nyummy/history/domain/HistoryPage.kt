package com.dandi.nyummy.history.domain

import com.dandi.nyummy.common.domain.navigation.NavRoute
import com.dandi.nyummy.common.domain.navigation.Page

/**
 * 히스토리(캘린더/식사 기록 조회) 화면으로 이동하기 위한 네비게이션 정보입니다.
 *
 * [PATH]는 앱 내 이동, 백스택, 화면 등록, 딥링크에서 공통으로 사용하는 히스토리 화면의 식별자입니다.
 */
object HistoryPage : Page {

    const val PATH = "/history"

    override fun toRoute(): NavRoute = NavRoute(PATH)
}
