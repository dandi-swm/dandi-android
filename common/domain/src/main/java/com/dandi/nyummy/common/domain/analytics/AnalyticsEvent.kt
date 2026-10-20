package com.dandi.nyummy.common.domain.analytics

/**
 * 분석 이벤트 하나. 이름과 매개변수를 이 파일에서만 정해 화면마다 철자가 달라지지 않게 한다.
 *
 * GA4 규칙: 이름과 매개변수 키는 영문 소문자와 밑줄로 40자 안, 값은 100자 안이다. 개인 정보(이메일, 닉네임 등)는 넣지 않는다.
 * 로그인과 회원가입은 GA4 권장 이벤트 이름(login, sign_up, method)을 그대로 써서 기본 보고서에 잡히게 한다.
 *
 * 단계 이벤트(가입 단계)는 그 단계에 도착할 때 남긴다. GA4 퍼널 탐색에서 단계를 차례로 놓으면
 * 어느 단계에서 다음 단계로 못 넘어갔는지(이탈)가 보인다.
 */
sealed class AnalyticsEvent(
    val name: String,
    val params: Map<String, String> = emptyMap(),
) {

    /** 로그인 성공. [method]는 email, kakao, google 같은 로그인 수단이다. 소셜 신규 회원은 가입이 끝날 때 [SignUp]으로 남는다. */
    class Login(method: String) : AnalyticsEvent("login", mapOf(PARAM_METHOD to method))

    /**
     * 회원가입 단계에 도착했다. [step]은 account(이메일과 비밀번호), code(이메일 인증 코드), profile(닉네임과 신체 정보)이고,
     * [method]는 email 또는 social이다. 소셜 가입은 profile부터 시작한다.
     */
    class SignUpStep(step: String, method: String) : AnalyticsEvent(
        "sign_up_step",
        mapOf(PARAM_STEP to step, PARAM_METHOD to method),
    )

    /** 회원가입 성공. [method]는 email 또는 social이다. */
    class SignUp(method: String) : AnalyticsEvent("sign_up", mapOf(PARAM_METHOD to method))

    private companion object {
        const val PARAM_METHOD = "method"
        const val PARAM_STEP = "step"
    }
}
