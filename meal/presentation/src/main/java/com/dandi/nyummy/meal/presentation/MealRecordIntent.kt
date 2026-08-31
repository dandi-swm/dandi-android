package com.dandi.nyummy.meal.presentation

import com.dandi.nyummy.common.presentation.mvi.MviIntent

/** 식사 기록(카메라) 화면에서 발생하는 사용자 입력·기기 이벤트입니다. */
sealed interface MealRecordIntent : MviIntent {

    /** 카메라 권한 요청 결과가 도착했습니다. */
    data class PermissionResult(val granted: Boolean) : MealRecordIntent

    /** 권한 거부 안내에서 `다시 허용하기` 버튼을 눌렀습니다. */
    data object ClickRequestPermission : MealRecordIntent

    /** 셔터 버튼을 눌렀습니다. */
    data object ClickShutter : MealRecordIntent

    /** 촬영이 완료되어 사진 파일이 저장됐습니다. */
    data class PhotoCaptured(val photoPath: String) : MealRecordIntent

    /** 촬영에 실패했습니다. */
    data object CaptureFailed : MealRecordIntent

    /** 촬영본 확인 중 `취소` 버튼을 눌러 재촬영으로 돌아갑니다. */
    data object ClickRetake : MealRecordIntent

    /** 촬영본 확인 중 `먹이기` 버튼을 눌렀습니다. */
    data object ClickSubmit : MealRecordIntent

    /** 닫기(X) 버튼을 눌렀습니다. */
    data object ClickClose : MealRecordIntent
}
