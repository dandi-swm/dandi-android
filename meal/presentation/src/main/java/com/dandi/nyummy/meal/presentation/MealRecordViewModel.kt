package com.dandi.nyummy.meal.presentation

import androidx.lifecycle.viewModelScope
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.message.IconType
import com.dandi.nyummy.common.presentation.mvi.MviViewModel
import com.dandi.nyummy.meal.domain.ImportGalleryMealPhotoUseCase
import com.dandi.nyummy.meal.domain.SubmitMealUseCase
import com.dandi.nyummy.meal.entity.MealPhotoSource
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
) : MviViewModel<MealRecordIntent, MealRecordUIState, MealRecordReducerEvent>(MealRecordUIState.empty) {

    override fun onIntent(intent: MealRecordIntent) {
        when (intent) {
            is MealRecordIntent.PermissionResult -> dispatch(
                MealRecordReducerEvent.PermissionChanged(
                    if (intent.granted) MealCameraPermission.Granted else MealCameraPermission.Denied,
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
                // 세리머니(업로드) 중에는 업로드 대상 파일을 지우면 안 되므로 재촬영을 막는다.
                val phase = currentState.phase as? MealCameraPhase.Captured ?: return
                deletePhotoFile(phase.photoPath)
                dispatch(MealRecordReducerEvent.ReturnedToPreview)
            }

            MealRecordIntent.ClickSubmit -> submitCapturedMeal()

            MealRecordIntent.ClickNext -> {
                val phase = currentState.phase as? MealCameraPhase.Feeding ?: return
                if (!currentState.isSubmitSucceeded) return
                deletePhotoFile(phase.photoPath)
                dispatch(MealRecordReducerEvent.FeedingCompleted)
            }

            MealRecordIntent.ClickDone -> finishRecord()

            MealRecordIntent.ClickClose -> when (val phase = currentState.phase) {
                // 업로드 진행 중 이탈은 차단하고, 성공 후에는 파일을 정리하고 곧장 마무리한다.
                is MealCameraPhase.Feeding ->
                    if (currentState.isSubmitSucceeded) {
                        deletePhotoFile(phase.photoPath)
                        finishRecord()
                    } else {
                        Unit
                    }

                MealCameraPhase.Done -> finishRecord()

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
                isSubmitSucceeded = false,
            )

            MealRecordReducerEvent.SubmitStarted ->
                (state.phase as? MealCameraPhase.Captured)?.let { captured ->
                    state.copy(
                        phase = MealCameraPhase.Feeding(captured.photoPath, captured.source),
                        isSubmitSucceeded = false,
                    )
                } ?: state

            MealRecordReducerEvent.SubmitSucceeded -> state.copy(isSubmitSucceeded = true)

            MealRecordReducerEvent.SubmitFailed ->
                (state.phase as? MealCameraPhase.Feeding)?.let { feeding ->
                    state.copy(
                        phase = MealCameraPhase.Captured(feeding.photoPath, feeding.source),
                        isSubmitSucceeded = false,
                    )
                } ?: state.copy(isSubmitSucceeded = false)

            MealRecordReducerEvent.FeedingCompleted -> state.copy(phase = MealCameraPhase.Done)

            MealRecordReducerEvent.FinishStarted -> state.copy(isFinishing = true)
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

    /** 촬영본을 업로드해 식사를 생성한다. 성공 알림은 세리머니 화면이 담당한다. */
    private fun submitCapturedMeal() {
        val phase = currentState.phase as? MealCameraPhase.Captured ?: return
        dispatch(MealRecordReducerEvent.SubmitStarted)
        viewModelScope.launch {
            submitMeal(phase.photoPath, phase.source)
                // 실패 스낵바는 UseCase 가 이미 띄우므로 여기서는 상태 복귀만 한다.
                .onSuccess { dispatch(MealRecordReducerEvent.SubmitSucceeded) }
                .onFailure { dispatch(MealRecordReducerEvent.SubmitFailed) }
        }
    }

    /**
     * 기록 플로우를 마치고 화면을 닫는다.
     * 완료·닫기·백이 같은 프레임에 겹쳐도 뒤로가기 신호는 한 번만 나가도록 래치한다.
     */
    private fun finishRecord() {
        if (currentState.isFinishing) return
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
