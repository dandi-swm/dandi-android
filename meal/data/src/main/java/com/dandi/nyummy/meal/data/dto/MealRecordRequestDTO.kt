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

/**
 * PUT /api/v2/meals/{mealId}/foods 요청 바디입니다. 사용자가 고른 음식 목록 전체를 보내며, 요청에 없는 기존 줄은 서버가 지웁니다.
 *
 * @property foods 음식 목록(1~10줄). 맨 위 줄이 대표 음식이다
 */
@Serializable
data class UpdateMealFoodsRequestDTO(
    val foods: List<MealFoodRequestDTO>,
)

/**
 * 보낼 음식 한 줄입니다.
 *
 * @property mealFoodId 음식 줄 ID. 새로 추가한 줄은 null이다
 * @property name 음식 이름(1~30자)
 * @property portion 인분(0.5~5.0, 0.5 단위)
 */
@Serializable
data class MealFoodRequestDTO(
    val mealFoodId: Long? = null,
    val name: String,
    val portion: Double,
)
