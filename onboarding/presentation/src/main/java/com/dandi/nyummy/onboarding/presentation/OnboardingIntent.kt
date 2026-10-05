package com.dandi.nyummy.onboarding.presentation

import com.dandi.nyummy.common.presentation.mvi.MviIntent

sealed interface OnboardingIntent : MviIntent {
    /** 화면 탭. 타이핑 중이면 대사를 끝까지 보여주고, 다 나왔으면 다음 대사·장면으로 넘어간다. */
    data object TapDialogue : OnboardingIntent

    /** 타이핑 애니메이션이 스스로 끝났다. */
    data object TypingFinished : OnboardingIntent

    data class SelectChoice(val index: Int) : OnboardingIntent

    data class InputCatName(val value: String) : OnboardingIntent

    data object SubmitCatName : OnboardingIntent

    data object Skip : OnboardingIntent

    data object ClickStart : OnboardingIntent
}
