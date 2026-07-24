package com.dandi.nyummy.history.domain

/**
 * 히스토리 화면에서 발생할 수 있는 에러 종류입니다.
 *
 * 백엔드 API 미구현 단계에서는 UIState 의 자리 표시로만 사용하며,
 * API 연동 시 UseCase 의 에러 처리 체인(handlingErrorOnUseCase)과 연결합니다.
 */
enum class HistoryErrorType {
    /** 네트워크 연결 없음 */
    OFFLINE,

    /** 월 캘린더/일별 기록 조회 실패 */
    LOAD_FAILED,

    /** 하루 영양 요약 조회 실패 */
    NUTRITION_FAILED,

    /** 식사 이름 수정 저장 실패 */
    EDIT_SAVE_FAILED,

    /** 식사 기록 삭제 실패 */
    DELETE_FAILED,
}
