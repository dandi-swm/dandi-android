package com.dandi.nyummy.home.presentation

import androidx.lifecycle.viewModelScope
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.entity.time.KstTime
import com.dandi.nyummy.common.presentation.mvi.MviViewModel
import com.dandi.nyummy.history.domain.GetDailyMealsUseCase
import com.dandi.nyummy.home.domain.GetHomeSummaryUseCase
import com.dandi.nyummy.meal.domain.MealRecordPage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val navigationHelper: NavigationHelper,
    private val getHomeSummary: GetHomeSummaryUseCase,
    private val getDailyMeals: GetDailyMealsUseCase,
) : MviViewModel<HomeIntent, HomeUIState, HomeReducerEvent>(HomeUIState.empty) {

    private var summaryJob: Job? = null
    private var todayMealsJob: Job? = null

    override fun onIntent(intent: HomeIntent) {
        when (intent) {
            HomeIntent.ScreenResumed -> loadSummary()
            // TODO: 지갑, 우편, 공지, 설정, 스트릭 화면이 생기면 각 목적지로 연결한다.
            HomeIntent.ClickWallet -> Unit
            HomeIntent.ClickMail -> Unit
            HomeIntent.ClickNotice -> Unit
            HomeIntent.ClickSettings -> Unit
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
        }
    }

    override fun reduce(state: HomeUIState, event: HomeReducerEvent): HomeUIState =
        when (event) {
            is HomeReducerEvent.SummaryLoaded -> state.copy(summary = event.summary)
            is HomeReducerEvent.TodaySheetVisibilityChanged -> state.copy(isTodaySheetVisible = event.visible)
            HomeReducerEvent.TodayMealsLoadStarted -> state.copy(isTodayMealsLoading = true, isTodayMealsFailed = false)
            is HomeReducerEvent.TodayMealsLoaded ->
                state.copy(todayMeals = event.meals, isTodayMealsLoading = false, isTodayMealsFailed = false)
            HomeReducerEvent.TodayMealsLoadFailed -> state.copy(isTodayMealsLoading = false, isTodayMealsFailed = true)
            is HomeReducerEvent.RoomMenuExpansionChanged -> state.copy(isRoomMenuExpanded = event.expanded)
        }

    /**
     * 홈 요약을 다시 읽는다. 식사를 기록하고 돌아오면 숫자가 바뀌어야 하므로 화면이 보일 때마다 부른다.
     * 이전 요청이 남아 있으면 취소하고, 실패하면 직전 값을 그대로 둔다(안내는 UseCase가 한다).
     */
    private fun loadSummary() {
        summaryJob?.cancel()
        summaryJob = viewModelScope.launch {
            getHomeSummary().onSuccess { dispatch(HomeReducerEvent.SummaryLoaded(it)) }
        }
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
