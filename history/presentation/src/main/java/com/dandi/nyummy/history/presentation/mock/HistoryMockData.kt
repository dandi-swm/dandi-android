package com.dandi.nyummy.history.presentation.mock

import com.dandi.nyummy.history.entity.DailyMealHistoryVO
import com.dandi.nyummy.history.entity.DailyNutritionStatus
import com.dandi.nyummy.history.entity.DailyNutritionVO
import com.dandi.nyummy.history.entity.HistoryCalendarDayVO
import com.dandi.nyummy.history.entity.HistoryCalendarVO
import com.dandi.nyummy.history.entity.HistoryDateVO
import com.dandi.nyummy.history.entity.MealHistoryVO
import com.dandi.nyummy.history.entity.NutrientProgressVO
import com.dandi.nyummy.history.presentation.util.isAfter
import java.util.GregorianCalendar
import kotlin.random.Random

/**
 * 백엔드 API 연동 전까지 히스토리 화면을 채우는 결정적(seed 기반) 목데이터입니다.
 *
 * 같은 날짜에 대해 항상 같은 데이터를 돌려주므로 월 이동/재진입 시에도 화면이 흔들리지 않습니다.
 * TODO: 식사 히스토리 조회 API 연동 시 UseCase 호출로 대체 (백엔드 미구현)
 */
internal object HistoryMockData {

    private const val TARGET_CALORIE_KCAL = 2_000
    private const val CARBOHYDRATE_GOAL_GRAM = 300
    private const val PROTEIN_GOAL_GRAM = 120
    private const val FAT_GOAL_GRAM = 70

    /** (이름, 아이콘 id, kcal, 탄수 g, 단백질 g, 지방 g) 목 식사 템플릿. */
    private data class MealTemplate(
        val name: String,
        val foodIconId: String,
        val calorieKcal: Int,
        val carbohydrateGram: Int,
        val proteinGram: Int,
        val fatGram: Int,
    )

    private val mealTemplates = listOf(
        MealTemplate("치킨 샐러드", "salad", 412, 18, 42, 21),
        MealTemplate("연어 덮밥", "rice", 545, 78, 31, 12),
        MealTemplate("크림 파스타", "pasta", 620, 84, 19, 24),
        MealTemplate("현미 비빔밥", "rice", 480, 92, 17, 9),
        MealTemplate("닭가슴살 포케", "salad", 390, 45, 38, 8),
        MealTemplate("토마토 파스타", "pasta", 510, 88, 16, 13),
    )

    private val mealHourSlots = listOf(8, 12, 15, 18, 21)

    /**
     * 요청한 달의 캘린더 데이터를 돌려줍니다.
     * [today] 이후의 날짜에는 기록을 만들지 않습니다.
     */
    fun monthOf(year: Int, month: Int, today: HistoryDateVO): HistoryCalendarVO {
        val lastDay = GregorianCalendar(year, month - 1, 1)
            .getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
        val days = (1..lastDay)
            .map { day -> HistoryDateVO(year, month, day) }
            .filterNot { date -> date.isAfter(today) }
            .mapNotNull { date -> calendarDayOf(date) }
        return HistoryCalendarVO(year = year, month = month, days = days)
    }

    /** 선택한 날짜의 상세 기록(식사 목록 + 하루 영양 요약)을 돌려줍니다. */
    fun dayOf(date: HistoryDateVO, today: HistoryDateVO): DailyMealHistoryVO {
        val meals = if (date.isAfter(today)) emptyList() else mealsOf(date)
        return DailyMealHistoryVO(
            date = date,
            meals = meals,
            nutrition = nutritionOf(meals),
        )
    }

    private fun calendarDayOf(date: HistoryDateVO): HistoryCalendarDayVO? {
        val meals = mealsOf(date)
        if (meals.isEmpty()) {
            return HistoryCalendarDayVO(
                date = date,
                status = DailyNutritionStatus.NOT_RECORDED,
                foodIconIds = emptyList(),
                mealCount = 0,
            )
        }
        return HistoryCalendarDayVO(
            date = date,
            status = DailyNutritionStatus.of(
                totalCalorieKcal = meals.sumOf { it.calorieKcal },
                targetCalorieKcal = TARGET_CALORIE_KCAL,
                hasRecord = true,
            ),
            foodIconIds = meals.take(2).map { it.foodIconId },
            mealCount = meals.size,
        )
    }

    private fun mealsOf(date: HistoryDateVO): List<MealHistoryVO> {
        val random = Random(seedOf(date))
        val mealCount = when (random.nextInt(10)) {
            in 0..1 -> 0
            in 2..3 -> 2
            in 4..6 -> 3
            in 7..8 -> 4
            else -> 5
        }
        return List(mealCount) { index ->
            val template = mealTemplates[random.nextInt(mealTemplates.size)]
            val hour = mealHourSlots[index]
            val minute = random.nextInt(60)
            MealHistoryVO(
                id = "mock-${seedOf(date)}-$index",
                name = template.name,
                photoUrl = "",
                foodIconId = template.foodIconId,
                recordedAtMillis = GregorianCalendar(date.year, date.month - 1, date.day, hour, minute)
                    .timeInMillis,
                calorieKcal = template.calorieKcal,
                carbohydrateGram = template.carbohydrateGram,
                proteinGram = template.proteinGram,
                fatGram = template.fatGram,
                orderIndex = index + 1,
            )
        }
    }

    private fun nutritionOf(meals: List<MealHistoryVO>): DailyNutritionVO =
        DailyNutritionVO(
            currentCalorieKcal = meals.sumOf { it.calorieKcal },
            targetCalorieKcal = TARGET_CALORIE_KCAL,
            carbohydrate = NutrientProgressVO(
                dailyGram = meals.sumOf { it.carbohydrateGram },
                goalGram = CARBOHYDRATE_GOAL_GRAM,
            ),
            protein = NutrientProgressVO(
                dailyGram = meals.sumOf { it.proteinGram },
                goalGram = PROTEIN_GOAL_GRAM,
            ),
            fat = NutrientProgressVO(
                dailyGram = meals.sumOf { it.fatGram },
                goalGram = FAT_GOAL_GRAM,
            ),
        )

    private fun seedOf(date: HistoryDateVO): Long =
        date.year * 10_000L + date.month * 100L + date.day
}
