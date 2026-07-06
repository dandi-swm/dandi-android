package com.swm.dandi.sprite.domain

import com.swm.dandi.common.domain.navigation.NavRoute
import com.swm.dandi.common.domain.navigation.Page

/**
 * sprite sample 화면의 임시 라우트.
 *
 * 실제 앱 기능 destination 이 아니라 renderer 사용법을 확인하기 위한 진입점이다.
 * 앱의 정식 시작 화면이 생기면 main 라우팅에서 debug/demo 전용으로 분리하거나 제거한다.
 */
object SpritePage : Page {
    const val PATH = "/sprite"

    override fun toRoute(): NavRoute = NavRoute(PATH)
}
