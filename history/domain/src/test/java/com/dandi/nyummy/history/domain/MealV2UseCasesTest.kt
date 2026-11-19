package com.dandi.nyummy.history.domain

import com.dandi.nyummy.common.domain.error.HttpResponseException
import com.dandi.nyummy.common.domain.error.HttpResponseStatus
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.common.domain.helper.StringResource
import com.dandi.nyummy.common.domain.message.IconType
import com.dandi.nyummy.common.domain.message.MessageEffect
import com.dandi.nyummy.common.domain.navigation.NavRoute
import com.dandi.nyummy.common.domain.navigation.NavSignal
import com.dandi.nyummy.common.domain.navigation.Page
import com.dandi.nyummy.history.entity.MealAnalysisStatus
import com.dandi.nyummy.history.entity.MealDetailV2VO
import com.dandi.nyummy.history.entity.MealFoodChangeVO
import com.dandi.nyummy.history.entity.MealFoodLineVO
import com.dandi.nyummy.tti.TTIHelper
import com.dandi.nyummy.tti.TTIMetaData
import com.dandi.nyummy.tti.TTIPage
import com.dandi.nyummy.tti.TimelineCategory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/** 음식 목록 방식(V2) 식사의 수정 UseCase와 분석 기다리기를 확인합니다. */
@OptIn(ExperimentalCoroutinesApi::class)
class MealV2UseCasesTest {

    private val repository = FakeMealV2Repository()
    private val messageHelper = RecordingMessageHelper()
    private val navigationHelper = NoOpNavigationHelper()
    private val resourceHelper = object : ResourceHelper {
        override fun getString(resource: StringResource): String = resource.name
    }
    private val ttiHelper = NoOpTTIHelper()

    @Test
    fun `이름을 바꾼 목록을 보내면 서버가 돌려준 계산 중 상태를 알려 준다`() = runTest {
        repository.onSubmitFoods = { MealAnalysisStatus.WAITING }

        val result = saveFoods()(7L, listOf(MealFoodLineVO(mealFoodId = 1, name = "제육볶음")))

        assertEquals(MealAnalysisStatus.WAITING, result.getOrNull())
        assertEquals(listOf("제육볶음"), repository.submitted.single().map { it.name })
    }

    @Test
    fun `순서만 바꾼 목록은 바로 완료다`() = runTest {
        repository.onSubmitFoods = { MealAnalysisStatus.COMPLETED }

        assertEquals(MealAnalysisStatus.COMPLETED, saveFoods()(7L, emptyList()).getOrNull())
    }

    @Test
    fun `계산 중이라 고칠 수 없으면 그 사유를 안내하고 실패다`() = runTest {
        repository.onSubmitFoods = { throw httpException(409, "api.meal.mealFoodsNotUpdatable") }

        val result = saveFoods()(7L, emptyList())

        assertTrue(result.isFailure)
        assertEquals(StringResource.HISTORY_FOODS_NOT_UPDATABLE.name, messageHelper.snackBars.single())
    }

    @Test
    fun `마지막 한 줄은 지울 수 없다고 안내하고 실패다`() = runTest {
        repository.onDeleteFood = { throw httpException(409, "api.meal.lastMealFoodNotDeletable") }

        val result = deleteFood()(7L, 1L)

        assertTrue(result.isFailure)
        assertEquals(StringResource.HISTORY_LAST_FOOD_NOT_DELETABLE.name, messageHelper.snackBars.single())
    }

    @Test
    fun `이미 지워진 줄을 지우면 성공으로 본다`() = runTest {
        repository.onDeleteFood = { throw httpException(404, "api.meal.mealFoodNotFound") }

        assertTrue(deleteFood()(7L, 1L).isSuccess)
        assertTrue(messageHelper.snackBars.isEmpty())
    }

    @Test
    fun `바꾼 것이 없으면 서버를 부르지 않는다`() = runTest {
        val result = updateFood()(7L, 1L, MealFoodChangeVO())

        assertTrue(result.isSuccess)
        assertTrue(repository.updated.isEmpty())
    }

