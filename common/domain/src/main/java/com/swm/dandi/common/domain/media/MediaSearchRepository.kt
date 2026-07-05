package com.swm.dandi.common.domain.media

import com.swm.dandi.common.entity.media.MediaSearchResultVO

interface MediaSearchRepository {
    /**
     * 키워드로 이미지+동영상 검색 결과의 한 페이지를 가져온다.
     * 페이지 내부에서만 datetime 최신순으로 정렬되며, 페이지 간 정렬은 하지 않는다.
     * 응답의 meta.is_end 를 합쳐 [MediaSearchResultVO.isEnd] 로 전달한다.
     */
    suspend fun search(query: String, page: Int): MediaSearchResultVO
}
