package com.dandi.nyummy.intro.presentation

import com.dandi.nyummy.tti.TTIPage
import com.dandi.nyummy.tti.TimelineCategory

object IntroTTIPage : TTIPage {
    override val pageName: String = "intro"
    override val timelines: List<TimelineCategory> = listOf(TimelineCategory.API_RESPONSE_TIME)
}