    @Test
    fun `인분만 바꾸면 그 항목만 보낸다`() = runTest {
        val change = MealFoodChangeVO(portion = 2.0)

        assertTrue(updateFood()(7L, 1L, change).isSuccess)
        assertEquals(listOf(Triple(7L, 1L, change)), repository.updated)
    }

    @Test
    fun `네트워크가 끊겨 보내지 못하면 안내하고 실패다`() = runTest {
        repository.onUpdateFood = { throw IOException("offline") }

        val result = updateFood()(7L, 1L, MealFoodChangeVO(name = "국"))

        assertTrue(result.isFailure)
        assertEquals(StringResource.HISTORY_FOOD_EDIT_FAILED.name, messageHelper.snackBars.single())
    }

    @Test
    fun `분석이 끝나면 기다리기를 마친다`() = runTest {
        repository.statuses += listOf(MealAnalysisStatus.WAITING, MealAnalysisStatus.ANALYZING, MealAnalysisStatus.COMPLETED)

        assertEquals(MealAnalysisWaitResult.COMPLETED, awaitAnalysis()(7L))
        assertEquals(3, repository.statusRequests)
        assertEquals(3 * AwaitMealAnalysisUseCase.POLL_INTERVAL_MILLIS, currentTime)
    }

    @Test
    fun `분석이 실패하면 실패로 마친다`() = runTest {
        repository.statuses += listOf(MealAnalysisStatus.ANALYZING, MealAnalysisStatus.FAILED)

        assertEquals(MealAnalysisWaitResult.FAILED, awaitAnalysis()(7L))
    }

    @Test
    fun `한도까지 끝나지 않으면 그만 기다린다`() = runTest {
        repository.defaultStatus = MealAnalysisStatus.ANALYZING

        assertEquals(MealAnalysisWaitResult.TIMED_OUT, awaitAnalysis()(7L))
        assertEquals(AwaitMealAnalysisUseCase.TIMEOUT_MILLIS, currentTime)
        assertEquals(
            (AwaitMealAnalysisUseCase.TIMEOUT_MILLIS / AwaitMealAnalysisUseCase.POLL_INTERVAL_MILLIS).toInt(),
            repository.statusRequests,
        )
    }

    @Test
    fun `잠깐의 오류는 다음 주기에 다시 묻는다`() = runTest {
        repository.statusErrors[1] = IOException("offline")
        repository.statusErrors[2] = httpException(500)
        repository.defaultStatus = MealAnalysisStatus.COMPLETED

        assertEquals(MealAnalysisWaitResult.COMPLETED, awaitAnalysis()(7L))
        assertEquals(3, repository.statusRequests)
    }

    @Test
    fun `기다리는 사이 식사가 지워졌으면 실패로 마친다`() = runTest {
        repository.statusErrors[1] = httpException(404, "api.meal.notFound")

        assertEquals(MealAnalysisWaitResult.FAILED, awaitAnalysis()(7L))
    }

    private fun saveFoods() = SaveMealFoodListUseCase(repository, resourceHelper, messageHelper, navigationHelper, ttiHelper)
    private fun updateFood() = UpdateMealFoodUseCase(repository, resourceHelper, messageHelper, navigationHelper, ttiHelper)
    private fun deleteFood() = DeleteMealFoodUseCase(repository, resourceHelper, messageHelper, navigationHelper, ttiHelper)
    private fun awaitAnalysis() = AwaitMealAnalysisUseCase(repository, resourceHelper, messageHelper, navigationHelper, ttiHelper)

    private fun httpException(code: Int, errorCode: String? = null) = HttpResponseException(
        status = HttpResponseStatus.create(code),
        rawCode = code,
        errorRequestUrl = "https://test/meals",
        msg = "Http Request Failed ($code)",
        cause = errorCode?.let(::Throwable),
    )

