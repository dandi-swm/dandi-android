package com.dandi.nyummy.meal.presentation

import com.dandi.nyummy.common.presentation.mvi.UiState
import com.dandi.nyummy.meal.entity.MealPhotoSource

/** 카메라 화면의 진행 단계입니다. */
sealed interface MealCameraPhase {

    /** 실시간 프리뷰를 보며 촬영을 준비하는 단계입니다. */
    data object Preview : MealCameraPhase

    /**
     * 촬영본(또는 갤러리에서 가져온 사진)을 확인하고 취소/먹이기를 선택하는 단계입니다.
     * [photoPath] 는 캐시 파일 절대 경로이고, [source] 에 따라 업로드 전 EXIF 처리가 달라집니다.
     */
    data class Captured(val photoPath: String, val source: MealPhotoSource) : MealCameraPhase

    /** 먹이기 세리머니(픽셀화 연출) 중이며 업로드가 진행되는 단계입니다. */
    data class Feeding(val photoPath: String, val source: MealPhotoSource) : MealCameraPhase

    /** 냐미가 다 먹은 뒤 기록 완료를 확인하는 단계입니다. 촬영 파일은 이미 정리된 상태입니다. */
    data object Done : MealCameraPhase
}

/** 촬영본이 있는 단계(확인·세리머니)의 사진 경로, 그 외에는 null 입니다. */
val MealCameraPhase.photoPathOrNull: String?
    get() = when (this) {
        MealCameraPhase.Preview -> null
        is MealCameraPhase.Captured -> photoPath
        is MealCameraPhase.Feeding -> photoPath
        MealCameraPhase.Done -> null
    }

/** 카메라 권한 상태입니다. */
enum class MealCameraPermission { Requesting, Granted, Denied }

/**
 * 식사 기록(카메라) 화면의 UI 상태입니다.
 *
 * 진입 직후에는 [MealCameraPermission.Requesting] 으로 시작해 화면이 곧바로 권한을 요청하며,
 * [isCapturing] 은 셔터 연타를 막고 촬영 실행을 View 에 지시하는 플래그입니다.
 * [isImportingGalleryPhoto] 는 갤러리에서 고른 사진을 검증·복사하는 동안 켜져 촬영과 재선택을 막습니다.
 * [MealCameraPhase.Feeding] 중에는 재촬영·이탈이 차단되고, [isSubmitSucceeded] 가 켜진 뒤에만
 * `다음` 으로 완료 화면([MealCameraPhase.Done])에 진입할 수 있습니다. [isFinishing] 은
 * 마무리(뒤로가기 신호)가 한 번만 나가도록 완료·닫기·백의 동시 입력을 래치합니다.
 */
data class MealRecordUIState(
    val phase: MealCameraPhase = MealCameraPhase.Preview,
    val cameraPermission: MealCameraPermission = MealCameraPermission.Requesting,
    val isCapturing: Boolean = false,
    val isImportingGalleryPhoto: Boolean = false,
    val isSubmitSucceeded: Boolean = false,
    val isFinishing: Boolean = false,
) : UiState {

    companion object {
        val empty = MealRecordUIState()
    }
}
