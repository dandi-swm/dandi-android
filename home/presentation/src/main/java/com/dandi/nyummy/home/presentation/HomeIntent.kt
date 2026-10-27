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

    /** 눈 버튼. 코인, 연속 기록, 말풍선, 하단 내비를 숨기고 냐미와 방만 보여 준다. 다시 누르면 돌아온다. */
    data object ToggleUiHidden : HomeIntent

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

    /** 튜토리얼 카드를 눌렀다. 다음 대사로 넘긴다. */
    data object TutorialAdvance : HomeIntent

    /** 튜토리얼이 가리킨 곳을 눌렀다. 이동은 그 버튼이 하고, 뒤 안내 단계면 마친다(첫 식사 튜토리얼은 서버가 마칠 때까지 둔다). */
    data object TutorialTargetClicked : HomeIntent

    /** 튜토리얼 건너뛰기. 남은 뒤 안내를 모두 끝낸다. 첫 식사 튜토리얼에는 없다. */
    data object TutorialSkip : HomeIntent

    /** 뒤 안내 중 뒤로 가기. 이 단계만 마친다. 첫 식사 튜토리얼은 뒤로 가기를 가로채지 않는다. */
    data object TutorialBack : HomeIntent
}
