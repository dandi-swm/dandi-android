package com.dandi.nyummy.auth.domain

import com.dandi.nyummy.common.domain.navigation.NavRoute
import com.dandi.nyummy.common.domain.navigation.Page

/**
 * 소셜 로그인 신규 회원의 프로필 입력(회원가입) 화면.
 * 가입에 쓸 토큰은 라우트 인자가 아니라 [SocialSignUpSession] 으로 전달된다.
 */
object SocialSignUpPage : Page {

    const val PATH = "/signup/social"

    override fun toRoute(): NavRoute = NavRoute(PATH)
}
