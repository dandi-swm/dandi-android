package com.dandi.nyummy.meal.presentation

import androidx.lifecycle.viewModelScope
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.message.IconType
import com.dandi.nyummy.common.domain.remoteconfig.RemoteConfigHelper
import com.dandi.nyummy.common.domain.remoteconfig.RemoteConfigKey
import com.dandi.nyummy.common.presentation.mvi.MviViewModel
import com.dandi.nyummy.meal.domain.ImportGalleryMealPhotoUseCase
import com.dandi.nyummy.meal.domain.SubmitMealUseCase
import com.dandi.nyummy.meal.entity.MealPhotoSource
import com.dandi.nyummy.room.domain.GetRoomBackgroundUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class MealRecordViewModel @Inject constructor(
    private val submitMeal: SubmitMealUseCase,
    private val importGalleryMealPhoto: ImportGalleryMealPhotoUseCase,
    private val navigationHelper: NavigationHelper,
    private val messageHelper: MessageHelper,
    remoteConfigHelper: RemoteConfigHelper,
    getRoomBackground: GetRoomBackgroundUseCase,
) : MviViewModel<MealRecordIntent, MealRecordUIState, MealRecordReducerEvent>(
    // 상자를 받을 우편함 아이템 탭이 보일 때만 상자를 우편함으로 보내는 연출을 한다.
    MealRecordUIState(isMailGiftEnabled = remoteConfigHelper.get(RemoteConfigKey.MailItemTabEnabled)),
) {

    init {
        // 연출의 고양이방은 지금 마이룸에 깔린 배경으로 그린다. 촬영하는 동안 미리 읽어 두고, 못 읽으면 기본 빈 방이다.
        viewModelScope.launch {
            getRoomBackground()?.let { dispatch(MealRecordReducerEvent.RoomBackgroundLoaded(it)) }
        }
    }

    override fun onIntent(intent: MealRecordIntent) {
        when (intent) {
            is MealRecordIntent.PermissionResult -> dispatch(
                MealRecordReducerEvent.PermissionChanged(
                    when {
                        intent.granted -> MealCameraPermission.Granted
                        intent.canAskAgain -> MealCameraPermission.Denied
                        else -> MealCameraPermission.Blocked
                    },
                ),
            )

            MealRecordIntent.ClickRequestPermission ->
                dispatch(MealRecordReducerEvent.PermissionChanged(MealCameraPermission.Requesting))

            MealRecordIntent.ClickShutter -> {
                val state = uiState.value
                val canCapture = !state.isCapturing &&
                    !state.isImportingGalleryPhoto &&
                    state.phase is MealCameraPhase.Preview &&
                    state.cameraPermission == MealCameraPermission.Granted
                if (canCapture) dispatch(MealRecordReducerEvent.CaptureStarted)
            }

            is MealRecordIntent.PhotoCaptured ->
                dispatch(MealRecordReducerEvent.CaptureSucceeded(intent.photoPath))

            MealRecordIntent.CaptureFailed -> {
                dispatch(MealRecordReducerEvent.CaptureEnded)
                messageHelper.showSnackBar(
                    iconType = IconType.ERROR,
                    messageRes = R.string.meal_record_capture_failed,
                )
            }

            is MealRecordIntent.GalleryPhotoPicked -> importGalleryPhoto(intent.photoUri)

            MealRecordIntent.ClickRetake -> {
                // 연출(업로드) 중에는 업로드 대상 파일을 지우면 안 되므로 재촬영을 막는다.
                val phase = currentState.phase as? MealCameraPhase.Captured ?: return
                deletePhotoFile(phase.photoPath)
                dispatch(MealRecordReducerEvent.ReturnedToPreview)
            }

            MealRecordIntent.ClickSubmit -> submitCapturedMeal()

            MealRecordIntent.ClickDone -> if (currentState.isSubmitSucceeded) finishRecord()

            // 기록하지 못했다고 알린 뒤에는 확인 단계로 돌아가지 않고 홈으로 간다.
            MealRecordIntent.CeremonyFailureShown ->
                if (currentState.phase is MealCameraPhase.Ceremony && currentState.isSubmitFailed) {
                    finishRecord()
                }

            MealRecordIntent.ClickClose -> when (currentState.phase) {
                // 업로드 진행 중 이탈은 차단하고, 접수됐거나 실패한 뒤에는 연출을 건너뛰고 곧장 마무리한다.
                is MealCameraPhase.Ceremony ->
                    if (currentState.isSubmitSucceeded || currentState.isSubmitFailed) finishRecord()

                else -> {
                    deleteCapturedFile()
                    navigationHelper.navigateToBack()
                }
            }
        }
    }

    override fun reduce(state: MealRecordUIState, event: MealRecordReducerEvent): MealRecordUIState =
        when (event) {
            is MealRecordReducerEvent.PermissionChanged ->
                state.copy(cameraPermission = event.permission)

            MealRecordReducerEvent.CaptureStarted -> state.copy(isCapturing = true)

            is MealRecordReducerEvent.CaptureSucceeded ->
                state.copy(phase = MealCameraPhase.Captured(event.photoPath, MealPhotoSource.CAMERA), isCapturing = false)

            MealRecordReducerEvent.CaptureEnded -> state.copy(isCapturing = false)

            MealRecordReducerEvent.GalleryImportStarted -> state.copy(isImportingGalleryPhoto = true)

            is MealRecordReducerEvent.GalleryImportSucceeded -> state.copy(
                phase = MealCameraPhase.Captured(event.photoPath, MealPhotoSource.GALLERY),
                isImportingGalleryPhoto = false,
            )

            MealRecordReducerEvent.GalleryImportFailed -> state.copy(isImportingGalleryPhoto = false)

            MealRecordReducerEvent.ReturnedToPreview -> state.copy(
                phase = MealCameraPhase.Preview,
                isCapturing = false,
                ceremonyResult = null,
                isSubmitFailed = false,
            )

            MealRecordReducerEvent.SubmitStarted ->
                (state.phase as? MealCameraPhase.Captured)?.let { captured ->
                    state.copy(
                        phase = MealCameraPhase.Ceremony(captured.photoPath, captured.source),
                        ceremonyResult = null,
                        isSubmitFailed = false,
                    )
                } ?: state

            is MealRecordReducerEvent.SubmitSucceeded -> state.copy(ceremonyResult = event.result)

            MealRecordReducerEvent.SubmitFailed -> state.copy(ceremonyResult = null, isSubmitFailed = true)

            MealRecordReducerEvent.FinishStarted -> state.copy(isFinishing = true)

            is MealRecordReducerEvent.RoomBackgroundLoaded -> state.copy(roomBackgroundItemId = event.itemId)
        }

    /**
     * 갤러리에서 고른 사진을 검증해 확인 단계로 가져온다.
     * 선택을 취소했거나, 선택기가 열린 사이 촬영이 진행됐거나, 이미 다른 사진을 가져오는 중이면 무시한다.
     */
    private fun importGalleryPhoto(photoUri: String?) {
        if (photoUri == null) return
        val state = currentState
        val canImport = state.phase is MealCameraPhase.Preview &&
            !state.isCapturing &&
            !state.isImportingGalleryPhoto
        if (!canImport) return
        dispatch(MealRecordReducerEvent.GalleryImportStarted)
        viewModelScope.launch {
            importGalleryMealPhoto(photoUri)
                // 실패 스낵바는 UseCase 가 이미 띄우므로 여기서는 상태 복귀만 한다.
                .onSuccess { dispatch(MealRecordReducerEvent.GalleryImportSucceeded(it)) }
                .onFailure { dispatch(MealRecordReducerEvent.GalleryImportFailed) }
        }
    }

    /**
     * 촬영본을 업로드해 식사를 생성한다. 성공 알림은 연출 화면이 담당한다.
     *
     * 연출은 분석 완료가 아니라 기록 접수(응답 도착)에 맞춰 넘어간다. 분석이 비동기로 바뀌어 응답이
     * "처리 중"으로 바로 와도 같은 흐름이다.
     */
    private fun submitCapturedMeal() {
        val phase = currentState.phase as? MealCameraPhase.Captured ?: return
        dispatch(MealRecordReducerEvent.SubmitStarted)
        viewModelScope.launch {
            submitMeal(phase.photoPath, phase.source)
                .onSuccess { created ->
                    val result = MealCeremonyResult(
                        rewardBoxGrade = created.rewardBoxGrade,
                        isAnalysisPending = created.isAnalysisPending,
                    )
                    dispatch(MealRecordReducerEvent.SubmitSucceeded(result))
                }
                // 실패 스낵바는 UseCase 가 이미 띄우므로 여기서는 상태 복귀만 한다.
                .onFailure { dispatch(MealRecordReducerEvent.SubmitFailed) }
        }
    }

    /**
     * 기록 플로우를 마치고 화면을 닫는다. 연출에 쓰던 촬영 파일도 이때 지운다.
     * 완료·닫기·백·연출 끝이 같은 프레임에 겹쳐도 뒤로가기 신호는 한 번만 나가도록 래치한다.
     */
    private fun finishRecord() {
        if (currentState.isFinishing) return
        (currentState.phase as? MealCameraPhase.Ceremony)?.let { deletePhotoFile(it.photoPath) }
        dispatch(MealRecordReducerEvent.FinishStarted)
        navigationHelper.navigateToBack()
    }

    /** 확인 단계에서 이탈할 때 캐시에 남은 촬영 파일을 정리한다. */
    private fun deleteCapturedFile() {
        val phase = uiState.value.phase
        if (phase is MealCameraPhase.Captured) {
            deletePhotoFile(phase.photoPath)
        }
    }

    private fun deletePhotoFile(photoPath: String) {
        runCatching { File(photoPath).delete() }
    }
}
