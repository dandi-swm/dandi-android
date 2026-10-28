package com.dandi.nyummy.home.presentation

import androidx.compose.runtime.Immutable
import com.dandi.nyummy.cat.domain.CatMood
import com.dandi.nyummy.cat.entity.CatState
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTutorialLesson
import com.dandi.nyummy.common.presentation.mvi.UiState
import com.dandi.nyummy.common.presentation.room.RoomSceneItem
import com.dandi.nyummy.home.entity.HomeSummaryVO
import kotlinx.collections.immutable.ImmutableList

/**
 * 홈 화면 상태.
 *
 * @property summary HUD에 그릴 요약 정보와 냐미 상태를 고르는 오늘 기록 횟수
 * @property isSummaryLoaded 홈 요약을 한 번이라도 받았다. 요약이 그려졌는지 화면이 알리는 데(홈 TTI) 쓴다.
 * @property isUiHidden 눈 버튼으로 코인, 연속 기록, 말풍선, 하단 내비를 숨기고 냐미와 방만 보여 주는 중
 * @property isCatAnimationFailed 냐미 애니메이션을 받지 못했다(고양이가 없거나 서버, 네트워크 실패). 기본 냐미로 대신한다.
 * @property catState 지금 냐미 동작을 고른 상황. 애니메이션을 읽기 전에는 null
 * @property catPlayId 동작을 새로 고를 때마다 바뀐다
 * @property catLine 말풍선 대사. 애니메이션이 없으면 null이고, 화면은 기본 대사를 쓴다.
 * @property catLineId 대사가 바뀔 때마다 바뀐다. 말풍선은 이 값이 바뀌면 처음부터 한 글자씩 찍는다.
 * @property isCatLineRevealed 지금 대사가 끝까지 다 찍혔다. 그 뒤에 냐미를 누르면 다음 대사로 넘어간다.
 * @property catMotion 지금 재생할 냐미 동작. 애니메이션이나 상태가 아직 없으면 null이다.
 * @property catMood 냐미 기분. 정하기 전에는 null
 * @property catDebugMotion 지금 동작 이름(debug 표시용)
 * @property catDebugLabel debug 빌드에서 냐미 위에 보일 기분·동작 표시. release 에서는 null
 * @property room 고양이방에 그릴 꾸민 방. 마이룸이 꺼져 있으면 null이고, 기본 빈 방만 그린다.
 * @property showsMailBadge 우편 버튼에 빨간 점을 단다. 받을 우편이 있어도 우편함 아이템 기능이 꺼져 있으면(준비 중) 달지 않는다.
 * @property tutorial 지금 띄울 첫 사용자 튜토리얼 코치마크. 없으면 null
 * @property tutorialSkipConfirm 건너뛰기 확인에 보일 남은 안내. 확인이 닫혀 있으면 null
 * @property healthStory 냐미 건강 이야기와 마침. 띄우지 않으면 null
 */
data class HomeUIState(
    val summary: HomeSummaryVO = HomeSummaryVO.empty,
    val isSummaryLoaded: Boolean = false,
    val isUiHidden: Boolean = false,
    val isCatAnimationFailed: Boolean = false,
    val catState: CatState? = null,
    val catPlayId: Int = 0,
    val catLine: String? = null,
    val catLineId: Int = 0,
    val isCatLineRevealed: Boolean = false,
    val catMotion: HomeCatMotion? = null,
    val catMood: CatMood? = null,
    val catDebugMotion: String? = null,
    val catDebugLabel: String? = null,
    val room: HomeRoomUiModel? = null,
    val showsMailBadge: Boolean = false,
    val tutorial: HomeTutorialUiState? = null,
    val tutorialSkipConfirm: ImmutableList<NyummyTutorialLesson>? = null,
    val healthStory: HomeHealthStoryUiState? = null,
) : UiState {

    /** 오늘 한 끼라도 기록했는지. 냐미 상태와 대사가 이 값으로 갈린다. */
    val hasRecordedToday: Boolean
        get() = summary.todayRecordedCount > 0

    companion object {
        val empty = HomeUIState()
    }
}

/**
 * 홈 고양이방에 그릴 꾸민 방. 마이룸에서 꾸민 그대로다.
 *
 * @property backgroundUrl 깔린 배경 그림. 없으면 기본 빈 방을 깐다.
 * @property items 놓인 인테리어. 뒤에서 앞 순서다.
 */
@Immutable
data class HomeRoomUiModel(
    val backgroundUrl: String?,
    val items: ImmutableList<RoomSceneItem>,
)
