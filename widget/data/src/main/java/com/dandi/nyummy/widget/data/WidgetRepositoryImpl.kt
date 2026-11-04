package com.dandi.nyummy.widget.data

import com.dandi.nyummy.common.data.token.TokenProvider
import com.dandi.nyummy.widget.domain.WidgetRepository
import com.dandi.nyummy.widget.entity.WidgetSnapshotVO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WidgetRepositoryImpl(
    private val dataSource: WidgetDataSource,
    private val tokenProvider: TokenProvider,
) : WidgetRepository {

    override suspend fun snapshot(): WidgetSnapshotVO = dataSource.read()

    override suspend fun update(transform: (WidgetSnapshotVO) -> WidgetSnapshotVO) = dataSource.update(transform)

    override suspend fun clear() = dataSource.clear()

    // 토큰 첫 읽기는 디스크를 막고 읽으므로 IO에서 한다. refresh 토큰이 만료되면 둘 다 지워진다.
    override suspend fun isLoggedIn(): Boolean = withContext(Dispatchers.IO) {
        !tokenProvider.refreshToken.isNullOrBlank() || !tokenProvider.accessToken.isNullOrBlank()
    }
}
