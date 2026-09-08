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
                // 세리머니(업로드) 중에는 업로드 대상 파일을 지우면 안 되므로 재촬영을 막는다.
                val phase = currentState.phase as? MealCameraPhase.Captured ?: return
                runCatching { File(phase.photoPath).delete() }
                dispatch(MealRecordReducerEvent.ReturnedToPreview)
            }

            MealRecordIntent.ClickSubmit -> submitCapturedMeal()

            MealRecordIntent.ClickNext -> {
                val phase = currentState.phase as? MealCameraPhase.Feeding ?: return
                if (!currentState.isSubmitSucceeded) return
                finishFeeding(phase.photoPath)
            }

            MealRecordIntent.ClickClose -> when (val phase = currentState.phase) {
                // 업로드 진행 중 이탈은 차단하고, 성공 후에는 `다음` 과 동일하게 마무리한다.
                is MealCameraPhase.Feeding ->
                    if (currentState.isSubmitSucceeded) finishFeeding(phase.photoPath) else Unit

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
                state.copy(phase = MealCameraPhase.Captured(event.photoPath), isCapturing = false)

            MealRecordReducerEvent.CaptureEnded -> state.copy(isCapturing = false)

            MealRecordReducerEvent.ReturnedToPreview -> state.copy(
                phase = MealCameraPhase.Preview,
                isCapturing = false,
                isSubmitSucceeded = false,
            )

            MealRecordReducerEvent.SubmitStarted ->
                (state.phase as? MealCameraPhase.Captured)?.let { captured ->
                    state.copy(
                        phase = MealCameraPhase.Feeding(captured.photoPath),
                        isSubmitSucceeded = false,
                    )
                } ?: state

            MealRecordReducerEvent.SubmitSucceeded -> state.copy(isSubmitSucceeded = true)

            MealRecordReducerEvent.SubmitFailed ->
                (state.phase as? MealCameraPhase.Feeding)?.let { feeding ->
                    state.copy(
                        phase = MealCameraPhase.Captured(feeding.photoPath),
                        isSubmitSucceeded = false,
                    )
                } ?: state.copy(isSubmitSucceeded = false)
        }

    /** 촬영본을 업로드해 식사를 생성한다. 성공 알림은 세리머니 화면이 담당한다. */
    private fun submitCapturedMeal() {
        val phase = currentState.phase as? MealCameraPhase.Captured ?: return
        dispatch(MealRecordReducerEvent.SubmitStarted)
        viewModelScope.launch {
            submitMeal(phase.photoPath)
                // 실패 스낵바는 UseCase 가 이미 띄우므로 여기서는 상태 복귀만 한다.
                .onSuccess { dispatch(MealRecordReducerEvent.SubmitSucceeded) }
                .onFailure { dispatch(MealRecordReducerEvent.SubmitFailed) }
        }
    }

    /** 세리머니를 마치고 촬영 파일을 정리한 뒤 화면을 닫는다. */
    private fun finishFeeding(photoPath: String) {
        runCatching { File(photoPath).delete() }
        navigationHelper.navigateToBack()
    }

    /** 확인 단계에서 이탈할 때 캐시에 남은 촬영 파일을 정리한다. */
    private fun deleteCapturedFile() {
        val phase = uiState.value.phase
        if (phase is MealCameraPhase.Captured) {
            runCatching { File(phase.photoPath).delete() }
        }
    }
}
