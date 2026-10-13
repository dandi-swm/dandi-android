package com.dandi.nyummy.history.data.dto

import kotlinx.serialization.Serializable

/**
 * POST /api/v1/meals/{mealId}/analysis/feedback 요청 (AI 영양 분석 평가). 성공은 본문 없는 201이다.
 *
 * @property rating 별점. 0.5 ~ 5.0, 0.5 단위
 * @property reason 평가 이유(선택, 500자까지). 없으면 보내지 않는다
 */
@Serializable
data class CreateAnalysisFeedbackRequestDTO(
    val rating: Float? = null,
    val reason: String? = null,
)
