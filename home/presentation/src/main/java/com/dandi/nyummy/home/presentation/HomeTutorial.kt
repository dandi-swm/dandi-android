package com.dandi.nyummy.home.presentation

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.res.stringResource
import com.dandi.nyummy.common.presentation.coachmark.CoachMark
import com.dandi.nyummy.common.presentation.coachmark.CoachMarkKey
import com.dandi.nyummy.common.presentation.coachmark.MainCoachMarkKeys
import com.dandi.nyummy.common.presentation.coachmark.ShowCoachMark
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyHealthStoryDialog
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyHealthStoryPage
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTutorialExpression
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTutorialFinishDialog
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTutorialLesson
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTutorialSkipDialog
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/** 홈 HUD 우편 버튼의 코치마크 대상. */
internal val HomeMailCoachMarkKey = CoachMarkKey("home.mail")

/** 홈이 보여 주는 튜토리얼 단계. */
enum class HomeTutorialStage {
    /** 튜토리얼 식사 전 홈 소개. 마지막에 카메라 버튼을 가리킨다. 서버가 튜토리얼을 마쳤다고 할 때까지 남는다. */
    Intro,

    /** 튜토리얼 식사 뒤 우편 버튼 가리키기. 우편 버튼이 건너뛰기 자리와 겹쳐 건너뛰기가 없다. */
    MailPointer,

    /** 우편함 단계 뒤 마이룸 탭 가리키기. */
    RoomTabPointer,

    /** 마이룸을 저장하고 돌아왔을 때 방이 잘 저장됐다고 알린다. 카드를 누르면 냐미 건강 이야기로 간다. */
    RoomSaved,
}

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
    val stage: HomeTutorialStage,
    val lines: ImmutableList<HomeTutorialLine> = HomeTutorialScript.linesOf(stage),
    val index: Int = 0,
) {
    val line: HomeTutorialLine
        get() = lines[index]

    /** 다음 대사가 있고, 지금 대사가 대상을 기다리지 않으면 넘길 수 있다. */
    val canAdvance: Boolean
        get() = line.target == null && index < lines.lastIndex
}

/**
 * 냐미 건강 이야기와 마침. [page]가 이야기 장 수보다 작으면 그 장을, 같으면 마침 카드를 보인다.
 */
@Immutable
data class HomeHealthStoryUiState(val page: Int = 0) {
    val isFinish: Boolean
        get() = page >= NyummyHealthStoryPage.entries.size
}

/** 홈 튜토리얼 대본. 단계마다 대사 묶음 하나다. */
internal object HomeTutorialScript {
    private val Intro: ImmutableList<HomeTutorialLine> = persistentListOf(
        HomeTutorialLine(R.string.home_tutorial_hello_title, R.string.home_tutorial_hello_body, NyummyTutorialExpression.Greet),
        HomeTutorialLine(R.string.home_tutorial_reward_title, R.string.home_tutorial_reward_body, NyummyTutorialExpression.Greet),
        HomeTutorialLine(
            R.string.home_tutorial_camera_title,
            R.string.home_tutorial_camera_body,
            NyummyTutorialExpression.Point,
            MainCoachMarkKeys.MealRecord,
        ),
    )

    private val MailPointer: ImmutableList<HomeTutorialLine> = persistentListOf(
        HomeTutorialLine(
            R.string.home_tutorial_mail_title,
            R.string.home_tutorial_mail_body,
            NyummyTutorialExpression.Surprise,
            HomeMailCoachMarkKey,
        ),
    )

    private val RoomTabPointer: ImmutableList<HomeTutorialLine> = persistentListOf(
        HomeTutorialLine(
            R.string.home_tutorial_room_title,
            R.string.home_tutorial_room_body,
            NyummyTutorialExpression.Cheer,
            MainCoachMarkKeys.MyRoomTab,
        ),
    )

    private val RoomSaved: ImmutableList<HomeTutorialLine> = persistentListOf(
        HomeTutorialLine(R.string.home_tutorial_room_saved_title, R.string.home_tutorial_room_saved_body, NyummyTutorialExpression.Cheer),
    )

    fun linesOf(stage: HomeTutorialStage): ImmutableList<HomeTutorialLine> = when (stage) {
        HomeTutorialStage.Intro -> Intro
        HomeTutorialStage.MailPointer -> MailPointer
        HomeTutorialStage.RoomTabPointer -> RoomTabPointer
        HomeTutorialStage.RoomSaved -> RoomSaved
    }
}

/**
 * 홈 튜토리얼을 코치마크로 올린다. 대상이 없는 소개 대사는 카메라 버튼 바로 위에 띄워 다음 대사와 자리가 이어지게 한다.
 * 건너뛰기는 우편 버튼 단계에만 없다(코치마크 층이 대상과 겹치는 건너뛰기도 숨긴다).
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
            onSkip = { onIntent(HomeIntent.TutorialSkip) }.takeUnless { tutorial.stage == HomeTutorialStage.MailPointer },
        )
    }
    ShowCoachMark(mark)
}

/** 건너뛰기 확인과 냐미 건강 이야기·마침. 홈 위에 다이얼로그 창으로 띄운다. */
@Composable
internal fun HomeTutorialDialogs(
    skipConfirm: ImmutableList<NyummyTutorialLesson>?,
    healthStory: HomeHealthStoryUiState?,
    onIntent: (HomeIntent) -> Unit,
) {
    if (skipConfirm != null) {
        NyummyTutorialSkipDialog(
            remaining = skipConfirm,
            onContinue = { onIntent(HomeIntent.TutorialSkipCancelled) },
            onSkip = { onIntent(HomeIntent.TutorialSkipConfirmed) },
        )
    }
    if (healthStory == null) return
    if (healthStory.isFinish) {
        NyummyTutorialFinishDialog(
            title = stringResource(R.string.home_tutorial_finish_title),
            body = stringResource(R.string.home_tutorial_finish_body),
            buttonText = stringResource(R.string.home_tutorial_finish_start),
            onStart = { onIntent(HomeIntent.TutorialFinished) },
        )
        return
    }
    val page = NyummyHealthStoryPage.entries[healthStory.page]
    val (title, body) = when (page) {
        NyummyHealthStoryPage.Steady -> R.string.home_health_steady_title to R.string.home_health_steady_body
        NyummyHealthStoryPage.Starving -> R.string.home_health_starving_title to R.string.home_health_starving_body
        NyummyHealthStoryPage.Overeating -> R.string.home_health_overeating_title to R.string.home_health_overeating_body
    }
    NyummyHealthStoryDialog(
        page = page,
        pageIndex = healthStory.page,
        title = stringResource(title),
        body = stringResource(body),
        buttonText = stringResource(
            if (healthStory.page == NyummyHealthStoryPage.entries.lastIndex) R.string.home_health_done else R.string.home_health_next,
        ),
        onNext = { onIntent(HomeIntent.HealthStoryNext) },
    )
}
