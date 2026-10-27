package com.dandi.nyummy.home.presentation

import androidx.lifecycle.viewModelScope
import com.dandi.nyummy.cat.domain.CatMotionPicker
import com.dandi.nyummy.cat.domain.GetCatAnimationsUseCase
import com.dandi.nyummy.cat.entity.CatAnimationSetVO
import com.dandi.nyummy.cat.entity.CatState
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.TutorialHelper
import com.dandi.nyummy.common.domain.remoteconfig.RemoteConfigHelper
import com.dandi.nyummy.common.domain.remoteconfig.RemoteConfigKey
import com.dandi.nyummy.common.entity.tutorial.TutorialStep
import com.dandi.nyummy.common.presentation.R as CommonR
import com.dandi.nyummy.common.presentation.mvi.MviViewModel
import com.dandi.nyummy.home.domain.GetHomeSummaryUseCase
import com.dandi.nyummy.home.domain.tti.HomeTTIPage
import com.dandi.nyummy.mailbox.domain.MailboxPage
import com.dandi.nyummy.settings.domain.SettingsPage
import com.dandi.nyummy.tti.TTIHelper
import com.dandi.nyummy.tti.TimelineCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val navigationHelper: NavigationHelper,
    private val messageHelper: MessageHelper,
    private val getHomeSummary: GetHomeSummaryUseCase,
    private val getCatAnimations: GetCatAnimationsUseCase,
    private val catMotionPicker: CatMotionPicker,
    private val ttiHelper: TTIHelper,
    private val remoteConfigHelper: RemoteConfigHelper,
    private val tutorialHelper: TutorialHelper,
) : MviViewModel<HomeIntent, HomeUIState, HomeReducerEvent>(HomeUIState.empty) {

    init {
        // 홈 TTI: 요약 숫자와 냐미가 둘 다 보일 때까지. 요약 API 구간은 GetHomeSummaryUseCase 가 찍는다.
        ttiHelper.startTTITracking(HomeTTIPage)
    }

    private var summaryJob: Job? = null
    private var catAnimationsJob: Job? = null
    private var tutorialJob: Job? = null
    private var tutorialRecordJob: Job? = null

    /** 홈 요약을 한 번이라도 읽었는지. 첫 진입을 "방금 기록하고 돌아옴"으로 착각하지 않게 한다. */
    private var hasLoadedSummary = false

    /** 냐미와 요약 숫자가 한 번이라도 화면에 그려졌는지. 둘 다 그려지면 홈 TTI 를 끝낸다. */
    private var isCatShown = false
    private var isSummaryShown = false
    private var isTTIFinished = false

    /**
     * 받아 둔 냐미 상태별 애니메이션. 아직 받지 못했으면 null이다.
     * 화면에는 지금 재생할 동작만 [HomeUIState.catMotion]으로 넘긴다.
     */
    private var catAnimations: CatAnimationSetVO? = null

    override fun onIntent(intent: HomeIntent) {
        when (intent) {
            HomeIntent.ScreenResumed -> {
                loadSummary()
                loadCatAnimationsIfNeeded()
            }
            // 지갑 화면이 생기기 전까지는 눌러도 반응이 있게 준비 중이라고 알린다.
            HomeIntent.ClickWallet -> messageHelper.showToast(CommonR.string.nyummy_not_ready)
            // 우편함은 받을 아이템만 있는 곳이라, 아이템 기능이 꺼져 있으면 들어가지 않고 준비 중이라고 알린다.
            HomeIntent.ClickMail -> if (remoteConfigHelper.get(RemoteConfigKey.MailItemTabEnabled)) {
                navigationHelper.navigateTo(MailboxPage)
            } else {
                messageHelper.showToast(CommonR.string.nyummy_not_ready)
            }
            HomeIntent.ClickNotice -> Unit
            HomeIntent.ClickSettings -> navigationHelper.navigateTo(SettingsPage)
            HomeIntent.ToggleUiHidden ->
                dispatch(HomeReducerEvent.UiHiddenChanged(hidden = !currentState.isUiHidden))
            HomeIntent.ClickCat -> currentState.catState?.let { startCatMotion(it, newLine = true) }
            is HomeIntent.CatMotionFinished -> if (intent.playId == currentState.catPlayId) finishCatMotion()
            HomeIntent.CatShown -> onCatShown()
            HomeIntent.SummaryShown -> onSummaryShown()
            HomeIntent.TutorialAdvance -> currentState.tutorial?.takeIf { it.canAdvance }?.let {
                dispatch(HomeReducerEvent.TutorialChanged(it.copy(index = it.index + 1)))
            }
            HomeIntent.TutorialTargetClicked, HomeIntent.TutorialBack -> endTutorial { tutorialHelper.complete(it) }
            HomeIntent.TutorialSkip -> endTutorial { tutorialHelper.skipAll() }
        }
    }

    override fun reduce(state: HomeUIState, event: HomeReducerEvent): HomeUIState =
        when (event) {
            is HomeReducerEvent.SummaryLoaded -> state.copy(
                summary = event.summary,
                room = event.room,
                showsMailBadge = event.showsMailBadge,
                isSummaryLoaded = true,
            )
            is HomeReducerEvent.UiHiddenChanged -> state.copy(isUiHidden = event.hidden)
            HomeReducerEvent.CatAnimationsLoaded -> state.copy(isCatAnimationFailed = false)
            HomeReducerEvent.CatAnimationsLoadFailed -> state.copy(isCatAnimationFailed = true)
            is HomeReducerEvent.CatMotionChanged -> state.copy(
                catState = event.state,
                catGroup = event.group,
                catPlayId = event.playId,
                catLine = event.line,
                catMotion = event.motion,
            )
            is HomeReducerEvent.TutorialChanged -> state.copy(tutorial = event.tutorial)
        }

    /**
     * 홈 요약을 다시 읽는다. 식사를 기록하고 돌아오면 숫자가 바뀌어야 하므로 화면이 보일 때마다 부른다.
     * 이전 요청이 남아 있으면 취소하고, 실패하면 직전 값을 그대로 둔다(안내는 UseCase가 한다).
     */
    private fun loadSummary() {
        summaryJob?.cancel()
        summaryJob = viewModelScope.launch {
            getHomeSummary().onSuccess { summary ->
                val recordedJustNow = hasLoadedSummary &&
                    summary.todayRecordedCount > currentState.summary.todayRecordedCount
                hasLoadedSummary = true
                // 마이룸이 켜져 있으면 고양이방에 꾸민 방을 그린다. 꺼져 있으면 기본 빈 방만 그린다.
                val room = summary.roomPlacements.toHomeRoom()
                    .takeIf { remoteConfigHelper.get(RemoteConfigKey.MyRoomEnabled) }
                val showsMailBadge = summary.hasMailboxBadge && remoteConfigHelper.get(RemoteConfigKey.MailItemTabEnabled)
                dispatch(HomeReducerEvent.SummaryLoaded(summary, room, showsMailBadge))
                updateCatState(recordedJustNow)
                updateTutorial(showsMailBadge)
            }
        }
    }

    /**
     * 요약을 받을 때마다 보여 줄 튜토리얼 단계를 고른다. 이미 보여 주는 중이면 그대로 둔다.
     * 첫 진입 소개 → (받을 우편이 있으면) 우편 버튼 → (우편함 안내를 마쳤고 마이룸이 켜져 있으면) 마이룸 탭 순으로 본다.
     * 우편 배지는 우편함 기능이 켜져 있을 때만 서므로, 우편함이 꺼져 있으면 우편 버튼을 가리키지 않는다.
     */
    private fun updateTutorial(showsMailBadge: Boolean) {
        if (currentState.tutorial != null || tutorialJob?.isActive == true) return
        tutorialJob = viewModelScope.launch {
            // 방금 마친 단계를 기록하는 중이면 끝난 뒤에 고른다. 그래야 같은 안내가 다시 뜨지 않는다.
            tutorialRecordJob?.join()
            val tutorial = when {
                tutorialHelper.isPending(TutorialStep.HOME_INTRO) ->
                    HomeTutorialUiState(TutorialStep.HOME_INTRO, HomeTutorialScript.Intro)
                showsMailBadge &&
                    tutorialHelper.isPending(TutorialStep.MAILBOX) &&
                    tutorialHelper.isPending(TutorialStep.HOME_MAIL_POINTER) ->
                    HomeTutorialUiState(TutorialStep.HOME_MAIL_POINTER, HomeTutorialScript.MailPointer)
                remoteConfigHelper.get(RemoteConfigKey.MyRoomEnabled) &&
                    !tutorialHelper.isPending(TutorialStep.MAILBOX) &&
                    tutorialHelper.isPending(TutorialStep.ROOM) &&
                    tutorialHelper.isPending(TutorialStep.HOME_ROOM_POINTER) ->
                    HomeTutorialUiState(TutorialStep.HOME_ROOM_POINTER, HomeTutorialScript.RoomPointer)
                else -> null
            }
            if (tutorial != null) dispatch(HomeReducerEvent.TutorialChanged(tutorial))
        }
    }

    /** 튜토리얼을 닫고, 지금 단계를 [record]로 남긴다(마침 또는 전체 건너뛰기). */
    private fun endTutorial(record: suspend (TutorialStep) -> Unit) {
        val step = currentState.tutorial?.step ?: return
        dispatch(HomeReducerEvent.TutorialChanged(null))
        tutorialRecordJob = viewModelScope.launch { record(step) }
    }

    /** 냐미 애니메이션을 아직 받지 못했으면 받는다. 실패하면 기본 냐미를 두고, 다음에 화면이 보일 때 다시 받는다. */
    private fun loadCatAnimationsIfNeeded() {
        if (catAnimations != null || catAnimationsJob?.isActive == true) return
        // 냐미가 보이기까지(애니메이션 정보 + 스프라이트 시트). 다시 받을 때는 처음 찍은 값이 유지된다.
        ttiHelper.startTTITimeline(TimelineCategory.IMAGE_LOADED_TIME)
        catAnimationsJob = viewModelScope.launch {
            getCatAnimations()
                .onSuccess {
                    catAnimations = it
                    dispatch(HomeReducerEvent.CatAnimationsLoaded)
                    currentState.catState?.let { state -> startCatMotion(state, newLine = true) }
                }
                .onFailure { dispatch(HomeReducerEvent.CatAnimationsLoadFailed) }
        }
    }

    /** 홈 요약이 바뀌면 냐미 상태를 다시 고른다. 상태가 그대로면 지금 동작을 끊지 않는다. */
    private fun updateCatState(recordedJustNow: Boolean) {
        val current = currentState.catState
        val next = homeCatState(
            hasRecordedToday = currentState.hasRecordedToday,
            recordedJustNow = recordedJustNow,
            isCelebrating = current == CatState.CONTENT,
        )
        if (next != current || recordedJustNow) startCatMotion(next, newLine = true)
    }

    /**
     * [state]에서 다음 동작을 고른다. 같은 상태 안에서는 바로 전 동작을 피한다.
     * 대사는 [newLine]일 때만 새로 고르고, 바로 전 대사를 피한다.
     */
    private fun startCatMotion(state: CatState, newLine: Boolean) {
        val animation = catAnimations?.animationFor(state)
        val previousGroup = currentState.catGroup.takeIf { currentState.catState == state }
        val line = if (newLine) {
            catMotionPicker.nextLine(animation?.lines.orEmpty(), currentState.catLine)
        } else {
            currentState.catLine
        }
        val group = catMotionPicker.nextGroup(animation?.groups?.size ?: 0, previousGroup)
        val playId = currentState.catPlayId + 1
        dispatch(
            HomeReducerEvent.CatMotionChanged(
                state = state,
                group = group,
                playId = playId,
                line = line,
                motion = animation?.toHomeCatMotion(group, playId, catMotionPicker.restMillis()),
            ),
        )
    }

    /** 먹는 반응이 끝나면 오늘 상황에 맞는 상태로 돌아가고, 그 밖에는 같은 상태의 다른 동작을 이어 간다. */
    private fun finishCatMotion() {
        val current = currentState.catState ?: return
        if (current == CatState.CONTENT) {
            val next = homeCatState(currentState.hasRecordedToday, recordedJustNow = false, isCelebrating = false)
            startCatMotion(next, newLine = true)
        } else {
            startCatMotion(current, newLine = false)
        }
    }

    private fun onCatShown() {
        if (isCatShown) return
        isCatShown = true
        ttiHelper.endTTITimeline(TimelineCategory.IMAGE_LOADED_TIME)
        finishTTIIfShown()
    }

    private fun onSummaryShown() {
        if (isSummaryShown) return
        isSummaryShown = true
        finishTTIIfShown()
    }

    /**
     * 요약 숫자와 냐미가 둘 다 그려졌으면 홈 TTI 를 끝내고 바로 보고한다. 홈은 루트라 오래 살아 있어서 이탈을 기다리지 않는다.
     * API 완료가 아니라 화면이 그렸다고 알린 시점을 쓴다(요약이 늦게 오면 그 숫자가 보인 때가 끝이다).
     */
    private fun finishTTIIfShown() {
        if (isTTIFinished || !isSummaryShown || !isCatShown) return
        isTTIFinished = true
        ttiHelper.endTTITracking()
        ttiHelper.shotTTILogging()
    }
}
