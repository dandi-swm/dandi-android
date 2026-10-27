package com.dandi.nyummy.home.presentation

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.res.stringResource
import com.dandi.nyummy.common.entity.tutorial.TutorialStep
import com.dandi.nyummy.common.presentation.coachmark.CoachMark
import com.dandi.nyummy.common.presentation.coachmark.CoachMarkKey
import com.dandi.nyummy.common.presentation.coachmark.MainCoachMarkKeys
import com.dandi.nyummy.common.presentation.coachmark.ShowCoachMark
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTutorialExpression
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/** 홈 HUD 우편 버튼의 코치마크 대상. */
internal val HomeMailCoachMarkKey = CoachMarkKey("home.mail")

/**
 * 홈 튜토리얼 대사 한 줄.
 *
 * @property target 가리킬 대상. 있으면 그 대상을 눌러야 진행되고, 없으면 카드를 눌러 넘긴다.
 */
@Immutable
data class HomeTutorialLine(
    @StringRes val title: Int,
    @StringRes val body: Int,
    val expression: NyummyTutorialExpression,
    val target: CoachMarkKey? = null,
)

/** 지금 보여 주는 홈 튜토리얼 단계와 몇 번째 대사인지. */
@Immutable
data class HomeTutorialUiState(
    val step: TutorialStep,
    val lines: ImmutableList<HomeTutorialLine>,
    val index: Int = 0,
) {
    val line: HomeTutorialLine
        get() = lines[index]

    /** 다음 대사가 있고, 지금 대사가 대상을 기다리지 않으면 넘길 수 있다. */
    val canAdvance: Boolean
        get() = line.target == null && index < lines.lastIndex
}

/** 홈 튜토리얼 대본. 단계마다 대사 묶음 하나다. */
internal object HomeTutorialScript {
    /** 홈 첫 진입 소개. 마지막에 가운데 카메라 버튼을 가리킨다. */
    val Intro: ImmutableList<HomeTutorialLine> = persistentListOf(
        HomeTutorialLine(R.string.home_tutorial_hello_title, R.string.home_tutorial_hello_body, NyummyTutorialExpression.Greet),
        HomeTutorialLine(R.string.home_tutorial_reward_title, R.string.home_tutorial_reward_body, NyummyTutorialExpression.Greet),
        HomeTutorialLine(
            R.string.home_tutorial_camera_title,
            R.string.home_tutorial_camera_body,
            NyummyTutorialExpression.Point,
            MainCoachMarkKeys.MealRecord,
        ),
    )

    /** 첫 기록 뒤 보상 상자 도착. 우편 버튼을 가리킨다. */
    val MailPointer: ImmutableList<HomeTutorialLine> = persistentListOf(
        HomeTutorialLine(R.string.home_tutorial_mail_title, R.string.home_tutorial_mail_body, NyummyTutorialExpression.Surprise, HomeMailCoachMarkKey),
    )

    /** 우편함 안내 뒤 마이룸으로 이어 주기. 마이룸 탭을 가리킨다. */
    val RoomPointer: ImmutableList<HomeTutorialLine> = persistentListOf(
        HomeTutorialLine(R.string.home_tutorial_room_title, R.string.home_tutorial_room_body, NyummyTutorialExpression.Cheer, MainCoachMarkKeys.MyRoomTab),
    )
}

/**
 * 홈 튜토리얼을 코치마크로 올린다. 대상이 없는 소개 대사는 카메라 버튼 바로 위에 띄워 다음 대사와 자리가 이어지게 한다.
 */
@Composable
internal fun HomeTutorialCoachMark(tutorial: HomeTutorialUiState?, onIntent: (HomeIntent) -> Unit) {
    val mark = tutorial?.line?.let { line ->
        CoachMark(
            title = stringResource(line.title),
            body = stringResource(line.body),
            expression = line.expression,
            target = line.target,
            anchor = MainCoachMarkKeys.MealRecord.takeIf { line.target == null },
            onAdvance = { onIntent(HomeIntent.TutorialAdvance) },
            onTargetClick = { onIntent(HomeIntent.TutorialTargetClicked) },
            onSkip = { onIntent(HomeIntent.TutorialSkip) },
            onBack = { onIntent(HomeIntent.TutorialBack) },
        )
    }
    ShowCoachMark(mark)
}
