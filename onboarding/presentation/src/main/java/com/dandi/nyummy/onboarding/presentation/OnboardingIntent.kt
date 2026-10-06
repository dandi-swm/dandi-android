package com.dandi.nyummy.onboarding.presentation

import com.dandi.nyummy.common.presentation.mvi.MviIntent
import com.dandi.nyummy.common.entity.meal.Meal
import com.dandi.nyummy.common.entity.meal.MealTimeVO

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

    /** 끼니 한 줄을 눌러 시간 시트를 연다. */
    data class ClickMealTime(val meal: Meal) : OnboardingIntent

    /** 시간 시트에서 시각을 정하거나 "안 먹어요"를 골랐다. */
    data class SelectMealTime(val meal: Meal, val time: MealTimeVO) : OnboardingIntent

    data object DismissMealTimeSheet : OnboardingIntent

    /** "이 시간에 알려 줘". 알림 권한 요청이 끝난 뒤에 보낸다. */
    data object ConfirmMealTimes : OnboardingIntent
}
