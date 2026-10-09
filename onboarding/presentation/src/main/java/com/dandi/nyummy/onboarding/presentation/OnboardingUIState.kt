package com.dandi.nyummy.onboarding.presentation

import com.dandi.nyummy.common.presentation.mvi.UiState
import com.dandi.nyummy.common.entity.meal.Meal
import com.dandi.nyummy.common.entity.meal.MealTimesVO
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
 * @property mealTimes 평소 식사 시각. 기본값(오전 8시, 오후 12시, 오후 6시)을 미리 채워 둔다.
 * @property editingMeal 시간 시트를 열어 고치고 있는 끼니. 닫혀 있으면 null
 * @property isFinishing 식사 시각을 저장하고 홈으로 넘어가는 중
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
    val mealTimes: MealTimesVO = MealTimesVO.default,
    val editingMeal: Meal? = null,
    val isFinishing: Boolean = false,
) : UiState {
    companion object {
        val empty: OnboardingUIState = OnboardingUIState()
    }
}
