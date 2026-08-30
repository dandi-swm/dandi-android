package com.dandi.nyummy.meal.presentation

import com.dandi.nyummy.common.presentation.mvi.UiState

/** 카메라 화면의 진행 단계입니다. */
sealed interface MealCameraPhase {

    /** 실시간 프리뷰를 보며 촬영을 준비하는 단계입니다. */
    data object Preview : MealCameraPhase

    /** 촬영본을 확인하고 취소/먹이기를 선택하는 단계입니다. [photoPath] 는 캐시 파일 절대 경로입니다. */
    data class Captured(val photoPath: String) : MealCameraPhase
}

/** 카메라 권한 상태입니다. */
enum class MealCameraPermission { Requesting, Granted, Denied }

/**
 * 식사 기록(카메라) 화면의 UI 상태입니다.
 *
 * 진입 직후에는 [MealCameraPermission.Requesting] 으로 시작해 화면이 곧바로 권한을 요청하며,
 * [isCapturing] 은 셔터 연타를 막고 촬영 실행을 View 에 지시하는 플래그입니다.
 */
data class MealRecordUIState(
    val phase: MealCameraPhase = MealCameraPhase.Preview,
    val cameraPermission: MealCameraPermission = MealCameraPermission.Requesting,
    val isCapturing: Boolean = false,
) : UiState {

    companion object {
        val empty = MealRecordUIState()
    }
}
