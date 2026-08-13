package com.dandi.nyummy.common.domain.navigation

/**
 * 단일 네비게이션 플로우에 흘려보내는 신호.
 *
 * - [GoToDestPage]: 앱 내 전진 이동(탭/아이템 클릭 등). 현재 백스택 위로 push.
 * - [DeepLink]: 앱 실행 중(웜 스타트) 도착한 deep-link. 기존 스택은 보존하고 대상만 최전면으로(bring-to-front).
 *   콜드 스타트의 synthetic 부모 체인과 달리 사용자의 현재 맥락을 유지하는 것이 의도된 동작이다.
 * - [Back]: 시스템/하드웨어 백 키와 동일하게 한 단계 뒤로 이동.
 * - [ResetToPage]: 백스택을 전부 비우고 대상만 루트로 남긴다. 로그인/회원가입 완료처럼
 *   이전 플로우로 되돌아가면 안 되는 전환에 사용한다.
 * - [BackToInitialPage]: 세션 만료(401). 스택을 비우고 초기 화면(로그인) 단독으로 교체한다.
 *   목적지를 인자로 받지 않는 의도(intent) 신호 — 실제 목적지는 NavHost 가 결정한다.
 */
sealed interface NavSignal {
    data class GoToDestPage(val route: NavRoute) : NavSignal
    data class DeepLink(val route: NavRoute) : NavSignal
    data object Back : NavSignal
    data class ResetToPage(val route: NavRoute) : NavSignal
    data object BackToInitialPage : NavSignal
}
