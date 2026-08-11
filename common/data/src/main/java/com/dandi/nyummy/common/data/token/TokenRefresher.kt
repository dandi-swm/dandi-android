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
     * 재발급 불가 시 null — 호출자([TokenAuthenticator])는 null 이면 원본 401 을
     * 그대로 전파시킨다. 리프레시 토큰이 확정 무효(401)일 때의 저장 토큰 폐기도
     * 구현이 책임진다.
     */
    suspend fun refresh(refreshToken: String): String?
}
