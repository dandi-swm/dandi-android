package com.dandi.nyummy.home.presentation.prepare

import androidx.lifecycle.viewModelScope
import com.dandi.nyummy.cat.domain.PrefetchCatAnimationsUseCase
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.presentation.mvi.MviViewModel
import com.dandi.nyummy.home.domain.HomePage
import com.dandi.nyummy.home.domain.PrefetchHomeNoticesUseCase
import com.dandi.nyummy.home.domain.PrefetchHomeSummaryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

/**
 * 로그인 직후 냐미 방 준비 ViewModel. 로그인과 온보딩 완료 뒤에 거친다.
 *
 * 생성 즉시 냐미 애니메이션과 앱이 재생할 시트 그림, 홈에서 띄울 새 소식 공지의 사진을 받고, 끝나면 진행바를 채운 뒤 홈을 루트로 연다.
 * 공지 사진은 인트로가 홈으로 갈 때 받는데, 로그인을 거쳐 온 회원은 인트로에서 받지 않았으므로 여기서 받는다.
 * 홈 요약도 함께 받기 시작하되 기다리지 않는다. 홈이 열리면 그 요청을 이어받는다.
 * 받지 못해도 홈으로 간다. 받는 중이면 홈이 그 요청을 이어받고, 실패했으면 홈은 기본 냐미를 보이다가 다음에 화면이 보일 때 다시 받는다.
 * 네트워크가 느려도 화면에 갇히지 않게 [PREFETCH_TIMEOUT_MS]까지만 기다린다.
 */
@HiltViewModel
class HomePrepareViewModel @Inject constructor(
    private val prefetchHomeSummary: PrefetchHomeSummaryUseCase,
    private val prefetchCatAnimations: PrefetchCatAnimationsUseCase,
    private val prefetchHomeNotices: PrefetchHomeNoticesUseCase,
    private val navigationHelper: NavigationHelper,
) : MviViewModel<HomePrepareIntent, HomePrepareUIState, HomePrepareReducerEvent>(HomePrepareUIState.empty) {

    init {
        // 홈이 열린 뒤에야 요약을 부르지 않게 지금 받기 시작한다.
        prefetchHomeSummary()
        viewModelScope.launch {
            withTimeoutOrNull(PREFETCH_TIMEOUT_MS) {
                coroutineScope {
                    launch { prefetchCatAnimations() }
                    launch { prefetchHomeNotices() }
                }
            }
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
        /** 냐미와 공지 사진 미리 받기를 기다리는 상한. 다 받지 못한 것은 홈이 이어서 받는다. */
        internal const val PREFETCH_TIMEOUT_MS = 3_000L

        /** 100% 채움 애니메이션(250ms) + 여유. 인트로와 같다. 화면 전환이 이만큼 늦어지는 의도된 지연. */
        internal const val COMPLETE_ANIMATION_WAIT_MS = 500L
    }
}
