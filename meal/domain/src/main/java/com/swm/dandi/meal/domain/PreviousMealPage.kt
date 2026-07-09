package com.swm.dandi.meal.domain

import com.swm.dandi.common.domain.navigation.NavRoute
import com.swm.dandi.common.domain.navigation.Page

object PreviousMealPage : Page {
    const val PATH: String = "/meal/previousMeal"

    override fun toRoute(): NavRoute = NavRoute(PATH)
}
