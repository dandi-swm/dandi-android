package com.dandi.nyummy.intro.presentation

import androidx.lifecycle.viewModelScope
import com.dandi.nyummy.common.domain.helper.AppPermission
import com.dandi.nyummy.common.presentation.mvi.MviViewModel
import com.dandi.nyummy.intro.domain.GetIntroUseCase
import com.dandi.nyummy.tti.TTIHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 시작 분기 ViewModel.
 *
 * 생성 즉시 [GetIntroUseCase] 가 (최초 1회) 권한 안내 → 버전 체크 → 홈/로그인 루트 전환을
 * 수행한다. 다이얼로그/네비게이션은 UseCase(domain)에서 종결되고, 권한 안내 바텀시트와
 * 시스템 권한 팝업만 UI 게이트([awaitPermissionFlow])로 이 레이어가 담당한다.
 */
@HiltViewModel
class IntroViewModel @Inject constructor(
    private val useCase: GetIntroUseCase,
    private val ttiHelper: TTIHelper,
) : MviViewModel<IntroIntent, IntroUIState, IntroReducerEvent>(IntroUIState.empty) {

    private var permissionResult: CompletableDeferred<Unit>? = null

    init {
        ttiHelper.startTTITracking(IntroTTIPage)
        start()
    }

    override fun onIntent(intent: IntroIntent) {
        when (intent) {
            IntroIntent.OnPermissionsResult -> permissionResult?.complete(Unit)
        }
    }

    override fun reduce(state: IntroUIState, event: IntroReducerEvent): IntroUIState =
        when (event) {
            is IntroReducerEvent.PermissionNoticeShown -> state.copy(
                showPermissionNotice = true,
                pendingPermissions = event.permissions,
            )
            IntroReducerEvent.PermissionNoticeFinished -> state.copy(
                showPermissionNotice = false,
                pendingPermissions = emptyList(),
            )
            IntroReducerEvent.SplashCompleted -> state.copy(isSplashComplete = true)
        }

    /** 시작 흐름 실행. 실패 시 UseCase 가 띄우는 다이얼로그의 재시도 버튼이 이 함수를 다시 부른다. */
    private fun start() {
        viewModelScope.launch {
            useCase(
                onRetry = ::start,
            requestPermissions = ::awaitPermissionFlow,
                onBeforeNavigate = ::completeSplash,
                ttiPage = IntroTTIPage,
            )
            ttiHelper.endTTITracking(IntroTTIPage)
            ttiHelper.shotTTILogging(IntroTTIPage)
        }
    }

    /** 진행바를 100% 로 채우고 채움 애니메이션이 끝날 때까지 잠깐 기다린 뒤 이동을 허용한다. */
    private suspend fun completeSplash() {
        dispatch(IntroReducerEvent.SplashCompleted)
        delay(SPLASH_COMPLETE_ANIMATION_WAIT_MS)
    }

    /** 권한 안내 게이트 — 바텀시트를 띄우고 시스템 요청 결과가 돌아올 때까지 suspend 한다. */
    private suspend fun awaitPermissionFlow(permissions: List<AppPermission>) {
        val deferred = CompletableDeferred<Unit>()
        permissionResult = deferred
        dispatch(IntroReducerEvent.PermissionNoticeShown(permissions))
        deferred.await()
        dispatch(IntroReducerEvent.PermissionNoticeFinished)
    }

    companion object {
        /** 100% 채움 애니메이션(250ms) + 여유. 화면 전환이 이만큼 늦어지는 의도된 지연. */
        private const val SPLASH_COMPLETE_ANIMATION_WAIT_MS = 500L
    }
}
