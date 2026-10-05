package com.dandi.nyummy.onboarding.presentation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/** 대사를 말하는 쪽. 나레이션은 이름표 없이, 사용자는 실루엣과 "나" 이름표로 보여준다. */
enum class OnboardingSpeaker {
    NARRATOR,
    USER,
    CAT,
}

/** 장면마다 무대에 서는 고양이 일러스트. 길냥이 장면은 반다나가 없고, 이름을 받은 뒤에만 반다나를 한다. */
enum class OnboardingCharacter(@DrawableRes val imageRes: Int?) {
    NONE(null),
    WARY(R.drawable.onboarding_nyami_wary),
    NUZZLE(R.drawable.onboarding_nyami_nuzzle),
    TALK(R.drawable.onboarding_nyami_talk),
    SHY(R.drawable.onboarding_nyami_shy),
    CELEBRATE(R.drawable.onboarding_nyami_celebrate),
}

/**
 * 대사 한 줄. 문자열에 `%1$s` 가 있으면 사용자가 지어 준 고양이 이름이 들어간다.
 */
data class OnboardingLine(
    val speaker: OnboardingSpeaker,
    @StringRes val textRes: Int,
)

/** 선택지 하나와, 고른 뒤 이어지는 반응 대사. */
data class OnboardingChoice(
    @StringRes val labelRes: Int,
    val reactionLines: ImmutableList<OnboardingLine>,
)

/** 장면의 대사를 모두 본 뒤 사용자에게 요구하는 행동. */
sealed interface OnboardingSceneAction {
    /** 탭하면 다음 장면으로 넘어간다. */
    data object None : OnboardingSceneAction

    /** 선택지를 고르면 반응 대사를 보여준 뒤 다음 장면으로 넘어간다. */
    data class Choice(val options: ImmutableList<OnboardingChoice>) : OnboardingSceneAction

    /** 고양이 이름을 입력받아 등록한다. 성공하면 다음 장면으로 넘어간다. */
    data object NameInput : OnboardingSceneAction

    /** 홈으로 넘어가는 마지막 버튼. */
    data object Start : OnboardingSceneAction
}

data class OnboardingScene(
    val character: OnboardingCharacter,
    val lines: ImmutableList<OnboardingLine>,
    val action: OnboardingSceneAction = OnboardingSceneAction.None,
)

/**
 * 냥줍 온보딩 대본. 퇴근길 골목에서 상자 속 길냥이를 만나 간택당하고, 서비스를 소개받은 뒤 이름을 지어 준다.
 */
object OnboardingScript {

    val scenes: ImmutableList<OnboardingScene> = persistentListOf(
        OnboardingScene(
            character = OnboardingCharacter.NONE,
            lines = persistentListOf(
                OnboardingLine(OnboardingSpeaker.NARRATOR, R.string.onboarding_line_alley_1),
                OnboardingLine(OnboardingSpeaker.NARRATOR, R.string.onboarding_line_alley_2),
            ),
        ),
        OnboardingScene(
            character = OnboardingCharacter.WARY,
            lines = persistentListOf(
                OnboardingLine(OnboardingSpeaker.CAT, R.string.onboarding_line_meet_1),
                OnboardingLine(OnboardingSpeaker.NARRATOR, R.string.onboarding_line_meet_2),
                OnboardingLine(OnboardingSpeaker.NARRATOR, R.string.onboarding_line_meet_3),
            ),
            action = OnboardingSceneAction.Choice(
                options = persistentListOf(
                    OnboardingChoice(
                        labelRes = R.string.onboarding_choice_hand,
                        reactionLines = persistentListOf(
                            OnboardingLine(OnboardingSpeaker.USER, R.string.onboarding_line_hand_1),
                            OnboardingLine(OnboardingSpeaker.NARRATOR, R.string.onboarding_line_hand_2),
                        ),
                    ),
                    OnboardingChoice(
                        labelRes = R.string.onboarding_choice_snack,
                        reactionLines = persistentListOf(
                            OnboardingLine(OnboardingSpeaker.USER, R.string.onboarding_line_snack_1),
                            OnboardingLine(OnboardingSpeaker.NARRATOR, R.string.onboarding_line_snack_2),
                        ),
                    ),
                ),
            ),
        ),
        OnboardingScene(
            character = OnboardingCharacter.NUZZLE,
            lines = persistentListOf(
                OnboardingLine(OnboardingSpeaker.NARRATOR, R.string.onboarding_line_chosen_1),
                OnboardingLine(OnboardingSpeaker.NARRATOR, R.string.onboarding_line_chosen_2),
            ),
        ),
        OnboardingScene(
            character = OnboardingCharacter.TALK,
            lines = persistentListOf(
                OnboardingLine(OnboardingSpeaker.CAT, R.string.onboarding_line_talk_1),
                OnboardingLine(OnboardingSpeaker.USER, R.string.onboarding_line_talk_2),
                OnboardingLine(OnboardingSpeaker.CAT, R.string.onboarding_line_talk_3),
                OnboardingLine(OnboardingSpeaker.CAT, R.string.onboarding_line_talk_4),
                OnboardingLine(OnboardingSpeaker.CAT, R.string.onboarding_line_talk_5),
                OnboardingLine(OnboardingSpeaker.USER, R.string.onboarding_line_talk_6),
            ),
        ),
        OnboardingScene(
            character = OnboardingCharacter.SHY,
            lines = persistentListOf(
                OnboardingLine(OnboardingSpeaker.CAT, R.string.onboarding_line_name_1),
                OnboardingLine(OnboardingSpeaker.CAT, R.string.onboarding_line_name_2),
            ),
            action = OnboardingSceneAction.NameInput,
        ),
        OnboardingScene(
            character = OnboardingCharacter.CELEBRATE,
            lines = persistentListOf(
                OnboardingLine(OnboardingSpeaker.CAT, R.string.onboarding_line_named_1),
                OnboardingLine(OnboardingSpeaker.CAT, R.string.onboarding_line_named_2),
                OnboardingLine(OnboardingSpeaker.CAT, R.string.onboarding_line_named_3),
            ),
            action = OnboardingSceneAction.Start,
        ),
    )

