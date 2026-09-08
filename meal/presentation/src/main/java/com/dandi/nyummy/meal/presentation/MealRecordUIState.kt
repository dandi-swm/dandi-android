package com.dandi.nyummy.meal.presentation

import com.dandi.nyummy.common.presentation.mvi.UiState

/** 카메라 화면의 진행 단계입니다. */
sealed interface MealCameraPhase {

    /** 실시간 프리뷰를 보며 촬영을 준비하는 단계입니다. */
    data object Preview : MealCameraPhase

    /** 촬영본을 확인하고 취소/먹이기를 선택하는 단계입니다. [photoPath] 는 캐시 파일 절대 경로입니다. */
    data class Captured(val photoPath: String) : MealCameraPhase

    /** 먹이기 세리머니(픽셀화 연출) 중이며 업로드가 진행되는 단계입니다. */
    data class Feeding(val photoPath: String) : MealCameraPhase
}

/** 촬영본이 있는 단계(확인·세리머니)의 사진 경로, 그 외에는 null 입니다. */
val MealCameraPhase.photoPathOrNull: String?
    get() = when (this) {
        MealCameraPhase.Preview -> null
        is MealCameraPhase.Captured -> photoPath
        is MealCameraPhase.Feeding -> photoPath
    }

/** 카메라 권한 상태입니다. */
enum class MealCameraPermission { Requesting, Granted, Denied }

/**
 * 식사 기록(카메라) 화면의 UI 상태입니다.
 *
 * 진입 직후에는 [MealCameraPermission.Requesting] 으로 시작해 화면이 곧바로 권한을 요청하며,
 * [isCapturing] 은 셔터 연타를 막고 촬영 실행을 View 에 지시하는 플래그입니다.
 * [MealCameraPhase.Feeding] 중에는 재촬영·이탈이 차단되고, [isSubmitSucceeded] 가 켜진 뒤에만
 * `다음` 으로 화면을 마무리할 수 있습니다. [isFinishing] 은 마무리(뒤로가기 신호)가 한 번만
 * 나가도록 다음·닫기·백의 동시 입력을 래치합니다.
 */
data class MealRecordUIState(
    val phase: MealCameraPhase = MealCameraPhase.Preview,
    val cameraPermission: MealCameraPermission = MealCameraPermission.Requesting,
    val isCapturing: Boolean = false,
    val isSubmitSucceeded: Boolean = false,
    val isFinishing: Boolean = false,
) : UiState {

    companion object {
        val empty = MealRecordUIState()
    }
}
