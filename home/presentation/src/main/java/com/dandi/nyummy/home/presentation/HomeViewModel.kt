package com.dandi.nyummy.home.presentation

import androidx.lifecycle.viewModelScope
import com.dandi.nyummy.cat.domain.CatBrain
import com.dandi.nyummy.cat.domain.CatMood
import com.dandi.nyummy.cat.domain.CatPick
import com.dandi.nyummy.cat.domain.CatReaction
import com.dandi.nyummy.cat.domain.GetCatAnimationsUseCase
import com.dandi.nyummy.cat.domain.RefreshCatWeightUseCase
import com.dandi.nyummy.cat.domain.GetCatLinesUseCase
import com.dandi.nyummy.cat.entity.CatAnimationSetVO
import com.dandi.nyummy.cat.entity.CatLineVO
import com.dandi.nyummy.cat.entity.CatState
import com.dandi.nyummy.common.domain.helper.CompanionMemoryHelper
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.remoteconfig.RemoteConfigHelper
import com.dandi.nyummy.common.domain.remoteconfig.RemoteConfigKey
import com.dandi.nyummy.common.entity.time.DeviceTime
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
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val navigationHelper: NavigationHelper,
    private val messageHelper: MessageHelper,
    private val getHomeSummary: GetHomeSummaryUseCase,
    private val getCatAnimations: GetCatAnimationsUseCase,
    private val refreshCatWeight: RefreshCatWeightUseCase,
    private val catBrain: CatBrain,
    private val getCatLines: GetCatLinesUseCase,
    private val companionMemory: CompanionMemoryHelper,
    private val ttiHelper: TTIHelper,
    private val remoteConfigHelper: RemoteConfigHelper,
) : MviViewModel<HomeIntent, HomeUIState, HomeReducerEvent>(HomeUIState.empty) {

    init {
        // 홈 TTI: 요약 숫자와 냐미가 둘 다 보일 때까지. 요약 API 구간은 GetHomeSummaryUseCase 가 찍는다.
        ttiHelper.startTTITracking(HomeTTIPage)
    }

    private var summaryJob: Job? = null
    private var catAnimationsJob: Job? = null

    /** 홈 요약을 한 번이라도 읽었는지. 첫 진입을 "방금 기록하고 돌아옴"으로 착각하지 않게 한다. */
    private var hasLoadedSummary = false

    /** 냐미와 요약 숫자가 한 번이라도 화면에 그려졌는지. 둘 다 그려지면 홈 TTI 를 끝낸다. */
    private var isCatShown = false
    private var isSummaryShown = false
    private var isTTIFinished = false

    /**
     * 내 냐미 체형의 상황별 애니메이션(앱 에셋). 아직 읽지 못했으면 null이다.
     * 화면에는 지금 재생할 동작만 [HomeUIState.catMotion]으로 넘긴다.
     */
    private var catAnimations: CatAnimationSetVO? = null

    /** 냐미 대사 은행. 읽기 전에는 비어 있고, 그동안은 대사 없이(화면 기본 대사) 움직인다. */
    private var catLines: List<CatLineVO> = emptyList()

    /** 다음에 틀 반응(들어올 때 인사, 기록 직후 먹기). */
    private var pendingReaction: CatReaction? = null


    /** 다음에 틀 일상 동작. 지금 동작을 도는 동안 시트를 미리 받으려고 미리 골라 둔다. 고른 분위기가 바뀌면 버린다. */
    private var upcoming: Pair<CatMood, CatPick>? = null

    /** 지금 대사로 일상 동작을 끝까지 몇 번 했는지. [LINE_EVERY_MOTIONS]번마다 대사를 바꾼다. 터치로 끊긴 동작은 세지 않는다. */
    private var motionsSinceLine = 0

    /** 지금 동작이 일상 동작인지. 끝까지 재생되면 [motionsSinceLine]을 센다. */
    private var playingMood = false

    /** 연타를 알아보려고 최근 누른 시각을 둔다. */
    private val tapTimes = ArrayDeque<Long>()

    /** 연타 반응 뒤 이 시각까지는 눌러도 반응하지 않는다. 귀찮아하는 동작이 바로 끊기지 않게 한다. */
    private var tapMutedUntil = 0L

    /** 마지막으로 홈에 들어오거나 냐미를 누른 시각. 한동안 그대로면 냐미가 졸려 한다. */
    private var lastInteractionAt = System.currentTimeMillis()

    /** debug 에서 고정한 분위기. null이면 자동. */
    private var forcedMood: CatMood? = null

    override fun onIntent(intent: HomeIntent) {
        when (intent) {
            HomeIntent.ScreenResumed -> {
                loadSummary()
                loadCatAnimationsIfNeeded()
                refreshCatWeightIfChanged()
                lastInteractionAt = System.currentTimeMillis()
                prepareGreeting()
            }
            HomeIntent.ScreenPaused -> rememberLastSeen()
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
            HomeIntent.ClickCat -> onCatTapped()
            is HomeIntent.CatLineRevealed -> dispatch(HomeReducerEvent.CatLineRevealed(intent.lineId))
            HomeIntent.ClickCatDebug -> cycleForcedMood()
            is HomeIntent.CatMotionFinished -> if (intent.playId == currentState.catPlayId) finishCatMotion()
            HomeIntent.CatShown -> onCatShown()
            HomeIntent.SummaryShown -> onSummaryShown()
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
                catPlayId = event.playId,
                catLine = event.line,
                catLineId = if (event.line != state.catLine) state.catLineId + 1 else state.catLineId,
                isCatLineRevealed = state.isCatLineRevealed && event.line == state.catLine,
                catMotion = event.motion,
                catDebugMotion = event.debugMotion,
                catDebugLabel = event.debugLabel,
            )
            is HomeReducerEvent.CatLineRevealed ->
                if (event.lineId == state.catLineId) state.copy(isCatLineRevealed = true) else state
            is HomeReducerEvent.CatMoodChanged -> state.copy(catMood = event.mood, catDebugLabel = event.debugLabel)
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
                if (recordedJustNow) pendingReaction = CatReaction.EAT
                driveCat()
            }
        }
    }

    /** 냐미 애니메이션을 아직 읽지 못했으면 읽는다. 실패하면 기본 냐미를 두고, 다음에 화면이 보일 때 다시 읽는다. */
    private fun loadCatAnimationsIfNeeded() {
        if (catAnimations != null || catAnimationsJob?.isActive == true) return
        // 냐미가 보이기까지(애니메이션 정보 + 스프라이트 시트). 다시 읽을 때는 처음 찍은 값이 유지된다.
        ttiHelper.startTTITimeline(TimelineCategory.IMAGE_LOADED_TIME)
        catAnimationsJob = viewModelScope.launch {
            getCatAnimations()
                .onSuccess {
                    catAnimations = it
                    dispatch(HomeReducerEvent.CatAnimationsLoaded)
                    driveCat()
                }
                .onFailure { dispatch(HomeReducerEvent.CatAnimationsLoadFailed) }
        }
    }

    /**
     * 고양이 조회로 체형을 다시 읽는다. 체형이 바뀌었으면 새 체형 애니메이션을 읽어 다음 동작부터 쓴다. 지금 동작은 끊지 않는다.
     * 실패하면 지금 체형을 그대로 쓴다.
     */
    private fun refreshCatWeightIfChanged() {
        viewModelScope.launch {
            if (!refreshCatWeight()) return@launch
            getCatAnimations().onSuccess {
                catAnimations = it
                upcoming = null
            }
        }
    }

    /**
     * 냐미를 움직일 준비가 됐으면(애니메이션) 지금 할 일을 정한다.
     * 미뤄 둔 반응(들어올 때 인사, 기록 직후 먹기)이 있으면 그것을, 아직 아무 동작도 없으면 일상 동작을 튼다.
     * 이미 움직이는 중이면 끊지 않는다. 낮·밤이 바뀌면 다음 동작을 고를 때 쓴다.
     */
    private fun driveCat() {
        val set = catAnimations ?: return
        val mood = currentMood()
        if (mood != currentState.catMood) dispatch(HomeReducerEvent.CatMoodChanged(mood, catDebugLabel(mood, currentState.catDebugMotion)))
        val reaction = pendingReaction
        when {
            reaction != null -> playReaction(reaction, set, mood)
            currentState.catMotion == null -> playMood(set, mood, newLine = true)
        }
    }

    /** 지금 분위기. debug 로 고정했으면 그것, 아니면 기기 시각과 마지막으로 건드린 뒤 지난 시간으로 정한다. */
    private fun currentMood(): CatMood =
        forcedMood ?: catBrain.moodOf(now(), idleMillis = System.currentTimeMillis() - lastInteractionAt)

    private fun now(): LocalDateTime = DeviceTime.now().toLocalDateTime()

    /** 분위기 상황에서 다음 동작을 고른다. [line]을 주면 그 대사를, 아니면 [newLine]일 때만 새로 고른다. */
    private fun playMood(set: CatAnimationSetVO, mood: CatMood, newLine: Boolean, line: String? = null) {
        val pick = upcoming?.takeIf { it.first == mood }?.second ?: catBrain.nextMotion(mood, set) ?: return
        upcoming = null
        val changeLine = line == null && (newLine || currentState.catLine == null || motionsSinceLine >= LINE_EVERY_MOTIONS)
        if (changeLine) motionsSinceLine = 0
        val shown = line ?: if (changeLine) catBrain.moodLine(now(), catLines) ?: currentState.catLine else currentState.catLine
        play(set, mood, pick, shown, isReaction = false)
    }

    /**
     * 반응 동작을 튼다. [keepLine]이면 대사는 그대로 두고, 아니면 반응 대사로 바꾼다. 반응 대사도 일상 대사처럼 동작 몇 번 동안 보인다.
     * 반응 동작이 없으면(그 체형에 아직 없는 상황) 대사만 바꾸고 분위기 동작을 이어 간다.
     */
    private fun playReaction(reaction: CatReaction, set: CatAnimationSetVO, mood: CatMood, keepLine: Boolean = false) {
        pendingReaction = null
        val pick = catBrain.reaction(reaction, set)
        val line = if (keepLine) null else catBrain.reactionLine(reaction, now(), catLines, pick?.motion)
        if (line != null) motionsSinceLine = 0
        if (pick == null) return playMood(set, mood, newLine = false, line = line)
        play(set, mood, pick, line ?: currentState.catLine, isReaction = true, label = reaction.name)
    }

    /**
     * 고른 동작을 화면에 넘긴다. 동작이 끝나면 앉은 자세로 짧게 멈췄다가 다음 동작으로 넘어간다.
     * 반응은 더 짧게 쉰다. 잠자는 동작은 고를 때 정한 만큼 조는 조각을 돈다.
     */
    private fun play(set: CatAnimationSetVO, mood: CatMood, pick: CatPick, line: String?, isReaction: Boolean, label: String? = null) {
        val playId = currentState.catPlayId + 1
        // 다음 일상 동작을 미리 골라 그 시트를 지금 읽어 둔다. 다음 동작이 시트를 읽느라 멈칫하지 않게 한다.
        val next = upcoming?.takeIf { it.first == mood }?.second ?: catBrain.nextMotion(mood, set)
        upcoming = next?.let { mood to it }
        val nextUrls = next?.motion?.clips?.map { it.url }.orEmpty()
        playingMood = !isReaction
        val motionLabel = "$pick" + (label?.let { " ($it)" } ?: "")
        dispatch(
            HomeReducerEvent.CatMotionChanged(
                state = pick.state,
                playId = playId,
                line = line,
                motion = pick.toHomeCatMotion(
                    playId = playId,
                    restMillis = if (isReaction) REACTION_REST_MILLIS else catBrain.restMillis(mood),
                    prefetchUrls = nextUrls,
                ),
                debugMotion = motionLabel,
                debugLabel = catDebugLabel(mood, motionLabel),
            ),
        )
    }

    /** 동작과 쉬는 시간이 끝나면 다음 동작을 고른다. */
    private fun finishCatMotion() {
        val set = catAnimations ?: return
        val mood = currentMood()
        if (playingMood) motionsSinceLine++
        pendingReaction?.let { return playReaction(it, set, mood) }
        playMood(set, mood, newLine = false)
    }

    /**
     * 냐미를 눌렀다. 바로 짧게 반응한다. 대사가 아직 찍히는 중이면 그대로 두고, 다 찍힌 뒤면 다음 대사로 넘어간다.
     * 2초 안에 다섯 번째 누름이면 귀찮아하거나 당황하고(대사도 바로 바꾼다), 그 뒤 1초 동안은 눌러도 반응하지 않는다.
     */
    private fun onCatTapped() {
        val set = catAnimations ?: return
        val now = System.currentTimeMillis()
        lastInteractionAt = now
        if (now < tapMutedUntil) return
        tapTimes.addLast(now)
        while (tapTimes.isNotEmpty() && now - tapTimes.first() > TAP_BURST_WINDOW_MILLIS) tapTimes.removeFirst()
        if (tapTimes.size >= TAP_BURST_COUNT) {
            tapTimes.clear()
            tapMutedUntil = now + TAP_MUTE_AFTER_BURST_MILLIS
            // 연타에는 대사가 찍히는 중이어도 바로 반응 대사로 바꾼다. 동작과 대사가 어긋나지 않게 한다.
            playReaction(CatReaction.IRRITATED, set, currentMood())
        } else {
            playReaction(CatReaction.TAP, set, currentMood(), keepLine = !currentState.isCatLineRevealed)
        }
    }

    /** 들어올 때의 인사를 마지막 방문 시각으로 정해 두고, 대사 은행을 읽어 둔다. */
    private fun prepareGreeting() {
        viewModelScope.launch {
            if (catLines.isEmpty()) catLines = getCatLines()
            val lastSeen = runCatching { companionMemory.lastSeenAtMillis() }.getOrNull()
                ?.let { DeviceTime.atZone(it).toLocalDateTime() }
            if (pendingReaction == null) pendingReaction = catBrain.greetingOf(lastSeen, now())
            driveCat()
        }
    }

    /** 홈을 떠날 때 마지막 방문 시각을 남긴다. 몇 분 만에 돌아오면 다시 인사하지 않는다. */
    private fun rememberLastSeen() {
        viewModelScope.launch { runCatching { companionMemory.setLastSeenAtMillis(System.currentTimeMillis()) } }
    }

    /** debug 빌드에서 냐미 위에 보일 상태 표시. release 에서는 null이다. */
    private fun catDebugLabel(mood: CatMood, motion: String?): String? {
        if (!BuildConfig.DEBUG) return null
        val moodName = when (mood) {
            CatMood.DAY -> "낮"
            CatMood.DROWSY -> "졸림"
            CatMood.NIGHT -> "밤"
        }
        val weight = catAnimations?.weight?.let { " · $it" }.orEmpty()
        return "$moodName${if (forcedMood != null) "(고정)" else ""}$weight · ${motion ?: "-"}"
    }

    /** debug: 분위기를 자동 → 낮 → 졸림 → 밤 순서로 고정해 바로 비교한다. */
    private fun cycleForcedMood() {
        if (!BuildConfig.DEBUG) return
        val order = listOf(null, CatMood.DAY, CatMood.DROWSY, CatMood.NIGHT)
        forcedMood = order[(order.indexOf(forcedMood) + 1) % order.size]
        val set = catAnimations ?: return
        val mood = currentMood()
        dispatch(HomeReducerEvent.CatMoodChanged(mood, catDebugLabel(mood, currentState.catDebugMotion)))
        playMood(set, mood, newLine = true)
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

    private companion object {
        const val REACTION_REST_MILLIS = 600L
        const val TAP_BURST_WINDOW_MILLIS = 2_000L
        const val TAP_BURST_COUNT = 5
        const val TAP_MUTE_AFTER_BURST_MILLIS = 1_000L
        const val LINE_EVERY_MOTIONS = 2
    }
}
