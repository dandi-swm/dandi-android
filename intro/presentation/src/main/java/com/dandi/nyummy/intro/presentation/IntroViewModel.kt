package com.dandi.nyummy.intro.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dandi.nyummy.intro.domain.GetIntroUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 시작 분기 전용 최소 ViewModel — 화면 상태가 없어 MVI 를 적용하지 않는 유일한 예외.
 * 생성 즉시 [GetIntroUseCase] 가 홈/로그인으로 루트 전환한다.
 */
@HiltViewModel
class IntroViewModel @Inject constructor(
    useCase: GetIntroUseCase,
) : ViewModel() {

    init {
        viewModelScope.launch {
            useCase()
        }
    }
}
