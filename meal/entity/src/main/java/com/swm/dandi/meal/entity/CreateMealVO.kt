package com.swm.dandi.meal.entity

/**
 * 식사 기록 생성 방식.
 */
enum class MealInputModeVO {
    PREVIOUS_FOOD,
    NEW_FOOD,
}

/**
 * 식사 기록 생성 요청.
 *
 * 이전 음식 빠른 기록은 `foodHistoryId`와 `eatenAt`만 필요하고,
 * 새 음식 추가는 `foodName`, `photoId`, `eatenAt`이 필요하다.
 */
data class CreateMealRequestVO(
    val inputMode: MealInputModeVO = MealInputModeVO.NEW_FOOD,
    val foodHistoryId: String = "",
    val foodName: String = "",
    val photoId: String = "",
    val eatenAt: String = "",
)

/**
 * 식사 기록 생성 결과.
 *
 * @property mealRecordId 생성된 식사 기록 id.
 * @property foodHistoryId 연결되거나 새로 만들어진 이전 음식 id.
 * @property inputMode 생성에 사용된 입력 방식.
 * @property displayName 화면에 표시할 음식명.
 * @property mealType 서버가 `eatenAt`에서 파생한 식사 구간.
 * @property eatenAt 사용자가 입력한 실제 섭취 시각.
 * @property photoUrl 저장된 음식 사진 URL.
 * @property analysisStatus 영양 분석 파이프라인 처리 상태.
 * @property createdAt 서버 생성 시각.
 */
data class CreateMealVO(
    val mealRecordId: String = "",
    val foodHistoryId: String = "",
    val inputMode: MealInputModeVO = MealInputModeVO.NEW_FOOD,
    val displayName: String = "",
    val mealType: MealTypeVO = MealTypeVO.UNKNOWN,
    val eatenAt: String = "",
    val photoUrl: String = "",
    val analysisStatus: MealAnalysisStatusVO = MealAnalysisStatusVO.UNKNOWN,
    val createdAt: String = "",
) {
    companion object {
        val empty: CreateMealVO = CreateMealVO()
    }
}

/**
 * 식사 기록에 대한 영양 분석 처리 상태.
 */
enum class MealAnalysisStatusVO {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    FAILED,
    UNKNOWN,
}
