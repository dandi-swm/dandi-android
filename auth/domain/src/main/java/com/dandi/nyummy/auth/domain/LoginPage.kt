package com.dandi.nyummy.auth.domain

import com.dandi.nyummy.common.domain.navigation.NavRoute
import com.dandi.nyummy.common.domain.navigation.Page

object LoginPage : Page {

    const val PATH = "/login"

    override fun toRoute(): NavRoute = NavRoute(PATH)
}
