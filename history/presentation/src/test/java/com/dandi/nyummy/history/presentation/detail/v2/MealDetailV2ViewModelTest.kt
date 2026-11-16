package com.dandi.nyummy.history.presentation.detail.v2

import com.dandi.nyummy.common.presentation.image.NyummyFoodIconCategory
import com.dandi.nyummy.history.domain.AwaitMealAnalysisUseCase
import com.dandi.nyummy.history.domain.DeleteMealFoodUseCase
import com.dandi.nyummy.history.domain.DeleteMealV2UseCase
import com.dandi.nyummy.history.domain.GetMealDetailV2UseCase
import com.dandi.nyummy.history.domain.SaveMealFoodListUseCase
import com.dandi.nyummy.history.domain.SubmitAnalysisFeedbackUseCase
import com.dandi.nyummy.history.domain.UpdateMealFoodUseCase
import com.dandi.nyummy.history.domain.UpdateMealNameV2UseCase
import com.dandi.nyummy.history.entity.MealAnalysisStatus
import com.dandi.nyummy.history.entity.MealAnalysisVersion
import com.dandi.nyummy.history.entity.MealDetailV2VO
import com.dandi.nyummy.history.entity.MealFoodChangeVO
import com.dandi.nyummy.history.entity.MealFoodNutritionVO
import com.dandi.nyummy.history.entity.MealFoodVO
import com.dandi.nyummy.history.entity.MealHistoryVO
import com.dandi.nyummy.history.presentation.FakeCompanionMemoryHelper
import com.dandi.nyummy.history.presentation.FakeHistoryRepository
import com.dandi.nyummy.history.presentation.FakeMealV2Repository
import com.dandi.nyummy.history.presentation.FakeMessageHelper
import com.dandi.nyummy.history.presentation.FakeNavigationHelper
import com.dandi.nyummy.history.presentation.FakeResourceHelper
import com.dandi.nyummy.history.presentation.FakeTTIHelper
import com.dandi.nyummy.history.presentation.R
import com.dandi.nyummy.history.presentation.detail.MealDetailMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
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
class MealDetailV2ViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val rice = MealFoodVO(1, "밥", 1.0, true, 300, 66, 6, 1, foodIconId = "5")
    private val soup = MealFoodVO(2, "된장찌개", 1.0, true, 110, 8, 7, 5, foodIconId = "15")
    private val beef = MealFoodVO(3, "불고기", 1.5, true, 150, 6, 9, 9, foodIconId = "14")
    private val repository = FakeMealV2Repository(detailOf(rice, soup, beef))
    private val historyRepository = FakeHistoryRepository()
    private val navigationHelper = FakeNavigationHelper()
    private val messageHelper = FakeMessageHelper()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // --- 보기, 식사 이름, 삭제, 평가 ---

    @Test
    fun `들어오면 식사와 음식 줄을 받아 보여 준다`() = runTest(testDispatcher) {
        val state = enter().uiState.value

        assertFalse(state.isLoading)
        assertEquals("한정식", state.meal?.name)
        assertEquals(listOf("밥", "된장찌개", "불고기"), state.foods.map { it.name })
        assertTrue(state.canEditFoods)
    }

    @Test
    fun `받아 둔 상세로 들어오면 다시 조회하지 않는다`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.onIntent(MealDetailV2Intent.Enter(7L, initial = detailOf(rice)))
        advanceUntilIdle()

        assertEquals(0, repository.getRequests)
        assertEquals(listOf("밥"), viewModel.uiState.value.foods.map { it.name })
    }

    @Test
    fun `받지 못하면 오류를 보여 주고 다시 시도하면 받는다`() = runTest(testDispatcher) {
        repository.getOverride = { throw IOException("offline") }
        val viewModel = enter()
        assertTrue(viewModel.uiState.value.isLoadFailed)

        repository.getOverride = null
        viewModel.onIntent(MealDetailV2Intent.RetryLoad)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoadFailed)
        assertNotNull(viewModel.uiState.value.meal)
    }

    @Test
    fun `식사 이름을 고치면 화면에 반영한다`() = runTest(testDispatcher) {
        val viewModel = enter()

        viewModel.onIntent(MealDetailV2Intent.ClickEditName)
        viewModel.onIntent(MealDetailV2Intent.ChangeNameDraft(" 집밥 "))
        viewModel.onIntent(MealDetailV2Intent.ConfirmEditName)
        advanceUntilIdle()

        assertEquals(listOf("집밥"), repository.renameRequests)
        assertEquals("집밥", viewModel.uiState.value.meal?.name)
        assertEquals(MealDetailMode.Viewing, viewModel.uiState.value.mode)
    }

    @Test
    fun `식사 이름 수정에 실패하면 다이얼로그를 그대로 둔다`() = runTest(testDispatcher) {
        repository.renameOverride = { throw IOException("offline") }
        val viewModel = enter()

        viewModel.onIntent(MealDetailV2Intent.ClickEditName)
        viewModel.onIntent(MealDetailV2Intent.ChangeNameDraft("집밥"))
        viewModel.onIntent(MealDetailV2Intent.ConfirmEditName)
        advanceUntilIdle()

        assertEquals("한정식", viewModel.uiState.value.meal?.name)
        assertEquals(MealDetailMode.EditingName, viewModel.uiState.value.mode)
        assertFalse(viewModel.uiState.value.isActionInFlight)
    }

    @Test
    fun `식사 이름은 20자까지만 받는다`() = runTest(testDispatcher) {
        val viewModel = enter()
        viewModel.onIntent(MealDetailV2Intent.ClickEditName)

        viewModel.onIntent(MealDetailV2Intent.ChangeNameDraft("가".repeat(21)))

        assertEquals("한정식", viewModel.uiState.value.nameDraft)
    }

    @Test
    fun `식사를 지우면 뒤로 간다`() = runTest(testDispatcher) {
        val viewModel = enter()

        viewModel.onIntent(MealDetailV2Intent.ClickDelete)
        assertEquals(MealDetailMode.ConfirmingDelete, viewModel.uiState.value.mode)
        viewModel.onIntent(MealDetailV2Intent.ConfirmDelete)
        advanceUntilIdle()

        assertEquals(listOf(7L), repository.deleteMealRequests)
        assertEquals(1, navigationHelper.backCount)
    }

    @Test
    fun `평가 대상이면 잠깐 뒤 평가 카드를 띄우고 제출하면 닫는다`() = runTest(testDispatcher) {
        repository.detail = detailOf(rice, canCreateAnalysisFeedback = true)
        val viewModel = createViewModel()
        viewModel.onIntent(MealDetailV2Intent.Enter(7L))
        runCurrent()
        assertNull(viewModel.uiState.value.feedback)

        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.feedback)
        viewModel.onIntent(MealDetailV2Intent.ChangeFeedbackRating(10))
        viewModel.onIntent(MealDetailV2Intent.SubmitFeedback)
        advanceUntilIdle()

        assertEquals(1, historyRepository.feedbackRequests.size)
        assertNull(viewModel.uiState.value.feedback)
    }

    @Test
    fun `음식 목록이 없는 식사면 화면을 닫는다`() = runTest(testDispatcher) {
        repository.detail = detailOf(status = MealAnalysisStatus.CONFIRMING)

        enter()

        assertEquals(1, navigationHelper.backCount)
    }

    // --- 음식 고치기 시트 ---

    @Test
    fun `연필을 누르면 전체 분류와 직접 설정 꺼짐으로 시트를 연다`() = runTest(testDispatcher) {
        val viewModel = enter()

        viewModel.onIntent(MealDetailV2Intent.ClickEditFood(3))

        val sheet = viewModel.uiState.value.foodSheet!!
        assertEquals("불고기", sheet.name)
        assertNull(sheet.iconCategory)
        assertFalse(sheet.isManualNutrition)
        assertTrue(sheet.canDelete)
    }

    @Test
    fun `아무것도 바꾸지 않고 저장하면 서버를 부르지 않고 닫는다`() = runTest(testDispatcher) {
        val viewModel = enterWithSheet(3)

        viewModel.onIntent(MealDetailV2Intent.SaveFoodSheet)
        advanceUntilIdle()

        assertTrue(repository.foodChanges.isEmpty())
        assertNull(viewModel.uiState.value.foodSheet)
        assertEquals(1, repository.getRequests)
    }

    @Test
    fun `직접 설정을 켜지 않으면 영양 칸이 바뀌지 않고 영양을 보내지 않는다`() = runTest(testDispatcher) {
        val viewModel = enterWithSheet(3)

        viewModel.onIntent(MealDetailV2Intent.ChangeNutrient(MealFoodNutrient.CALORIE, "999"))
        viewModel.onIntent(MealDetailV2Intent.StepPortion(increase = true))
        viewModel.onIntent(MealDetailV2Intent.SaveFoodSheet)
        advanceUntilIdle()

        assertEquals(listOf(3L to MealFoodChangeVO(portion = 2.0)), repository.foodChanges)
    }

    @Test
    fun `직접 설정을 켜고 고치면 영양을 보낸다`() = runTest(testDispatcher) {
        val viewModel = enterWithSheet(3)

        viewModel.onIntent(MealDetailV2Intent.SetManualNutrition(true))
        viewModel.onIntent(MealDetailV2Intent.ChangeNutrient(MealFoodNutrient.CALORIE, "200"))
        viewModel.onIntent(MealDetailV2Intent.SaveFoodSheet)
        advanceUntilIdle()

        assertEquals(
            listOf(3L to MealFoodChangeVO(nutrition = MealFoodNutritionVO(200, 6, 9, 9))),
            repository.foodChanges,
        )
    }

    @Test
    fun `직접 설정을 켰다 끄면 영양 칸이 처음 값으로 돌아간다`() = runTest(testDispatcher) {
        val viewModel = enterWithSheet(3)

        viewModel.onIntent(MealDetailV2Intent.SetManualNutrition(true))
        viewModel.onIntent(MealDetailV2Intent.ChangeNutrient(MealFoodNutrient.FAT, "50"))
        viewModel.onIntent(MealDetailV2Intent.SetManualNutrition(false))

        assertEquals("9", viewModel.uiState.value.foodSheet?.fat)
    }

    @Test
    fun `인분은 반 인분에서 다섯 인분 사이로만 바뀐다`() = runTest(testDispatcher) {
        val viewModel = enterWithSheet(1)

        viewModel.onIntent(MealDetailV2Intent.StepPortion(increase = false))
        viewModel.onIntent(MealDetailV2Intent.StepPortion(increase = false))
        assertEquals(0.5, viewModel.uiState.value.foodSheet!!.portion, 0.0)

        repeat(12) { viewModel.onIntent(MealDetailV2Intent.StepPortion(increase = true)) }
        assertEquals(5.0, viewModel.uiState.value.foodSheet!!.portion, 0.0)
    }

    @Test
    fun `아이콘과 분류를 고르면 아이콘 번호만 보낸다`() = runTest(testDispatcher) {
        val viewModel = enterWithSheet(3)

        viewModel.onIntent(MealDetailV2Intent.SelectIconCategory(NyummyFoodIconCategory.FRUIT))
        viewModel.onIntent(MealDetailV2Intent.SelectIcon("20"))
        assertEquals(NyummyFoodIconCategory.FRUIT, viewModel.uiState.value.foodSheet?.iconCategory)
        viewModel.onIntent(MealDetailV2Intent.SaveFoodSheet)
        advanceUntilIdle()

        assertEquals(listOf(3L to MealFoodChangeVO(iconId = 20)), repository.foodChanges)
    }

    @Test
    fun `이름을 바꾸면 계산을 기다렸다가 새 값을 보여 준다`() = runTest(testDispatcher) {
        val viewModel = enterWithSheet(3)
        val pork = beef.copy(name = "제육볶음", hasNutrition = false)
        // 서버: 이름이 바뀌면 그 줄을 다시 계산한다.
        repository.updateFoodOverride = { repository.detail = detailOf(rice, soup, pork, status = MealAnalysisStatus.WAITING) }
        repository.statuses += MealAnalysisStatus.ANALYZING

        viewModel.onIntent(MealDetailV2Intent.ChangeFoodName("제육볶음"))
        viewModel.onIntent(MealDetailV2Intent.SaveFoodSheet)
        runCurrent()

        assertTrue(viewModel.uiState.value.isCalculating)
        assertNull(viewModel.uiState.value.foodSheet)
        assertFalse(viewModel.uiState.value.canEditFoods)

        repository.detail = detailOf(rice, soup, pork.copy(hasNutrition = true, calorieKcal = 420))
        advanceUntilIdle()

        assertEquals(2, repository.statusRequests)
        assertFalse(viewModel.uiState.value.isCalculating)
        assertEquals(420, viewModel.uiState.value.foods.last().calorieKcal)
    }

    @Test
    fun `계산 중에는 시트를 열 수도 순서를 바꿀 수도 음식을 추가할 수도 없다`() = runTest(testDispatcher) {
        repository.detail = detailOf(rice, soup, status = MealAnalysisStatus.ANALYZING)
        repository.statuses += List(40) { MealAnalysisStatus.ANALYZING }
        val viewModel = createViewModel()
        viewModel.onIntent(MealDetailV2Intent.Enter(7L))
        runCurrent()

        viewModel.onIntent(MealDetailV2Intent.ClickEditFood(1))
        viewModel.onIntent(MealDetailV2Intent.MoveFood(0, 1))
        viewModel.onIntent(MealDetailV2Intent.ClickAddFood)
        runCurrent()

        assertTrue(viewModel.uiState.value.isCalculating)
        assertNull(viewModel.uiState.value.foodSheet)
        assertNull(viewModel.uiState.value.addNameInput)
        assertTrue(repository.submittedFoods.isEmpty())
    }

    @Test
    fun `계산이 실패하면 안내하고 기록 화면으로 돌아간다`() = runTest(testDispatcher) {
        repository.detail = detailOf(rice, status = MealAnalysisStatus.WAITING)
        repository.statuses += MealAnalysisStatus.FAILED

        enter()

        assertEquals(listOf(R.string.history_detail_v2_calculation_failed), messageHelper.snackBarResources)
        assertEquals(1, navigationHelper.backCount)
    }

    @Test
    fun `계산이 한도까지 끝나지 않으면 안내하고 기록 화면으로 돌아간다`() = runTest(testDispatcher) {
        repository.detail = detailOf(rice, status = MealAnalysisStatus.ANALYZING)
        repository.statuses += List(40) { MealAnalysisStatus.ANALYZING }

        enter()

        assertEquals(listOf(R.string.history_detail_v2_calculation_slow), messageHelper.snackBarResources)
        assertEquals(1, navigationHelper.backCount)
    }

    @Test
    fun `시트에서 음식을 지우면 다시 받아 보여 준다`() = runTest(testDispatcher) {
        val viewModel = enterWithSheet(2)
        repository.detail = detailOf(rice, beef)

        viewModel.onIntent(MealDetailV2Intent.ClickDeleteFood)
        advanceUntilIdle()

        assertEquals(listOf(2L), repository.deletedFoods)
        assertNull(viewModel.uiState.value.foodSheet)
        assertEquals(listOf("밥", "불고기"), viewModel.uiState.value.foods.map { it.name })
    }

    @Test
    fun `한 줄만 남은 식사에서는 음식을 지울 수 없다`() = runTest(testDispatcher) {
        repository.detail = detailOf(rice)
        val viewModel = enterWithSheet(1)
        assertFalse(viewModel.uiState.value.foodSheet!!.canDelete)

        viewModel.onIntent(MealDetailV2Intent.ClickDeleteFood)
        advanceUntilIdle()

        assertTrue(repository.deletedFoods.isEmpty())
    }

    @Test
    fun `한 줄 수정에 실패하면 시트를 그대로 두고 식사를 다시 받는다`() = runTest(testDispatcher) {
        val viewModel = enterWithSheet(3)
        repository.updateFoodOverride = { throw IOException("offline") }

        viewModel.onIntent(MealDetailV2Intent.StepPortion(increase = true))
        viewModel.onIntent(MealDetailV2Intent.SaveFoodSheet)
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.foodSheet)
        assertFalse(viewModel.uiState.value.isSavingFoods)
        assertEquals(2, repository.getRequests)
    }

    // --- 순서 바꾸기, 음식 추가 ---

    @Test
    fun `맨 아래 음식을 맨 위로 올리면 그 순서로 보내고 대표 음식이 바뀐다`() = runTest(testDispatcher) {
        val viewModel = enter()
        repository.submitOverride = {
            repository.detail = detailOf(beef, rice, soup)
            MealAnalysisStatus.COMPLETED
        }

        viewModel.onIntent(MealDetailV2Intent.MoveFood(2, 0))
        assertEquals("불고기", viewModel.uiState.value.foods.first().name)
        advanceUntilIdle()

        assertEquals(listOf(3L, 1L, 2L), repository.submittedFoods.single().map { it.mealFoodId })
        assertEquals(listOf("불고기", "밥", "된장찌개"), viewModel.uiState.value.foods.map { it.name })
        assertFalse(viewModel.uiState.value.isSavingFoods)
    }

    @Test
    fun `순서를 보내지 못하면 원래 순서로 되돌린다`() = runTest(testDispatcher) {
        val viewModel = enter()
        repository.submitOverride = { throw IOException("offline") }

        viewModel.onIntent(MealDetailV2Intent.MoveFood(2, 0))
        advanceUntilIdle()

        assertEquals(listOf("밥", "된장찌개", "불고기"), viewModel.uiState.value.foods.map { it.name })
        assertTrue(viewModel.uiState.value.canEditFoods)
    }

    @Test
    fun `음식을 추가하면 새 줄을 끝에 붙여 보내고 계산이 끝나면 반영한다`() = runTest(testDispatcher) {
        val viewModel = enter()
        val egg = MealFoodVO(4, "계란말이", 1.0, true, 120, 2, 8, 9, foodIconId = "30")
        repository.submitOverride = { MealAnalysisStatus.WAITING }

        viewModel.onIntent(MealDetailV2Intent.ClickAddFood)
        viewModel.onIntent(MealDetailV2Intent.ChangeAddName(" 계란말이 "))
        viewModel.onIntent(MealDetailV2Intent.ConfirmAddFood)
        runCurrent()

        val sent = repository.submittedFoods.single()
        assertEquals(listOf(1L, 2L, 3L, 0L), sent.map { it.mealFoodId })
        assertEquals("계란말이", sent.last().name)
        assertTrue(viewModel.uiState.value.isCalculating)
        assertNull(viewModel.uiState.value.addNameInput)

        repository.detail = detailOf(rice, soup, beef, egg)
        advanceUntilIdle()

        assertEquals(4, viewModel.uiState.value.foods.size)
        assertFalse(viewModel.uiState.value.isCalculating)
    }

    @Test
    fun `이름이 비었으면 추가하지 않고 취소하면 입력 줄을 닫는다`() = runTest(testDispatcher) {
        val viewModel = enter()

        viewModel.onIntent(MealDetailV2Intent.ClickAddFood)
        viewModel.onIntent(MealDetailV2Intent.ChangeAddName("  "))
        viewModel.onIntent(MealDetailV2Intent.ConfirmAddFood)
        advanceUntilIdle()
        assertTrue(repository.submittedFoods.isEmpty())

        viewModel.onIntent(MealDetailV2Intent.CancelAddFood)
        assertNull(viewModel.uiState.value.addNameInput)
    }

    @Test
    fun `음식이 열 줄이면 더 추가할 수 없다`() = runTest(testDispatcher) {
        repository.detail = MealDetailV2VO(
            meal = mealOf(MealAnalysisStatus.COMPLETED, false),
            foods = List(MealFoodVO.MAX_FOODS) { rice.copy(mealFoodId = it + 1L) },
        )
        val viewModel = enter()

        viewModel.onIntent(MealDetailV2Intent.ClickAddFood)

        assertFalse(viewModel.uiState.value.canAddFood)
        assertNull(viewModel.uiState.value.addNameInput)
    }

    private fun mealOf(status: MealAnalysisStatus, canCreateAnalysisFeedback: Boolean) = MealHistoryVO(
        id = "7",
        name = "한정식",
        status = status,
        analysisVersion = MealAnalysisVersion.V2,
        canCreateAnalysisFeedback = canCreateAnalysisFeedback,
    )

    private fun detailOf(
        vararg foods: MealFoodVO,
        status: MealAnalysisStatus = MealAnalysisStatus.COMPLETED,
        canCreateAnalysisFeedback: Boolean = false,
    ) = MealDetailV2VO(meal = mealOf(status, canCreateAnalysisFeedback), foods = foods.toList())

    private fun TestScope.enter(): MealDetailV2ViewModel = createViewModel().also {
        it.onIntent(MealDetailV2Intent.Enter(7L))
        advanceUntilIdle()
    }

    private fun TestScope.enterWithSheet(mealFoodId: Long): MealDetailV2ViewModel = enter().also {
        it.onIntent(MealDetailV2Intent.ClickEditFood(mealFoodId))
    }

    private fun createViewModel(): MealDetailV2ViewModel {
        val resourceHelper = FakeResourceHelper()
        val ttiHelper = FakeTTIHelper()
        return MealDetailV2ViewModel(
            getMealDetail = GetMealDetailV2UseCase(repository, resourceHelper, messageHelper, navigationHelper, ttiHelper),
            updateMealName = UpdateMealNameV2UseCase(repository, resourceHelper, messageHelper, navigationHelper, ttiHelper),
            deleteMeal = DeleteMealV2UseCase(repository, resourceHelper, messageHelper, navigationHelper, ttiHelper),
            submitAnalysisFeedback = SubmitAnalysisFeedbackUseCase(
                historyRepository, FakeCompanionMemoryHelper(), resourceHelper, messageHelper, navigationHelper, ttiHelper,
            ),
            saveMealFoodList = SaveMealFoodListUseCase(repository, resourceHelper, messageHelper, navigationHelper, ttiHelper),
            updateMealFood = UpdateMealFoodUseCase(repository, resourceHelper, messageHelper, navigationHelper, ttiHelper),
            deleteMealFood = DeleteMealFoodUseCase(repository, resourceHelper, messageHelper, navigationHelper, ttiHelper),
            awaitMealAnalysis = AwaitMealAnalysisUseCase(repository, resourceHelper, messageHelper, navigationHelper, ttiHelper),
            navigationHelper = navigationHelper,
            messageHelper = messageHelper,
        )
    }
}
