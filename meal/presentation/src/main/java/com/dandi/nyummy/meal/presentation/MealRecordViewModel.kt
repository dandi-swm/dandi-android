package com.dandi.nyummy.meal.presentation

import androidx.lifecycle.viewModelScope
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.message.IconType
import com.dandi.nyummy.common.presentation.mvi.MviViewModel
import com.dandi.nyummy.meal.domain.SubmitMealUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class MealRecordViewModel @Inject constructor(
    private val submitMeal: SubmitMealUseCase,
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

            MealRecordIntent.ClickRetake -> {
                // 제출 중에는 업로드 대상 파일을 지우면 안 되므로 재촬영을 막는다.
                if (currentState.isSubmitting) return
                deleteCapturedFile()
                dispatch(MealRecordReducerEvent.ReturnedToPreview)
            }

            MealRecordIntent.ClickSubmit -> submitCapturedMeal()

            MealRecordIntent.ClickClose -> {
                if (currentState.isSubmitting) return
                deleteCapturedFile()
                navigationHelper.navigateToBack()
            }
        }
    }

    override fun reduce(state: MealRecordUIState, event: MealRecordReducerEvent): MealRecordUIState =
        when (event) {
            is MealRecordReducerEvent.PermissionChanged ->
                state.copy(cameraPermission = event.permission)

            MealRecordReducerEvent.CaptureStarted -> state.copy(isCapturing = true)

            is MealRecordReducerEvent.CaptureSucceeded ->
                state.copy(phase = MealCameraPhase.Captured(event.photoPath), isCapturing = false)

            MealRecordReducerEvent.CaptureEnded -> state.copy(isCapturing = false)

            MealRecordReducerEvent.ReturnedToPreview ->
                state.copy(phase = MealCameraPhase.Preview, isCapturing = false)

            MealRecordReducerEvent.SubmitStarted -> state.copy(isSubmitting = true)

            MealRecordReducerEvent.SubmitFailed -> state.copy(isSubmitting = false)
        }

    /** 촬영본을 업로드해 식사를 생성한다. 성공하면 촬영 파일을 정리하고 화면을 닫는다. */
    private fun submitCapturedMeal() {
        val phase = currentState.phase as? MealCameraPhase.Captured ?: return
        if (currentState.isSubmitting) return
        dispatch(MealRecordReducerEvent.SubmitStarted)
        viewModelScope.launch {
            submitMeal(phase.photoPath)
                .onSuccess {
                    deleteCapturedFile()
                    messageHelper.showSnackBar(
                        iconType = IconType.SUCCESS,
                        messageRes = R.string.meal_record_submit_success,
                    )
                    navigationHelper.navigateToBack()
                }
                .onFailure { dispatch(MealRecordReducerEvent.SubmitFailed) }
        }
    }

    /** 확인 단계에서 이탈할 때 캐시에 남은 촬영 파일을 정리한다. */
    private fun deleteCapturedFile() {
        val phase = uiState.value.phase
        if (phase is MealCameraPhase.Captured) {
            runCatching { File(phase.photoPath).delete() }
        }
    }
}
