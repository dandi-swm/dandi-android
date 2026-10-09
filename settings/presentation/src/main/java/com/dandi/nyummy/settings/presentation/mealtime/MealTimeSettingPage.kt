package com.dandi.nyummy.settings.presentation.mealtime

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dandi.nyummy.common.entity.meal.Meal
import com.dandi.nyummy.common.entity.meal.MealTimesVO
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyBottomCta
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyMealTimeField
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyMealTimeSheet
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTopBar
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.settings.presentation.R

/**
 * 평소 식사 시간. 온보딩과 같은 드롭다운 세 줄(아침, 점심, 저녁)이고, 줄을 누르면 시간 시트가 열린다.
 * 정시 단위로만 고르고, 저장을 눌러야 기기에 저장한다.
 */
@Composable
fun MealTimeSettingPage(
    modifier: Modifier = Modifier,
    viewModel: MealTimeSettingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.onIntent(MealTimeSettingIntent.Enter)
    }

    MealTimeSettingScreen(
        uiState = uiState,
        onIntent = viewModel::onIntent,
        modifier = modifier,
    )
}

@Composable
internal fun MealTimeSettingScreen(
    uiState: MealTimeSettingUIState,
    onIntent: (MealTimeSettingIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NyummyTheme.colors.bg.canvas),
    ) {
        NyummyTopBar(
            title = stringResource(R.string.settings_meal_time_title),
            onBackClick = { onIntent(MealTimeSettingIntent.ClickBack) },
        )
        if (uiState.isLoaded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .wrapContentWidth(Alignment.CenterHorizontally)
                    .widthIn(max = ContentMaxWidth)
                    .padding(horizontal = NyummyTheme.spacing.gutter)
                    .padding(top = NyummyTheme.spacing.s16, bottom = NyummyTheme.spacing.s24),
                verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s8),
            ) {
                Meal.entries.forEach { meal ->
                    NyummyMealTimeField(
                        meal = meal,
                        time = uiState.mealTimes[meal],
                        onClick = { onIntent(MealTimeSettingIntent.ClickMeal(meal)) },
                    )
                }
            }
        } else {
            Spacer(Modifier.weight(1f))
        }
        NyummyBottomCta(
            primaryText = stringResource(R.string.settings_meal_time_save),
            onPrimaryClick = { onIntent(MealTimeSettingIntent.ClickSave) },
            primaryEnabled = uiState.isLoaded && !uiState.isSaving,
        )
    }

    uiState.editingMeal?.let { meal ->
        NyummyMealTimeSheet(
            meal = meal,
            initial = uiState.mealTimes[meal],
            onSelect = { onIntent(MealTimeSettingIntent.SelectMealTime(meal, it)) },
            onDismissRequest = { onIntent(MealTimeSettingIntent.DismissMealTimeSheet) },
        )
    }
}

private val ContentMaxWidth = 480.dp

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun MealTimeSettingScreenPreview() {
    NyummyTheme {
        MealTimeSettingScreen(
            uiState = MealTimeSettingUIState(
                mealTimes = MealTimesVO(breakfast = MealTimesVO.DefaultBreakfast.copy(isSkipped = true)),
                isLoaded = true,
            ),
            onIntent = {},
        )
    }
}
