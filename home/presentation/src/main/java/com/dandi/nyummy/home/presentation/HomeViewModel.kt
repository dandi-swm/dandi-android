package com.dandi.nyummy.home.presentation

import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.presentation.mvi.MviViewModel
import com.dandi.nyummy.home.domain.GetHomeSummaryUseCase
import com.dandi.nyummy.meal.domain.MealRecordPage
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val navigationHelper: NavigationHelper,
    private val getHomeSummary: GetHomeSummaryUseCase,
) : MviViewModel<HomeIntent, HomeUIState, HomeReducerEvent>(HomeUIState.empty) {

    private var summaryJob: Job? = null

    override fun onIntent(intent: HomeIntent) {
        when (intent) {
            // TODO: 지갑/우편/공지/설정/스트릭 화면이 추가되면 각 목적지로 연결한다.
            HomeIntent.ClickWallet -> Unit
            HomeIntent.ClickMail -> Unit
            HomeIntent.ClickNotice -> Unit
            HomeIntent.ClickSettings -> Unit
            HomeIntent.ClickStreak -> Unit
            // TODO: 공유 이미지 카드/마이룸 편집 모드/말풍선 재생 기능 구현 시 연결한다.
            HomeIntent.ClickShare -> Unit
            HomeIntent.ClickRoomEdit -> Unit
            HomeIntent.ClickSpeechReplay -> Unit
            HomeIntent.ClickAddMeal -> {
                navigationHelper.navigateTo(MealRecordPage)
            }
            HomeIntent.ToggleRoomActionMenu ->
                dispatch(
                    HomeReducerEvent.RoomActionMenuExpansionChanged(
                        expanded = !currentState.isRoomActionMenuExpanded,
                    ),
                )
            HomeIntent.ClickTodaySummary ->
                dispatch(HomeReducerEvent.TodaySummarySheetVisibilityChanged(visible = true))

            HomeIntent.DismissTodaySummarySheet ->
                dispatch(HomeReducerEvent.TodaySummarySheetVisibilityChanged(visible = false))

            HomeIntent.ScreenResumed -> loadSummary()
        }
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

    override fun reduce(state: HomeUIState, event: HomeReducerEvent): HomeUIState =
        when (event) {
            is HomeReducerEvent.SummaryLoaded -> state.copy(summary = event.summary)

            is HomeReducerEvent.TodaySummarySheetVisibilityChanged ->
                state.copy(isTodaySummarySheetVisible = event.visible)

            is HomeReducerEvent.RoomActionMenuExpansionChanged ->
                state.copy(isRoomActionMenuExpanded = event.expanded)
        }
}
