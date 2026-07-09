package com.swm.dandi.meal.data.dto

import com.swm.dandi.meal.entity.MealPhotoUploadUrlRequestVO
import com.swm.dandi.meal.entity.MealPhotoUploadUrlVO
import kotlinx.serialization.Serializable

/**
 * S3 presigned URL 발급 요청 body.
 */
@Serializable
data class MealPhotoUploadUrlRequestDTO(
    val fileName: String? = null,
    val contentType: String? = null,
    val fileSizeBytes: Long? = null,
)

/**
 * S3 presigned URL 발급 응답.
 *
 * `photoId`는 이후 `POST /meals`에서 업로드된 사진을 참조하는 key다.
 */
@Serializable
data class MealPhotoUploadUrlResponseDTO(
    val photoId: String? = null,
    val uploadUrl: String? = null,
    val expiresAt: String? = null,
)

fun MealPhotoUploadUrlRequestVO.toDTO(): MealPhotoUploadUrlRequestDTO =
    MealPhotoUploadUrlRequestDTO(
        fileName = fileName,
        contentType = contentType,
        fileSizeBytes = fileSizeBytes,
    )

fun MealPhotoUploadUrlResponseDTO.toVO(): MealPhotoUploadUrlVO =
    MealPhotoUploadUrlVO(
        photoId = photoId.orEmpty(),
        uploadUrl = uploadUrl.orEmpty(),
        expiresAt = expiresAt.orEmpty(),
    )
