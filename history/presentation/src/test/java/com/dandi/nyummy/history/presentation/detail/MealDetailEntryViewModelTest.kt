package com.dandi.nyummy.history.presentation.detail

import com.dandi.nyummy.history.domain.GetMealDetailV2UseCase
import com.dandi.nyummy.history.entity.MealAnalysisStatus
import com.dandi.nyummy.history.entity.MealAnalysisVersion
import com.dandi.nyummy.history.entity.MealDetailV2VO
import com.dandi.nyummy.history.entity.MealHistoryVO
import com.dandi.nyummy.history.presentation.FakeMealV2Repository
import com.dandi.nyummy.history.presentation.FakeMessageHelper
import com.dandi.nyummy.history.presentation.FakeNavigationHelper
import com.dandi.nyummy.history.presentation.FakeResourceHelper
import com.dandi.nyummy.history.presentation.FakeTTIHelper
import com.dandi.nyummy.meal.domain.MealFoodPickPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

/** 분석 버전을 모르는 채 들어온 식사 상세가 버전과 상태에 맞는 곳을 여는지 확인합니다. */
@OptIn(ExperimentalCoroutinesApi::class)
class MealDetailEntryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val repository = FakeMealV2Repository()
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
    fun `예전 방식 식사면 기존 상세를 보여 준다`() = runTest(testDispatcher) {
        repository.detail = detailOf(MealAnalysisVersion.V1, MealAnalysisStatus.COMPLETED)

        assertEquals(MealDetailDestination.V1, enter().uiState.value.destination)
        assertEquals(0, navigationHelper.backCount)
    }

    @Test
    fun `음식 목록 방식의 완료 식사면 받은 상세를 넘겨 새 상세를 보여 준다`() = runTest(testDispatcher) {
        val detail = detailOf(MealAnalysisVersion.V2, MealAnalysisStatus.COMPLETED)
        repository.detail = detail

        assertEquals(MealDetailDestination.V2(detail), enter().uiState.value.destination)
    }

    @Test
    fun `영양 계산 중인 식사도 새 상세를 보여 준다`() = runTest(testDispatcher) {
        val detail = detailOf(MealAnalysisVersion.V2, MealAnalysisStatus.ANALYZING)
        repository.detail = detail

        assertEquals(MealDetailDestination.V2(detail), enter().uiState.value.destination)
    }

    @Test
    fun `확인 대기 식사면 이 화면을 닫고 고르기 화면을 연다`() = runTest(testDispatcher) {
        repository.detail = detailOf(MealAnalysisVersion.V2, MealAnalysisStatus.CONFIRMING)

        val viewModel = enter()

        assertNull(viewModel.uiState.value.destination)
        assertEquals(1, navigationHelper.backCount)
        assertEquals(listOf(MealFoodPickPage(7L)), navigationHelper.pages)
    }

    @Test
    fun `인식 중이거나 실패한 식사면 기록 탭으로 돌아간다`() = runTest(testDispatcher) {
        assertEquals(
            MealDetailDestination.History,
            mealDetailDestinationOf(detailOf(MealAnalysisVersion.V2, MealAnalysisStatus.FAILED)),
        )
        repository.detail = detailOf(MealAnalysisVersion.V2, MealAnalysisStatus.RECOGNIZING)

        enter()

        assertEquals(1, navigationHelper.backCount)
        assertTrue(navigationHelper.pages.isEmpty())
    }

    @Test
    fun `조회에 실패하면 다시 시도를 보여 주고 다시 시도하면 연다`() = runTest(testDispatcher) {
        repository.getOverride = { throw IOException("offline") }
        val viewModel = enter()
        assertTrue(viewModel.uiState.value.isLoadFailed)

        repository.getOverride = null
        repository.detail = detailOf(MealAnalysisVersion.V1, MealAnalysisStatus.COMPLETED)
        viewModel.onIntent(MealDetailEntryIntent.RetryLoad)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoadFailed)
        assertEquals(MealDetailDestination.V1, viewModel.uiState.value.destination)
    }

    private fun detailOf(version: MealAnalysisVersion, status: MealAnalysisStatus) =
        MealDetailV2VO(meal = MealHistoryVO(id = "7", status = status, analysisVersion = version))

    private fun TestScope.enter(): MealDetailEntryViewModel {
        val viewModel = MealDetailEntryViewModel(
            getMealDetail = GetMealDetailV2UseCase(
                repository, FakeResourceHelper(), FakeMessageHelper(), navigationHelper, FakeTTIHelper(),
            ),
            navigationHelper = navigationHelper,
        )
        viewModel.onIntent(MealDetailEntryIntent.Enter(7L))
        advanceUntilIdle()
        return viewModel
    }
}
