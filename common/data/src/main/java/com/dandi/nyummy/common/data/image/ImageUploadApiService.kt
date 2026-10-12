package com.dandi.nyummy.common.data.image

import com.dandi.nyummy.common.data.image.dto.ImageUploadUrlDTO
import com.dandi.nyummy.common.data.image.dto.ImageUploadUrlRequestDTO
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * 이미지 업로드 URL 발급 API. 식사, 문의 등 용도와 상관없이 같은 API를 쓰고 [ImageUploadUrlRequestDTO.purpose]로 구분한다.
 * 로그인한 사용자 기준으로 발급하므로 인증 Retrofit으로 만든다.
 */
interface ImageUploadApiService {
    /** 이미지 업로드 URL 발급 */
    @POST("$IMAGES_PATH/presigned-url")
    suspend fun issueUploadUrl(@Body request: ImageUploadUrlRequestDTO): Response<ImageUploadUrlDTO>

    companion object {
        const val IMAGES_PATH = "/api/v1/images"
    }
}
