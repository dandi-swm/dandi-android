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

    /** 홈이 화면에서 사라졌다. 마지막 방문 시각을 남긴다. */
    data object ScreenPaused : HomeIntent

    /** 냐미를 눌렀다. 바로 짧게 반응하고, 대사가 다 찍힌 뒤라면 다음 대사로 넘어간다. 연타하면 살짝 귀찮아한다. */
    data object ClickCat : HomeIntent

    /** 말풍선이 [lineId] 대사를 끝까지 다 찍었다. */
    data class CatLineRevealed(val lineId: Int) : HomeIntent

    /** debug 상태 표시를 눌렀다. 냐미 기분을 차례로 고정해 본다. */
    data object ClickCatDebug : HomeIntent

    /**
     * 냐미 동작 하나가 끝나고 쉬는 시간도 지났다.
     * 그사이 다른 동작으로 바뀌었으면 늦게 온 신호이므로 [playId]로 걸러 낸다.
     */
    data class CatMotionFinished(val playId: Int) : HomeIntent

    /** 냐미가 처음 화면에 그려졌다(받은 스프라이트든 앱에 든 기본 냐미든). 홈 TTI 끝 판단에 쓴다. */
    data object CatShown : HomeIntent

    /** 받은 요약 숫자가 처음 화면에 그려졌다. 홈 TTI 끝 판단에 쓴다. */
    data object SummaryShown : HomeIntent

    /** 튜토리얼 카드를 눌렀다. 다음 대사로 넘긴다. 방 저장 안내의 마지막 대사면 냐미 건강 이야기로 간다. */
    data object TutorialAdvance : HomeIntent

    /** 튜토리얼이 가리킨 곳을 눌렀다. 이동은 그 버튼이 하고, 우편 버튼·마이룸 탭 단계면 마친다. */
    data object TutorialTargetClicked : HomeIntent

    /** 화면 오른쪽 위 건너뛰기를 눌렀다. 건너뛰기 확인을 띄운다. */
    data object TutorialSkip : HomeIntent

    /** 건너뛰기 확인에서 계속 배우기로 했다(바깥·뒤로 가기 포함). */
    data object TutorialSkipCancelled : HomeIntent

    /** 건너뛰기 확인에서 그래도 건너뛰기로 했다. 냐미 건강 이야기로 간다. */
    data object TutorialSkipConfirmed : HomeIntent

    /** 냐미 건강 이야기에서 다음(마지막 장이면 알겠어)을 눌렀다. */
    data object HealthStoryNext : HomeIntent

    /** 마침에서 시작하기를 눌렀다. 튜토리얼을 끝낸다. */
    data object TutorialFinished : HomeIntent
}
