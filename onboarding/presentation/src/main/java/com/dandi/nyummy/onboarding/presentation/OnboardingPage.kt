package com.dandi.nyummy.onboarding.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyText
import com.dandi.nyummy.common.presentation.designsystem.foundation.nyummyClickable
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.onboarding.presentation.component.OnboardingCatNameSlot
import com.dandi.nyummy.onboarding.presentation.component.OnboardingChoiceList
import com.dandi.nyummy.onboarding.presentation.component.OnboardingDialogueBox
import com.dandi.nyummy.onboarding.presentation.component.OnboardingMealTimeSheet
import com.dandi.nyummy.onboarding.presentation.component.OnboardingMealTimeSlot
import com.dandi.nyummy.onboarding.presentation.component.OnboardingStage
import com.dandi.nyummy.onboarding.presentation.component.OnboardingStartButton

@Composable
fun OnboardingPage(viewModel: OnboardingViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    OnboardingScreen(uiState = uiState, onIntent = viewModel::onIntent)
}

/**
 * 냥줍 온보딩. 화면 어디를 탭해도 대사가 진행된다.
 *
 * 선택지와 시작 버튼은 대화창 아래에, 이름 입력은 대화창 안에 둔다. 아래 패널이 커지면 냐미가 그만큼 위로 올라간다.
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

    // 온보딩은 루트 화면이다. 이름 등록 중이나 마무리 중에는 뒤로 가기로 앱이 닫히지 않게 막는다.
    BackHandler(enabled = uiState.isSubmitting || uiState.isFinishing) {}

    // 식사 알림을 받을 끼니가 있으면 마치기 전에 알림 권한을 묻는다. 허용 여부와 관계없이 이어서 마친다.
    val context = LocalContext.current
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        onIntent(OnboardingIntent.ConfirmMealTimes)
    }
    val onMealTimesDone = {
        val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            uiState.mealTimes.hasAnyMeal &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        if (needsPermission) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            onIntent(OnboardingIntent.ConfirmMealTimes)
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(NyummyTheme.colors.bg.surfaceInverse)
            .clickable(interactionSource = null, indication = null) { onIntent(OnboardingIntent.TapDialogue) },
    ) {
        val density = LocalDensity.current
        var panelHeightPx by remember { mutableIntStateOf(0) }
        val bottomInset = with(density) {
            WindowInsets.navigationBars.union(WindowInsets.ime).getBottom(this).toDp()
        }
        val panelTop = maxHeight - bottomInset - PanelBottomGap - with(density) { panelHeightPx.toDp() }

        OnboardingStage(
            character = uiState.currentScene.character,
            isUserOnStage = uiState.isUserOnStage,
            panelTop = panelTop,
        )

        if (uiState.isSkipVisible) {
            SkipButton(
                onClick = { onIntent(OnboardingIntent.Skip) },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = NyummyTheme.spacing.s16, end = NyummyTheme.spacing.gutter),
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .widthIn(max = PanelMaxWidth)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
                .padding(horizontal = NyummyTheme.spacing.gutter)
                .padding(bottom = PanelBottomGap)
                .onSizeChanged { panelHeightPx = it.height },
            verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s12),
        ) {
            OnboardingDialogueBox(
                speaker = line.speaker,
                speakerName = speakerName,
                text = stringResource(line.textRes, uiState.catName),
                lineKey = Triple(uiState.sceneIndex, uiState.choiceIndex, uiState.lineIndex),
                revealed = uiState.isLineRevealed,
                showContinueHint = uiState.isContinueHintVisible,
                onRevealed = { onIntent(OnboardingIntent.TypingFinished) },
                slot = when {
                    !uiState.isActionVisible -> null
                    action == OnboardingSceneAction.NameInput -> {
                        {
                            OnboardingCatNameSlot(
                                value = uiState.catNameInput,
                                error = uiState.catNameError,
                                isSubmitting = uiState.isSubmitting,
                                onValueChange = { onIntent(OnboardingIntent.InputCatName(it)) },
                                onSubmit = { onIntent(OnboardingIntent.SubmitCatName) },
                            )
                        }
                    }
                    action == OnboardingSceneAction.MealTime -> {
                        {
                            OnboardingMealTimeSlot(
                                mealTimes = uiState.mealTimes,
                                enabled = !uiState.isFinishing,
                                onClickMeal = { onIntent(OnboardingIntent.ClickMealTime(it)) },
                                onDone = onMealTimesDone,
                            )
                        }
                    }
                    else -> null
                },
            )
            // 대화창 아래 행동(선택지, 시작 버튼)은 그 장면의 행동을 그대로 들고 사라져야 한다.
            // 현재 action을 바로 읽으면 다음 장면의 버튼이 잠깐 비칠 수 있어 AnimatedContent의 상태로 그린다.
            AnimatedContent(
                targetState = action.takeIf { uiState.isActionVisible && !it.isInsideDialogue },
                transitionSpec = {
                    (fadeIn(tween(PanelFadeInMillis)) + slideInVertically(tween(PanelFadeInMillis)) { it / 4 }) togetherWith
                        fadeOut(tween(PanelFadeOutMillis)) using SizeTransform(clip = false)
                },
                contentKey = { it?.let { shown -> shown::class } },
                label = "OnboardingAction",
            ) { shownAction ->
                when (shownAction) {
                    is OnboardingSceneAction.Choice -> OnboardingChoiceList(
                        options = shownAction.options,
                        onSelect = { onIntent(OnboardingIntent.SelectChoice(it)) },
                    )
                    OnboardingSceneAction.Start -> OnboardingStartButton(
                        onClick = { onIntent(OnboardingIntent.ClickStart) },
                    )
                    OnboardingSceneAction.NameInput, OnboardingSceneAction.MealTime, OnboardingSceneAction.None, null -> Unit
                }
            }
        }
    }

    uiState.editingMeal?.let { meal ->
        OnboardingMealTimeSheet(
            meal = meal,
            initial = uiState.mealTimes[meal],
            onSelect = { onIntent(OnboardingIntent.SelectMealTime(meal, it)) },
            onDismissRequest = { onIntent(OnboardingIntent.DismissMealTimeSheet) },
        )
    }
}

/** 대화창 안에 그리는 행동(이름 입력, 식사 시각)인지. 나머지는 대화창 아래에 둔다. */
private val OnboardingSceneAction.isInsideDialogue: Boolean
    get() = this == OnboardingSceneAction.NameInput || this == OnboardingSceneAction.MealTime