    private class FakeMealV2Repository : MealV2Repository {
        val submitted = mutableListOf<List<MealFoodLineVO>>()
        val updated = mutableListOf<Triple<Long, Long, MealFoodChangeVO>>()
        var onSubmitFoods: () -> MealAnalysisStatus = { MealAnalysisStatus.COMPLETED }
        var onUpdateFood: () -> Unit = { }
        var onDeleteFood: () -> Unit = { }

        /** 차례로 돌려줄 분석 상태. 다 쓰면 [defaultStatus]를 돌려준다. */
        val statuses = ArrayDeque<MealAnalysisStatus>()
        var defaultStatus = MealAnalysisStatus.ANALYZING

        /** n번째 상태 조회(1부터)에서 던질 오류. */
        val statusErrors = mutableMapOf<Int, Exception>()
        var statusRequests = 0

        override suspend fun getMeal(mealId: Long) = MealDetailV2VO.empty
        override suspend fun updateMealName(mealId: Long, name: String) = MealDetailV2VO.empty
        override suspend fun deleteMeal(mealId: Long) = Unit
        override suspend fun submitFoods(mealId: Long, foods: List<MealFoodLineVO>): MealAnalysisStatus {
            submitted += foods
            return onSubmitFoods()
        }

        override suspend fun updateFood(mealId: Long, mealFoodId: Long, change: MealFoodChangeVO) {
            onUpdateFood()
            updated += Triple(mealId, mealFoodId, change)
        }

        override suspend fun deleteFood(mealId: Long, mealFoodId: Long) = onDeleteFood()

        override suspend fun getAnalysisStatus(mealId: Long): MealAnalysisStatus {
            statusRequests++
            statusErrors[statusRequests]?.let { throw it }
            return statuses.removeFirstOrNull() ?: defaultStatus
        }
    }

    private class NoOpNavigationHelper : NavigationHelper {
        override val navigationFlow: Flow<NavSignal> = emptyFlow()
        override fun navigateByRoute(route: NavRoute) = Unit
        override fun navigateTo(page: Page) = Unit
        override fun navigateToAppLink(link: String, asRoot: Boolean) = Unit
        override fun navigateToAppLinkAfterEntry(link: String, waitForTabRoot: Boolean) = Unit
        override fun navigateToBack() = Unit
        override fun navigateToAsRoot(page: Page) = Unit
        override fun navigateToInitial() = Unit
        override fun navigateToExternalLink(url: String) = Unit
    }

    private class RecordingMessageHelper : MessageHelper {
        val snackBars = mutableListOf<String>()
        override val effect: Flow<MessageEffect> = emptyFlow()
        override fun showToast(toastMsg: String) = Unit
        override fun showToast(messageRes: Int) = Unit
        override fun showSnackBar(iconType: IconType, messageText: String, callToActionText: String?, onClickCTA: (() -> Unit)?) {
            snackBars += messageText
        }

        override fun showSnackBar(iconType: IconType, messageRes: Int, callToActionText: String?, onClickCTA: (() -> Unit)?) = Unit
        override fun showOneButtonDialog(
            titleText: String?,
            descText: String,
            cantIgnore: Boolean,
            buttonText: String,
            onClickButton: (() -> Unit)?,
        ) = Unit

        override fun showTwoButtonDialog(
            titleText: String?,
            descText: String,
            cantIgnore: Boolean,
            leftButtonText: String,
            onClickLeftButton: (() -> Unit)?,
            rightButtonText: String,
            onClickRightButton: (() -> Unit)?,
        ) = Unit
    }

    private class NoOpTTIHelper : TTIHelper {
        override fun startTTITracking(page: TTIPage) = Unit
        override fun startTTITimeline(category: TimelineCategory) = Unit
        override fun endTTITimeline(category: TimelineCategory) = Unit
        override fun endTTITracking() = Unit
        override fun shotTTILogging() = Unit
        override fun addTTIMetaData(metadata: TTIMetaData, value: Any?) = Unit
    }
}
