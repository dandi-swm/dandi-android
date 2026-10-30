package com.dandi.nyummy.room.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.res.stringResource
import com.dandi.nyummy.common.presentation.cat.catNameStringResource
import com.dandi.nyummy.common.presentation.coachmark.CoachMark
import com.dandi.nyummy.common.presentation.coachmark.CoachMarkKey
import com.dandi.nyummy.common.presentation.coachmark.ShowCoachMark
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTutorialExpression
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTutorialLesson
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTutorialSkipDialog
import kotlinx.collections.immutable.ImmutableList

/** 튜토리얼이 가리키는 보관함 칸. 그 칸에만 등록한다. */
internal val RoomItemCoachMarkKey = CoachMarkKey("room.item")

/** 상단 저장 버튼. */
internal val RoomSaveCoachMarkKey = CoachMarkKey("room.save")

/** 보관함 시트. 소개 카드를 이 바로 위에 띄운다. */
internal val RoomInventoryCoachMarkKey = CoachMarkKey("room.inventory")

enum class MyRoomTutorialPhase {
    /** 가진 아이템이 없어 꾸미기를 소개한다. */
    Intro,

    /** 보관함 칸 하나를 눌러 방에 놓게 한다. */
    Place,

    /** 놓은 뒤 저장을 가리킨다. 화면을 막지 않아 놓은 아이템을 직접 옮기고 크기를 바꿔 볼 수 있다. */
    Save,

    /** 안내대로 저장을 눌러 결과를 기다린다. 코치마크를 내린다. */
    Saving,
}

/**
 * 마이룸 첫 사용자 튜토리얼.
 *
 * @property itemId [MyRoomTutorialPhase.Place]에서 가리킬 보관함 칸
 * @property introIndex [MyRoomTutorialPhase.Intro]에서 몇 번째 대사인지
 * @property skipConfirm 건너뛰기 확인에 보일 남은 안내. 확인이 닫혀 있으면 null
 */
@Immutable
data class MyRoomTutorialUiState(
    val phase: MyRoomTutorialPhase,
    val itemId: Long? = null,
    val introIndex: Int = 0,
    val skipConfirm: ImmutableList<NyummyTutorialLesson>? = null,
) {
    val isLastIntro: Boolean
        get() = introIndex >= IntroLines.lastIndex

    companion object {
        /** 소개 대사(제목, 본문). */
        internal val IntroLines = listOf(
            R.string.my_room_tutorial_intro_title to R.string.my_room_tutorial_intro_body,
            R.string.my_room_tutorial_box_title to R.string.my_room_tutorial_box_body,
        )
    }
}

/**
 * 마이룸 튜토리얼을 코치마크로 올린다. 건너뛰기는 화면 오른쪽 위에 두고 확인을 거친다
 * (저장 버튼을 가리킬 때는 겹쳐서 코치마크 층이 건너뛰기를 숨긴다). 뒤로 가기는 가로채지 않는다.
 */
@Composable
internal fun MyRoomTutorialCoachMark(tutorial: MyRoomTutorialUiState?, onIntent: (MyRoomIntent) -> Unit) {
    val onSkip = { onIntent(MyRoomIntent.TutorialSkip) }
    val mark = when (tutorial?.phase) {
        MyRoomTutorialPhase.Intro -> {
            val (title, body) = MyRoomTutorialUiState.IntroLines[tutorial.introIndex]
            CoachMark(
                title = catNameStringResource(title),
                body = stringResource(body),
                expression = if (tutorial.isLastIntro) NyummyTutorialExpression.Cheer else NyummyTutorialExpression.Greet,
                anchor = RoomInventoryCoachMarkKey,
                onAdvance = { onIntent(MyRoomIntent.TutorialAdvance) },
                onSkip = onSkip,
            )
        }
        MyRoomTutorialPhase.Place -> CoachMark(
            title = stringResource(R.string.my_room_tutorial_place_title),
            body = stringResource(R.string.my_room_tutorial_place_body),
            expression = NyummyTutorialExpression.Point,
            target = RoomItemCoachMarkKey,
            onTargetClick = { onIntent(MyRoomIntent.TutorialTargetClicked) },
            onSkip = onSkip,
        )
        MyRoomTutorialPhase.Save -> CoachMark(
            title = stringResource(R.string.my_room_tutorial_save_title),
            body = stringResource(R.string.my_room_tutorial_save_body),
            expression = NyummyTutorialExpression.Cheer,
            target = RoomSaveCoachMarkKey,
            onSkip = onSkip,
            blocksTouches = false,
        )
        MyRoomTutorialPhase.Saving, null -> null
    }
    ShowCoachMark(mark)
    tutorial?.skipConfirm?.let { remaining ->
        NyummyTutorialSkipDialog(
            remaining = remaining,
            onContinue = { onIntent(MyRoomIntent.TutorialSkipCancelled) },
            onSkip = { onIntent(MyRoomIntent.TutorialSkipConfirmed) },
        )
    }
}
