package com.swm.dandi.history.entity

/**
 * 먹은 시간을 기준으로 서버가 파생한 식사 구간.
 *
 * 사용자가 직접 선택하는 primary 입력값이 아니라, 식사 목록과 요약 표시를 위한 분류값이다.
 */
enum class HistoryMealTypeVO {
    BREAKFAST,
    LUNCH,
    DINNER,
    SNACK,
    UNKNOWN,
}
