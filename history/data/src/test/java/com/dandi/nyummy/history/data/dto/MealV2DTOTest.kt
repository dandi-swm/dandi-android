package com.dandi.nyummy.history.data.dto

import com.dandi.nyummy.history.data.toRequest
import com.dandi.nyummy.history.entity.MealAnalysisStatus
import com.dandi.nyummy.history.entity.MealAnalysisVersion
import com.dandi.nyummy.history.entity.MealFoodChangeVO
import com.dandi.nyummy.history.entity.MealFoodLineVO
import com.dandi.nyummy.history.entity.MealFoodNutritionVO
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 식사 단건 v2 응답과 음식 줄 수정 요청이 앱 모델과 맞게 오가는지 확인합니다. */
class MealV2DTOTest {

    // 앱의 Retrofit 설정과 같다: null인 항목은 JSON에 쓰지 않는다.
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Test
    fun `음식 줄을 서버가 준 순서대로 옮긴다`() {
        val detail = json.decodeFromString<MealV2DTO>(
            """
            {"mealId":9,"analysisVersion":"V2","name":"한정식","status":"COMPLETED",
             "nutrition":{"calory":612,"carbs":82,"protein":24,"fat":18},
             "foods":[
               {"mealFoodId":3,"name":"밥","portion":1.0,"nutrition":{"calory":300,"carbs":66,"protein":6,"fat":1},"isNutritionFixed":false,"iconId":5},
               {"mealFoodId":1,"name":"불고기","portion":1.5,"nutrition":{"calory":150,"carbs":6,"protein":9,"fat":9},"isNutritionFixed":true,"iconId":14}
             ]}
            """.trimIndent(),
        ).toVO()

        assertEquals("9", detail.meal.id)
        assertEquals(MealAnalysisVersion.V2, detail.meal.analysisVersion)
        assertEquals(MealAnalysisStatus.COMPLETED, detail.meal.status)
        assertEquals(612, detail.meal.calorieKcal)
        assertEquals(listOf(3L, 1L), detail.foods.map { it.mealFoodId })
        val second = detail.foods[1]
        assertEquals("불고기", second.name)
        assertEquals(1.5, second.portion, 0.0)
        assertTrue(second.hasNutrition)
        assertEquals(150, second.calorieKcal)
        assertTrue(second.isNutritionFixed)
        assertEquals("14", second.foodIconId)
    }

    @Test
    fun `영양과 아이콘이 아직 없는 줄은 계산 전으로 옮긴다`() {
        val food = json.decodeFromString<MealFoodV2DTO>("""{"mealFoodId":4,"name":"김치찌개","portion":1.0}""").toVO()!!

        assertFalse(food.hasNutrition)
        assertEquals(0, food.calorieKcal)
        assertEquals("", food.foodIconId)
        assertFalse(food.isNutritionFixed)
    }

    @Test
    fun `foods가 없으면 빈 목록이다`() {
        val detail = json.decodeFromString<MealV2DTO>("""{"mealId":9,"analysisVersion":"V1","status":"COMPLETED"}""").toVO()

        assertTrue(detail.foods.isEmpty())
        assertEquals(MealAnalysisVersion.V1, detail.meal.analysisVersion)
    }

    @Test
    fun `ID가 없는 줄은 버린다`() {
        val detail = MealV2DTO(foods = listOf(MealFoodV2DTO(name = "밥"), MealFoodV2DTO(mealFoodId = 2, name = "국"))).toVO()

        assertEquals(listOf(2L), detail.foods.map { it.mealFoodId })
    }

    @Test
    fun `모르는 분석 버전은 V1로 본다`() {
        assertEquals(MealAnalysisVersion.V1, MealV2DTO(analysisVersion = "V9").toVO().meal.analysisVersion)
        assertEquals(MealAnalysisVersion.V1, MealV2DTO().toVO().meal.analysisVersion)
    }

    @Test
    fun `한 줄 수정 요청에는 바뀐 항목만 들어간다`() {
        val body = json.encodeToString(MealFoodChangeVO(portion = 2.0).toRequest())

        assertEquals("""{"portion":2.0}""", body)
    }

    @Test
    fun `영양은 직접 설정한 변경에만 들어간다`() {
        val withoutNutrition = json.encodeToString(MealFoodChangeVO(name = "제육볶음", iconId = 14).toRequest())
        val withNutrition = json.encodeToString(
            MealFoodChangeVO(nutrition = MealFoodNutritionVO(200, 10, 20, 8)).toRequest(),
        )

        assertEquals("""{"name":"제육볶음","iconId":14}""", withoutNutrition)
        assertEquals("""{"nutrition":{"calory":200,"carbs":10,"protein":20,"fat":8}}""", withNutrition)
    }

    @Test
    fun `목록 요청에서 새 줄은 mealFoodId 없이 보낸다`() {
        val body = json.encodeToString(
            UpdateMealFoodsV2RequestDTO(
                listOf(
                    MealFoodLineVO(mealFoodId = 3, name = "밥", portion = 1.0).toRequest(),
                    MealFoodLineVO(name = "계란말이").toRequest(),
                ),
            ),
        )

        assertEquals(
            """{"foods":[{"mealFoodId":3,"name":"밥","portion":1.0},{"name":"계란말이","portion":1.0}]}""",
            body,
        )
    }
}
