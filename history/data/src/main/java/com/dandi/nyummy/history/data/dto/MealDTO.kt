package com.dandi.nyummy.history.data.dto

import com.dandi.nyummy.history.data.util.foodIconIdOf
import com.dandi.nyummy.history.data.util.toDisplayTime
import com.dandi.nyummy.history.data.util.toDeviceHistoryDate
import com.dandi.nyummy.history.data.util.toMealAnalysisStatus
import com.dandi.nyummy.history.entity.MealHistoryVO
import kotlinx.serialization.Serializable

/**
 * GET · PUT /api/v1/meals/{mealId} 응답 (식사 단건 상세).
 *
 * @property mealId 식사 기록 식별자
 * @property name 음식 이름
 * @property mealAt 식사 시각 (ISO date-time). data 레이어가 기기 타임존으로 환산한다.
 * @property status 영양 분석 상태 (WAITING / ANALYZING / COMPLETED / FAILED / UNKNOWN)
 * @property nutrition 이 식사의 영양 정보
 * @property imageUrl 촬영 사진 URL
 * @property catComment 이 식사에 대한 냐미의 한 줄 피드백
 * @property iconId 음식 픽셀 아이콘 식별자. 분석 전이거나 실패한 식사에도 서버 기본값(1)이 온다.
 * @property canCreateAnalysisFeedback AI 분석 평가를 받을 식사인지 여부. 분석이 끝났고 아직 평가하지 않은 식사 중 서버가 고른 것만 true.
 */
@Serializable
data class MealDTO(
    val mealId: Long? = null,
    val name: String? = null,
    val mealAt: String? = null,
    val status: String? = null,
    val nutrition: NutritionDTO? = null,
    val imageUrl: String? = null,
    val catComment: String? = null,
    val iconId: Long? = null,
    val canCreateAnalysisFeedback: Boolean? = null,
) {
    // 단건 조회에는 하루 내 순서 정보가 없으므로 orderIndex 는 0. 음식 아이콘은 분석이 끝난 식사만 쓴다.
    fun toVO(): MealHistoryVO {
        val analysisStatus = status.toMealAnalysisStatus()
        return MealHistoryVO(
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
            catComment = catComment?.trim() ?: "",
            date = mealAt.toDeviceHistoryDate(),
            canCreateAnalysisFeedback = canCreateAnalysisFeedback == true,
        )
    }
}
