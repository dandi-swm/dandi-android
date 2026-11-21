package com.dandi.nyummy.common.domain.helper

/**
 * 이 기기에 저장한 로그인 토큰(AccessToken, RefreshToken)을 domain에서 지우는 통로.
 * 토큰 저장소는 data(TokenProvider)에 있어 domain이 직접 볼 수 없다. 구현은 common/data가 한다.
 */
interface SessionTokenHelper {

    /** 저장한 토큰을 모두 지운다. 지운 뒤에는 토큰 재발급도 하지 않아 자동 로그인이 다시 일어나지 않는다. */
    suspend fun clearTokens()
}
