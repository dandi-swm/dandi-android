package com.dandi.nyummy.common.data.image.dto

import com.dandi.nyummy.common.data.image.ImageUploadPurpose
import kotlinx.serialization.Serializable

/**
 * 이미지 업로드 URL 발급 요청 바디입니다.
 *
 * @property purpose 이미지 용도. 서버가 용도별 경로와 최대 크기를 정한다.
 * @property contentType 업로드할 이미지의 MIME 타입 (예: image/jpeg)
 * @property fileSizeBytes 업로드할 이미지 파일 크기 (byte)
 */
@Serializable
data class ImageUploadUrlRequestDTO(
    val purpose: ImageUploadPurpose,
    val contentType: String,
    val fileSizeBytes: Long,
)

/**
 * 이미지 업로드 URL 발급 응답입니다. 화면 모델로 바꾸는 것은 이 이미지를 쓰는 기능의 data 레이어가 한다.
 *
 * @property uploadUrl 이미지를 업로드할 presigned URL
 * @property imageKey 업로드한 뒤 각 도메인의 생성 API에 넘기는 이미지 키
 * @property uploadMethod 업로드에 사용할 HTTP 메서드 (예: PUT)
 * @property uploadHeaders 업로드 요청에 그대로 실어야 하는 헤더 목록
 * @property expiresAt presigned URL 만료 시각 문자열
 */
@Serializable
data class ImageUploadUrlDTO(
    val uploadUrl: String? = null,
    val imageKey: String? = null,
    val uploadMethod: String? = null,
    val uploadHeaders: Map<String, String>? = null,
    val expiresAt: String? = null,
)
