package com.dandi.nyummy.meal.presentation

import androidx.lifecycle.viewModelScope
import com.dandi.nyummy.common.domain.analytics.AnalyticsEvent
import com.dandi.nyummy.common.domain.analytics.AnalyticsHelper
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
import kotlin.time.TimeSource
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class MealRecordViewModel @Inject constructor(
    private val submitMeal: SubmitMealUseCase,
    private val importGalleryMealPhoto: ImportGalleryMealPhotoUseCase,
    private val navigationHelper: NavigationHelper,
    private val messageHelper: MessageHelper,
    private val analyticsHelper: AnalyticsHelper,
    remoteConfigHelper: RemoteConfigHelper,
    getRoomBackground: GetRoomBackgroundUseCase,
) : MviViewModel<MealRecordIntent, MealRecordUIState, MealRecordReducerEvent>(
    // 상자를 받을 우편함 아이템 탭이 보일 때만 상자를 우편함으로 보내는 연출을 한다.
    MealRecordUIState(isMailGiftEnabled = remoteConfigHelper.get(RemoteConfigKey.MailItemTabEnabled)),
) {

    /** 화면에 들어온 순간. 저장까지 걸린 시간을 잰다. */
    private val openedAt = TimeSource.Monotonic.markNow()

    init {
        analyticsHelper.logEvent(AnalyticsEvent.MealRecordOpen)
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

            is MealRecordIntent.PhotoCaptured -> {
                dispatch(MealRecordReducerEvent.CaptureSucceeded(intent.photoPath))
                analyticsHelper.logEvent(AnalyticsEvent.MealPhotoReady(MealPhotoSource.CAMERA.analyticsName))
            }

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
                analyticsHelper.logEvent(AnalyticsEvent.MealRetake(phase.source.analyticsName))
                deletePhotoFile(phase.photoPath)
                dispatch(MealRecordReducerEvent.ReturnedToPreview)
            }

            MealRecordIntent.ClickSubmit -> submitCapturedMeal()

            MealRecordIntent.ClickDone -> if (currentState.isSubmitSucceeded) finishRecord()

            MealRecordIntent.ClickClose -> when (currentState.phase) {
                // 업로드 진행 중 이탈은 차단하고, 접수된 뒤에는 연출을 건너뛰고 곧장 마무리한다.
                is MealCameraPhase.Ceremony -> if (currentState.isSubmitSucceeded) finishRecord()

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
                .onSuccess {
                    dispatch(MealRecordReducerEvent.GalleryImportSucceeded(it))
                    analyticsHelper.logEvent(AnalyticsEvent.MealPhotoReady(MealPhotoSource.GALLERY.analyticsName))
                }
                .onFailure { dispatch(MealRecordReducerEvent.GalleryImportFailed) }
        }
    }

    /**
     * 촬영본을 업로드해 식사를 생성한다. 성공 알림은 연출 화면이 담당한다.
     *
     * 응답이 오면 기록이 접수된 것이라 연출이 맛 리액션으로 넘어간다. 영양 분석은 접수 뒤에 진행되므로 기다리지 않는다.
     * 기록하지 못하면 UseCase 가 안내 스낵바를 띄우고, 연출을 멈추고 곧장 홈으로 간다.
     */
    private fun submitCapturedMeal() {
        val phase = currentState.phase as? MealCameraPhase.Captured ?: return
        dispatch(MealRecordReducerEvent.SubmitStarted)
        val source = phase.source.analyticsName
        analyticsHelper.logEvent(AnalyticsEvent.MealSubmit(source))
        viewModelScope.launch {
            submitMeal(phase.photoPath, phase.source)
                .onSuccess { created ->
                    dispatch(MealRecordReducerEvent.SubmitSucceeded(MealCeremonyResult(created.rewardBoxGrade)))
                    analyticsHelper.logEvent(
                        AnalyticsEvent.MealSubmitSuccess(source = source, durationSec = openedAt.elapsedNow().inWholeSeconds),
                    )
                }
                .onFailure {
                    dispatch(MealRecordReducerEvent.SubmitFailed)
                    finishRecord()
                }
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

    override fun onCleared() {
        // 닫기, 시스템 뒤로가기, 실패 뒤 홈으로 가기 모두 여기를 지난다. 저장하지 못하고 떠났으면 떠난 단계를 남긴다.
        if (!currentState.isSubmitSucceeded) {
            analyticsHelper.logEvent(AnalyticsEvent.MealRecordExit(currentState.analyticsStage))
        }
        super.onCleared()
    }
}

/** 분석에 남기는 사진 출처 이름(camera, gallery). */
private val MealPhotoSource.analyticsName: String
    get() = name.lowercase()

/** 분석에 남기는 지금 단계(camera, confirm, uploading, failed). */
private val MealRecordUIState.analyticsStage: String
    get() = when (phase) {
        MealCameraPhase.Preview -> "camera"
        is MealCameraPhase.Captured -> "confirm"
        is MealCameraPhase.Ceremony -> if (isSubmitFailed) "failed" else "uploading"
    }
