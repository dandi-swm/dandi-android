package com.dandi.nyummy.home.presentation

import com.dandi.nyummy.common.presentation.mvi.ReducerEvent
import com.dandi.nyummy.home.entity.HomeSummaryVO

/** 홈 화면 상태를 변이시키는 내부 이벤트. */
sealed interface HomeReducerEvent : ReducerEvent {

    /** 홈 요약 정보 로딩이 끝났다. */
    data class SummaryLoaded(val summary: HomeSummaryVO) : HomeReducerEvent

    /** 오늘 식사 요약 바텀시트 표시 여부가 바뀌었다. */
    data class TodaySummarySheetVisibilityChanged(val visible: Boolean) : HomeReducerEvent

    /** 마이룸 카드 플로팅 액션 메뉴 펼침 여부가 바뀌었다. */
    data class RoomActionMenuExpansionChanged(val expanded: Boolean) : HomeReducerEvent
}
