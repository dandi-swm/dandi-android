package com.dandi.nyummy.intro.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dandi.nyummy.intro.domain.GetIntroUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 시작 분기 전용 최소 ViewModel — 화면 상태가 없어 MVI 를 적용하지 않는 유일한 예외.
 * 생성 즉시 [GetIntroUseCase] 가 버전 체크 후 홈/로그인으로 루트 전환하거나
 * 강제 업데이트 다이얼로그를 띄운다. 다이얼로그/네비게이션은 UseCase(domain)에서 종결된다.
 */
@HiltViewModel
class IntroViewModel @Inject constructor(
    private val useCase: GetIntroUseCase,
) : ViewModel() {

    init {
        viewModelScope.launch {
            useCase()
        }
    }
}
