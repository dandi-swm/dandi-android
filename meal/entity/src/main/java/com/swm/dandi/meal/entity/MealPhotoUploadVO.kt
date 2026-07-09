package com.swm.dandi.meal.entity

/**
 * S3 presigned URL 발급 요청에 필요한 사진 메타데이터.
 */
data class MealPhotoUploadUrlRequestVO(
    val fileName: String = "",
    val contentType: String = "",
    val fileSizeBytes: Long = 0L,
) {
    companion object {
        val skeleton: MealPhotoUploadUrlRequestVO = MealPhotoUploadUrlRequestVO(
            fileName = "meal-photo.jpg",
            contentType = "image/jpeg",
            fileSizeBytes = 0L,
        )
    }
}

/**
 * 서버가 발급한 식사 사진 업로드 URL.
 *
 * @property photoId 식사 생성 요청에서 참조할 사진 id.
 * @property uploadUrl 클라이언트가 파일을 업로드할 presigned URL.
 * @property expiresAt 업로드 URL 만료 시각.
 */
data class MealPhotoUploadUrlVO(
    val photoId: String = "",
    val uploadUrl: String = "",
    val expiresAt: String = "",
) {
    companion object {
        val empty: MealPhotoUploadUrlVO = MealPhotoUploadUrlVO()
    }
}
