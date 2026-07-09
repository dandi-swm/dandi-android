package com.swm.dandi.meal.domain

import com.swm.dandi.common.domain.navigation.NavRoute
import com.swm.dandi.common.domain.navigation.Page

object NewFoodPage : Page {
    const val PATH: String = "/meal/newFood"

    override fun toRoute(): NavRoute = NavRoute(PATH)
}
