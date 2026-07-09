package com.swm.dandi.meal.data.dto

import com.swm.dandi.meal.entity.CreateMealRequestVO
import com.swm.dandi.meal.entity.CreateMealVO
import kotlinx.serialization.Serializable

/**
 * 식사 기록 생성 요청 body.
 *
 * `inputMode`가 `PREVIOUS_FOOD`면 `foodHistoryId`, `eatenAt`이 필요하고,
 * `NEW_FOOD`면 `foodName`, `photoId`, `eatenAt`이 필요하다.
 */
@Serializable
data class CreateMealRequestDTO(
    val inputMode: String? = null,
    val foodHistoryId: String? = null,
    val foodName: String? = null,
    val photoId: String? = null,
    val eatenAt: String? = null,
)

/**
 * 식사 기록 생성 응답.
 *
 * 코드값(`inputMode`, `mealType`, `analysisStatus`)은 VO 변환에서 enum으로 정규화한다.
 */
@Serializable
data class CreateMealResponseDTO(
    val mealRecordId: String? = null,
    val foodHistoryId: String? = null,
    val inputMode: String? = null,
    val displayName: String? = null,
    val mealType: String? = null,
    val eatenAt: String? = null,
    val photoUrl: String? = null,
    val analysisStatus: String? = null,
    val createdAt: String? = null,
)

fun CreateMealRequestVO.toDTO(): CreateMealRequestDTO =
    CreateMealRequestDTO(
        inputMode = inputMode.name,
        foodHistoryId = foodHistoryId.ifEmpty { null },
        foodName = foodName.ifEmpty { null },
        photoId = photoId.ifEmpty { null },
        eatenAt = eatenAt,
    )

fun CreateMealResponseDTO.toVO(): CreateMealVO =
    CreateMealVO(
        mealRecordId = mealRecordId.orEmpty(),
        foodHistoryId = foodHistoryId.orEmpty(),
        inputMode = inputMode.toMealInputModeVO(),
        displayName = displayName.orEmpty(),
        mealType = mealType.toMealTypeVO(),
        eatenAt = eatenAt.orEmpty(),
        photoUrl = photoUrl.orEmpty(),
        analysisStatus = analysisStatus.toMealAnalysisStatusVO(),
        createdAt = createdAt.orEmpty(),
    )
