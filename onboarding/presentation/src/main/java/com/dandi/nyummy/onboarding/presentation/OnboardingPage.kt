package com.dandi.nyummy.onboarding.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dandi.nyummy.common.presentation.component.DandiText
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemThemeImpl
import com.dandi.nyummy.onboarding.presentation.component.OnboardingCatNamePanel
import com.dandi.nyummy.onboarding.presentation.component.OnboardingChoiceList
import com.dandi.nyummy.onboarding.presentation.component.OnboardingDialogueBox
import com.dandi.nyummy.onboarding.presentation.component.OnboardingStage
import com.dandi.nyummy.onboarding.presentation.component.OnboardingStartButton

@Composable
fun OnboardingPage(viewModel: OnboardingViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    OnboardingScreen(uiState = uiState, onIntent = viewModel::onIntent)
}

/**
 * 냥줍 온보딩. 화면 어디를 탭해도 대사가 진행되고, 대사가 끝난 장면에서는 선택지·이름 입력·시작 버튼이 대사창 위에 뜬다.
 */
@Composable
internal fun OnboardingScreen(
    uiState: OnboardingUIState,
    onIntent: (OnboardingIntent) -> Unit,
) {
    val line = uiState.currentLine
    val action = uiState.currentScene.action
    val catDisplayName = uiState.catName.ifBlank { stringResource(R.string.onboarding_speaker_unknown_cat) }
    val speakerName = when (line.speaker) {
        OnboardingSpeaker.USER -> stringResource(R.string.onboarding_speaker_user)
        OnboardingSpeaker.CAT -> catDisplayName
        OnboardingSpeaker.NARRATOR -> ""
    }

    // 온보딩은 루트 화면이다. 이름 등록 중에는 뒤로가기로 앱이 닫히지 않게 막는다.
    BackHandler(enabled = uiState.isSubmitting) {}

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { onIntent(OnboardingIntent.TapDialogue) },
    ) {
        OnboardingStage(
            character = uiState.currentScene.character,
            isUserOnStage = uiState.isUserOnStage,
        )

        if (uiState.isSkipVisible) {
            SkipButton(
                onClick = { onIntent(OnboardingIntent.Skip) },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(DesignSystemThemeImpl.designSystemSpacing.space16),
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(
                    horizontal = DesignSystemThemeImpl.designSystemLayout.mobileGutter,
                    vertical = DesignSystemThemeImpl.designSystemSpacing.space16,
                ),
        ) {
            // 행동 패널은 "그 장면의" 행동을 그대로 들고 사라져야 한다. 현재 action 을 읽어 그리면 이름 등록 직후
            // 이름 입력 패널이 사라지는 동안 다음 장면의 시작 버튼이 잠깐 비친다.
            AnimatedContent(
                targetState = action.takeIf { uiState.isActionVisible },
                transitionSpec = {
                    (fadeIn(tween(220)) + slideInVertically(tween(220)) { it / 4 }) togetherWith
                        fadeOut(tween(120)) using SizeTransform(clip = false)
                },
                contentKey = { it?.let { shown -> shown::class } },
                label = "onboardingAction",
            ) { shownAction ->
                val panelModifier = Modifier.padding(bottom = DesignSystemThemeImpl.designSystemSpacing.space12)
                when (shownAction) {
                    is OnboardingSceneAction.Choice -> OnboardingChoiceList(
                        options = shownAction.options,
                        onSelect = { onIntent(OnboardingIntent.SelectChoice(it)) },
                        modifier = panelModifier,
                    )
                    OnboardingSceneAction.NameInput -> OnboardingCatNamePanel(
                        value = uiState.catNameInput,
                        error = uiState.catNameError,
                        isSubmitting = uiState.isSubmitting,
                        onValueChange = { onIntent(OnboardingIntent.InputCatName(it)) },
                        onSubmit = { onIntent(OnboardingIntent.SubmitCatName) },
                        modifier = panelModifier,
                    )
                    OnboardingSceneAction.Start -> OnboardingStartButton(
                        onClick = { onIntent(OnboardingIntent.ClickStart) },
                        modifier = panelModifier,
                    )
                    OnboardingSceneAction.None, null -> Unit
                }
            }

            OnboardingDialogueBox(
                speaker = line.speaker,
                speakerName = speakerName,
                text = stringResource(line.textRes, uiState.catName),
                lineKey = Triple(uiState.sceneIndex, uiState.choiceIndex, uiState.lineIndex),
                revealed = uiState.isLineRevealed,
                showContinueHint = uiState.isContinueHintVisible,
                onRevealed = { onIntent(OnboardingIntent.TypingFinished) },
            )
        }
    }
}

@Composable
private fun SkipButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        onClick = onClick,
        shape = DesignSystemThemeImpl.designSystemShape.pill,
        color = DesignSystemThemeImpl.designSystemColor.bgScrimDefault,
    ) {
        DandiText(
            text = stringResource(R.string.onboarding_skip),
            modifier = Modifier.padding(
                horizontal = DesignSystemThemeImpl.designSystemSpacing.space16,
                vertical = DesignSystemThemeImpl.designSystemSpacing.space8,
            ),
            color = DesignSystemThemeImpl.designSystemColor.contentInverseDefault,
            style = DesignSystemThemeImpl.typeScale.textStrongL,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun OnboardingScreenPreview() {
    OnboardingScreen(
        uiState = OnboardingUIState(sceneIndex = 3, lineIndex = 0, isLineRevealed = true),
        onIntent = {},
    )
}
