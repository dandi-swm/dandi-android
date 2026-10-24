package com.dandi.nyummy.home.domain

import com.dandi.nyummy.common.domain.navigation.NavRoute
import com.dandi.nyummy.common.domain.navigation.Page

/**
 * 로그인 직후 홈에 들어가기 전에 냐미 방을 준비하는 화면으로 이동하기 위한 네비게이션 정보입니다.
 *
 * 이 화면이 냐미 애니메이션과 시트 그림을 미리 받은 뒤 [HomePage]로 넘어갑니다.
 * 로그인과 온보딩 완료가 홈 대신 이 화면으로 보냅니다. 앱을 켜서 자동 로그인될 때는 거치지 않고,
 * 인트로가 같은 미리 받기를 끝낸 뒤 바로 [HomePage]로 갑니다.
 */
object HomePreparePage : Page {

    const val PATH = "/home/prepare"

    override fun toRoute(): NavRoute = NavRoute(PATH)
}
