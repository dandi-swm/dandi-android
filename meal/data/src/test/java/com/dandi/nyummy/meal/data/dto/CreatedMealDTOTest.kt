package com.dandi.nyummy.meal.data.dto

import com.dandi.nyummy.meal.entity.FoundFoodVO
import com.dandi.nyummy.meal.entity.MealRecognition
import com.dandi.nyummy.meal.entity.RewardBoxGrade
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 식사 생성 응답(v2)을 음식 인식 결과와 찾은 음식 목록으로 옮기는지 확인합니다. */
class CreatedMealDTOTest {

    @Test
    fun `음식을 찾으면 이름과 찾은 음식을 순서대로 옮긴다`() {
        val created = CreatedMealDTO(
            id = 7,
            status = "CONFIRMING",
            rewardBoxCategory = "SILVER",
            name = "한정식",
            foods = listOf(MealFoodDTO(mealFoodId = 11, name = "밥"), MealFoodDTO(mealFoodId = 12, name = "된장찌개")),
        ).toVO()

        assertEquals(7L, created.mealId)
        assertEquals(MealRecognition.CONFIRMING, created.recognition)
        assertEquals(RewardBoxGrade.SILVER, created.rewardBoxGrade)
        assertEquals("한정식", created.name)
        assertEquals(listOf(FoundFoodVO(11, "밥"), FoundFoodVO(12, "된장찌개")), created.foods)
    }

    @Test
    fun `음식이 아니면 음식 목록이 비고 상자 등급은 그대로다`() {
        val created = CreatedMealDTO(id = 7, status = "FAILED", rewardBoxCategory = "BRONZE", name = "", foods = null).toVO()

        assertEquals(MealRecognition.NOT_FOOD, created.recognition)
        assertEquals(RewardBoxGrade.BRONZE, created.rewardBoxGrade)
        assertTrue(created.foods.isEmpty())
    }

    @Test
    fun `기한 안에 찾지 못하면 인식 중이고 이름과 목록이 없다`() {
        val created = CreatedMealDTO(id = 7, status = "RECOGNIZING", rewardBoxCategory = "GOLD").toVO()

        assertEquals(MealRecognition.RECOGNIZING, created.recognition)
        assertEquals("", created.name)
        assertTrue(created.foods.isEmpty())
    }

    @Test
    fun `모르는 상태나 빈 상태는 홈에서 기다리는 인식 중으로 둔다`() {
        assertEquals(MealRecognition.RECOGNIZING, CreatedMealDTO(id = 7, status = "WAITING").toVO().recognition)
        assertEquals(MealRecognition.RECOGNIZING, CreatedMealDTO(id = 7, status = null).toVO().recognition)
    }

    @Test
    fun `음식을 찾았다고 하지 않았으면 목록이 와도 쓰지 않는다`() {
        val created = CreatedMealDTO(status = "RECOGNIZING", foods = listOf(MealFoodDTO(11, "밥"))).toVO()

        assertTrue(created.foods.isEmpty())
    }

    @Test
    fun `ID나 이름이 없는 줄은 버린다`() {
        val created = CreatedMealDTO(
            status = "CONFIRMING",
            foods = listOf(MealFoodDTO(null, "밥"), MealFoodDTO(12, " "), MealFoodDTO(13, "김치")),
        ).toVO()

        assertEquals(listOf(FoundFoodVO(13, "김치")), created.foods)
    }

    @Test
    fun `확인 대기 식사만 음식을 고를 수 있다`() {
        val foods = listOf(MealFoodDTO(11, "밥"))

        assertTrue(MealFoodConfirmDTO(status = "CONFIRMING", foods = foods).toVO().isConfirmable)
        assertEquals(listOf(FoundFoodVO(11, "밥")), MealFoodConfirmDTO(status = "CONFIRMING", foods = foods).toVO().foods)
        listOf("WAITING", "ANALYZING", "COMPLETED", "FAILED", "RECOGNIZING", null).forEach { status ->
            assertFalse("$status", MealFoodConfirmDTO(status = status, foods = foods).toVO().isConfirmable)
        }
    }
}
