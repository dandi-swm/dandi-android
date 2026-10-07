package com.dandi.nyummy.home.data

import com.dandi.nyummy.home.domain.HomeRepository
import com.dandi.nyummy.home.entity.HomeSummaryVO

class HomeRepositoryImpl(
    private val dataSource: HomeDataSource,
) : HomeRepository {
    override suspend fun getHomeSummary(): HomeSummaryVO =
        dataSource.getHome().toVO()
}
