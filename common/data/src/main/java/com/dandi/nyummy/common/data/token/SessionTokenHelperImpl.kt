package com.dandi.nyummy.common.data.token

import com.dandi.nyummy.common.domain.helper.SessionTokenHelper

/** [SessionTokenHelper] 구현. 토큰 저장소([TokenProvider])를 비운다. */
class SessionTokenHelperImpl(
    private val tokenProvider: TokenProvider,
) : SessionTokenHelper {

    override suspend fun clearTokens() = tokenProvider.clear()
}
