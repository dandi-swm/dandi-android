package com.dandi.nyummy.history.data.dto

import com.dandi.nyummy.history.data.util.toDisplayTime
import com.dandi.nyummy.history.data.util.toHistoryDateVO
import com.dandi.nyummy.history.entity.DailyMealHistoryVO
import com.dandi.nyummy.history.entity.DailyNutritionVO
import com.dandi.nyummy.history.entity.MealHistoryVO
import com.dandi.nyummy.history.entity.NutrientProgressVO
import kotlinx.serialization.Serializable

/**
 * GET /api/v1/meals/daily 응답.
 *
 * @property date 날짜 (yyyy-MM-dd)
 * @property meals 그날의 식사 목록
 * @property dailyNutrition 하루 영양 합계(현재 섭취량·목표 섭취량)
 */
@Serializable
data class DailyMealsDTO(
    val date: String? = null,
    val meals: List<DailyMealDTO>? = null,
    val dailyNutrition: DailyNutritionDTO? = null,
) {
    fun toVO(): DailyMealHistoryVO {
        // mealAt 오름차순으로 정렬한 뒤 하루 안의 순서(orderIndex)를 1부터 부여한다.
        val orderedMeals = meals.orEmpty()
            .sortedBy { it.mealAt.orEmpty() }
            .mapIndexed { index, meal -> meal.toVO(orderIndex = index + 1) }
        return DailyMealHistoryVO(
            date = date.toHistoryDateVO(),
            meals = orderedMeals,
            nutrition = dailyNutrition?.toVO() ?: DailyNutritionVO.empty,
        )
    }
}

/**
 * 일일 식사 목록의 식사 1건.
 *
 * @property mealId 식사 기록 식별자
 * @property name 음식 이름
 * @property mealAt 식사 시각 (ISO date-time)
 * @property calory 열량(kcal)
 * @property carbs 탄수화물(g)
 * @property protein 단백질(g)
 * @property fat 지방(g)
 * @property status 영양 분석 상태 (WAITING / ANALYZING / COMPLETED / FAILED / UNKNOWN)
 */
@Serializable
data class DailyMealDTO(
    val mealId: Long? = null,
    val name: String? = null,
    val mealAt: String? = null,
    val calory: Int? = null,
    val carbs: Int? = null,
    val protein: Int? = null,
    val fat: Int? = null,
    val status: String? = null,
) {
    // 일일 API 는 음식 아이콘/사진 URL 을 내려주지 않는다(UI 브랜치에서 보완 예정).
    fun toVO(orderIndex: Int): MealHistoryVO = MealHistoryVO(
        id = mealId?.toString() ?: "",
        name = name ?: "",
        photoUrl = "",
        foodIconId = "",
        recordedAt = mealAt.toDisplayTime(),
        calorieKcal = calory ?: 0,
        carbohydrateGram = carbs ?: 0,
        proteinGram = protein ?: 0,
        fatGram = fat ?: 0,
        orderIndex = orderIndex,
    )
}

/**
 * 하루 영양 합계.
 *
 * @property current 현재 섭취량
 * @property target 목표 섭취량
 */
@Serializable
data class DailyNutritionDTO(
    val current: NutritionDTO? = null,
    val target: NutritionDTO? = null,
) {
    fun toVO(): DailyNutritionVO = DailyNutritionVO(
        currentCalorieKcal = current?.calory ?: 0,
        targetCalorieKcal = target?.calory ?: 0,
        carbohydrate = NutrientProgressVO(
            dailyGram = current?.carbs ?: 0,
            goalGram = target?.carbs ?: 0,
        ),
        protein = NutrientProgressVO(
            dailyGram = current?.protein ?: 0,
            goalGram = target?.protein ?: 0,
        ),
        fat = NutrientProgressVO(
            dailyGram = current?.fat ?: 0,
            goalGram = target?.fat ?: 0,
        ),
    )
}

/**
 * 영양 정보.
 *
 * @property calory 열량(kcal)
 * @property carbs 탄수화물(g)
 * @property protein 단백질(g)
 * @property fat 지방(g)
 */
@Serializable
data class NutritionDTO(
    val calory: Int? = null,
    val carbs: Int? = null,
    val protein: Int? = null,
    val fat: Int? = null,
)
