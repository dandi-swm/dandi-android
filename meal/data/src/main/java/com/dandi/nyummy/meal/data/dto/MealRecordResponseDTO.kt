package com.dandi.nyummy.meal.data.dto

import com.dandi.nyummy.common.data.image.dto.ImageUploadUrlDTO
import com.dandi.nyummy.meal.entity.CreatedMealVO
import com.dandi.nyummy.meal.entity.MealImageUploadVO
import com.dandi.nyummy.meal.entity.RewardBoxGrade
import kotlinx.serialization.Serializable

/** 공통 이미지 업로드 URL 발급 응답을 식사 사진 업로드 정보로 바꾼다. */
internal fun ImageUploadUrlDTO.toVO(): MealImageUploadVO = MealImageUploadVO(
    uploadUrl = uploadUrl.orEmpty(),
    imageKey = imageKey.orEmpty(),
    uploadMethod = uploadMethod.orEmpty(),
    uploadHeaders = uploadHeaders.orEmpty(),
    expiresAt = expiresAt.orEmpty(),
)

/**
 * 식사 생성 응답입니다.
 *
 * @property id 생성된 식사 ID
 * @property status 영양 분석 상태 문자열 (WAITING/ANALYZING/COMPLETED/FAILED/UNKNOWN)
 * @property rewardBoxCategory 이번 기록으로 생긴 보상 상자 우편의 등급 (BRONZE/SILVER/GOLD)
 */
@Serializable
data class CreatedMealDTO(
    val id: Long? = null,
    val status: String? = null,
    val rewardBoxCategory: String? = null,
) {
    fun toVO(): CreatedMealVO = CreatedMealVO(
        mealId = id ?: 0L,
        status = status.orEmpty(),
        rewardBoxGrade = RewardBoxGrade.from(rewardBoxCategory),
    )
}
