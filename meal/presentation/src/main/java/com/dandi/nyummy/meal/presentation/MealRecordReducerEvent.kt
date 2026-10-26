package com.dandi.nyummy.meal.presentation

import com.dandi.nyummy.common.presentation.mvi.ReducerEvent
import com.dandi.nyummy.meal.entity.RewardBoxGrade

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

    /** 갤러리에서 고른 사진의 검증·복사가 시작됐습니다. */
    data object GalleryImportStarted : MealRecordReducerEvent

    /** 갤러리 사진을 가져와 확인 단계로 전환합니다. */
    data class GalleryImportSucceeded(val photoPath: String) : MealRecordReducerEvent

    /** 갤러리 사진을 가져오지 못해(오늘 찍은 사진이 아님 등) 가져오는 중 플래그만 해제합니다. */
    data object GalleryImportFailed : MealRecordReducerEvent

    /** 촬영본을 버리고 프리뷰 단계로 돌아갑니다. */
    data object ReturnedToPreview : MealRecordReducerEvent

    /** 제출이 시작되어 연출 단계로 전환합니다. */
    data object SubmitStarted : MealRecordReducerEvent

    /** 기록이 접수되어 연출이 맛 리액션으로 넘어갑니다. */
    data class SubmitSucceeded(val rewardBoxGrade: RewardBoxGrade) : MealRecordReducerEvent

    /** 제출이 실패했습니다. 안내 후 곧장 홈으로 돌아갑니다. */
    data object SubmitFailed : MealRecordReducerEvent

    /** 연출이 [stage] 장면으로 넘어갑니다. */
    data class CeremonyStageChanged(val stage: MealCeremonyStage) : MealRecordReducerEvent

    /** 세리머니 마무리가 시작되어 추가 이탈 입력을 래치합니다. */
    data object FinishStarted : MealRecordReducerEvent

    /** 지금 마이룸에 깔린 배경을 읽었습니다. 연출의 고양이방을 이 배경으로 그립니다. */
    data class RoomBackgroundLoaded(val itemId: Long) : MealRecordReducerEvent
}
