package com.dandi.nyummy.history.data.dto

import kotlinx.serialization.Serializable

/**
 * PUT /api/v2/meals/{mealId} 요청 (식사 이름 수정 v2).
 *
 * @property name 새 대표 이름(1~20자)
 */
@Serializable
data class UpdateMealNameV2RequestDTO(
    val name: String,
)

/**
 * PUT /api/v2/meals/{mealId}/foods 요청 (음식 목록 전체 보내기). 요청에 없는 기존 줄은 서버가 지운다.
 *
 * @property foods 음식 목록(1~10줄). 맨 위 줄이 대표 음식이다
 */
@Serializable
data class UpdateMealFoodsV2RequestDTO(
    val foods: List<MealFoodLineRequestDTO>,
)

/**
 * 음식 목록의 한 줄. 순서 변경과 음식 추가에만 쓰므로 영양과 아이콘은 싣지 않는다
 * (실어 보내면 서버가 그 줄의 값을 고정한다).
 *
 * @property mealFoodId 음식 줄 ID. 새로 추가한 줄은 null이다
 * @property name 음식 이름(1~30자)
 * @property portion 인분(0.5~5.0, 0.5 단위)
 */
@Serializable
data class MealFoodLineRequestDTO(
    val mealFoodId: Long? = null,
    val name: String,
    val portion: Double,
)

/**
 * PATCH /api/v2/meals/{mealId}/foods/{mealFoodId} 요청 (음식 줄 수정). 바꾸려는 항목만 채운다.
 * null인 항목은 JSON에 싣지 않는다(공통 Json 설정 explicitNulls = false).
 *
 * @property name 새 이름(1~30자)
 * @property portion 새 인분(0.5~5.0, 0.5 단위)
 * @property nutrition 직접 적은 영양. 보내면 그 줄이 이 값으로 고정된다
 * @property iconId 새 아이콘 번호. 보내면 그 줄의 아이콘이 고정된다
 */
@Serializable
data class UpdateMealFoodV2RequestDTO(
    val name: String? = null,
    val portion: Double? = null,
    val nutrition: MealFoodNutritionRequestDTO? = null,
    val iconId: Long? = null,
)

/**
 * 직접 적는 영양. 네 값을 모두 보낸다.
 *
 * @property calory 열량(0~9999)
 * @property carbs 탄수화물(0~999)
 * @property protein 단백질(0~999)
 * @property fat 지방(0~999)
 */
@Serializable
data class MealFoodNutritionRequestDTO(
    val calory: Int,
    val carbs: Int,
    val protein: Int,
    val fat: Int,
)
