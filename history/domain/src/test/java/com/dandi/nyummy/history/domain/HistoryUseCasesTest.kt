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
import com.dandi.nyummy.history.entity.DailyMealHistoryVO
import com.dandi.nyummy.history.entity.HistoryCalendarVO
import com.dandi.nyummy.history.entity.MealHistoryVO
import com.dandi.nyummy.tti.TTIHelper
import com.dandi.nyummy.tti.TTIMetaData
import com.dandi.nyummy.tti.TTIPage
import com.dandi.nyummy.tti.TimelineCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryUseCasesTest {

    private val navigationHelper = RecordingNavigationHelper()
    private val messageHelper = RecordingMessageHelper()

    @Test
    fun `월간 조회 성공 시 캘린더 VO 를 돌려준다`() = runBlocking {
        val calendar = HistoryCalendarVO(year = 2026, month = 8)
        val useCase = GetMonthlyMealsUseCase(
            repository = FakeHistoryRepository(monthly = calendar),
            resourceHelper = FakeResourceHelper(),
            messageHelper = messageHelper,
            navigationHelper = navigationHelper,
            ttiHelper = FakeTTIHelper(),
        )

        val result = useCase(2026, 8)

        assertEquals(Result.success(calendar), result)
        assertTrue(messageHelper.oneButtonDialogs.isEmpty())
    }

    @Test
    fun `월간 조회 중 401 이면 세션 만료 다이얼로그를 띄우고 실패를 돌려준다`() = runBlocking {
        val useCase = GetMonthlyMealsUseCase(
            repository = ThrowingHistoryRepository(httpException(401)),
            resourceHelper = FakeResourceHelper(),
            messageHelper = messageHelper,
            navigationHelper = navigationHelper,
            ttiHelper = FakeTTIHelper(),
        )

        val result = useCase(2026, 8)

        val dialog = messageHelper.oneButtonDialogs.single()
        assertTrue(dialog.cantIgnore)
        dialog.onClickButton?.invoke()
        assertEquals(1, navigationHelper.initialCount)
        assertTrue(result.isFailure)
    }

    @Test
    fun `월간 조회 중 비공통 에러면 에러 스낵바를 띄우고 실패를 돌려준다`() = runBlocking {
        val useCase = GetMonthlyMealsUseCase(
            repository = ThrowingHistoryRepository(httpException(400)),
            resourceHelper = FakeResourceHelper(),
            messageHelper = messageHelper,
            navigationHelper = navigationHelper,
            ttiHelper = FakeTTIHelper(),
        )

        val result = useCase(2026, 8)

        val snackBar = messageHelper.snackBars.single()
        assertEquals(IconType.ERROR, snackBar.iconType)
        assertTrue(messageHelper.oneButtonDialogs.isEmpty())
        assertTrue(result.isFailure)
    }

    @Test
    fun `일일 조회 성공 시 상세 VO 를 돌려준다`() = runBlocking {
        val daily = DailyMealHistoryVO.empty
        val useCase = GetDailyMealsUseCase(
            repository = FakeHistoryRepository(daily = daily),
            resourceHelper = FakeResourceHelper(),
            messageHelper = messageHelper,
            navigationHelper = navigationHelper,
            ttiHelper = FakeTTIHelper(),
        )

        val result = useCase(2026, 8, 23)

        assertEquals(Result.success(daily), result)
    }

    @Test
    fun `이름 수정 성공 시 수정된 식사를 돌려준다`() = runBlocking {
        val renamed = MealHistoryVO(id = "1", name = "연어 포케")
        val useCase = UpdateMealNameUseCase(
            repository = FakeHistoryRepository(meal = renamed),
            resourceHelper = FakeResourceHelper(),
            messageHelper = messageHelper,
            navigationHelper = navigationHelper,
            ttiHelper = FakeTTIHelper(),
        )

        val result = useCase(1L, "연어 포케")

        assertEquals(Result.success(renamed), result)
    }

    @Test
    fun `삭제 중 5xx 이면 일시 오류 다이얼로그를 띄우고 실패를 돌려준다`() = runBlocking {
        val useCase = DeleteMealUseCase(
            repository = ThrowingHistoryRepository(httpException(500)),
            resourceHelper = FakeResourceHelper(),
            messageHelper = messageHelper,
            navigationHelper = navigationHelper,
            ttiHelper = FakeTTIHelper(),
        )

        val result = useCase(1L)

        assertEquals("A temporary error occurred.", messageHelper.oneButtonDialogs.single().titleText)
        assertTrue(result.isFailure)
    }

    private fun httpException(code: Int): HttpResponseException = HttpResponseException(
        status = HttpResponseStatus.create(code),
        rawCode = code,
        errorRequestUrl = "https://test/meals",
        msg = "Http Request Failed ($code)",
    )

    private class FakeHistoryRepository(
        private val monthly: HistoryCalendarVO = HistoryCalendarVO.empty,
        private val daily: DailyMealHistoryVO = DailyMealHistoryVO.empty,
        private val meal: MealHistoryVO = MealHistoryVO.empty,
    ) : HistoryRepository {
        override suspend fun getMonthlyCalendar(year: Int, month: Int) = monthly
        override suspend fun getDailyMeals(year: Int, month: Int, day: Int) = daily
        override suspend fun getMeal(mealId: Long) = meal
        override suspend fun updateMealName(mealId: Long, name: String) = meal
        override suspend fun deleteMeal(mealId: Long) = Unit
    }

    private class ThrowingHistoryRepository(
        private val exception: HttpResponseException,
    ) : HistoryRepository {
        override suspend fun getMonthlyCalendar(year: Int, month: Int) = throw exception
        override suspend fun getDailyMeals(year: Int, month: Int, day: Int) = throw exception
        override suspend fun getMeal(mealId: Long) = throw exception
        override suspend fun updateMealName(mealId: Long, name: String) = throw exception
        override suspend fun deleteMeal(mealId: Long) = throw exception
    }

    private class RecordingNavigationHelper : NavigationHelper {
        var initialCount = 0
        var backCount = 0

        override val navigationFlow: Flow<NavSignal> = emptyFlow()
        override fun navigateByRoute(route: NavRoute) = Unit
        override fun navigateTo(page: Page) = Unit
        override fun navigateDeepLink(route: NavRoute) = Unit
        override fun navigateToBack() {
            backCount++
        }

        override fun navigateToAsRoot(page: Page) = Unit
        override fun navigateToInitial() {
            initialCount++
        }

        override fun navigateToExternalLink(url: String) = Unit
    }

    private class FakeResourceHelper : ResourceHelper {
        override fun getString(resource: StringResource): String = ""
    }

    private class RecordingMessageHelper : MessageHelper {
        data class OneButtonDialogCall(
            val titleText: String?,
            val descText: String,
            val cantIgnore: Boolean,
            val buttonText: String,
            val onClickButton: (() -> Unit)?,
        )

        data class SnackBarCall(val iconType: IconType, val messageText: String)

        val oneButtonDialogs = mutableListOf<OneButtonDialogCall>()
        val snackBars = mutableListOf<SnackBarCall>()

        override val effect: Flow<MessageEffect> = emptyFlow()
        override fun showToast(toastMsg: String) = Unit
        override fun showSnackBar(
            iconType: IconType,
            messageText: String,
            callToActionText: String?,
            onClickCTA: (() -> Unit)?,
        ) {
            snackBars += SnackBarCall(iconType, messageText)
        }

        override fun showSnackBar(
            iconType: IconType,
            messageRes: Int,
            callToActionText: String?,
            onClickCTA: (() -> Unit)?,
        ) = Unit

        override fun showOneButtonDialog(
            titleText: String?,
            descText: String,
            cantIgnore: Boolean,
            buttonText: String,
            onClickButton: (() -> Unit)?,
        ) {
            oneButtonDialogs += OneButtonDialogCall(titleText, descText, cantIgnore, buttonText, onClickButton)
        }

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

    private class FakeTTIHelper : TTIHelper {
        override fun startTTITracking(page: TTIPage) = Unit
        override fun startTTITimeline(page: TTIPage, timelineCategory: TimelineCategory) = Unit
        override fun endTTITimeline(page: TTIPage, timelineCategory: TimelineCategory) = Unit
        override fun endTTITracking(page: TTIPage) = Unit
        override fun shotTTILogging(page: TTIPage) = Unit
        override fun addTTIMetaData(page: TTIPage, metadata: TTIMetaData, value: Any?) = Unit
    }
}
