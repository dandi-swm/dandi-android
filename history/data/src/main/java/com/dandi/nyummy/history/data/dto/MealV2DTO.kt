package com.dandi.nyummy.history.data.dto

import com.dandi.nyummy.history.data.util.foodIconIdOf
import com.dandi.nyummy.history.data.util.toDeviceHistoryDate
import com.dandi.nyummy.history.data.util.toDisplayTime
import com.dandi.nyummy.history.data.util.toMealAnalysisStatus
import com.dandi.nyummy.history.data.util.toMealAnalysisVersion
import com.dandi.nyummy.history.entity.MealDetailV2VO
import com.dandi.nyummy.history.entity.MealFoodVO
import com.dandi.nyummy.history.entity.MealHistoryVO
import kotlinx.serialization.Serializable

/**
 * GET · PUT /api/v2/meals/{mealId} 응답 (식사 단건 상세 v2).
 *
 * @property mealId 식사 기록 식별자
 * @property analysisVersion 식사를 분석한 방식 (V1 / V2)
 * @property name 식사 대표 이름
 * @property mealAt 식사 시각 (ISO date-time). data 레이어가 기기 타임존으로 환산한다.
 * @property status 영양 분석 상태 (RECOGNIZING / CONFIRMING / WAITING / ANALYZING / COMPLETED / FAILED / UNKNOWN)
 * @property nutrition 이 식사의 영양 합계
 * @property imageUrl 촬영 사진 URL
 * @property canCreateAnalysisFeedback AI 분석 평가를 받을 식사인지 여부
 * @property catComment 이 식사에 대한 냐미의 한 줄 피드백. 처음 계산이 끝날 때 한 번 만들고 바뀌지 않는다.
 * @property iconId 식사 아이콘 번호. 맨 위 줄(대표 음식)의 아이콘이다.
 * @property foods 음식 줄. V1 식사, 인식 중인 식사, 음식이 아닌 식사는 null이다.
 */
@Serializable
data class MealV2DTO(
    val mealId: Long? = null,
    val analysisVersion: String? = null,
    val name: String? = null,
    val mealAt: String? = null,
    val status: String? = null,
    val nutrition: NutritionDTO? = null,
    val imageUrl: String? = null,
    val canCreateAnalysisFeedback: Boolean? = null,
    val catComment: String? = null,
    val iconId: Long? = null,
    val foods: List<MealFoodV2DTO>? = null,
) {
    fun toVO(): MealDetailV2VO {
        val analysisStatus = status.toMealAnalysisStatus()
        return MealDetailV2VO(
            meal = MealHistoryVO(
                id = mealId?.toString() ?: "",
                name = name ?: "",
                photoUrl = imageUrl ?: "",
                foodIconId = foodIconIdOf(iconId, analysisStatus),
                recordedAt = mealAt.toDisplayTime(),
                calorieKcal = nutrition?.calory ?: 0,
                carbohydrateGram = nutrition?.carbs ?: 0,
                proteinGram = nutrition?.protein ?: 0,
                fatGram = nutrition?.fat ?: 0,
                orderIndex = 0,
                status = analysisStatus,
                analysisVersion = analysisVersion.toMealAnalysisVersion(),
                catComment = catComment?.trim() ?: "",
                date = mealAt.toDeviceHistoryDate(),
                canCreateAnalysisFeedback = canCreateAnalysisFeedback == true,
            ),
            // ID가 없는 줄은 고칠 수도 지울 수도 없어 버린다. 순서는 서버가 준 그대로다(맨 위가 대표 음식).
            foods = foods.orEmpty().mapNotNull { it.toVO() },
        )
    }
}

/**
 * 식사에 든 음식 한 줄.
 *
 * @property mealFoodId 음식 줄 ID
 * @property name 음식 이름
 * @property portion 인분 (0.5 ~ 5.0, 0.5 단위)
 * @property nutrition 이 줄의 영양. 계산 전이면 null이다.
 * @property isNutritionFixed 사용자가 영양을 직접 적어 고정한 줄인지 여부
 * @property iconId 음식 아이콘 번호. 계산 전이면 null이다.
 */
@Serializable
data class MealFoodV2DTO(
    val mealFoodId: Long? = null,
    val name: String? = null,
    val portion: Double? = null,
    val nutrition: NutritionDTO? = null,
    val isNutritionFixed: Boolean? = null,
    val iconId: Long? = null,
) {
    fun toVO(): MealFoodVO? {
        val id = mealFoodId ?: return null
        return MealFoodVO(
            mealFoodId = id,
            name = name ?: "",
            portion = portion ?: MealFoodVO.DEFAULT_PORTION,
            hasNutrition = nutrition != null,
            calorieKcal = nutrition?.calory ?: 0,
            carbohydrateGram = nutrition?.carbs ?: 0,
            proteinGram = nutrition?.protein ?: 0,
            fatGram = nutrition?.fat ?: 0,
            isNutritionFixed = isNutritionFixed == true,
            foodIconId = iconId?.toString() ?: "",
        )
    }
}
