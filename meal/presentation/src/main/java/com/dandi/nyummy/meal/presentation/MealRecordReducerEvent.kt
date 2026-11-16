package com.dandi.nyummy.meal.presentation

import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTutorialLesson
import com.dandi.nyummy.common.presentation.mvi.ReducerEvent
import com.dandi.nyummy.meal.entity.FoundFoodVO
import com.dandi.nyummy.meal.entity.RewardBoxGrade
import kotlinx.collections.immutable.ImmutableList

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
    data class SubmitSucceeded(val rewardBoxGrade: RewardBoxGrade, val cutscene: MealCutscene) : MealRecordReducerEvent

    /** 서버가 사진에서 음식을 찾았습니다. 찾은 음식 고르기로 넘어갑니다. */
    data class FoodsFound(
        val mealId: Long,
        val rewardBoxGrade: RewardBoxGrade,
        val foods: List<FoundFoodVO>,
    ) : MealRecordReducerEvent

    /** 서버가 사진이 음식이 아니라고 했습니다. */
    data object NotFoodJudged : MealRecordReducerEvent

    /** 서버가 기한 안에 음식을 찾지 못했습니다. */
    data object RecognitionDelayed : MealRecordReducerEvent

    /** 찾은 음식 줄의 체크가 바뀌었습니다. */
    data class FoodToggled(val key: Int) : MealRecordReducerEvent

    /** 음식 이름 입력 줄이 열렸습니다. */
    data object FoodNameInputOpened : MealRecordReducerEvent

    /** 추가할 음식 이름이 바뀌었습니다. */
    data class FoodNameInputChanged(val text: String) : MealRecordReducerEvent

    /** 입력한 이름이 목록에 추가됐습니다. */
    data object FoodNameInputConfirmed : MealRecordReducerEvent

    /** 고른 목록을 보내거나 기록을 취소하기 시작했습니다. 끝날 때까지 고르기 화면의 입력을 막습니다. */
    data object FoodPickBusy : MealRecordReducerEvent

    /** 고른 목록을 보내지 못했습니다. 고르기 화면에 머뭅니다. */
    data object FoodsSubmitFailed : MealRecordReducerEvent

    /** 서버가 고른 목록을 받았습니다. 맛 리액션부터 연출을 잇습니다. */
    data class FoodsTaken(val cutscene: MealCutscene) : MealRecordReducerEvent

    /** 제출이 실패했습니다. 안내 후 곧장 홈으로 돌아갑니다. */
    data object SubmitFailed : MealRecordReducerEvent

    /** 연출이 [stage] 장면으로 넘어갑니다. */
    data class CeremonyStageChanged(val stage: MealCeremonyStage) : MealRecordReducerEvent

    /** 세리머니 마무리가 시작되어 추가 이탈 입력을 래치합니다. */
    data object FinishStarted : MealRecordReducerEvent

    /** 지금 마이룸에 깔린 배경을 읽었습니다. 연출의 고양이방을 이 배경으로 그립니다. */
    data class RoomBackgroundLoaded(val itemId: Long) : MealRecordReducerEvent

    /** 기기에서 이 앱의 알림을 허용했는지 다시 읽었습니다. */
    data class DeviceNotificationChanged(val isEnabled: Boolean) : MealRecordReducerEvent

    /** 식사 기록 튜토리얼로 열지 확인했습니다. 튜토리얼이면 예시 사진 확인 단계에서 시작합니다. */
    data class TutorialChecked(val isTutorial: Boolean) : MealRecordReducerEvent

    /** 건너뛰기 확인을 띄웠습니다(남은 안내) 또는 닫았습니다(null). */
    data class TutorialSkipConfirmChanged(val remaining: ImmutableList<NyummyTutorialLesson>?) : MealRecordReducerEvent
}
