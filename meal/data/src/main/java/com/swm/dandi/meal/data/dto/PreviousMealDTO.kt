package com.swm.dandi.meal.data.dto

import com.swm.dandi.meal.entity.PreviousFoodVO
import com.swm.dandi.meal.entity.PreviousMealPageVO
import kotlinx.serialization.Serializable

/**
 * 이전 음식 선택 화면 조회 응답.
 */
@Serializable
data class PreviousMealPageResponseDTO(
    val previousFoods: List<PreviousFoodDTO>? = null,
)

/**
 * 이전 음식 후보.
 *
 * 화면 표시 문구인 `recordCountLabel`, `lastRecordedLabel`은 받지 않고,
 * 정수와 시간 원자료를 받은 뒤 presentation에서 문구로 바꾼다.
 */
@Serializable
data class PreviousFoodDTO(
    val foodHistoryId: String? = null,
    val displayName: String? = null,
    val iconImageUrl: String? = null,
    val representativePhotoUrl: String? = null,
    val lastRecordedAt: String? = null,
    val recordCount: Int? = null,
    val rankScore: Double? = null,
)

fun PreviousMealPageResponseDTO.toVO(): PreviousMealPageVO = PreviousMealPageVO(
    previousFoods = previousFoods.orEmpty().map { it.toVO() },
)

fun PreviousFoodDTO.toVO(): PreviousFoodVO = PreviousFoodVO(
    foodHistoryId = foodHistoryId.orEmpty(),
    displayName = displayName.orEmpty(),
    iconImageUrl = iconImageUrl ?: representativePhotoUrl.orEmpty(),
    lastRecordedAt = lastRecordedAt.orEmpty(),
    recordCount = recordCount ?: 0,
    rankScore = rankScore ?: 0.0,
)
