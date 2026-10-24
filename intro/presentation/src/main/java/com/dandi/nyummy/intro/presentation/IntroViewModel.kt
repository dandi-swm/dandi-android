package com.dandi.nyummy.intro.presentation

import androidx.lifecycle.viewModelScope
import com.dandi.nyummy.cat.domain.PrefetchCatAnimationsUseCase
import com.dandi.nyummy.common.presentation.mvi.MviViewModel
import com.dandi.nyummy.intro.domain.GetIntroUseCase
import com.dandi.nyummy.intro.domain.tti.IntroTTIPage
import com.dandi.nyummy.tti.TTIHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

/**
 * 시작 분기 ViewModel.
 *
 * 생성 즉시 [GetIntroUseCase] 가 (최초 1회) 권한 안내 → 버전 체크 → 홈/로그인 루트 전환을
 * 수행한다. 다이얼로그/네비게이션은 UseCase(domain)에서 종결되고, 권한 안내 바텀시트만
 * UI 게이트([awaitPermissionNotice])로 이 레이어가 담당한다. 시스템 권한 팝업은 여기서 띄우지 않는다.
 */
@HiltViewModel
class IntroViewModel @Inject constructor(
    private val useCase: GetIntroUseCase,
    private val prefetchCatAnimations: PrefetchCatAnimationsUseCase,
    private val ttiHelper: TTIHelper,
) : MviViewModel<IntroIntent, IntroUIState, IntroReducerEvent>(IntroUIState.empty) {

    private var permissionNoticeConfirmed: CompletableDeferred<Unit>? = null

    /**
     * 홈 냐미가 쓸 애니메이션과 시트 그림을 받는 작업. 인트로는 이것이 끝난 뒤에 넘어간다.
     * 리프레시 토큰이 없으면(로그인 전) 받지 않고 바로 끝난다. 로그인한 뒤에는 방 준비 화면이 받는다.
     */
    private val catPrefetch: Job

    init {
        ttiHelper.startTTITracking(IntroTTIPage)
        // 권한 안내, 원격 설정, 버전 확인과 함께 받기 시작해 기다리는 시간을 줄인다.
        catPrefetch = viewModelScope.launch { prefetchCatAnimations() }
        start()
    }

    override fun onIntent(intent: IntroIntent) {
        when (intent) {
            IntroIntent.ConfirmPermissionNotice -> permissionNoticeConfirmed?.complete(Unit)
        }
    }

    override fun reduce(state: IntroUIState, event: IntroReducerEvent): IntroUIState =
        when (event) {
            IntroReducerEvent.PermissionNoticeShown -> state.copy(showPermissionNotice = true)
            IntroReducerEvent.PermissionNoticeFinished -> state.copy(showPermissionNotice = false)
            IntroReducerEvent.SplashCompleted -> state.copy(isSplashComplete = true)
        }

    /** 시작 흐름 실행. 실패 시 UseCase 가 띄우는 다이얼로그의 재시도 버튼이 이 함수를 다시 부른다. */
    private fun start() {
        viewModelScope.launch {
            useCase(
                onRetry = ::start,
                requestPermissions = { awaitPermissionNotice() },
                onBeforeNavigate = ::completeSplash,
            ).onSuccess {
                // 끝 표시는 UseCase가 버전 확인 직후에 한다. 실패하면 재시도 결과까지 기다리도록 보내지 않는다.
                ttiHelper.shotTTILogging()
            }
        }
    }

    /**
     * 냐미 미리 받기가 끝나길 기다린 뒤 진행바를 100% 로 채우고, 채움 애니메이션이 끝날 때까지 잠깐 기다린 뒤 이동을 허용한다.
     * 네트워크가 느려도 인트로에 갇히지 않게 [CAT_PREFETCH_TIMEOUT_MS]까지만 기다린다. 다 받지 못한 것은 홈이 이어서 받는다.
     */
    private suspend fun completeSplash() {
        withTimeoutOrNull(CAT_PREFETCH_TIMEOUT_MS) { catPrefetch.join() }
        dispatch(IntroReducerEvent.SplashCompleted)
        delay(SPLASH_COMPLETE_ANIMATION_WAIT_MS)
    }

    /**
     * 권한 안내 게이트 — 바텀시트를 띄우고 사용자가 확인할 때까지 suspend 한다.
     * 맥락 없이 미리 요청해 거부되면, 기능에서 다시 물을 때 한 번 더 거부해 "다시 묻지 않음"이 되기 쉬워 여기서는 요청하지 않는다.
     */
    private suspend fun awaitPermissionNotice() {
        val deferred = CompletableDeferred<Unit>()
        permissionNoticeConfirmed = deferred
        dispatch(IntroReducerEvent.PermissionNoticeShown)
        deferred.await()
        dispatch(IntroReducerEvent.PermissionNoticeFinished)
    }

    companion object {
        /** 100% 채움 애니메이션(250ms) + 여유. 화면 전환이 이만큼 늦어지는 의도된 지연. */
        private const val SPLASH_COMPLETE_ANIMATION_WAIT_MS = 500L

        /** 냐미 미리 받기를 기다리는 상한. 방 준비 화면과 같다. */
        private const val CAT_PREFETCH_TIMEOUT_MS = 3_000L
    }
}
