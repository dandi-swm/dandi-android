package com.dandi.nyummy.home.presentation

import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.presentation.mvi.MviViewModel
import com.dandi.nyummy.home.presentation.mock.HomeMockData
import com.dandi.nyummy.meal.domain.MealRecordPage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    val navigationHelper: NavigationHelper
) :
    MviViewModel<HomeIntent, HomeUIState, HomeReducerEvent>(HomeUIState.empty) {

    init {
        // TODO: 홈 요약 API 연동 시 UseCase 호출로 교체한다.
        dispatch(HomeReducerEvent.SummaryLoaded(HomeMockData.summary))
    }

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
            HomeIntent.ClickFeed -> {
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
