package com.dandi.nyummy.onboarding.presentation.component

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.dandi.nyummy.common.entity.meal.Meal
import com.dandi.nyummy.common.entity.meal.MealTimesVO
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyButton
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyButtonSize
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyMealTimeField
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.onboarding.presentation.R

/**
 * 대화창 안에 들어가는 평소 식사 시각 입력. 아침, 점심, 저녁 세 줄에 기본값을 미리 채워 두어 그대로 넘어갈 수 있다.
 * 줄을 누르면 시간 시트([com.dandi.nyummy.common.presentation.designsystem.component.NyummyMealTimeSheet])가 열리고,
 * 맨 아래 버튼으로 마친다.
 */
@Composable
internal fun ColumnScope.OnboardingMealTimeSlot(
    mealTimes: MealTimesVO,
    enabled: Boolean,
    onClickMeal: (Meal) -> Unit,
    onDone: () -> Unit,
) {
    Meal.entries.forEach { meal ->
        NyummyMealTimeField(
            meal = meal,
            time = mealTimes[meal],
            enabled = enabled,
            onClick = { onClickMeal(meal) },
        )
        Spacer(Modifier.height(NyummyTheme.spacing.s8))
    }
    Spacer(Modifier.height(NyummyTheme.spacing.s8))
    NyummyButton(
        text = stringResource(R.string.onboarding_meal_done),
        onClick = onDone,
        size = NyummyButtonSize.M,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
    )
}
