package com.dandi.nyummy.common.data.token

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

/**
 * 401 전용 silent re-auth 훅. 인증 클라이언트에만 부착한다.
 *
 * null 반환 = 원본 401 을 errorBody 보존한 채 그대로 전파 —
 * 이후 BaseRemoteDataSource 가 HttpResponseException(401) 을 throw 하고,
 * 세션 만료 다이얼로그·로그인 이동은 domain(BaseUseCase.executeCommonErrorHanding)이
 * 담당한다. 네트워크 레이어는 UI/네비게이션을 트리거하지 않는다.
 *
 * 동시 401 N건은 [mutex] 로 single-flight 를 보장한다: 첫 요청만 실제 재발급을 하고,
 * 나머지는 락 해제 후 "현재 저장 토큰 ≠ 내가 실패했던 토큰" 재확인으로 결과를 재사용한다.
 * 서버가 리프레시 토큰을 rotate 하므로, 이 재확인 없이 두 번째 요청이 이미 폐기된
 * 리프레시 토큰으로 재호출하면 세션이 파괴된다.
 *
 * Authenticator 는 동기 API 라 [runBlocking] 을 쓴다 — OkHttp 워커 스레드이므로
 * 메인 스레드를 막지 않는다 (TokenProviderImpl 최초 로드와 같은 선례).
 *
 * login 등 비인증 엔드포인트는 @NoAuthApi 클라이언트를 쓰므로
 * 이 코드에 구조적으로 도달할 수 없다 — 여기 오는 401 은 전부 토큰 문제다.
 */
class TokenAuthenticator(
    private val tokenProvider: TokenProvider,
    private val tokenRefresher: TokenRefresher,
) : Authenticator {

    private val mutex = Mutex()

    override fun authenticate(route: Route?, response: Response): Request? {
        // 재시도 1회 가드: priorResponse 체인이 있으면 이미 재발급 후 재시도한 요청이다.
        if (response.retryCount() >= MAX_RETRY) return null

        val failedToken = response.request.header(HEADER_AUTHORIZATION)
            ?.removePrefix(BEARER_PREFIX)

        return runBlocking {
            mutex.withLock {
                // 다른 요청이 이미 갱신을 끝냈다면 그 결과로 재시도만 한다.
                val current = tokenProvider.accessToken
                if (!current.isNullOrBlank() && current != failedToken) {
                    return@withLock response.request.withToken(current)
                }

                val refreshToken = tokenProvider.refreshToken?.takeIf { it.isNotBlank() }
                    ?: return@withLock null

                val newToken = tokenRefresher.refresh(refreshToken)
                    ?: return@withLock null
                response.request.withToken(newToken)
            }
        }
    }

    // Authenticator 의 재요청은 application interceptor 를 다시 타지 않으므로
    // header() 로 직접 교체한다.
    private fun Request.withToken(token: String): Request =
        newBuilder()
            .header(HEADER_AUTHORIZATION, "$BEARER_PREFIX$token")
            .build()

    private fun Response.retryCount(): Int {
        var count = 0
        var prior = priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }

    companion object {
        private const val MAX_RETRY = 1
        const val HEADER_AUTHORIZATION = "Authorization"
        const val BEARER_PREFIX = "Bearer "
    }
}
