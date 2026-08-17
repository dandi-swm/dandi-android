package com.dandi.nyummy.intro.presentation

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

/**
 * 앱 시작 부트스트랩 화면. 렌더링할 UI 없이 [IntroViewModel] 생성만 트리거하며,
 * ViewModel 이 즉시 홈/로그인으로 루트 전환하므로 사용자에게 보이지 않는다.
 */
@Composable
fun IntroPage(
    viewModel: IntroViewModel = hiltViewModel(),
) {
}
