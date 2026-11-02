package com.dandi.nyummy.common.domain.helper

import com.dandi.nyummy.common.domain.navigation.NavRoute
import com.dandi.nyummy.common.domain.navigation.NavSignal
import com.dandi.nyummy.common.domain.navigation.Page
import kotlinx.coroutines.flow.Flow

/**
 * 단일 네비게이션 플로우. 전진/후진 모두 [NavSignal] 한 가지 형식으로 emit 된다.
 *
 * 호출부는 다음 중 하나로 사용한다.
 * - [navigateByRoute] — 직접 [NavRoute] 를 구성해서 전진 이동(앱 내 push).
 * - [navigateTo] — 각 feature 가 정의한 [Page] 객체를 그대로 전달 (권장).
 * - [navigateToAppLink] — 앱 링크(`https://link.nyummy.co.kr/...`)를 연다. 푸시·외부 앱 링크(웜 스타트)와
 *   서버 SDUI 액션·홈 공지 CTA가 쓴다. 호스트가 외부 앱 링크와 같은 해석기로 라우트를 찾고 bring-to-front 한다.
 *   앱 링크가 아니거나 등록된 화면이 없으면 무시한다. `asRoot`면 스택을 비우고 그 화면만 남긴다(로그인 뒤 이동).
 * - [navigateToBack] — 하드웨어 백 키와 동일하게 한 단계 뒤로 이동.
 * - [navigateToAsRoot] — 백스택을 전부 비우고 대상 페이지만 루트로 남긴다 (로그인/가입 완료 → 홈 전환).
 * - [navigateToInitial] — 세션 만료(401) 등으로 인한 초기 화면 이동 신호를 전달한다.
 * - [navigateToExternalLink] — 외부 앱(브라우저/스토어)으로 URL 을 연다(앱 외부 이동).
 */
interface NavigationHelper {
    val navigationFlow: Flow<NavSignal>
    fun navigateByRoute(route: NavRoute)
    fun navigateTo(page: Page)
    fun navigateToAppLink(link: String, asRoot: Boolean = false)
    fun navigateToBack()
    fun navigateToAsRoot(page: Page)
    fun navigateToInitial()
    fun navigateToExternalLink(url: String)
}
