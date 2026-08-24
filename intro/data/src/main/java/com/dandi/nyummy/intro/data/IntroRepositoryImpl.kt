package com.dandi.nyummy.intro.data

import com.dandi.nyummy.common.data.token.TokenProvider
import com.dandi.nyummy.intro.domain.IntroRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

class IntroRepositoryImpl(
    private val tokenProvider: TokenProvider,
    private val ioDispatcher: CoroutineDispatcher,
) : IntroRepository {

    /**
     * [TokenProvider] 의 첫 읽기는 디스크 로드가 블로킹으로 일어나므로
     * 메인 스레드 호출을 피해 IO 디스패처에서 수행한다.
     */
    override suspend fun hasRefreshToken(): Boolean = withContext(ioDispatcher) {
        !tokenProvider.refreshToken.isNullOrBlank()
    }
}
