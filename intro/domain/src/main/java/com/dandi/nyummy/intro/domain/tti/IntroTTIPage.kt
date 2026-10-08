package com.dandi.nyummy.intro.domain.tti

import com.dandi.nyummy.tti.TTIPage
import com.dandi.nyummy.tti.TimelineCategory

/**
 * 인트로(스플래시) TTI. 앱 진입부터 버전 확인이 끝나 다음 행동이 정해질 때까지를 잰다.
 * 강제 업데이트 다이얼로그, 진행바 채움 대기, 화면 이동은 넣지 않는다.
 */
object IntroTTIPage : TTIPage {
    override val pageName: String = "intro"
    override val timelines: List<TimelineCategory> = listOf(TimelineCategory.API_RESPONSE_TIME)
}
