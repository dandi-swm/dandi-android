package com.dandi.nyummy.intro.data

import com.dandi.nyummy.intro.domain.IntroRepository

class IntroRepositoryImpl(
    private val dataSource: IntroDataSource,
) : IntroRepository {

    override suspend fun getIntro() = dataSource.getIntro().toVO()
}
