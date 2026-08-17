package com.dandi.nyummy.intro.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dandi.nyummy.common.domain.error.HttpResponseException
import com.dandi.nyummy.common.domain.error.handlingErrorOnUseCase
import com.dandi.nyummy.intro.domain.GetIntroUseCase
import com.dandi.nyummy.intro.domain.IntroErrorType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 시작 분기 전용 최소 ViewModel — 화면 상태가 없어 MVI 를 적용하지 않는 유일한 예외.
 * 생성 즉시 [GetIntroUseCase] 가 홈/로그인으로 루트 전환한다.
 */
@HiltViewModel
class IntroViewModel @Inject constructor(
    private val useCase: GetIntroUseCase,
) : ViewModel() {

    init {
        viewModelScope.launch {
            useCase().onFailure(::handleIntroPageError)
        }
    }

    private fun handleIntroPageError(throwable: Throwable) {
        val exception = throwable as? HttpResponseException ?: return
        exception.handlingErrorOnUseCase<IntroErrorType>()?.let { errorType ->
            // 다이얼로그/네비게이션은 UseCase(domain)에서 끝났다.
            // 화면 상태 반영이 필요해지면 여기서 errorType 별로 처리한다.
            when (errorType) {
                IntroErrorType.REQUIRED_FORCE_UPDATE -> Unit
            }
        }
    }
}
