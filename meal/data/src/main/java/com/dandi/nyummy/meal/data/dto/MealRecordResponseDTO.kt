package com.dandi.nyummy.meal.data.dto

import com.dandi.nyummy.meal.entity.CreatedMealVO
import com.dandi.nyummy.meal.entity.MealImageUploadVO
import kotlinx.serialization.Serializable

/**
 * 이미지 업로드 URL 발급 응답입니다.
 *
 * @property uploadUrl 사진을 업로드할 presigned URL
 * @property imageKey 식사 생성 요청에 사용하는 이미지 키
 * @property uploadMethod 업로드에 사용할 HTTP 메서드 (예: PUT)
 * @property uploadHeaders 업로드 요청에 그대로 실어야 하는 헤더 목록
 * @property expiresAt presigned URL 만료 시각 문자열
 */
@Serializable
data class MealImageUploadDTO(
    val uploadUrl: String? = null,
    val imageKey: String? = null,
    val uploadMethod: String? = null,
    val uploadHeaders: Map<String, String>? = null,
    val expiresAt: String? = null,
) {
    fun toVO(): MealImageUploadVO = MealImageUploadVO(
        uploadUrl = uploadUrl.orEmpty(),
        imageKey = imageKey.orEmpty(),
        uploadMethod = uploadMethod.orEmpty(),
        uploadHeaders = uploadHeaders.orEmpty(),
        expiresAt = expiresAt.orEmpty(),
    )
}

/**
 * 식사 생성 응답입니다.
 *
 * @property id 생성된 식사 ID
 * @property status 영양 분석 상태 문자열 (WAITING/ANALYZING/COMPLETED/FAILED/UNKNOWN)
 */
@Serializable
data class CreatedMealDTO(
    val id: Long? = null,
    val status: String? = null,
) {
    fun toVO(): CreatedMealVO = CreatedMealVO(
        mealId = id ?: 0L,
        status = status.orEmpty(),
    )
}
