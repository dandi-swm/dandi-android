package com.dandi.nyummy.auth.entity

/**
 * 집사 프로필에서 고를 수 있는 키·몸무게·태어난 해의 범위. 회원가입과 마이페이지가 같은 범위를 쓴다.
 * 서버는 범위를 막지 않아 앱이 정한다. 태어난 해의 끝은 올해(기기 타임존)라 쓰는 곳에서 정한다.
 */
object UserProfileRange {
    val HEIGHT_CM = 120..220
    val WEIGHT_KG = 30..200
    const val BIRTH_YEAR_FIRST = 1900
}
