package com.dandi.nyummy.tti

/**
 * TTI 측정 단위가 되는 화면. 어떤 페이지가 존재하고 각 페이지가 어떤 [TimelineCategory]
 * 들을 측정하는지는 :tti 모듈이 알 필요가 없으므로, 본 인터페이스를 의존하는 각 기능
 * domain 모듈에서 정의한다.
 *
 * [timelines] 의 순서가 의미를 가진다. 마지막 항목이 끝나야 endTTITracking 이 받아들여지므로,
 * 실제로 측정하는 구간만 넣는다.
 *
 * Example:
 * ```
 * object FeatureTTIPage : TTIPage {
 *     override val pageName = "feature"
 *     override val timelines = listOf(
 *         TimelineCategory.TTI_TIME,
 *         TimelineCategory.API_REQUEST_READY_TIME,
 *         TimelineCategory.API_RESPONSE_TIME,
 *     )
 * }
 * ```
 *
 * 같은 페이지는 [pageName] 으로 묶이고, 같은 페이지가 여러 번 열리면 instance_no 로 구분한다.
 * 동일 페이지에 대해서는 항상 동일한 문자열을 반환해야 한다.
 */
interface TTIPage {
    val pageName: String
    val timelines: List<TimelineCategory>
}
