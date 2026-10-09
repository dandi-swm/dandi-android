package com.dandi.nyummy.home.presentation

import androidx.lifecycle.viewModelScope
import com.dandi.nyummy.cat.domain.CatMotionPicker
import com.dandi.nyummy.cat.domain.GetCatAnimationsUseCase
import com.dandi.nyummy.cat.entity.CatAnimationSetVO
import com.dandi.nyummy.cat.entity.CatState
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.entity.time.KstTime
import com.dandi.nyummy.common.presentation.mvi.MviViewModel
import com.dandi.nyummy.history.domain.GetDailyMealsUseCase
import com.dandi.nyummy.home.domain.GetHomeSummaryUseCase
import com.dandi.nyummy.home.domain.tti.HomeTTIPage
import com.dandi.nyummy.mailbox.domain.MailboxPage
import com.dandi.nyummy.meal.domain.MealRecordPage
import com.dandi.nyummy.settings.domain.SettingsPage
import com.dandi.nyummy.tti.TTIHelper
import com.dandi.nyummy.tti.TimelineCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val navigationHelper: NavigationHelper,
    private val getHomeSummary: GetHomeSummaryUseCase,
    private val getDailyMeals: GetDailyMealsUseCase,
    private val getCatAnimations: GetCatAnimationsUseCase,
    private val catMotionPicker: CatMotionPicker,
    private val ttiHelper: TTIHelper,
) : MviViewModel<HomeIntent, HomeUIState, HomeReducerEvent>(HomeUIState.empty) {

    init {
        // 홈 TTI: 요약 숫자와 냐미가 둘 다 보일 때까지. 요약 API 구간은 GetHomeSummaryUseCase 가 찍는다.
        ttiHelper.startTTITracking(HomeTTIPage)
    }

    private var summaryJob: Job? = null
    private var todayMealsJob: Job? = null
    private var catAnimationsJob: Job? = null

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
            // TODO: 지갑, 공지, 설정, 스트릭 화면이 생기면 각 목적지로 연결한다.
            HomeIntent.ClickWallet -> Unit
            HomeIntent.ClickMail -> navigationHelper.navigateTo(MailboxPage)
            HomeIntent.ClickSettings -> navigationHelper.navigateTo(SettingsPage)
            HomeIntent.ClickStreak -> Unit
            // TODO: 마이룸, 친구에게 공유, 냐미 상태 화면이 생기면 연결한다.
            HomeIntent.ClickMyRoom -> Unit
            HomeIntent.ClickShareFriend -> Unit
            HomeIntent.ClickNyamiStatus -> Unit
            HomeIntent.ToggleRoomMenu ->
                dispatch(HomeReducerEvent.RoomMenuExpansionChanged(expanded = !currentState.isRoomMenuExpanded))
            HomeIntent.ClickTodayBar -> clickTodayBar()
            HomeIntent.DismissTodaySheet ->
                dispatch(HomeReducerEvent.TodaySheetVisibilityChanged(visible = false))
            HomeIntent.RetryTodayMeals -> loadTodayMeals()
            HomeIntent.ClickAddMeal -> {
                dispatch(HomeReducerEvent.TodaySheetVisibilityChanged(visible = false))
                navigationHelper.navigateTo(MealRecordPage)
            }
            HomeIntent.ClickCat -> currentState.catState?.let { startCatMotion(it, newLine = true) }
            is HomeIntent.CatMotionFinished -> if (intent.playId == currentState.catPlayId) finishCatMotion()
            HomeIntent.CatShown -> onCatShown()
            HomeIntent.SummaryShown -> onSummaryShown()
        }
    }

    override fun reduce(state: HomeUIState, event: HomeReducerEvent): HomeUIState =
        when (event) {
            is HomeReducerEvent.SummaryLoaded -> state.copy(summary = event.summary, isSummaryLoaded = true)
            is HomeReducerEvent.TodaySheetVisibilityChanged -> state.copy(isTodaySheetVisible = event.visible)
            HomeReducerEvent.TodayMealsLoadStarted -> state.copy(isTodayMealsLoading = true, isTodayMealsFailed = false)
            is HomeReducerEvent.TodayMealsLoaded -> state.copy(
                todayNutrition = event.meals.nutrition,
                todayMeals = event.meals.meals.toImmutableList(),
                isTodayMealsLoading = false,
                isTodayMealsFailed = false,
            )
            HomeReducerEvent.TodayMealsLoadFailed -> state.copy(isTodayMealsLoading = false, isTodayMealsFailed = true)
            is HomeReducerEvent.RoomMenuExpansionChanged -> state.copy(isRoomMenuExpanded = event.expanded)
            HomeReducerEvent.CatAnimationsLoaded -> state.copy(isCatAnimationFailed = false)
            HomeReducerEvent.CatAnimationsLoadFailed -> state.copy(isCatAnimationFailed = true)
            is HomeReducerEvent.CatMotionChanged -> state.copy(
                catState = event.state,
                catGroup = event.group,
                catPlayId = event.playId,
                catLine = event.line,
                catMotion = event.motion,
            )
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
                dispatch(HomeReducerEvent.SummaryLoaded(summary))
                updateCatState(recordedJustNow)
            }
        }
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

    /** 오늘 기록이 없으면 바로 식사 기록으로, 있으면 오늘 식사 시트를 열고 내용을 읽는다. */
    private fun clickTodayBar() {
        if (!currentState.hasRecordedToday) {
            navigationHelper.navigateTo(MealRecordPage)
            return
        }
        dispatch(HomeReducerEvent.TodaySheetVisibilityChanged(visible = true))
        loadTodayMeals()
    }

    /** 오늘(KST) 식사 목록과 탄단지 합계를 읽는다. 시트를 열 때마다 새로 읽어 방금 기록한 식사도 보이게 한다. */
    private fun loadTodayMeals() {
        todayMealsJob?.cancel()
        dispatch(HomeReducerEvent.TodayMealsLoadStarted)
        val today = KstTime.now()
        todayMealsJob = viewModelScope.launch {
            getDailyMeals(today.year, today.month, today.day)
                .onSuccess { dispatch(HomeReducerEvent.TodayMealsLoaded(it)) }
                .onFailure { dispatch(HomeReducerEvent.TodayMealsLoadFailed) }
        }
    }
}
