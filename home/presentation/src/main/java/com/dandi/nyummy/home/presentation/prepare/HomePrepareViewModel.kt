package com.dandi.nyummy.home.presentation.prepare

import androidx.lifecycle.viewModelScope
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.presentation.mvi.MviViewModel
import com.dandi.nyummy.home.domain.HomePage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 로그인 직후 냐미 방 준비 ViewModel. 로그인과 온보딩 완료 뒤에 거친다.
 *
 * 생성 즉시 진행바를 채운 뒤 홈을 루트로 연다.
 */
@HiltViewModel
class HomePrepareViewModel @Inject constructor(
    private val navigationHelper: NavigationHelper,
) : MviViewModel<HomePrepareIntent, HomePrepareUIState, HomePrepareReducerEvent>(HomePrepareUIState.empty) {

    init {
        viewModelScope.launch {
            dispatch(HomePrepareReducerEvent.PrepareCompleted)
            delay(COMPLETE_ANIMATION_WAIT_MS)
            navigationHelper.navigateToAsRoot(HomePage)
        }
    }

    override fun onIntent(intent: HomePrepareIntent) = Unit

    override fun reduce(state: HomePrepareUIState, event: HomePrepareReducerEvent): HomePrepareUIState =
        when (event) {
            HomePrepareReducerEvent.PrepareCompleted -> state.copy(isComplete = true)
        }

    companion object {
        /** 100% 채움 애니메이션(250ms) + 여유. 인트로와 같다. 화면 전환이 이만큼 늦어지는 의도된 지연. */
        internal const val COMPLETE_ANIMATION_WAIT_MS = 500L
    }
}
