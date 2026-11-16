package com.dandi.nyummy.meal.domain

import com.dandi.nyummy.common.domain.error.HttpResponseException
import com.dandi.nyummy.common.domain.error.HttpResponseStatus
import com.dandi.nyummy.common.domain.helper.StringResource
import com.dandi.nyummy.meal.entity.FoundFoodVO
import com.dandi.nyummy.meal.entity.MealFoodConfirmVO
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 찾은 음식 확인 흐름(불러오기, 고른 목록 보내기, 기록 취소)의 결과와 안내를 확인합니다. */
class MealFoodUseCasesTest {

    private val messageHelper = SubmitMealUseCaseTest.RecordingMessageHelper()
    private val repository = FakeMealFoodRepository()
    private val foods = listOf(FoundFoodVO(11, "밥"), FoundFoodVO(name = "김밥"))

    private val submitFoods = SubmitMealFoodsUseCase(
        repository, SubmitMealUseCaseTest.FakeResourceHelper(), messageHelper,
        SubmitMealUseCaseTest.FakeNavigationHelper(), SubmitMealUseCaseTest.FakeTTIHelper(),
    )
    private val cancelMeal = CancelMealUseCase(
        repository, SubmitMealUseCaseTest.FakeResourceHelper(), messageHelper,
        SubmitMealUseCaseTest.FakeNavigationHelper(), SubmitMealUseCaseTest.FakeTTIHelper(),
    )
    private val getFoundFoods = GetFoundFoodsUseCase(
        repository, SubmitMealUseCaseTest.FakeResourceHelper(), messageHelper,
        SubmitMealUseCaseTest.FakeNavigationHelper(), SubmitMealUseCaseTest.FakeTTIHelper(),
    )

    @Test
    fun `고른 음식을 서버가 받으면 접수고 안내하지 않는다`() = runBlocking {
        val result = submitFoods(7L, foods)

        assertEquals(Result.success(SubmitMealFoodsResult.ACCEPTED), result)
        assertEquals(listOf(7L to foods), repository.submitted)
        assertTrue(messageHelper.snackBars.isEmpty())
    }

    @Test
    fun `이미 계산을 시작한 식사면 그 사실을 알리고 닫으라고 돌려준다`() = runBlocking {
        repository.error = httpException(409, MealErrorType.MEAL_FOODS_NOT_UPDATABLE.type)

        val result = submitFoods(7L, foods)

        assertEquals(Result.success(SubmitMealFoodsResult.CLOSED), result)
        assertEquals(listOf(StringResource.MEAL_FOODS_ALREADY_ANALYZING.name), messageHelper.snackBars.map { it.messageText })
    }

    @Test
    fun `지워진 식사에 보내도 닫으라고 돌려준다`() = runBlocking {
        repository.error = httpException(404, MealErrorType.MEAL_NOT_FOUND.type)

        assertEquals(Result.success(SubmitMealFoodsResult.CLOSED), submitFoods(7L, foods))
        assertEquals(listOf(StringResource.MEAL_FOODS_MEAL_GONE.name), messageHelper.snackBars.map { it.messageText })
    }

    @Test
    fun `네트워크가 끊기거나 모르는 오류면 다시 시도하라고 알리고 실패로 돌려준다`() = runBlocking {
        repository.error = java.io.IOException("network")
        assertTrue(submitFoods(7L, foods).isFailure)

        repository.error = httpException(400, "api.common.invalidInputValue")
        assertTrue(submitFoods(7L, foods).isFailure)

        assertEquals(
            List(2) { StringResource.MEAL_FOODS_SUBMIT_FAILED.name },
            messageHelper.snackBars.map { it.messageText },
        )
    }

    @Test
    fun `기록 취소는 이미 지워진 식사도 성공이다`() = runBlocking {
        assertTrue(cancelMeal(7L).isSuccess)
        assertEquals(listOf(7L), repository.canceled)

        repository.error = httpException(404, MealErrorType.MEAL_NOT_FOUND.type)
        assertTrue(cancelMeal(7L).isSuccess)
    }

    @Test
    fun `기록 취소가 실패해도 안내하지 않는다`() = runBlocking {
        repository.error = java.io.IOException("network")
        assertTrue(cancelMeal(7L).isFailure)

        repository.error = httpException(500)
        assertTrue(cancelMeal(7L).isFailure)

        assertTrue(messageHelper.snackBars.isEmpty())
        assertTrue(messageHelper.dialogs.isEmpty())
    }

    @Test
    fun `확인 대기 식사의 찾은 음식을 돌려준다`() = runBlocking {
        repository.confirm = MealFoodConfirmVO(isConfirmable = true, foods = foods)

        assertEquals(Result.success(MealFoodConfirmVO(isConfirmable = true, foods = foods)), getFoundFoods(7L))
        assertTrue(messageHelper.snackBars.isEmpty())
    }

    @Test
    fun `이미 계산을 시작한 식사를 불러오면 그 사실을 알린다`() = runBlocking {
        repository.confirm = MealFoodConfirmVO(isConfirmable = false, foods = foods)

        val result = getFoundFoods(7L)

        assertFalse(result.getOrThrow().isConfirmable)
        assertEquals(listOf(StringResource.MEAL_FOODS_ALREADY_ANALYZING.name), messageHelper.snackBars.map { it.messageText })
    }

    @Test
    fun `지워진 식사를 불러오면 알리고 확인할 수 없는 식사로 돌려준다`() = runBlocking {
        repository.error = httpException(404, MealErrorType.MEAL_NOT_FOUND.type)

        assertEquals(Result.success(MealFoodConfirmVO.empty), getFoundFoods(7L))
        assertEquals(listOf(StringResource.MEAL_FOODS_MEAL_GONE.name), messageHelper.snackBars.map { it.messageText })
    }

    @Test
    fun `찾은 음식을 불러오지 못하면 알리고 실패로 돌려준다`() = runBlocking {
        repository.error = java.io.IOException("network")

        assertTrue(getFoundFoods(7L).isFailure)
        assertEquals(listOf(StringResource.MEAL_FOODS_LOAD_FAILED.name), messageHelper.snackBars.map { it.messageText })
    }

    /** 서버 에러 바디의 `code` 를 cause 로 담는 실제 변환(BaseRemoteDataSource)과 같은 형태로 만든다. */
    private fun httpException(code: Int, errorCode: String? = null) = HttpResponseException(
        status = HttpResponseStatus.create(code),
        rawCode = code,
        errorRequestUrl = "https://test/api/v2/meals/7/foods",
        msg = "Http Request Failed ($code)",
        cause = errorCode?.let(::Throwable),
    )

    private class FakeMealFoodRepository : MealFoodRepository {
        var error: Exception? = null
        var confirm = MealFoodConfirmVO()
        val submitted = mutableListOf<Pair<Long, List<FoundFoodVO>>>()
        val canceled = mutableListOf<Long>()

        override suspend fun getFoundFoods(mealId: Long): MealFoodConfirmVO {
            error?.let { throw it }
            return confirm
        }

        override suspend fun submitFoods(mealId: Long, foods: List<FoundFoodVO>) {
            error?.let { throw it }
            submitted += mealId to foods
        }

        override suspend fun cancelMeal(mealId: Long) {
            error?.let { throw it }
            canceled += mealId
        }
    }
}
