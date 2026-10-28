package com.dandi.nyummy.cat.domain

import com.dandi.nyummy.cat.entity.CatLineVO

/** 냐미 대사 은행. 지금은 앱에 든 파일을 읽고, 나중에 CDN으로 옮길 수 있다. */
interface CatLineRepository {
    suspend fun getLines(): List<CatLineVO>
}
