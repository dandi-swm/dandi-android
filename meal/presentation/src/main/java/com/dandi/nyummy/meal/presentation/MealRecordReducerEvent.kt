package com.dandi.nyummy.meal.presentation

import com.dandi.nyummy.common.presentation.mvi.ReducerEvent

/** 식사 기록(카메라) 화면의 상태를 변이시키는 내부 이벤트입니다. */
sealed interface MealRecordReducerEvent : ReducerEvent {

    /** 카메라 권한 상태가 바뀌었습니다. */
    data class PermissionChanged(val permission: MealCameraPermission) : MealRecordReducerEvent

    /** 촬영이 시작됐습니다. */
    data object CaptureStarted : MealRecordReducerEvent

    /** 촬영이 성공해 확인 단계로 전환합니다. */
    data class CaptureSucceeded(val photoPath: String) : MealRecordReducerEvent

    /** 촬영이 실패해 촬영 중 플래그만 해제합니다. */
    data object CaptureEnded : MealRecordReducerEvent

    /** 촬영본을 버리고 프리뷰 단계로 돌아갑니다. */
    data object ReturnedToPreview : MealRecordReducerEvent

    /** 먹이기 제출이 시작됐습니다. */
    data object SubmitStarted : MealRecordReducerEvent

    /** 먹이기 제출이 실패해 제출 중 플래그만 해제합니다. */
    data object SubmitFailed : MealRecordReducerEvent
}
