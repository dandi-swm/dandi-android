package com.swm.dandi.meal.entity

/**
 * 오늘 기준 주요 영양소의 누적 섭취 현황.
 *
 * @property nutrients 권장량 대비 비율을 계산할 수 있는 영양소별 원자료 목록.
 */
data class NutritionSummaryVO(
    val nutrients: List<NutrientRatioVO> = emptyList(),
) {
    companion object {
        val empty: NutritionSummaryVO = NutritionSummaryVO()
    }
}

/**
 * 단일 영양소의 현재 섭취량과 목표량.
 *
 * `ratio`가 있으므로 퍼센트 문자열은 VO에 저장하지 않는다. 화면에서는 `ratio`를 이용해 표시한다.
 *
 * @property nutrientType 영양소 코드.
 * @property currentAmount 오늘 누적 섭취량.
 * @property targetAmount 사용자 기준 하루 권장량 또는 목표량.
 * @property unit 수치 단위. 예: `kcal`, `g`, `mg`.
 * @property ratio `currentAmount / targetAmount`에 해당하는 비율. 서버가 계산한 값을 우선 사용한다.
 */
data class NutrientRatioVO(
    val nutrientType: NutrientTypeVO = NutrientTypeVO.UNKNOWN,
    val currentAmount: Double = 0.0,
    val targetAmount: Double = 0.0,
    val unit: String = "",
    val ratio: Float = 0f,
) {
    companion object {
        val empty: NutrientRatioVO = NutrientRatioVO()
    }
}

/**
 * Dandi가 오늘 현황에서 우선 표시하는 주요 영양소 코드.
 */
enum class NutrientTypeVO {
    CALORIE,
    CARBOHYDRATE,
    PROTEIN,
    FAT,
    SODIUM,
    UNKNOWN,
}
