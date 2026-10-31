package com.dandi.nyummy.meal.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.dandi.nyummy.common.domain.analytics.AnalyticsEvent
import com.dandi.nyummy.common.domain.analytics.AnalyticsHelper
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.TutorialHelper
import com.dandi.nyummy.common.domain.message.IconType
import com.dandi.nyummy.common.domain.remoteconfig.RemoteConfigHelper
import com.dandi.nyummy.common.domain.remoteconfig.RemoteConfigKey
import com.dandi.nyummy.common.entity.time.DeviceTime
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTutorialLesson
import com.dandi.nyummy.common.presentation.mvi.MviViewModel
import com.dandi.nyummy.meal.domain.ImportGalleryMealPhotoUseCase
import com.dandi.nyummy.meal.domain.SubmitMealUseCase
import com.dandi.nyummy.meal.domain.SubmitTutorialMealUseCase
import com.dandi.nyummy.meal.domain.TutorialMealResult
import com.dandi.nyummy.meal.entity.MealPhotoSource
import com.dandi.nyummy.meal.entity.RewardBoxGrade
import com.dandi.nyummy.room.domain.GetRoomBackgroundUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import javax.inject.Inject
import kotlin.time.TimeSource

@HiltViewModel
class MealRecordViewModel @Inject constructor(
    private val submitMeal: SubmitMealUseCase,
    private val submitTutorialMeal: SubmitTutorialMealUseCase,
    private val tutorialHelper: TutorialHelper,
    private val importGalleryMealPhoto: ImportGalleryMealPhotoUseCase,
    private val navigationHelper: NavigationHelper,
    private val messageHelper: MessageHelper,
    private val analyticsHelper: AnalyticsHelper,
    private val savedStateHandle: SavedStateHandle,
    private val remoteConfigHelper: RemoteConfigHelper,
    getRoomBackground: GetRoomBackgroundUseCase,
) : MviViewModel<MealRecordIntent, MealRecordUIState, MealRecordReducerEvent>(
    MealRecordUIState(
        // 앱이 백그라운드에서 종료됐다 돌아왔으면 저장해 둔 사실로 하던 단계를 되살린다.
        phase = MealRecordSnapshot.readFrom(savedStateHandle)?.toPhase() ?: MealCameraPhase.Preview,
        // 상자를 받을 우편함 아이템 탭이 보일 때만 상자를 우편함으로 보내는 연출을 한다.
        isMailGiftEnabled = remoteConfigHelper.get(RemoteConfigKey.MailItemTabEnabled),
        // 되살린 화면은 저장해 둔 사진 경로로 튜토리얼인지 이미 안다.
        isTutorialChecked = MealRecordSnapshot.readFrom(savedStateHandle) != null,
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
        if (!isRestored) checkTutorial()
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
                // 연출(업로드) 중에는 업로드 대상 파일을 지우면 안 되므로 재촬영을 막는다. 튜토리얼은 찍지 않는다.
                val phase = currentState.phase as? MealCameraPhase.Captured ?: return
                if (phase.isTutorial) return
                analyticsHelper.logEvent(AnalyticsEvent.MealRetake(phase.source.analyticsName))
                deletePhotoFile(phase.photoPath)
                dispatch(MealRecordReducerEvent.ReturnedToPreview)
            }

            MealRecordIntent.ClickSubmit -> if (currentState.phase.isTutorial) submitTutorial() else submitCapturedMeal()

            MealRecordIntent.ClickDone -> if (currentState.isSubmitSucceeded) finishRecord()

            is MealRecordIntent.CeremonyStageFinished -> advanceCeremony(intent.stage)

            MealRecordIntent.ClickSkipCeremony -> skipCeremony()

            is MealRecordIntent.DeviceNotificationChecked -> dispatch(MealRecordReducerEvent.DeviceNotificationChanged(intent.isEnabled))

            MealRecordIntent.TutorialSkip -> if (currentState.phase.isTutorial && currentState.phase is MealCameraPhase.Captured) {
                dispatch(MealRecordReducerEvent.TutorialSkipConfirmChanged(remainingTutorialLessons()))
            }

            MealRecordIntent.TutorialSkipCancelled -> dispatch(MealRecordReducerEvent.TutorialSkipConfirmChanged(null))

            MealRecordIntent.TutorialSkipConfirmed -> skipTutorial()

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
                state.updateCeremony { it.accepted(event.rewardBoxGrade, event.cutscene) }

            MealRecordReducerEvent.SubmitFailed ->
                state.updateCeremony { it.copy(submit = MealSubmitStatus.Failed) }

            is MealRecordReducerEvent.CeremonyStageChanged ->
                state.updateCeremony { it.copy(stage = event.stage) }

            MealRecordReducerEvent.FinishStarted -> state.copy(isFinishing = true)

            is MealRecordReducerEvent.RoomBackgroundLoaded -> state.copy(roomBackgroundItemId = event.itemId)

            is MealRecordReducerEvent.DeviceNotificationChanged -> state.copy(isDeviceNotificationEnabled = event.isEnabled)

            is MealRecordReducerEvent.TutorialSkipConfirmChanged -> state.copy(tutorialSkipConfirm = event.remaining)

            is MealRecordReducerEvent.TutorialChecked -> state.copy(
                isTutorialChecked = true,
                phase = if (event.isTutorial && state.phase is MealCameraPhase.Preview) {
                    MealCameraPhase.Captured(TUTORIAL_PHOTO_PATH, MealPhotoSource.CAMERA)
                } else {
                    state.phase
                },
            )
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
     * 응답이 빨라도 접수 결과는 [MIN_WAIT_MILLIS]가 지난 뒤에 반영해, 냐미가 숟가락을 들고 기다리는 장면을 보여 준다.
     * 서버가 기록을 비동기로 받아 바로 응답하게 바뀌어도 이 장면이 줄지 않는다.
     * 기록하지 못하면 UseCase 가 안내 스낵바를 띄우고, 연출을 멈추고 곧장 홈으로 간다.
     */
    private fun submitCapturedMeal() {
        val phase = currentState.phase as? MealCameraPhase.Captured ?: return
        dispatch(MealRecordReducerEvent.SubmitStarted)
        val startedAt = TimeSource.Monotonic.markNow()
        val source = phase.source.analyticsName
        analyticsHelper.logEvent(AnalyticsEvent.MealSubmit(source))
        viewModelScope.launch {
            submitMeal(phase.photoPath, phase.source)
                .onSuccess { created ->
                    // 걸린 시간은 기다림을 더하기 전, 실제 응답 기준으로 남긴다.
                    analyticsHelper.logEvent(
                        AnalyticsEvent.MealSubmitSuccess(source = source, durationSec = openedAt.elapsedNow().inWholeSeconds),
                    )
                    waitMinimum(startedAt)
                    dispatch(submitSucceeded(created.rewardBoxGrade))
                }
                .onFailure {
                    dispatch(MealRecordReducerEvent.SubmitFailed)
                    finishRecord()
                }
        }
    }

    /** 홈이 옮겨 둔 서버 값으로 식사 기록 튜토리얼인지 정한다. 읽지 못하면 평소처럼 찍는다. */
    private fun checkTutorial() {
        viewModelScope.launch {
            val isTutorial = try {
                tutorialHelper.isMealTutorialRequired()
            } catch (_: IOException) {
                false
            }
            dispatch(MealRecordReducerEvent.TutorialChecked(isTutorial))
        }
    }

    /**
     * 냐미가 준비한 예시 사진으로 튜토리얼 식사를 기록한다. 사진은 올리지 않는다.
     * 접수되면 평소처럼 연출을 이어 가고, 이미 했다고 하면 연출 없이 홈으로 간다. 실패하면 UseCase 가 안내하고 홈으로 간다.
     * 사진을 올리지 않아 응답이 바로 오므로, 평소 기록처럼 접수 결과는 [MIN_WAIT_MILLIS]가 지난 뒤에 반영한다.
     */
    private fun submitTutorial() {
        if (currentState.phase !is MealCameraPhase.Captured) return
        dispatch(MealRecordReducerEvent.SubmitStarted)
        val startedAt = TimeSource.Monotonic.markNow()
        viewModelScope.launch {
            submitTutorialMeal()
                .onSuccess { result ->
                    when (result) {
                        is TutorialMealResult.Accepted -> {
                            waitMinimum(startedAt)
                            dispatch(submitSucceeded(result.meal.rewardBoxGrade))
                        }
                        TutorialMealResult.AlreadyCompleted -> finishRecord()
                    }
                }
                .onFailure {
                    dispatch(MealRecordReducerEvent.SubmitFailed)
                    finishRecord()
                }
        }
    }

    /** 기록을 보낸 지 [MIN_WAIT_MILLIS]가 될 때까지 기다린다. 이미 지났으면 곧장 돌아온다. */
    private suspend fun waitMinimum(startedAt: TimeSource.Monotonic.ValueTimeMark) {
        delay((MIN_WAIT_MILLIS - startedAt.elapsedNow().inWholeMilliseconds).coerceAtLeast(0L))
    }

    /** 접수되면 이번에 틀 컷신을 함께 정한다. 시간대는 기기 타임존 기준이다. */
    private fun submitSucceeded(rewardBoxGrade: RewardBoxGrade) =
        MealRecordReducerEvent.SubmitSucceeded(rewardBoxGrade, pickMealCutscene(DeviceTime.now().hour))

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

    /** 띠욧 컷신은 건너뛰기로 넘긴다. 컷신만 건너뛰고, 이어지는 먹었어요와 상자 연출은 그대로 보여 준다. */
    private fun skipCeremony() {
        val ceremony = currentState.phase as? MealCameraPhase.Ceremony ?: return
        if (ceremony.stage.isSkippable) advanceCeremony(ceremony.stage)
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

    /** 건너뛰기 확인에 보일 남은 안내. 꺼진 기능은 뺀다. */
    private fun remainingTutorialLessons(): ImmutableList<NyummyTutorialLesson> = buildList {
        add(NyummyTutorialLesson.RecordMeal)
        if (remoteConfigHelper.get(RemoteConfigKey.MailItemTabEnabled)) add(NyummyTutorialLesson.OpenBox)
        if (remoteConfigHelper.get(RemoteConfigKey.MyRoomEnabled)) add(NyummyTutorialLesson.DecorateRoom)
        add(NyummyTutorialLesson.CatHealth)
    }.toImmutableList()

    /**
     * 건너뛰기를 확정했다. 연출 없이 튜토리얼 식사를 대신 기록해 서버에 마쳤다고 남기고(첫 보상 상자도 받는다),
     * 냐미 건강 이야기만 남긴 뒤 홈으로 돌아간다. 기록 결과와 상관없이 홈이 건강 이야기를 보인다(실패 안내는 UseCase가 한다).
     */
    private fun skipTutorial() {
        if (currentState.isFinishing) return
        dispatch(MealRecordReducerEvent.TutorialSkipConfirmChanged(null))
        dispatch(MealRecordReducerEvent.FinishStarted)
        viewModelScope.launch {
            submitTutorialMeal()
            try {
                tutorialHelper.skipToHealthStory()
            } catch (_: IOException) {
            }
            navigationHelper.navigateToBack()
        }
    }

    private fun deletePhotoFile(photoPath: String) {
        if (photoPath == TUTORIAL_PHOTO_PATH) return
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

/**
 * 기록을 보내고 접수 결과를 반영하기까지 기다리는 최소 시간. 사진 카드가 올라오고(약 1초)
 * 냐미가 숟가락을 들고 두근두근 기다리는 장면을 2초쯤 보여 준다.
 */
private const val MIN_WAIT_MILLIS = 3_000L
