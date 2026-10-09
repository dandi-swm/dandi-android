package com.dandi.nyummy.home.presentation

import com.dandi.nyummy.common.presentation.mvi.MviIntent

/** 홈 화면에서 발생하는 사용자 의도. */
sealed interface HomeIntent : MviIntent {

    /** 홈이 화면에 보이게 됐다(첫 진입, 다른 화면에서 복귀). 요약을 다시 읽는다. */
    data object ScreenResumed : HomeIntent

    /** 상단 지갑 카드(보유 코인). */
    data object ClickWallet : HomeIntent

    /** 우편함. */
    data object ClickMail : HomeIntent

    /** 공지. */
    data object ClickNotice : HomeIntent

    /** 설정. */
    data object ClickSettings : HomeIntent

    /** 연속 기록 배너. */
    data object ClickStreak : HomeIntent

    /** 오늘 바. 오늘 기록이 없으면 식사 기록으로, 있으면 오늘 식사 시트로 간다. */
    data object ClickTodayBar : HomeIntent

    /** 고양이방 메뉴 펼치기, 접기. */
    data object ToggleRoomMenu : HomeIntent

    /** 방 메뉴: 마이룸 꾸미기. */
    data object ClickMyRoom : HomeIntent

    /** 방 메뉴: 친구에게 공유. */
    data object ClickShareFriend : HomeIntent

    /** 방 메뉴: 냐미 상태. */
    data object ClickNyamiStatus : HomeIntent

    /** 오늘 식사 시트를 닫았다. */
    data object DismissTodaySheet : HomeIntent

    /** 오늘 식사 시트를 다시 읽는다(불러오기 실패 후). */
    data object RetryTodayMeals : HomeIntent

    /** 식사 추가하기(시트 안 버튼). */
    data object ClickAddMeal : HomeIntent

    /** 냐미를 눌렀다. 같은 상태의 다른 동작과 새 대사를 보여 준다. */
    data object ClickCat : HomeIntent

    /**
     * 냐미 동작 하나가 끝나고 쉬는 시간도 지났다.
     * 그사이 다른 동작으로 바뀌었으면 늦게 온 신호이므로 [playId]로 걸러 낸다.
     */
    data class CatMotionFinished(val playId: Int) : HomeIntent

    /** 냐미가 처음 화면에 그려졌다(받은 스프라이트든 앱에 든 기본 냐미든). 홈 TTI 끝 판단에 쓴다. */
    data object CatShown : HomeIntent

    /** 받은 요약 숫자가 처음 화면에 그려졌다. 홈 TTI 끝 판단에 쓴다. */
    data object SummaryShown : HomeIntent
}
