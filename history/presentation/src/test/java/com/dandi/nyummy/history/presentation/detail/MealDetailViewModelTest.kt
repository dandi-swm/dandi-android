package com.dandi.nyummy.history.presentation.detail

import com.dandi.nyummy.history.domain.DeleteMealUseCase
import com.dandi.nyummy.history.domain.GetMealDetailUseCase
import com.dandi.nyummy.history.domain.SubmitAnalysisFeedbackUseCase
import com.dandi.nyummy.history.domain.UpdateMealNameUseCase
import com.dandi.nyummy.history.entity.AnalysisFeedbackReason
import com.dandi.nyummy.history.entity.MealHistoryVO
import com.dandi.nyummy.history.presentation.FakeHistoryRepository
import com.dandi.nyummy.history.presentation.FakeMessageHelper
import com.dandi.nyummy.history.presentation.FakeNavigationHelper
import com.dandi.nyummy.history.presentation.FakeResourceHelper
import com.dandi.nyummy.history.presentation.FakeTTIHelper
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class MealDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val repository = FakeHistoryRepository()
    private val navigationHelper = FakeNavigationHelper()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `들어오면 그 식사를 받아 보여 준다`() = runTest(testDispatcher) {
        repository.mealOverride = { MealHistoryVO(id = it.toString(), name = "비빔밥", catComment = "맛있겠다!") }

        val viewModel = enter(mealId = 3L)

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("비빔밥", state.meal?.name)
        assertEquals("맛있겠다!", state.meal?.catComment)
    }

    @Test
    fun `같은 식사로 다시 들어와도 다시 받지 않는다`() = runTest(testDispatcher) {
        var requests = 0
        repository.mealOverride = {
            requests++
            MealHistoryVO(id = it.toString())
        }
        val viewModel = enter(mealId = 3L)

        viewModel.onIntent(MealDetailIntent.Enter(3L))
        advanceUntilIdle()

        assertEquals(1, requests)
    }

    @Test
    fun `받지 못하면 오류를 보여 주고 다시 시도하면 받는다`() = runTest(testDispatcher) {
        repository.mealOverride = { throw IOException("offline") }
        val viewModel = enter()
        assertTrue(viewModel.uiState.value.isLoadFailed)
        assertNull(viewModel.uiState.value.meal)

        repository.mealOverride = { MealHistoryVO(id = it.toString(), name = "김밥") }
        viewModel.onIntent(MealDetailIntent.RetryLoad)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoadFailed)
        assertEquals("김밥", viewModel.uiState.value.meal?.name)
    }

    @Test
    fun `이름을 바꾸면 상세의 이름을 바꾸고 보기로 돌아간다`() = runTest(testDispatcher) {
        val viewModel = enter()

        viewModel.onIntent(MealDetailIntent.ClickEditName)
        viewModel.onIntent(MealDetailIntent.ChangeNameDraft("  연어 포케 "))
        viewModel.onIntent(MealDetailIntent.ConfirmEditName)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("연어 포케", state.meal?.name)
        assertEquals(MealDetailMode.Viewing, state.mode)
        assertFalse(state.isActionInFlight)
    }

    @Test
    fun `저장하는 동안에는 이름 수정을 취소할 수 없다`() = runTest(testDispatcher) {
        val gate = CompletableDeferred<MealHistoryVO>()
        repository.updateOverride = { _, _ -> gate.await() }
        val viewModel = enter()
        viewModel.onIntent(MealDetailIntent.ClickEditName)
        viewModel.onIntent(MealDetailIntent.ChangeNameDraft("연어 포케"))
        viewModel.onIntent(MealDetailIntent.ConfirmEditName)
        runCurrent()

        viewModel.onIntent(MealDetailIntent.CancelEditName)

        assertEquals(MealDetailMode.EditingName, viewModel.uiState.value.mode)
        gate.complete(MealHistoryVO(id = "1", name = "연어 포케"))
        advanceUntilIdle()
    }

    @Test
    fun `지우면 한 번만 보내고 기록 화면으로 돌아간다`() = runTest(testDispatcher) {
        val gate = CompletableDeferred<Unit>()
        repository.deleteOverride = { gate.await() }
        val viewModel = enter(mealId = 5L)

        viewModel.onIntent(MealDetailIntent.ClickDelete)
        viewModel.onIntent(MealDetailIntent.ConfirmDelete)
        runCurrent()
        viewModel.onIntent(MealDetailIntent.ConfirmDelete)
        viewModel.onIntent(MealDetailIntent.CancelDelete) // 지우는 중 취소도 무시된다
        assertEquals(MealDetailMode.ConfirmingDelete, viewModel.uiState.value.mode)
        gate.complete(Unit)
        advanceUntilIdle()

        assertEquals(listOf(5L), repository.deleteRequests)
        assertEquals(1, navigationHelper.backCount)
    }

    @Test
    fun `지우지 못하면 확인 다이얼로그를 열어 둔 채 다시 시도하게 한다`() = runTest(testDispatcher) {
        repository.deleteOverride = { throw IOException("offline") }
        val viewModel = enter()

        viewModel.onIntent(MealDetailIntent.ClickDelete)
        viewModel.onIntent(MealDetailIntent.ConfirmDelete)
        advanceUntilIdle()

        assertEquals(MealDetailMode.ConfirmingDelete, viewModel.uiState.value.mode)
        assertFalse(viewModel.uiState.value.isActionInFlight)
        assertEquals(0, navigationHelper.backCount)
    }

    @Test
    fun `평가 대상이면 상세를 받고 1500ms 뒤에 평가 카드를 띄운다`() = runTest(testDispatcher) {
        repository.mealOverride = { MealHistoryVO(id = it.toString(), canCreateAnalysisFeedback = true) }
        val viewModel = createViewModel()
        viewModel.onIntent(MealDetailIntent.Enter(1L))
        runCurrent()

        advanceTimeBy(1_499)
        assertNull(viewModel.uiState.value.feedback)
        advanceTimeBy(2)
        assertNotNull(viewModel.uiState.value.feedback)
    }

    @Test
    fun `평가 대상이 아니면 평가 카드를 띄우지 않는다`() = runTest(testDispatcher) {
        val viewModel = enter()

        assertNull(viewModel.uiState.value.feedback)
    }

    @Test
    fun `아쉬운 점은 하나만 고르고 같은 것을 다시 누르면 풀린다`() = runTest(testDispatcher) {
        val viewModel = enterFeedbackTarget()

        viewModel.onIntent(MealDetailIntent.SelectFeedbackReason(AnalysisFeedbackReason.WRONG_FOOD))
        viewModel.onIntent(MealDetailIntent.SelectFeedbackReason(AnalysisFeedbackReason.MISSING_FOOD))
        assertEquals(AnalysisFeedbackReason.MISSING_FOOD, viewModel.uiState.value.feedback?.selectedReason)

        viewModel.onIntent(MealDetailIntent.SelectFeedbackReason(AnalysisFeedbackReason.MISSING_FOOD))
        assertNull(viewModel.uiState.value.feedback?.selectedReason)
    }

    @Test
    fun `별점을 고르기 전에는 보내지 않는다`() = runTest(testDispatcher) {
        val viewModel = enterFeedbackTarget()

        viewModel.onIntent(MealDetailIntent.SubmitFeedback)
        advanceUntilIdle()

        assertTrue(repository.feedbackRequests.isEmpty())
    }

    @Test
    fun `평가를 보내면 카드를 닫고 상세는 그대로 둔다`() = runTest(testDispatcher) {
        val viewModel = enterFeedbackTarget()

        viewModel.onIntent(MealDetailIntent.ChangeFeedbackRating(7))
        viewModel.onIntent(MealDetailIntent.SelectFeedbackReason(AnalysisFeedbackReason.WRONG_PORTION))
        viewModel.onIntent(MealDetailIntent.SubmitFeedback)
        advanceUntilIdle()

        assertEquals(Triple(1L, 7, "양을 다르게 봤어요"), repository.feedbackRequests.single())
        assertNull(viewModel.uiState.value.feedback)
        assertNotNull(viewModel.uiState.value.meal)
    }

    @Test
    fun `평가를 보내지 못하면 카드를 남겨 다시 보낼 수 있게 한다`() = runTest(testDispatcher) {
        repository.feedbackOverride = { throw IOException("offline") }
        val viewModel = enterFeedbackTarget()

        viewModel.onIntent(MealDetailIntent.ChangeFeedbackRating(4))
        viewModel.onIntent(MealDetailIntent.SubmitFeedback)
        advanceUntilIdle()

        val feedback = viewModel.uiState.value.feedback
        assertNotNull(feedback)
        assertFalse(feedback!!.isSubmitting)
        assertTrue(feedback.canSubmit)
    }

    @Test
    fun `보내는 동안 바꾼 입력은 무시해 화면과 보낸 내용이 같게 둔다`() = runTest(testDispatcher) {
        val gate = CompletableDeferred<Unit>()
        repository.feedbackOverride = {
            gate.await()
            throw IOException("offline")
        }
        val viewModel = enterFeedbackTarget()
        viewModel.onIntent(MealDetailIntent.ChangeFeedbackRating(7))
        viewModel.onIntent(MealDetailIntent.SelectFeedbackReason(AnalysisFeedbackReason.WRONG_FOOD))
        viewModel.onIntent(MealDetailIntent.ChangeFeedbackComment("국이 빠졌어요"))
        viewModel.onIntent(MealDetailIntent.SubmitFeedback)
        runCurrent() // 요청이 나가 응답을 기다리는 중

        viewModel.onIntent(MealDetailIntent.ChangeFeedbackRating(2))
        viewModel.onIntent(MealDetailIntent.SelectFeedbackReason(AnalysisFeedbackReason.MISSING_FOOD))
        viewModel.onIntent(MealDetailIntent.ChangeFeedbackComment("다른 내용"))
        viewModel.onIntent(MealDetailIntent.DismissFeedback) // 보내는 중에는 닫히지 않는다

        val feedback = viewModel.uiState.value.feedback
        assertEquals(7, feedback?.ratingHalfSteps)
        assertEquals(AnalysisFeedbackReason.WRONG_FOOD, feedback?.selectedReason)
        assertEquals("국이 빠졌어요", feedback?.comment)
        assertEquals(Triple(1L, 7, "음식을 잘못 알아봤어요\n국이 빠졌어요"), repository.feedbackRequests.single())

        gate.complete(Unit)
        advanceUntilIdle()
        viewModel.onIntent(MealDetailIntent.SelectFeedbackReason(AnalysisFeedbackReason.MISSING_FOOD))
        assertEquals(AnalysisFeedbackReason.MISSING_FOOD, viewModel.uiState.value.feedback?.selectedReason)
    }

    @Test
    fun `뒤로를 누르면 기록 화면으로 돌아간다`() = runTest(testDispatcher) {
        val viewModel = enter()

        viewModel.onIntent(MealDetailIntent.ClickBack)

        assertEquals(1, navigationHelper.backCount)
    }

    private fun TestScope.enter(mealId: Long = 1L): MealDetailViewModel =
        createViewModel().also {
            it.onIntent(MealDetailIntent.Enter(mealId))
            advanceUntilIdle()
        }

    /** 평가 대상인 식사 1 의 상세에 들어와 평가 카드가 뜰 때까지 기다린다. */
    private fun TestScope.enterFeedbackTarget(): MealDetailViewModel {
        repository.mealOverride = { MealHistoryVO(id = it.toString(), canCreateAnalysisFeedback = true) }
        return enter(mealId = 1L)
    }

    private fun createViewModel(): MealDetailViewModel {
        val resourceHelper = FakeResourceHelper()
        val messageHelper = FakeMessageHelper()
        val ttiHelper = FakeTTIHelper()
        return MealDetailViewModel(
            getMealDetail = GetMealDetailUseCase(repository, resourceHelper, messageHelper, navigationHelper, ttiHelper),
            updateMealName = UpdateMealNameUseCase(repository, resourceHelper, messageHelper, navigationHelper, ttiHelper),
            deleteMeal = DeleteMealUseCase(repository, resourceHelper, messageHelper, navigationHelper, ttiHelper),
            submitAnalysisFeedback = SubmitAnalysisFeedbackUseCase(
                repository, resourceHelper, messageHelper, navigationHelper, ttiHelper,
            ),
            navigationHelper = navigationHelper,
        )
    }
}
