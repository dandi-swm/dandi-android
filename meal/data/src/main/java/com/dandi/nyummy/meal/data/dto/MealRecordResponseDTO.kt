package com.dandi.nyummy.meal.data.dto

import com.dandi.nyummy.common.data.image.dto.ImageUploadUrlDTO
import com.dandi.nyummy.meal.entity.CreatedMealVO
import com.dandi.nyummy.meal.entity.FoundFoodVO
import com.dandi.nyummy.meal.entity.MealFoodConfirmVO
import com.dandi.nyummy.meal.entity.MealRecognition
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
 * 식사 생성 응답입니다. 기록하지 못한 경우는 이 응답이 아니라 에러 코드로 옵니다.
 *
 * @property id 생성된 식사 ID
 * @property status 음식 인식 결과. CONFIRMING(음식 찾음) / FAILED(음식 아님) / RECOGNIZING(기한 안에 못 찾음)
 * @property rewardBoxCategory 이번 기록으로 생긴 보상 상자 우편의 등급 (BRONZE/SILVER/GOLD)
 * @property name 서버가 지은 식사 이름. 음식을 찾았을 때만 있고, 음식이 아니면 빈 문자열, 인식 중이면 null이다
 * @property foods 찾은 음식 줄. 맨 위 줄이 대표 음식이다. 음식을 찾았을 때만 있다
 */
@Serializable
data class CreatedMealDTO(
    val id: Long? = null,
    val status: String? = null,
    val rewardBoxCategory: String? = null,
    val name: String? = null,
    val foods: List<MealFoodDTO>? = null,
) {
    fun toVO(): CreatedMealVO {
        val recognition = status.toMealRecognition()
        return CreatedMealVO(
            mealId = id ?: 0L,
            recognition = recognition,
            rewardBoxGrade = RewardBoxGrade.from(rewardBoxCategory),
            name = name.orEmpty(),
            // 음식을 찾았다고 했을 때만 목록을 믿는다.
            foods = if (recognition == MealRecognition.CONFIRMING) foods.toFoundFoods() else emptyList(),
        )
    }
}

/**
 * 식사의 음식 한 줄. 고르기에 필요한 값만 읽는다(인분·영양·아이콘은 식사 상세가 읽는다).
 *
 * @property mealFoodId 음식 줄 ID
 * @property name 음식 이름
 */
@Serializable
data class MealFoodDTO(
    val mealFoodId: Long? = null,
    val name: String? = null,
)

/**
 * GET /api/v2/meals/{mealId} 응답에서 찾은 음식을 고르는 데 필요한 값만 읽는다.
 *
 * @property status 영양 분석 상태. CONFIRMING일 때만 사용자가 음식을 고를 수 있다
 * @property foods 찾은 음식 줄. 인식 중이거나 음식이 아닌 식사는 null이다
 */
@Serializable
data class MealFoodConfirmDTO(
    val status: String? = null,
    val foods: List<MealFoodDTO>? = null,
) {
    fun toVO(): MealFoodConfirmVO = MealFoodConfirmVO(
        isConfirmable = status.toMealRecognitionOrNull() == MealRecognition.CONFIRMING,
        foods = foods.toFoundFoods(),
    )
}

/**
 * PUT /api/v2/meals/{mealId}/foods 응답. 기록 화면은 상태를 쓰지 않는다(영양 계산 결과는 알림으로 온다).
 *
 * @property id 식사 ID
 * @property status 보낸 뒤의 분석 상태 (WAITING / COMPLETED)
 */
@Serializable
data class MealFoodsStatusDTO(
    val id: Long? = null,
    val status: String? = null,
)

/** ID나 이름이 없는 줄은 보낼 수도 보여 줄 수도 없어 버린다. */
private fun List<MealFoodDTO>?.toFoundFoods(): List<FoundFoodVO> = orEmpty().mapNotNull { food ->
    val id = food.mealFoodId ?: return@mapNotNull null
    val name = food.name?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
    FoundFoodVO(mealFoodId = id, name = name)
}

/** 서버 status를 음식 인식 결과로 읽는다. 세 값이 아니면 null이다. */
private fun String?.toMealRecognitionOrNull(): MealRecognition? = when (this?.trim()?.uppercase()) {
    "CONFIRMING" -> MealRecognition.CONFIRMING
    "FAILED" -> MealRecognition.NOT_FOOD
    "RECOGNIZING" -> MealRecognition.RECOGNIZING
    else -> null
}

/**
 * 생성 응답의 status를 음식 인식 결과로 읽는다. 모르는 값이어도 식사는 만들어졌으므로,
 * 홈에서 기다리게 하는 [MealRecognition.RECOGNIZING]으로 둔다.
 */
private fun String?.toMealRecognition(): MealRecognition = toMealRecognitionOrNull() ?: MealRecognition.RECOGNIZING
