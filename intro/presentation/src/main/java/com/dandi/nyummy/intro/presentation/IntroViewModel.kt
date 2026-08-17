package com.dandi.nyummy.intro.presentation

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * 시작 분기 전용 최소 ViewModel — 화면 상태가 없어 MVI 를 적용하지 않는 유일한 예외.
 */
@HiltViewModel
class IntroViewModel @Inject constructor() : ViewModel()
