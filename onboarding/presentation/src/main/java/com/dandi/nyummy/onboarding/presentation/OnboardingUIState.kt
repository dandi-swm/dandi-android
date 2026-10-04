package com.dandi.nyummy.onboarding.presentation

import com.dandi.nyummy.common.presentation.mvi.UiState
import com.dandi.nyummy.onboarding.domain.CatNameError

/**
 * 온보딩 진행 상태. 장면·대사 내용은 [OnboardingScript] 에 있고, 여기는 "어디까지 봤는지"만 들고 있다.
 *
 * @property sceneIndex [OnboardingScript.scenes] 의 현재 장면
 * @property lineIndex 현재 대사 목록(장면 대사 또는 고른 선택지의 반응 대사) 안의 위치
 * @property choiceIndex 현재 장면에서 고른 선택지. 고르기 전이면 null
 * @property isLineRevealed 현재 대사의 타이핑이 끝났는지
 * @property catNameInput 이름 입력창의 값
 * @property catName 등록이 끝난 고양이 이름. 등록 전에는 빈 문자열(이름표는 "???")
 */
data class OnboardingUIState(
    val sceneIndex: Int = 0,
    val lineIndex: Int = 0,
    val choiceIndex: Int? = null,
    val isLineRevealed: Boolean = false,
    val catNameInput: String = "",
    val catNameError: CatNameError? = null,
    val catName: String = "",
    val isSubmitting: Boolean = false,
) : UiState {
    companion object {
        val empty: OnboardingUIState = OnboardingUIState()
    }
}
