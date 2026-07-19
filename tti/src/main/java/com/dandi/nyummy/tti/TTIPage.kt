package com.dandi.nyummy.tti

/**
 * TTI 측정 단위가 되는 화면. 어떤 페이지가 존재하고 각 페이지가 어떤 [TimelineCategory]
 * 들을 측정하는지는 :tti 모듈이 알 필요가 없으므로, 본 인터페이스를 의존하는 각 기능
 * presentation 모듈에서 정의한다.
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
 * 같은 페이지를 식별하는 키는 [pageName] 이므로 동일 페이지에 대해서는 항상
 * 동일한 문자열을 반환해야 한다.
 */
interface TTIPage {
    val pageName: String
    val timelines: List<TimelineCategory>
}
