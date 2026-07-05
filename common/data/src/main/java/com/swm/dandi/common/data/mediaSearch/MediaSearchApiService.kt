package com.swm.dandi.common.data.mediaSearch

import com.swm.dandi.common.data.mediaSearch.dto.ImageSearchResponse
import com.swm.dandi.common.data.mediaSearch.dto.VideoSearchResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface MediaSearchApiService {

    @GET("v2/search/image")
    suspend fun searchImages(
        @Query("query") query: String,
        @Query("page") page: Int,
        @Query("sort") sort: String = SORT_RECENCY,
        @Query("size") size: Int = PAGE_SIZE,
    ): Response<ImageSearchResponse>

    @GET("v2/search/vclip")
    suspend fun searchVideos(
        @Query("query") query: String,
        @Query("page") page: Int,
        @Query("sort") sort: String = SORT_RECENCY,
        @Query("size") size: Int = PAGE_SIZE,
    ): Response<VideoSearchResponse>

    companion object {
        const val SORT_RECENCY = "recency"
        const val PAGE_SIZE = 10
    }
}
