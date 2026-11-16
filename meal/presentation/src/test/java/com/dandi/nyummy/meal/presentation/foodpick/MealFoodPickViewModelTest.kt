package com.dandi.nyummy.meal.presentation.foodpick

import com.dandi.nyummy.common.domain.error.HttpResponseException
import com.dandi.nyummy.common.domain.error.HttpResponseStatus
import com.dandi.nyummy.history.domain.HistoryPage
import com.dandi.nyummy.meal.domain.GetFoundFoodsUseCase
import com.dandi.nyummy.meal.domain.MealErrorType
import com.dandi.nyummy.meal.domain.SubmitMealFoodsUseCase
import com.dandi.nyummy.meal.entity.FoundFoodVO
import com.dandi.nyummy.meal.entity.MealFoodConfirmVO
import com.dandi.nyummy.meal.presentation.FakeMealFoodRepository
import com.dandi.nyummy.meal.presentation.FakeResourceHelper
import com.dandi.nyummy.meal.presentation.FakeTTIHelper
import com.dandi.nyummy.meal.presentation.RecordingMessageHelper
import com.dandi.nyummy.meal.presentation.RecordingNavigationHelper
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class MealFoodPickViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val repository = FakeMealFoodRepository()
    private val navigationHelper = RecordingNavigationHelper()
    private val messageHelper = RecordingMessageHelper()
    private val foods = listOf(FoundFoodVO(11, "밥"), FoundFoodVO(12, "된장찌개"), FoundFoodVO(13, "배추김치"))

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository.confirm = MealFoodConfirmVO(isConfirmable = true, foods = foods)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `들어오면 찾은 음식을 모두 체크한 채로 보여 준다`() = runTest(testDispatcher) {
        val viewModel = enter()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(listOf("밥", "된장찌개", "배추김치"), state.draft.lines.map { it.name })
        assertEquals(3, state.draft.checkedCount)
        assertTrue(navigationHelper.calls.isEmpty())
    }

    @Test
    fun `같은 식사로 다시 들어와도 한 번만 불러온다`() = runTest(testDispatcher) {
        val viewModel = enter()

        viewModel.onIntent(MealFoodPickIntent.Enter(MEAL_ID))
        advanceUntilIdle()

        assertEquals(1, repository.loadCount)
    }

    @Test
    fun `이미 계산을 시작한 식사면 목록을 보여 주지 않고 기록 탭으로 간다`() = runTest(testDispatcher) {
        repository.confirm = MealFoodConfirmVO(isConfirmable = false, foods = foods)

        val viewModel = enter()

        assertEquals(BACK_TO_HISTORY, navigationHelper.calls)
        assertTrue(viewModel.uiState.value.draft.lines.isEmpty())
    }

    @Test
    fun `지워진 식사면 기록 탭으로 간다`() = runTest(testDispatcher) {
        repository.loadError = httpException(404, MealErrorType.MEAL_NOT_FOUND.type)

        enter()

        assertEquals(BACK_TO_HISTORY, navigationHelper.calls)
    }

    @Test
    fun `불러오지 못하면 머물고 다시 불러올 수 있다`() = runTest(testDispatcher) {
        repository.loadError = IOException("network")
        val viewModel = enter()

        assertTrue(viewModel.uiState.value.isLoadFailed)
        assertTrue(navigationHelper.calls.isEmpty())

        repository.loadError = null
        viewModel.onIntent(MealFoodPickIntent.ClickRetry)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoadFailed)
        assertEquals(3, viewModel.uiState.value.draft.lines.size)
    }

    @Test
    fun `가져가면 체크한 음식만 보내고 연출 없이 기록 탭으로 간다`() = runTest(testDispatcher) {
        val viewModel = enter()
        viewModel.onIntent(MealFoodPickIntent.ToggleFood(3))

        viewModel.onIntent(MealFoodPickIntent.ClickSubmit)
        advanceUntilIdle()

        assertEquals(listOf(MEAL_ID to foods.take(2)), repository.submitted)
        assertEquals(BACK_TO_HISTORY, navigationHelper.calls)
    }

    @Test
    fun `이름으로 추가한 음식도 같이 보낸다`() = runTest(testDispatcher) {
        val viewModel = enter()
        viewModel.onIntent(MealFoodPickIntent.OpenNameInput)
        viewModel.onIntent(MealFoodPickIntent.ChangeNameInput("김밥"))
        viewModel.onIntent(MealFoodPickIntent.ConfirmNameInput)

        viewModel.onIntent(MealFoodPickIntent.ClickSubmit)
        advanceUntilIdle()

        assertEquals(foods + FoundFoodVO(name = "김밥"), repository.submitted.single().second)
    }

    @Test
    fun `가져가는 사이 서버가 계산을 시작했으면 기록 탭으로 간다`() = runTest(testDispatcher) {
        repository.submitError = httpException(409, MealErrorType.MEAL_FOODS_NOT_UPDATABLE.type)
        val viewModel = enter()

        viewModel.onIntent(MealFoodPickIntent.ClickSubmit)
        advanceUntilIdle()

        assertEquals(BACK_TO_HISTORY, navigationHelper.calls)
    }

    @Test
    fun `보내지 못하면 머물고 다시 가져갈 수 있다`() = runTest(testDispatcher) {
        repository.submitError = IOException("network")
        val viewModel = enter()

        viewModel.onIntent(MealFoodPickIntent.ClickSubmit)
        advanceUntilIdle()

        assertTrue(navigationHelper.calls.isEmpty())
        assertFalse(viewModel.uiState.value.isSubmitting)

        repository.submitError = null
        viewModel.onIntent(MealFoodPickIntent.ClickSubmit)
        advanceUntilIdle()

        assertEquals(BACK_TO_HISTORY, navigationHelper.calls)
    }

    @Test
    fun `하나도 고르지 않으면 보내지 않는다`() = runTest(testDispatcher) {
        val viewModel = enter()
        (1..3).forEach { viewModel.onIntent(MealFoodPickIntent.ToggleFood(it)) }

        viewModel.onIntent(MealFoodPickIntent.ClickSubmit)
        advanceUntilIdle()

        assertTrue(repository.submitted.isEmpty())
        assertTrue(navigationHelper.calls.isEmpty())
    }

    @Test
    fun `보내는 중에는 다시 보내거나 고치거나 닫을 수 없다`() = runTest(testDispatcher) {
        repository.submitGate = CompletableDeferred()
        val viewModel = enter()
        viewModel.onIntent(MealFoodPickIntent.ClickSubmit)
        advanceUntilIdle()

        viewModel.onIntent(MealFoodPickIntent.ClickSubmit)
        viewModel.onIntent(MealFoodPickIntent.ToggleFood(1))
        viewModel.onIntent(MealFoodPickIntent.ClickClose)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isSubmitting)
        assertEquals(3, viewModel.uiState.value.draft.checkedCount)
        assertTrue(navigationHelper.calls.isEmpty())

        repository.submitGate?.complete(Unit)
        advanceUntilIdle()

        assertEquals(1, repository.submitted.size)
        assertEquals(BACK_TO_HISTORY, navigationHelper.calls)
    }

    @Test
    fun `닫기는 기록을 취소하지 않고 돌아가기만 한다`() = runTest(testDispatcher) {
        val viewModel = enter()

        viewModel.onIntent(MealFoodPickIntent.ClickClose)
        advanceUntilIdle()

        assertEquals(listOf("back"), navigationHelper.calls)
        assertTrue(repository.canceled.isEmpty())
        assertTrue(repository.submitted.isEmpty())
    }

    private suspend fun kotlinx.coroutines.test.TestScope.enter(): MealFoodPickViewModel {
        val viewModel = MealFoodPickViewModel(
            getFoundFoods = GetFoundFoodsUseCase(repository, FakeResourceHelper(), messageHelper, navigationHelper, FakeTTIHelper()),
            submitMealFoods = SubmitMealFoodsUseCase(repository, FakeResourceHelper(), messageHelper, navigationHelper, FakeTTIHelper()),
            navigationHelper = navigationHelper,
        )
        viewModel.onIntent(MealFoodPickIntent.Enter(MEAL_ID))
        advanceUntilIdle()
        return viewModel
    }

    private fun httpException(code: Int, errorCode: String? = null) = HttpResponseException(
        status = HttpResponseStatus.create(code),
        rawCode = code,
        errorRequestUrl = "https://test/api/v2/meals/7",
        msg = "Http Request Failed ($code)",
        cause = errorCode?.let(::Throwable),
    )

    private companion object {
        const val MEAL_ID = 7L

        /** 이 화면을 닫고 기록 탭을 여는 이동. 홈에서 링크로 들어왔어도 기록 탭에 닿는다. */
        val BACK_TO_HISTORY = listOf("back", "to:${HistoryPage.PATH}")
    }
}
