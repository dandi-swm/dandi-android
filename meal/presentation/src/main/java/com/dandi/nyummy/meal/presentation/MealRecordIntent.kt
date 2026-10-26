package com.dandi.nyummy.meal.presentation

import com.dandi.nyummy.common.presentation.mvi.MviIntent

/** 식사 기록(카메라) 화면에서 발생하는 사용자 입력·기기 이벤트입니다. */
sealed interface MealRecordIntent : MviIntent {

    /**
     * 카메라 권한 요청 결과가 도착했거나, 기기 설정에서 돌아와 다시 확인했습니다.
     *
     * @param canAskAgain 거부됐을 때 시스템 팝업으로 다시 물을 수 있는지. 아니면 기기 설정으로 안내합니다.
     */
    data class PermissionResult(val granted: Boolean, val canAskAgain: Boolean = true) : MealRecordIntent

    /** 권한 거부 안내에서 `다시 허용하기` 버튼을 눌렀습니다. */
    data object ClickRequestPermission : MealRecordIntent

    /** 셔터 버튼을 눌렀습니다. */
    data object ClickShutter : MealRecordIntent

    /** 촬영이 완료되어 사진 파일이 저장됐습니다. */
    data class PhotoCaptured(val photoPath: String) : MealRecordIntent

    /** 촬영에 실패했습니다. */
    data object CaptureFailed : MealRecordIntent

    /** 갤러리 선택기가 닫혔습니다. [photoUri] 는 고른 사진의 content URI, 선택을 취소했으면 null 입니다. */
    data class GalleryPhotoPicked(val photoUri: String?) : MealRecordIntent

    /** 촬영본 확인 중 `다시 찍기` 버튼을 눌러 재촬영으로 돌아갑니다. */
    data object ClickRetake : MealRecordIntent

    /** 촬영본 확인 중 `냐미에게 주기` 버튼을 눌렀습니다. */
    data object ClickSubmit : MealRecordIntent

    /** 연출이 끝난 뒤 `완료` 버튼을 눌러 화면을 마무리합니다. */
    data object ClickDone : MealRecordIntent

    /** 연출 장면 [stage] 의 움직임이 끝났습니다. 다음 장면으로 넘어가거나 연출을 마칩니다. */
    data class CeremonyStageFinished(val stage: MealCeremonyStage) : MealRecordIntent

    /** 연출 중 `건너뛰기` 칩을 눌렀습니다. */
    data object ClickSkipCeremony : MealRecordIntent

    /** 닫기(X) 버튼을 눌렀습니다. */
    data object ClickClose : MealRecordIntent
}
