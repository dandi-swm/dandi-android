package com.swm.dandi.history.domain

import com.swm.dandi.common.domain.navigation.NavRoute
import com.swm.dandi.common.domain.navigation.Page

object HistoryPage : Page {
    const val PATH: String = "/history"

    override fun toRoute(): NavRoute = NavRoute(PATH)
}
