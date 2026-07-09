package com.swm.dandi.meal.entity

/**
 * 식사 상태 바텀시트에 표시할 오늘의 식사 상태 데이터.
 *
 * 서버 응답은 화면 문구가 아니라 코드값과 원자료를 담고, 실제 표시 문구는 presentation 레이어에서 만든다.
 *
 * @property mealSessions 아침/점심/저녁/간식별 기록 또는 분석 상태 목록.
 * @property nutritionSummary 오늘 누적 영양소의 권장량 대비 상태.
 */
data class MealStatusSheetVO(
    val mealSessions: List<MealSessionStatusVO> = emptyList(),
    val nutritionSummary: NutritionSummaryVO = NutritionSummaryVO(),
) {
    companion object {
        val empty: MealStatusSheetVO = MealStatusSheetVO()
    }
}

/**
 * 하루 식사 세션 하나의 상태.
 *
 * @property mealType 먹은 시간에서 파생된 식사 구간. 사용자가 직접 선택하는 primary 입력값이 아니다.
 * @property status 해당 구간의 기록/분석 상태 코드.
 * @property mealRecordId 기록된 식사 또는 분석 대상 식사 기록 id.
 * @property displayName 기록된 음식명이 있으면 서버가 내려주는 음식 표시명. 상태 라벨이 아니다.
 */
data class MealSessionStatusVO(
    val mealType: MealTypeVO = MealTypeVO.UNKNOWN,
    val status: MealSessionStatusTypeVO = MealSessionStatusTypeVO.UNKNOWN,
    val mealRecordId: String = "",
    val displayName: String = "",
) {
    companion object {
        val empty: MealSessionStatusVO = MealSessionStatusVO()
    }
}

/**
 * 먹은 시간을 기준으로 서버가 파생한 식사 구간.
 */
enum class MealTypeVO {
    BREAKFAST,
    LUNCH,
    DINNER,
    SNACK,
    UNKNOWN,
}

/**
 * 식사 세션 카드가 표현할 서버 상태 코드.
 */
enum class MealSessionStatusTypeVO {
    RECORDED,
    NOT_RECORDED,
    PENDING,
    IN_PROGRESS,
    FAILED,
    UNKNOWN,
}
