package com.swm.dandi.common.data.mediaSearch

import com.swm.dandi.common.data.BaseRemoteDataSource
import com.swm.dandi.common.data.mediaSearch.dto.ImageSearchResponse
import com.swm.dandi.common.data.mediaSearch.dto.VideoSearchResponse

class MediaSearchDataSource(private val apiService: MediaSearchApiService) : BaseRemoteDataSource() {

    suspend fun searchImages(query: String, page: Int): ImageSearchResponse {
        return checkResponse(apiService.searchImages(query = query, page = page))
    }

    suspend fun searchVideos(query: String, page: Int): VideoSearchResponse {
        return checkResponse(apiService.searchVideos(query = query, page = page))
    }
}
