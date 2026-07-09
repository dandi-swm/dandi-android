package com.swm.dandi.meal.entity

/**
 * 식사 1건에 대한 영양 분석 조회 결과.
 *
 * @property mealRecordId 분석 대상 식사 기록 id.
 * @property analysisStatus 영양 분석 파이프라인 처리 상태.
 * @property nutrients 분석 완료 시 확인할 수 있는 영양소별 수치 목록.
 * @property analyzedAt 분석 완료 시각.
 * @property retryable 실패 시 사용자가 재시도할 수 있는지 여부.
 */
data class NutritionAnalysisVO(
    val mealRecordId: String = "",
    val analysisStatus: MealAnalysisStatusVO = MealAnalysisStatusVO.UNKNOWN,
    val nutrients: List<NutrientAmountVO> = emptyList(),
    val analyzedAt: String = "",
    val retryable: Boolean = false,
) {
    companion object {
        val empty: NutritionAnalysisVO = NutritionAnalysisVO()
    }
}

/**
 * 분석된 단일 영양소의 절대 수치.
 */
data class NutrientAmountVO(
    val nutrientType: NutrientTypeVO = NutrientTypeVO.UNKNOWN,
    val amount: Double = 0.0,
    val unit: String = "",
) {
    companion object {
        val empty: NutrientAmountVO = NutrientAmountVO()
    }
}
