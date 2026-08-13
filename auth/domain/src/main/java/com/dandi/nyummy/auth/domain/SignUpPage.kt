package com.dandi.nyummy.auth.domain

import com.dandi.nyummy.common.domain.navigation.NavRoute
import com.dandi.nyummy.common.domain.navigation.Page

object SignUpPage : Page {

    const val PATH = "/signup"

    override fun toRoute(): NavRoute = NavRoute(PATH)
}