    /** "건너뛰기"로 바로 가는 이름 짓기 장면. */
    val namingSceneIndex: Int = scenes.indexOfFirst { it.action == OnboardingSceneAction.NameInput }
}

// ── UIState 에서 현재 무대를 읽는 순수 헬퍼 (화면과 ViewModel 이 같은 규칙을 쓴다) ──

val OnboardingUIState.currentScene: OnboardingScene
    get() = OnboardingScript.scenes[sceneIndex]

/** 선택지를 고른 뒤에는 그 선택지의 반응 대사를, 그 전에는 장면 대사를 보여준다. */
val OnboardingUIState.currentLines: ImmutableList<OnboardingLine>
    get() {
        val action = currentScene.action
        val choiceIndex = choiceIndex
        return if (action is OnboardingSceneAction.Choice && choiceIndex != null) {
            action.options[choiceIndex].reactionLines
        } else {
            currentScene.lines
        }
    }

val OnboardingUIState.currentLine: OnboardingLine
    get() = currentLines[lineIndex]

val OnboardingUIState.isLastLine: Boolean
    get() = lineIndex >= currentLines.lastIndex

/** 마지막 대사가 다 나온 뒤 선택지/이름 입력/시작 버튼을 띄울 차례인지. */
val OnboardingUIState.isActionVisible: Boolean
    get() = isLineRevealed && isLastLine && when (currentScene.action) {
        OnboardingSceneAction.None -> false
        is OnboardingSceneAction.Choice -> choiceIndex == null
        OnboardingSceneAction.NameInput, OnboardingSceneAction.Start -> true
    }

/**
 * "탭해서 계속" 칩을 띄울지. 대사가 다 나왔고 탭으로 넘어갈 다음이 있을 때만 띄운다.
 * 선택지·이름 입력·시작 버튼이 뜬 마지막 대사는 탭으로 넘어가지 않으므로 숨긴다.
 */
val OnboardingUIState.isContinueHintVisible: Boolean
    get() = isLineRevealed && !isActionVisible

/**
 * 사용자 실루엣을 무대에 세울 차례인지. 사용자가 대사를 말할 때만 보인다.
 * 선택지를 고르는 동안이나 이름을 짓는 동안에는 세우지 않고, 고른 뒤 사용자 대사가 시작될 때 들어온다.
 */
val OnboardingUIState.isUserOnStage: Boolean
    get() = currentLine.speaker == OnboardingSpeaker.USER

/** 이름 짓기 전·후 장면에서는 건너뛰기를 숨긴다. */
val OnboardingUIState.isSkipVisible: Boolean
    get() = sceneIndex < OnboardingScript.namingSceneIndex
