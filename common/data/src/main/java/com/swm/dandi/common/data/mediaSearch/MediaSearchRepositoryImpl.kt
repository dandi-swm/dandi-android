package com.swm.dandi.common.data.mediaSearch

import com.swm.dandi.common.data.mediaSearch.dto.toVO
import com.swm.dandi.common.domain.media.MediaSearchRepository
import com.swm.dandi.common.entity.media.MediaSearchResultVO
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class MediaSearchRepositoryImpl(
    private val searchDataSource: MediaSearchDataSource,
) : MediaSearchRepository {

    override suspend fun search(query: String, page: Int): MediaSearchResultVO = coroutineScope {
        // 이미지/동영상 검색을 동시에 호출한 뒤, 해당 페이지 안에서만 datetime 최신순으로 합쳐 정렬한다.
        val images = async { searchDataSource.searchImages(query, page) }
        val videos = async { searchDataSource.searchVideos(query, page) }
        val imageResponse = images.await()
        val videoResponse = videos.await()
        val merged =
            (imageResponse.documents.orEmpty().map { it.toVO() } + videoResponse.documents.orEmpty()
                .map { it.toVO() })
                // 동일 식별자(이미지=image_url) 중복 항목을 제거해 리스트/페이저 key 충돌을 방지한다.
                .distinctBy { it.urlKey }
                .sortedByDescending { it.dateTime.take(SORT_KEY_LENGTH) }
                .map { it.copy(page = page) }
        MediaSearchResultVO(
            items = merged,
            // meta 가 없으면(파싱 누락 등) 안전하게 마지막 페이지로 간주하고,
            // 이미지·동영상 양쪽 모두 is_end 일 때만 끝으로 본다. 한쪽이라도 남아 있으면 계속 불러온다.
            isEnd = (imageResponse.meta?.isEnd ?: true) && (videoResponse.meta?.isEnd ?: true),
        )
    }

    private companion object {
        // "YYYY-MM-DDTHH:MM:SS" 까지(밀리초·타임존 제외)를 정렬 키로 사용해 초 단위 최신순 정렬.
        const val SORT_KEY_LENGTH = 19
    }
}
