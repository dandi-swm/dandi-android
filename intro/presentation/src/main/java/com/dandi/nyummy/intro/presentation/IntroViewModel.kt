package com.dandi.nyummy.intro.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dandi.nyummy.intro.domain.GetIntroUseCase
import com.dandi.nyummy.tti.TTIHelper
import com.dandi.nyummy.tti.TimelineCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 시작 분기 전용 ViewModel
 *
 * 생성 즉시 [GetIntroUseCase] 가 버전 체크 후 홈/로그인으로 루트 전환하거나
 * 강제 업데이트 다이얼로그를 띄운다. 다이얼로그/네비게이션은 UseCase(domain)에서 종결된다.
 */
@HiltViewModel
class IntroViewModel @Inject constructor(
    private val useCase: GetIntroUseCase,
    private val ttiHelper: TTIHelper,
) : ViewModel() {

    init {
        ttiHelper.startTTITracking(IntroTTIPage)
        start()
    }

    /** 시작 흐름 실행. 실패 시 UseCase 가 띄우는 다이얼로그의 재시도 버튼이 이 함수를 다시 부른다. */
    private fun start() {
        viewModelScope.launch {
            ttiHelper.startTTITimeline(IntroTTIPage, TimelineCategory.API_RESPONSE_TIME)
            useCase(onRetry = ::start)
            ttiHelper.endTTITimeline(IntroTTIPage, TimelineCategory.API_RESPONSE_TIME)
            ttiHelper.endTTITracking(IntroTTIPage)
            ttiHelper.shotTTILogging(IntroTTIPage)
        }
    }
}
