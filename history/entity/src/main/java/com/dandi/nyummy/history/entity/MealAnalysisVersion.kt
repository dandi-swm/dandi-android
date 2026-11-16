package com.dandi.nyummy.history.entity

/**
 * 식사를 분석한 방식입니다. 서버 analysisVersion 문자열과 1:1 매칭되며, 상세 화면을 고르는 기준입니다.
 */
enum class MealAnalysisVersion {
    /** 사진 한 장을 통째로 분석한 식사 — 음식 목록이 없습니다. */
    V1,

    /** 음식 줄을 인식한 뒤 확인한 목록으로 영양을 계산한 식사 */
    V2,
}
