package com.dandi.nyummy.legal.presentation

import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.presentation.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class LegalViewModel @Inject constructor(
    private val navigationHelper: NavigationHelper,
) : MviViewModel<LegalIntent, LegalUIState, LegalReducerEvent>(LegalUIState.empty) {

    override fun onIntent(intent: LegalIntent) {
        when (intent) {
            LegalIntent.PageStarted -> dispatch(LegalReducerEvent.LoadStarted)
            LegalIntent.PageFinished -> dispatch(LegalReducerEvent.LoadFinished)
            LegalIntent.PageFailed -> dispatch(LegalReducerEvent.LoadFailed)
            is LegalIntent.HistoryChanged ->
                dispatch(LegalReducerEvent.HistoryChanged(policyPageOf(intent.url), intent.canGoBack))
            is LegalIntent.ClickExternalLink -> navigationHelper.navigateToExternalLink(intent.url)
            LegalIntent.ClickClose -> navigationHelper.navigateToBack()
        }
    }

    override fun reduce(state: LegalUIState, event: LegalReducerEvent): LegalUIState = when (event) {
        LegalReducerEvent.LoadStarted -> state.copy(loadState = LegalLoadState.Loading)
        // 본문을 읽지 못한 페이지도 다 읽었다고 알려 오므로, 실패는 다시 읽기 시작할 때까지 그대로 둔다.
        LegalReducerEvent.LoadFinished ->
            if (state.loadState == LegalLoadState.Failed) state else state.copy(loadState = LegalLoadState.Loaded)
        LegalReducerEvent.LoadFailed -> state.copy(loadState = LegalLoadState.Failed)
        is LegalReducerEvent.HistoryChanged -> state.copy(shownPage = event.shownPage, canGoBack = event.canGoBack)
    }
}
