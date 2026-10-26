package com.dandi.nyummy.meal.presentation

import com.dandi.nyummy.common.presentation.mvi.UiState
import com.dandi.nyummy.meal.entity.MealPhotoSource
import com.dandi.nyummy.meal.entity.RewardBoxGrade

/** 카메라 화면의 진행 단계입니다. */
sealed interface MealCameraPhase {

    /** 실시간 프리뷰를 보며 촬영을 준비하는 단계입니다. */
    data object Preview : MealCameraPhase

    /**
     * 촬영본(또는 갤러리에서 가져온 사진)을 확인하고 취소/먹이기를 선택하는 단계입니다.
     * [photoPath] 는 캐시 파일 절대 경로이고, [source] 에 따라 업로드 전 EXIF 처리가 달라집니다.
     */
    data class Captured(val photoPath: String, val source: MealPhotoSource) : MealCameraPhase

    /**
     * 냐미에게 한 끼를 차려 준 뒤의 연출 단계입니다. 업로드가 이 단계에서 진행됩니다.
     * 응답을 기다리는 동안 냐미가 두근두근하고, 응답이 오면 맛 리액션과 마무리까지 같은 화면에서 이어집니다.
     */
    data class Ceremony(val photoPath: String, val source: MealPhotoSource) : MealCameraPhase
}

/** 촬영본이 있는 단계(확인·연출)의 사진 경로, 그 외에는 null 입니다. */
val MealCameraPhase.photoPathOrNull: String?
    get() = when (this) {
        MealCameraPhase.Preview -> null
        is MealCameraPhase.Captured -> photoPath
        is MealCameraPhase.Ceremony -> photoPath
    }

/**
 * 카메라 권한 상태입니다.
 *
 * [Denied] 는 다시 물을 수 있는 거부, [Blocked] 는 "다시 묻지 않음"이 된 거부입니다.
 * [Blocked] 면 시스템 팝업이 뜨지 않으므로 다시 요청하지 않고 기기 설정으로 안내합니다.
 */
enum class MealCameraPermission { Requesting, Granted, Denied, Blocked }

/**
 * 기록이 접수된 뒤 연출에 필요한 값입니다.
 *
 * @property rewardBoxGrade 우편함으로 보낼 보상 상자 등급. [RewardBoxGrade.NONE] 이면 상자 연출을 하지 않습니다.
 */
data class MealCeremonyResult(
    val rewardBoxGrade: RewardBoxGrade = RewardBoxGrade.NONE,
)

/**
 * 식사 기록(카메라) 화면의 UI 상태입니다.
 *
 * 진입 직후에는 [MealCameraPermission.Requesting] 으로 시작해 화면이 곧바로 권한을 요청하며,
 * [isCapturing] 은 셔터 연타를 막고 촬영 실행을 View 에 지시하는 플래그입니다.
 * [isImportingGalleryPhoto] 는 갤러리에서 고른 사진을 검증·복사하는 동안 켜져 촬영과 재선택을 막습니다.
 * [MealCameraPhase.Ceremony] 중에는 재촬영·이탈이 차단되고, 기록이 접수되면([ceremonyResult]) 연출이
 * 맛 리액션으로 넘어갑니다. 접수에 실패하면 [isSubmitFailed] 가 켜지고, 냐미가 기록하지 못했다고 알린 뒤
 * 홈으로 돌아갑니다. [isFinishing] 은 마무리(뒤로가기 신호)가 한 번만 나가도록 완료·닫기·백의
 * 동시 입력을 래치합니다.
 *
 * @property isMailGiftEnabled 보상 상자를 우편함으로 보내는 연출을 보여 줄지. 우편함 아이템 탭이 보일 때만 켭니다.
 * @property roomBackgroundItemId 지금 마이룸에 깔린 배경의 아이템 id. 연출의 고양이방을 이 배경으로 그리고, 없으면 기본 빈 방입니다.
 */
data class MealRecordUIState(
    val phase: MealCameraPhase = MealCameraPhase.Preview,
    val cameraPermission: MealCameraPermission = MealCameraPermission.Requesting,
    val isCapturing: Boolean = false,
    val isImportingGalleryPhoto: Boolean = false,
    val ceremonyResult: MealCeremonyResult? = null,
    val isSubmitFailed: Boolean = false,
    val isFinishing: Boolean = false,
    val isMailGiftEnabled: Boolean = false,
    val roomBackgroundItemId: Long? = null,
) : UiState {

    /** 기록이 접수됐는지. 접수된 뒤에만 연출 중 닫기·백으로 화면을 마칠 수 있습니다. */
    val isSubmitSucceeded: Boolean
        get() = ceremonyResult != null

    companion object {
        val empty = MealRecordUIState()
    }
}