@Composable
private fun SkipButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    NyummyText(
        text = stringResource(R.string.onboarding_skip),
        style = NyummyTheme.typography.labelM,
        color = NyummyTheme.colors.content.onInverse,
        modifier = modifier
            .nyummyClickable(onClick = onClick)
            .background(NyummyTheme.colors.bg.scrim, RoundedCornerShape(NyummyTheme.radius.full))
            .padding(horizontal = NyummyTheme.spacing.s16, vertical = NyummyTheme.spacing.s8),
    )
}

private val PanelBottomGap = 16.dp
private val PanelMaxWidth = 480.dp
private const val PanelFadeInMillis = 220
private const val PanelFadeOutMillis = 120

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun OnboardingCatLinePreview() {
    NyummyTheme {
        OnboardingScreen(
            uiState = OnboardingUIState(sceneIndex = 1, lineIndex = 0, isLineRevealed = true),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun OnboardingMealTimePreview() {
    NyummyTheme {
        OnboardingScreen(
            uiState = OnboardingUIState(
                sceneIndex = OnboardingScript.scenes.lastIndex,
                isLineRevealed = true,
                catName = "냐미",
            ),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun OnboardingNameInputPreview() {
    NyummyTheme {
        OnboardingScreen(
            uiState = OnboardingUIState(
                sceneIndex = OnboardingScript.namingSceneIndex,
                lineIndex = 1,
                isLineRevealed = true,
            ),
            onIntent = {},
        )
    }
}
