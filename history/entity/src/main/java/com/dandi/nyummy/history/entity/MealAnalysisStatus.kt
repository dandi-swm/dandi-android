package com.dandi.nyummy.history.entity

/**
 * 식사 사진의 AI 영양 분석 상태입니다. 서버 status 문자열과 1:1 매칭됩니다.
 *
 * [FAILED]는 대개 "사진에서 음식을 찾지 못했다"는 판단이라, 화면에서는 재분석/삭제를 안내합니다.
 */
enum class MealAnalysisStatus {
    /** 사진에서 음식을 찾는 중 — 아직 음식 목록이 없습니다. */
    RECOGNIZING,

    /** 음식을 찾았고 사용자의 확인을 기다리는 중 — 확인하지 않아도 5분 뒤 서버가 영양 계산을 시작합니다. */
    CONFIRMING,

    /** 분석 대기 */
    WAITING,

    /** 분석 진행 중 */
    ANALYZING,

    /** 분석 완료 — 이름/영양 정보가 채워져 있습니다. */
    COMPLETED,

    /** 분석 실패 — 음식으로 인식하지 못했습니다. */
    FAILED,

    /** 서버가 알 수 없는 값을 내려준 경우 */
    UNKNOWN,
    ;

    /** 사용자가 할 일 없이 결과를 기다리는 중인지 여부 (음식 인식 중 + 대기 + 진행 중). */
    val isInProgress: Boolean
        get() = this == RECOGNIZING || this == WAITING || this == ANALYZING

    /** 음식 목록이 정해져 서버가 영양을 계산하는 중인지 여부 (대기 + 진행 중). */
    val isCalculating: Boolean
        get() = this == WAITING || this == ANALYZING
}
