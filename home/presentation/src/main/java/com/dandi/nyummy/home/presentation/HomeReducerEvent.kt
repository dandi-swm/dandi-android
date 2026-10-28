package com.dandi.nyummy.home.presentation

import com.dandi.nyummy.cat.domain.CatMood
import com.dandi.nyummy.cat.entity.CatState
import com.dandi.nyummy.common.presentation.mvi.ReducerEvent
import com.dandi.nyummy.home.entity.HomeSummaryVO

/** 홈 화면 상태를 변이시키는 내부 이벤트. */
sealed interface HomeReducerEvent : ReducerEvent {

    /**
     * 홈 요약 정보 로딩이 끝났다. [room]은 고양이방에 그릴 꾸민 방이고, 마이룸이 꺼져 있으면 null이다.
     * [showsMailBadge]는 우편 버튼에 빨간 점을 달지(받을 우편이 있고 우편함 아이템 기능이 켜져 있을 때)다.
     */
    data class SummaryLoaded(
        val summary: HomeSummaryVO,
        val room: HomeRoomUiModel?,
        val showsMailBadge: Boolean,
    ) : HomeReducerEvent

    /** 눈 버튼으로 화면 요소를 숨기거나 다시 보였다. */
    data class UiHiddenChanged(val hidden: Boolean) : HomeReducerEvent

    data object CatAnimationsLoaded : HomeReducerEvent

    data object CatAnimationsLoadFailed : HomeReducerEvent

    /** 냐미 상태나 동작이 바뀌었다. [motion]은 애니메이션을 아직 받지 못했으면 null이다. */
    data class CatMotionChanged(
        val state: CatState,
        val playId: Int,
        val line: String?,
        val motion: HomeCatMotion?,
        val debugMotion: String? = null,
        val debugLabel: String? = null,
    ) : HomeReducerEvent

    /** 말풍선이 [lineId] 대사를 끝까지 다 찍었다. */
    data class CatLineRevealed(val lineId: Int) : HomeReducerEvent

    /** 냐미 기분이 바뀌었다. 지금 동작은 끊지 않고 다음 동작부터 새 기분으로 고른다. */
    data class CatMoodChanged(val mood: CatMood, val debugLabel: String?) : HomeReducerEvent
}
