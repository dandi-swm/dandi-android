package com.dandi.nyummy.auth.domain

import com.dandi.nyummy.common.domain.navigation.NavRoute
import com.dandi.nyummy.common.domain.navigation.Page

/** 비밀번호 찾기. 이메일 인증을 마치면 서버가 임시 비밀번호를 메일로 보낸다. */
object PasswordResetPage : Page {

    const val PATH = "/login/password-reset"

    override fun toRoute(): NavRoute = NavRoute(PATH)
}
