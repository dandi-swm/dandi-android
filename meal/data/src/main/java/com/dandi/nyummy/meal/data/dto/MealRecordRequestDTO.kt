package com.dandi.nyummy.meal.data.dto

import kotlinx.serialization.Serializable

/**
 * 식사 생성 요청 바디입니다.
 *
 * @property imageKey 업로드 완료된 이미지의 키
 */
@Serializable
data class CreateMealRequestDTO(
    val imageKey: String,
)
