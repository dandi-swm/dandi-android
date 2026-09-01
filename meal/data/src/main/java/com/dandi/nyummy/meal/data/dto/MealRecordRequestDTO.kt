package com.dandi.nyummy.meal.data.dto

import kotlinx.serialization.Serializable

/**
 * 이미지 업로드 URL 발급 요청 바디입니다.
 *
 * @property contentType 업로드할 사진의 MIME 타입 (예: image/jpeg)
 * @property fileSizeBytes 업로드할 사진 파일 크기 (byte)
 */
@Serializable
data class UploadImageUrlRequestDTO(
    val contentType: String,
    val fileSizeBytes: Long,
)

/**
 * 식사 생성 요청 바디입니다.
 *
 * @property imageKey 업로드 완료된 이미지의 키
 */
@Serializable
data class CreateMealRequestDTO(
    val imageKey: String,
)
