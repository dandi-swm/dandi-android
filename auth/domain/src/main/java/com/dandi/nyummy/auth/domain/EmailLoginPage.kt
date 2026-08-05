package com.dandi.nyummy.auth.domain

import com.dandi.nyummy.common.domain.navigation.NavRoute
import com.dandi.nyummy.common.domain.navigation.Page

object EmailLoginPage : Page {
    const val PATH = "/login/email"

    override fun toRoute(): NavRoute = NavRoute(PATH)
}
