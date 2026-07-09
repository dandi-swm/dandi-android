package com.swm.dandi.history.entity

/**
 * 히스토리 식사 1건에 포함된 단일 영양 성분.
 *
 * 하루 누적량이나 권장량 대비 비율이 아니라, 선택한 음식 자체의 분석 수치다.
 *
 * @property nutrientType 영양 성분 종류.
 * @property amount 선택한 음식에 포함된 영양 성분 수치.
 * @property unit 수치 단위. 예: `kcal`, `g`, `mg`.
 */
data class HistoryNutrientVO(
    val nutrientType: HistoryNutrientTypeVO = HistoryNutrientTypeVO.UNKNOWN,
    val amount: Double = 0.0,
    val unit: String = "",
) {
    companion object {
        val empty: HistoryNutrientVO = HistoryNutrientVO()
    }
}

/**
 * 히스토리 음식 영양 성분에서 표시하는 주요 영양소 코드.
 */
enum class HistoryNutrientTypeVO {
    CALORIE,
    CARBOHYDRATE,
    PROTEIN,
    FAT,
    SODIUM,
    UNKNOWN,
}
