package com.dandi.nyummy.meal.presentation

import androidx.lifecycle.SavedStateHandle
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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
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
    private val savedStateHandle: SavedStateHandle,
    remoteConfigHelper: RemoteConfigHelper,
    getRoomBackground: GetRoomBackgroundUseCase,
) : MviViewModel<MealRecordIntent, MealRecordUIState, MealRecordReducerEvent>(
    MealRecordUIState(
        // 앱이 백그라운드에서 종료됐다 돌아왔으면 저장해 둔 사실로 하던 단계를 되살린다.
        phase = MealRecordSnapshot.readFrom(savedStateHandle)?.toPhase() ?: MealCameraPhase.Preview,
        // 상자를 받을 우편함 아이템 탭이 보일 때만 상자를 우편함으로 보내는 연출을 한다.
        isMailGiftEnabled = remoteConfigHelper.get(RemoteConfigKey.MailItemTabEnabled),
    ),
) {

    /** 화면에 들어온 순간. 저장까지 걸린 시간을 잰다. */
    private val openedAt = TimeSource.Monotonic.markNow()

    /** 앱이 종료됐다 되살아난 화면인지. 진입은 종료 전에 이미 기록했다. */
    private val isRestored = MealRecordSnapshot.readFrom(savedStateHandle) != null

    init {
        if (!isRestored) analyticsHelper.logEvent(AnalyticsEvent.MealRecordOpen)
        // 앱이 백그라운드에서 종료돼도 돌아와서 이어 갈 수 있게, 되살리는 데 필요한 사실을 저장해 둔다.
        viewModelScope.launch {
            uiState.map { MealRecordSnapshot.of(it.phase) }
                .distinctUntilChanged()
                .collect { it.writeTo(savedStateHandle) }
        }
        leaveIfSubmitInterrupted()
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
                // 화면이 다시 만들어지며 겹친 촬영처럼 기다리지 않던 결과는 버리고 파일도 지운다.
                if (!currentState.isCapturing || currentState.phase !is MealCameraPhase.Preview) {
                    deletePhotoFile(intent.photoPath)
                    return
                }
                dispatch(MealRecordReducerEvent.CaptureSucceeded(intent.photoPath))
                analyticsHelper.logEvent(AnalyticsEvent.MealPhotoReady(MealPhotoSource.CAMERA.analyticsName))
            }

            MealRecordIntent.CaptureFailed -> {
                if (!currentState.isCapturing) return
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

            is MealRecordIntent.CeremonyStageFinished -> advanceCeremony(intent.stage)

            MealRecordIntent.ClickSkipCeremony -> skipCeremony()

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

            MealRecordReducerEvent.ReturnedToPreview -> state.copy(phase = MealCameraPhase.Preview, isCapturing = false)

            MealRecordReducerEvent.SubmitStarted ->
                (state.phase as? MealCameraPhase.Captured)?.let { captured ->
                    state.copy(phase = MealCameraPhase.Ceremony(captured.photoPath, captured.source))
                } ?: state

            is MealRecordReducerEvent.SubmitSucceeded ->
                state.updateCeremony { it.accepted(event.rewardBoxGrade) }

            MealRecordReducerEvent.SubmitFailed ->
                state.updateCeremony { it.copy(submit = MealSubmitStatus.Failed) }

            is MealRecordReducerEvent.CeremonyStageChanged ->
                state.updateCeremony { it.copy(stage = event.stage) }

            MealRecordReducerEvent.FinishStarted -> state.copy(isFinishing = true)

            is MealRecordReducerEvent.RoomBackgroundLoaded -> state.copy(roomBackgroundItemId = event.itemId)
        }

    /** 연출 단계일 때만 [block] 으로 연출 상태를 바꾼다. */
    private inline fun MealRecordUIState.updateCeremony(
        block: (MealCameraPhase.Ceremony) -> MealCameraPhase.Ceremony,
    ): MealRecordUIState = (phase as? MealCameraPhase.Ceremony)?.let { copy(phase = block(it)) } ?: this

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
                    dispatch(MealRecordReducerEvent.SubmitSucceeded(created.rewardBoxGrade))
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
     * 연출 장면 [finished] 의 움직임이 끝나면 다음 장면으로 넘긴다. 다음 장면이 없으면 연출을 마치고 홈으로 간다.
     * 화면이 다시 만들어지며 늦게 온 지난 장면의 알림은 무시한다.
     */
    private fun advanceCeremony(finished: MealCeremonyStage) {
        val ceremony = currentState.phase as? MealCameraPhase.Ceremony ?: return
        if (ceremony.stage != finished) return
        when (val next = ceremony.nextStage(currentState.isMailGiftEnabled)) {
            null -> finishRecord()
            ceremony.stage -> Unit
            else -> dispatch(MealRecordReducerEvent.CeremonyStageChanged(next))
        }
    }

    /** 띠욧부터는 건너뛰기로 끝 장면으로 간다. 상자를 보내는 기록이면 상자는 이미 우편함에 있으니 곧장 홈으로 간다. */
    private fun skipCeremony() {
        val ceremony = currentState.phase as? MealCameraPhase.Ceremony ?: return
        if (!ceremony.stage.isSkippable) return
        ceremony.skippedStage(currentState.isMailGiftEnabled)
            ?.let { dispatch(MealRecordReducerEvent.CeremonyStageChanged(it)) }
            ?: finishRecord()
    }

    /**
     * 업로드를 시작했지만 접수 결과를 받기 전에 앱이 종료됐다 돌아왔으면 홈으로 보낸다.
     * 서버에는 기록됐을 수 있어 다시 보내지 않고(다시 보내면 같은 끼니가 두 번 기록될 수 있다) 기록 탭에서 확인하도록 안내한다.
     */
    private fun leaveIfSubmitInterrupted() {
        val phase = currentState.phase as? MealCameraPhase.Ceremony ?: return
        if (phase.submit != MealSubmitStatus.Uploading) return
        messageHelper.showSnackBar(iconType = IconType.WARNING, messageRes = R.string.meal_record_submit_interrupted)
        finishRecord()
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
    get() = when (val phase = phase) {
        MealCameraPhase.Preview -> "camera"
        is MealCameraPhase.Captured -> "confirm"
        is MealCameraPhase.Ceremony -> if (phase.submit == MealSubmitStatus.Failed) "failed" else "uploading"
    }
