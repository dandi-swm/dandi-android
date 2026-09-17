package com.dandi.nyummy.common.domain.analysis

/**
 * 서버의 식사 영양 분석이 끝났음을 알리는 이벤트입니다.
 *
 * 앱이 포그라운드일 때 도착한 FCM 메시지를 화면이 구독할 수 있게 옮겨 담은 것으로,
 * 어떤 식사가 어느 날짜에서 갱신되었는지만 담고 결과 데이터는 싣지 않습니다 —
 * 값의 출처는 항상 API 재조회입니다.
 *
 * @property mealId 분석이 끝난 식사 식별자. 서버가 주지 않으면 빈 값입니다.
 * @property date 해당 식사의 날짜 (yyyy-MM-dd). 서버가 주지 않으면 빈 값입니다.
 */
data class MealAnalysisEvent(
    val mealId: String = "",
    val date: String = "",
)
