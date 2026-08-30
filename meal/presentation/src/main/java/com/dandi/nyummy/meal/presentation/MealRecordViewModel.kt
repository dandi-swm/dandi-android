package com.dandi.nyummy.meal.presentation

import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.message.IconType
import com.dandi.nyummy.common.presentation.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject

@HiltViewModel
class MealRecordViewModel @Inject constructor(
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
                deleteCapturedFile()
                dispatch(MealRecordReducerEvent.ReturnedToPreview)
            }

            MealRecordIntent.ClickSubmit -> Unit // TODO: 음식 분석 제출 API 연동 (백엔드 미구현)

            MealRecordIntent.ClickClose -> {
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
        }

    /** 확인 단계에서 이탈할 때 캐시에 남은 촬영 파일을 정리한다. */
    private fun deleteCapturedFile() {
        val phase = uiState.value.phase
        if (phase is MealCameraPhase.Captured) {
            runCatching { File(phase.photoPath).delete() }
        }
    }
}
