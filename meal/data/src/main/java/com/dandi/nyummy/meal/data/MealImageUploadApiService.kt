package com.dandi.nyummy.meal.data

import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.HeaderMap
import retrofit2.http.PUT
import retrofit2.http.Url

/**
 * presigned URL 기반 외부 스토리지 이미지 업로드 API
 *
 * 서버가 아닌 외부 스토리지(S3)로 직접 요청하므로 `@NoAuthApi` Retrofit 으로 생성해야 한다 —
 * presigned 쿼리 서명과 Bearer Authorization 헤더가 함께 실리면 스토리지가 요청을 거부한다.
 */
interface MealImageUploadApiService {
    /** presigned URL 로 이미지 업로드 */
    @PUT
    suspend fun uploadImage(
        @Url uploadUrl: String,
        @HeaderMap headers: Map<String, String>,
        @Body body: RequestBody,
    ): Response<Unit>
}
