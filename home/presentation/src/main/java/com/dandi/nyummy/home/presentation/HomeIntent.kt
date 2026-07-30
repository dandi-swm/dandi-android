package com.dandi.nyummy.home.presentation

import com.dandi.nyummy.common.presentation.mvi.MviIntent

/** 홈 화면에서 발생하는 사용자 입력. */
sealed interface HomeIntent : MviIntent {

    /** 지갑(보유 코인) 카드를 탭했다. */
    data object ClickWallet : HomeIntent

    /** 우편 버튼을 탭했다. */
    data object ClickMail : HomeIntent

    /** 공지 버튼을 탭했다. */
    data object ClickNotice : HomeIntent

    /** 설정 버튼을 탭했다. */
    data object ClickSettings : HomeIntent

    /** 스트릭(연속 기록) 배너를 탭했다. */
    data object ClickStreak : HomeIntent

    /** 마이룸 카드 하단의 오늘 요약 바를 탭했다. */
    data object ClickTodaySummary : HomeIntent

    /** 마이룸 카드의 플로팅 액션 메뉴 토글(더보기) 버튼을 탭했다. */
    data object ToggleRoomActionMenu : HomeIntent

    /** 마이룸 카드의 공유(사진 찍기) 플로팅 버튼을 탭했다. */
    data object ClickShare : HomeIntent

    /** 마이룸 카드의 꾸미기(편집 모드) 플로팅 버튼을 탭했다. */
    data object ClickRoomEdit : HomeIntent

    /** 마이룸 카드의 말풍선 다시 보기 플로팅 버튼을 탭했다. */
    data object ClickSpeechReplay : HomeIntent

    /** 오늘 식사 요약 바텀시트를 닫았다(스크림 탭). */
    data object DismissTodaySummarySheet : HomeIntent
}
