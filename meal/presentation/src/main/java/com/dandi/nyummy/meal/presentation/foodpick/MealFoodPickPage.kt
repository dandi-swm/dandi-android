package com.dandi.nyummy.meal.presentation.foodpick

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyButton
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyButtonSize
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyButtonStyle
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyCircularProgress
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyIconButton
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyIconButtonStyle
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyText
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.meal.presentation.R
import com.dandi.nyummy.meal.presentation.component.FoodPickContent
import com.dandi.nyummy.common.presentation.R as CommonR

/**
 * 기록 탭의 확인 대기 식사에서 들어온 찾은 음식 고르기 화면.
 * 목록은 기록 직후와 같은 [FoodPickContent]를 쓰고, 불러오는 동안과 불러오지 못했을 때만 따로 그린다.
 */
@Composable
fun MealFoodPickPage(
    mealId: Long,
    modifier: Modifier = Modifier,
    viewModel: MealFoodPickViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(mealId) {
        viewModel.onIntent(MealFoodPickIntent.Enter(mealId))
    }
    MealFoodPickScreen(uiState = uiState, onIntent = viewModel::onIntent, modifier = modifier)
}

@Composable
internal fun MealFoodPickScreen(
    uiState: MealFoodPickUIState,
    onIntent: (MealFoodPickIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler { onIntent(MealFoodPickIntent.ClickClose) }
    when {
        uiState.isLoading || uiState.isLoadFailed -> FoodPickPlaceholder(
            isLoadFailed = uiState.isLoadFailed,
            onClose = { onIntent(MealFoodPickIntent.ClickClose) },
            onRetry = { onIntent(MealFoodPickIntent.ClickRetry) },
            modifier = modifier,
        )
        else -> FoodPickContent(
            draft = uiState.draft,
            isSubmitting = uiState.isSubmitting,
            onClose = { onIntent(MealFoodPickIntent.ClickClose) },
            onToggle = { onIntent(MealFoodPickIntent.ToggleFood(it)) },
            onOpenNameInput = { onIntent(MealFoodPickIntent.OpenNameInput) },
            onNameInputChange = { onIntent(MealFoodPickIntent.ChangeNameInput(it)) },
            onConfirmNameInput = { onIntent(MealFoodPickIntent.ConfirmNameInput) },
            onSubmit = { onIntent(MealFoodPickIntent.ClickSubmit) },
            modifier = modifier,
        )
    }
}

/** 찾은 음식을 불러오는 중이거나 불러오지 못했을 때. 닫기는 늘 둔다. */
@Composable
private fun FoodPickPlaceholder(
    isLoadFailed: Boolean,
    onClose: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NyummyTheme.colors.bg.surface)
            .statusBarsPadding(),
    ) {
        NyummyIconButton(
            icon = CommonR.drawable.nyummy_ic_x,
            contentDescription = stringResource(R.string.meal_record_close_content_description),
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(horizontal = NyummyTheme.spacing.s16, vertical = NyummyTheme.spacing.s8),
            style = NyummyIconButtonStyle.Ghost,
        )
        if (isLoadFailed) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = NyummyTheme.spacing.gutter),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s12),
            ) {
                NyummyText(
                    text = stringResource(R.string.meal_food_pick_load_failed),
                    style = NyummyTheme.typography.bodyL,
                    color = NyummyTheme.colors.content.secondary,
                    textAlign = TextAlign.Center,
                )
                NyummyButton(
                    text = stringResource(R.string.meal_food_pick_load_retry),
                    onClick = onRetry,
                    style = NyummyButtonStyle.Neutral,
                    size = NyummyButtonSize.S,
                )
            }
        } else {
            NyummyCircularProgress(modifier = Modifier.align(Alignment.Center))
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun MealFoodPickLoadingPreview() {
    NyummyTheme { MealFoodPickScreen(uiState = MealFoodPickUIState(), onIntent = {}) }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun MealFoodPickLoadFailedPreview() {
    NyummyTheme { MealFoodPickScreen(uiState = MealFoodPickUIState(isLoading = false, isLoadFailed = true), onIntent = {}) }
}
