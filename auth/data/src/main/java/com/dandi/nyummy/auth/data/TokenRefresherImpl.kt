package com.dandi.nyummy.auth.data

import com.dandi.nyummy.auth.data.dto.RefreshTokenRequestDTO
import com.dandi.nyummy.common.data.token.TokenProvider
import com.dandi.nyummy.common.data.token.TokenRefresher
import java.io.IOException

/**
 * [TokenRefresher] 구현. refresh API·DTO 소유권이 auth feature 에 있으므로 여기 둔다.
 *
 * 세션이 끝났을 때만 null 로 알린다 — 호출자(TokenAuthenticator)는 null 이면
 * 원본 401 을 그대로 전파시켜 domain 의 공통 401 처리(세션 만료)로 잇는다.
 * 세션이 끝났다고 서버가 확정하는 응답은 401(`api.auth.invalidRefreshToken`)뿐이다(Swagger 계약).
 * 세션 만료 처리는 이 기기의 로그인 정보를 모두 지우므로, 그 밖에 재발급을 끝내지 못한 실패
 * (네트워크 끊김, 서버 5xx, 401이 아닌 4xx(게이트웨이의 403, 429 등), 토큰이 없는 성공 응답)는 [IOException]으로 던진다.
 * 그러면 원래 요청이 네트워크 오류로 실패하고 토큰은 남는다.
 */
class TokenRefresherImpl(
    private val apiService: AuthApiService,
    private val tokenProvider: TokenProvider,
) : TokenRefresher {

    override suspend fun refresh(refreshToken: String): String? {
        // 네트워크 장애는 IOException 그대로 던진다: 토큰 보존, 원래 요청은 네트워크 오류로 끝난다.
        // 그 밖의 예외(응답 해석 실패 등)도 IOException으로 감싼다. Authenticator가 다른 예외를 던지면 OkHttp가 앱을 종료시킨다.
        val response = try {
            apiService.refresh(RefreshTokenRequestDTO(refreshToken = refreshToken))
        } catch (e: IOException) {
            throw e
        } catch (e: Exception) {
            throw IOException("Token refresh failed", e)
        }

        // 401(api.auth.invalidRefreshToken): 리프레시 토큰 확정 무효 → 세션 종료.
        // 죽은 토큰을 남기면 이후 모든 401 이 헛된 refresh 를 반복한다.
        if (response.code() == HTTP_UNAUTHORIZED) {
            tokenProvider.clear()
            return null
        }

        // 401 밖의 실패 응답(5xx, 게이트웨이의 403, 429 등)은 세션이 끝났다는 뜻이 아니다. 토큰 보존, 일시적인 실패로 던진다.
        if (!response.isSuccessful) throw IOException("Token refresh failed: ${response.code()}")

        // 성공인데 새 토큰이 없으면 재발급을 끝내지 못한 것이다. 세션 만료로 보내지 않고 토큰을 남긴다.
        val body = response.body()
        val newAccessToken = body?.accessToken?.takeIf { it.isNotBlank() }
            ?: throw IOException("Token refresh failed: empty access token")

        tokenProvider.update(
            access = newAccessToken,
            // 서버는 rotate 된 리프레시 토큰을 함께 내려준다. 누락 시 기존 값 유지.
            refresh = body.refreshToken?.takeIf { it.isNotBlank() } ?: refreshToken,
        )
        return newAccessToken
    }

    private companion object {
        const val HTTP_UNAUTHORIZED = 401
    }
}
