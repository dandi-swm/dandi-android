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
     * 응답을 기다리는 동안 냐미가 두근두근하고, 접수되면([submit]) 맛 리액션과 마무리까지 같은 화면에서 이어집니다.
     *
     * 연출이 어느 장면까지 왔는지([stage])를 화면이 아니라 여기에 둡니다. 화면이 다시 만들어져도 이미 본 장면을
     * 다시 틀지 않고 지금 장면부터 이어 갑니다.
     */
    data class Ceremony(
        val photoPath: String,
        val source: MealPhotoSource,
        val submit: MealSubmitStatus = MealSubmitStatus.Uploading,
        val stage: MealCeremonyStage = MealCeremonyStage.Serving,
    ) : MealCameraPhase
}

/** 연출 중 기록 접수 상태입니다. */
sealed interface MealSubmitStatus {

    /** 사진을 올리고 접수를 기다립니다. */
    data object Uploading : MealSubmitStatus

    /** 기록이 접수됐습니다. [rewardBoxGrade] 가 [RewardBoxGrade.NONE] 이면 상자 연출을 하지 않습니다. */
    data class Accepted(val rewardBoxGrade: RewardBoxGrade) : MealSubmitStatus

    /** 기록하지 못했습니다. 안내 스낵바를 띄우고 곧장 홈으로 돌아갑니다. */
    data object Failed : MealSubmitStatus
}

/**
 * 연출 장면입니다. 화면은 지금 장면의 움직임만 재생하고, 다음 장면은 [nextStage] 가 정합니다.
 *
 * @property isSkippable 건너뛰기 칩으로 건너뛸 수 있는지. 띠욧 컷신만 건너뛸 수 있고(접수된 뒤에만 나오는 장면),
 * 이어지는 먹었어요와 상자 연출은 그대로 보여 줍니다.
 */
enum class MealCeremonyStage(val isSkippable: Boolean = false) {
    /** 사진 카드가 식탁 위로 올라온다. */
    Serving,

    /** 응답을 기다리며 냐미가 두근두근한다. */
    Waiting,

    /** 띠욧! 맛 리액션 컷인. */
    Reaction(isSkippable = true),

    /** 냐미가 맛있게 먹었어요. */
    Ate,

    /** 진한 그린 무대 위에 상자가 등장하고 등급이 오른다. */
    BoxReveal,

    /** 상자가 우편 아이콘으로 날아간다. */
    BoxSend,

    /** 연출이 끝나 완료 버튼을 보여 준다. */
    Finished,
}

/** 기록이 접수됐는지. */
val MealCameraPhase.Ceremony.isAccepted: Boolean
    get() = submit is MealSubmitStatus.Accepted

/** 접수된 기록의 보상 상자 등급. 접수 전이면 [RewardBoxGrade.NONE] 입니다. */
val MealCameraPhase.Ceremony.rewardBoxGrade: RewardBoxGrade
    get() = (submit as? MealSubmitStatus.Accepted)?.rewardBoxGrade ?: RewardBoxGrade.NONE

/** 상자를 우편함으로 보내는 장면까지 보여 줄지. 우편함 연출이 켜져 있고 이번 기록에 상자가 있을 때만입니다. */
fun MealCameraPhase.Ceremony.sendsRewardBox(isMailGiftEnabled: Boolean): Boolean =
    isMailGiftEnabled && rewardBoxGrade != RewardBoxGrade.NONE

/** 접수 결과를 싣습니다. 응답을 기다리던 중이면 곧장 맛 리액션으로, 사진 카드가 올라오는 중이면 그 장면이 끝난 뒤 넘어갑니다. */
fun MealCameraPhase.Ceremony.accepted(rewardBoxGrade: RewardBoxGrade): MealCameraPhase.Ceremony = copy(
    submit = MealSubmitStatus.Accepted(rewardBoxGrade),
    stage = if (stage == MealCeremonyStage.Waiting) MealCeremonyStage.Reaction else stage,
)

