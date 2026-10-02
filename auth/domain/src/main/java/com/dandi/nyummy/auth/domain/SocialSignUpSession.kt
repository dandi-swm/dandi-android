package com.dandi.nyummy.auth.domain

import java.util.concurrent.atomic.AtomicReference

/**
 * 소셜 로그인 후 회원가입을 기다리는 검증 완료 토큰을 메모리에만 보관한다.
 *
 * 토큰을 라우트 인자로 넘기면 백스택과 함께 저장되고, 등록된 경로는 외부 앱 링크로도 열리므로
 * 위조된 값이 들어올 수 있다. 프로세스가 종료되면 사라지며, 그때 소셜 가입 화면은 다시 로그인하도록 안내한다.
 */
class SocialSignUpSession {

    private val verifiedToken = AtomicReference<String?>(null)

    /** 가입을 기다리는 토큰. 없으면 null. */
    val pendingToken: String?
        get() = verifiedToken.get()

    /** 소셜 로그인 응답의 검증 완료 토큰을 보관한다. */
    fun start(token: String) {
        verifiedToken.set(token)
    }

    /** 가입 완료·포기·새 로그인 시도 시 토큰을 버린다. */
    fun clear() {
        verifiedToken.set(null)
    }
}
