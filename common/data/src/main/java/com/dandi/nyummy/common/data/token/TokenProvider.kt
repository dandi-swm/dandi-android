package com.dandi.nyummy.common.data.token

/**
 * 인증 토큰 저장소 계약.
 *
 * 액세스 토큰은 모든 요청 헤더에 부착되는 cross-cutting 관심사이므로
 * auth feature 가 아닌 common/data 가 소유한다 (향후 OkHttp Authenticator 가 여기서 읽는다).
 *
 * - [accessToken]/[refreshToken] 은 동기(메모리 캐시) 읽기 — OkHttp Interceptor/Authenticator 는
 *   suspend 를 쓸 수 없으므로 디스크 I/O 없이 즉시 반환해야 한다. 최초 1회 디스크 로드가
 *   블로킹으로 일어나므로 메인 스레드에서 호출하지 않는다.
 * - 쓰기([update]/[clear])는 suspend — 디스크(DataStore) 반영 후 캐시를 갱신한다.
 */
interface TokenProvider {
    val accessToken: String?
    val refreshToken: String?

    /** 로그인/회원가입/재발급 성공 시 토큰 쌍을 저장한다. */
    suspend fun update(access: String, refresh: String)

    /** 세션 종료(리프레시 토큰 만료·로그아웃) 시 토큰을 폐기한다. */
    suspend fun clear()
}