/**
 * 지금 장면이 끝났을 때 이어질 장면. null 이면 연출을 마치고 홈으로 갑니다.
 *
 * 접수되기 전에는 기다림 장면에 머물고, 상자를 보내지 않는 기록은 먹었어요에서 끝 장면으로 갑니다.
 * 상자를 보내는 기록은 상자가 우편함에 들어가면 따로 끝 장면 없이 홈으로 갑니다.
 */
fun MealCameraPhase.Ceremony.nextStage(isMailGiftEnabled: Boolean): MealCeremonyStage? = when (stage) {
    MealCeremonyStage.Serving, MealCeremonyStage.Waiting ->
        if (isAccepted) MealCeremonyStage.Reaction else MealCeremonyStage.Waiting
    MealCeremonyStage.Reaction -> MealCeremonyStage.Ate
    MealCeremonyStage.Ate ->
        if (sendsRewardBox(isMailGiftEnabled)) MealCeremonyStage.BoxReveal else MealCeremonyStage.Finished
    MealCeremonyStage.BoxReveal -> MealCeremonyStage.BoxSend
    MealCeremonyStage.BoxSend -> null
    MealCeremonyStage.Finished -> MealCeremonyStage.Finished
}

/**
 * 카메라 권한 상태입니다.
 *
 * [Denied] 는 다시 물을 수 있는 거부, [Blocked] 는 "다시 묻지 않음"이 된 거부입니다.
 * [Blocked] 면 시스템 팝업이 뜨지 않으므로 다시 요청하지 않고 기기 설정으로 안내합니다.
 */
enum class MealCameraPermission { Requesting, Granted, Denied, Blocked }

/**
 * 식사 기록(카메라) 화면의 UI 상태입니다.
 *
 * 진입 직후에는 [MealCameraPermission.Requesting] 으로 시작해 화면이 곧바로 권한을 요청하며,
 * [isCapturing] 은 셔터 연타를 막고 촬영 실행을 View 에 지시하는 플래그입니다.
 * [isImportingGalleryPhoto] 는 갤러리에서 고른 사진을 검증·복사하는 동안 켜져 촬영과 재선택을 막습니다.
 * [MealCameraPhase.Ceremony] 중에는 재촬영·이탈이 차단되고, 접수되면 연출이 맛 리액션으로 넘어갑니다.
 * 접수에 실패하면 안내 스낵바와 함께 곧장 홈으로 돌아갑니다. [isFinishing] 은 마무리(뒤로가기 신호)가 한 번만
 * 나가도록 완료·닫기·백·연출 끝의 동시 입력을 래치합니다.
 *
 * @property isMailGiftEnabled 보상 상자를 우편함으로 보내는 연출을 보여 줄지. 우편함 아이템 탭이 보일 때만 켭니다.
 * @property roomBackgroundItemId 지금 마이룸에 깔린 배경의 아이템 id. 연출의 고양이방을 이 배경으로 그리고, 없으면 기본 빈 방입니다.
 * @property isTutorialChecked 식사 기록 튜토리얼로 열지 확인을 마쳤는지. 마치기 전에는 카메라 권한을 묻지 않습니다
 * (튜토리얼은 예시 사진을 쓰므로 권한을 묻지 않습니다).
 */
data class MealRecordUIState(
    val phase: MealCameraPhase = MealCameraPhase.Preview,
    val cameraPermission: MealCameraPermission = MealCameraPermission.Requesting,
    val isCapturing: Boolean = false,
    val isImportingGalleryPhoto: Boolean = false,
    val isFinishing: Boolean = false,
    val isMailGiftEnabled: Boolean = false,
    val roomBackgroundItemId: Long? = null,
    val isTutorialChecked: Boolean = false,
) : UiState {

    /** 기록이 접수됐는지. 접수된 뒤에만 연출 중 닫기·백으로 화면을 마칠 수 있습니다. */
    val isSubmitSucceeded: Boolean
        get() = (phase as? MealCameraPhase.Ceremony)?.isAccepted == true

    companion object {
        val empty = MealRecordUIState()
    }
}
