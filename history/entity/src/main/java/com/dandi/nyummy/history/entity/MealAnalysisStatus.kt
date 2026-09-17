package com.dandi.nyummy.history.entity

/**
 * 식사 사진의 AI 영양 분석 상태입니다. 서버 status 문자열과 1:1 매칭됩니다.
 *
 * [FAILED]는 대개 "사진에서 음식을 찾지 못했다"는 판단이라, 화면에서는 재분석/삭제를 안내합니다.
 */
enum class MealAnalysisStatus {
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

    /** 아직 결과를 기다리는 중인지 여부 (대기 + 진행 중). */
    val isInProgress: Boolean
        get() = this == WAITING || this == ANALYZING
}
