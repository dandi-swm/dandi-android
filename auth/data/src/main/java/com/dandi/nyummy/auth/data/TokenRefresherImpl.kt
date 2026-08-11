package com.dandi.nyummy.auth.data

import com.dandi.nyummy.auth.data.dto.RefreshTokenRequestDTO
import com.dandi.nyummy.common.data.token.TokenProvider
import com.dandi.nyummy.common.data.token.TokenRefresher

/**
 * [TokenRefresher] 구현. refresh API·DTO 소유권이 auth feature 에 있으므로 여기 둔다.
 *
 * 실패를 예외가 아닌 null 로 알린다 — 호출자(TokenAuthenticator)는 null 이면
 * 원본 401 을 그대로 전파시켜 domain 의 공통 401 처리(세션 만료)로 잇는다.
 */
class TokenRefresherImpl(
    private val apiService: AuthApiService,
    private val tokenProvider: TokenProvider,
) : TokenRefresher {

    override suspend fun refresh(refreshToken: String): String? {
        val response = runCatching {
            apiService.refresh(RefreshTokenRequestDTO(refreshToken = refreshToken))
        }.getOrNull() ?: return null // IOException 등 네트워크 장애: 토큰 보존, 원본 401 전파

        // 401(api.auth.unauthorized): 리프레시 토큰 확정 무효 → 세션 종료.
        // 죽은 토큰을 남기면 이후 모든 401 이 헛된 refresh 를 반복한다.
        if (response.code() == 401) {
            tokenProvider.clear()
            return null
        }

        val body = response.takeIf { it.isSuccessful }?.body() ?: return null // 5xx 등: 토큰 보존
        val newAccessToken = body.accessToken?.takeIf { it.isNotBlank() } ?: return null

        tokenProvider.update(
            access = newAccessToken,
            // 서버는 rotate 된 리프레시 토큰을 함께 내려준다. 누락 시 기존 값 유지.
            refresh = body.refreshToken?.takeIf { it.isNotBlank() } ?: refreshToken,
        )
        return newAccessToken
    }
}
