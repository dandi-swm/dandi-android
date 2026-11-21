package com.dandi.nyummy.common.data.token

/**
 * 토큰 재발급 계약.
 *
 * refresh API·DTO 의 소유권은 auth feature(AuthApiService)에 있지만,
 * 호출자는 common/data 의 [TokenAuthenticator] 다. common/data → auth/data 는
 * Gradle 순환이므로 계약만 여기 두고 구현(TokenRefresherImpl)·바인딩은 auth/data 가 제공한다.
 */
interface TokenRefresher {

    /**
     * 리프레시 토큰으로 새 토큰 쌍을 발급받아 저장까지 마친 뒤 새 액세스 토큰을 반환한다.
     *
     * 세션이 끝나 재발급할 수 없으면 null — 호출자([TokenAuthenticator])는 null 이면 원본 401 을
     * 그대로 전파시켜 세션 만료(이 기기의 로그인 정보 정리)로 잇는다. 리프레시 토큰이 확정 무효(401)일 때의
     * 저장 토큰 폐기도 구현이 책임진다.
     *
     * 일시적인 실패(네트워크 끊김, 서버 5xx)는 세션이 끝난 것이 아니므로 [java.io.IOException]을 던진다.
     * Authenticator가 던진 IOException은 원래 요청의 실패가 되어 화면은 네트워크 오류로 안내하고 토큰은 남는다.
     */
    suspend fun refresh(refreshToken: String): String?
}
