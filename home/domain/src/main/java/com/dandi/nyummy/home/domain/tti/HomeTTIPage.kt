package com.dandi.nyummy.home.domain.tti

import com.dandi.nyummy.tti.TTIPage
import com.dandi.nyummy.tti.TimelineCategory

/**
 * 홈 TTI. 홈에 들어와 요약 숫자와 냐미가 둘 다 보일 때까지를 잰다.
 * - API_RESPONSE_TIME: 홈 요약 API
 * - IMAGE_LOADED_TIME: 냐미가 보이기까지(애니메이션 정보 API + 스프라이트 시트 받기, 실패하면 앱에 든 기본 냐미)
 */
object HomeTTIPage : TTIPage {
    override val pageName: String = "home"
    override val timelines: List<TimelineCategory> = listOf(
        TimelineCategory.API_RESPONSE_TIME,
        TimelineCategory.IMAGE_LOADED_TIME,
    )
}
