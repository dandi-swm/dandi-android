package com.dandi.nyummy.settings.domain

import com.dandi.nyummy.common.domain.navigation.NavRoute
import com.dandi.nyummy.common.domain.navigation.Page

/** 설정. 홈 상단 설정 버튼에서 들어온다. */
object SettingsPage : Page {

    const val PATH = "/settings"

    override fun toRoute(): NavRoute = NavRoute(PATH)
}

/** 평소 식사 시간(아침, 점심, 저녁)을 드롭다운으로 고친다. */
object MealTimeSettingPage : Page {

    const val PATH = "/settings/meal-time"

    override fun toRoute(): NavRoute = NavRoute(PATH)
}
