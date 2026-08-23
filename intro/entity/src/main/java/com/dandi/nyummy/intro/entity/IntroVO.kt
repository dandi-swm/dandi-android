package com.dandi.nyummy.intro.entity

/**
 * 앱 시작 게이트 정보 (버전 강제 업데이트 판단용).
 *
 * @property devTestMsg 서버 개발용 테스트 메시지
 * @property minAppVersion 최소 요구 앱 버전 — 미만이면 강제 업데이트
 * @property recommendAppVersion 권장 앱 버전
 */
data class IntroVO(
    val devTestMsg: String = "",
    val minAppVersion: String = "",
    val recommendAppVersion: String = "",
) {
    companion object {
        val empty: IntroVO = IntroVO("", "", "")
    }
}
