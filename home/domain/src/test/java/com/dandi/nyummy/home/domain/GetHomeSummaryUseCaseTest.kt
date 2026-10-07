package com.dandi.nyummy.home.domain

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
import com.dandi.nyummy.home.entity.HomeSummaryVO
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
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

class GetHomeSummaryUseCaseTest {

    private val navigationHelper = RecordingNavigationHelper()
    private val messageHelper = RecordingMessageHelper()

    private fun useCase(repository: HomeRepository) = GetHomeSummaryUseCase(
        repository = repository,
        resourceHelper = FakeResourceHelper,
        messageHelper = messageHelper,
        navigationHelper = navigationHelper,
        ttiHelper = FakeTTIHelper,
    )

    @Test
    fun `성공하면 홈 요약을 돌려주고 아무것도 띄우지 않는다`() = runBlocking {
        val summary = HomeSummaryVO(coinBalance = 1240, streakDays = 7, todayRecordedCount = 1)

        val result = useCase(FakeHomeRepository { summary })()

        assertEquals(Result.success(summary), result)
        assertTrue(messageHelper.snackBars.isEmpty())
        assertTrue(messageHelper.dialogs.isEmpty())
    }

    @Test
    fun `401 이면 세션 만료 안내 후 처음 화면으로 보낸다`() = runBlocking {
        val result = useCase(FakeHomeRepository { throw httpException(401) })()

        val dialog = messageHelper.dialogs.single()
        assertTrue(dialog.cantIgnore)
        dialog.onClickButton?.invoke()
        assertEquals(1, navigationHelper.initialCount)
        assertTrue(messageHelper.snackBars.isEmpty())
        assertTrue(result.isFailure)
    }

    @Test
    fun `5xx 는 공통 처리로 일시 오류를 안내한다`() = runBlocking {
        val result = useCase(FakeHomeRepository { throw httpException(500) })()

        assertEquals(1, messageHelper.dialogs.size)
        assertTrue(messageHelper.snackBars.isEmpty())
        assertTrue(result.isFailure)
    }

    @Test
    fun `공통 처리 대상이 아닌 오류는 스낵바로 알린다`() = runBlocking {
        val result = useCase(FakeHomeRepository { throw httpException(400) })()

        assertEquals(listOf(IconType.ERROR to "홈 정보를 불러오지 못했어요"), messageHelper.snackBars)
        assertTrue(messageHelper.dialogs.isEmpty())
        assertTrue(result.isFailure)
    }

    @Test
    fun `네트워크 오류도 앱이 죽지 않고 스낵바로 알린다`() = runBlocking {
        val result = useCase(FakeHomeRepository { throw IOException("offline") })()

        assertEquals(listOf(IconType.ERROR to "홈 정보를 불러오지 못했어요"), messageHelper.snackBars)
        assertTrue(result.isFailure)
    }

    @Test(expected = CancellationException::class)
    fun `취소는 삼키지 않고 그대로 던진다`(): Unit = runBlocking {
        useCase(FakeHomeRepository { throw CancellationException("left home") })()
        Unit
    }

    private fun httpException(code: Int) = HttpResponseException(
        status = HttpResponseStatus.create(code),
        rawCode = code,
        errorRequestUrl = "https://test/api/v1/home",
        msg = "Http Request Failed ($code)",
    )

    private class FakeHomeRepository(private val block: () -> HomeSummaryVO) : HomeRepository {
        override suspend fun getHomeSummary(): HomeSummaryVO = block()
    }

    private class RecordingNavigationHelper : NavigationHelper {
        var initialCount = 0

        override val navigationFlow: Flow<NavSignal> = emptyFlow()
        override fun navigateByRoute(route: NavRoute) = Unit
        override fun navigateTo(page: Page) = Unit
        override fun navigateDeepLink(route: NavRoute) = Unit
        override fun navigateToBack() = Unit
        override fun navigateToAsRoot(page: Page) = Unit
        override fun navigateToInitial() {
            initialCount++
        }

        override fun navigateToExternalLink(url: String) = Unit
    }

    private class RecordingMessageHelper : MessageHelper {
        data class DialogCall(val cantIgnore: Boolean, val onClickButton: (() -> Unit)?)

        val dialogs = mutableListOf<DialogCall>()
        val snackBars = mutableListOf<Pair<IconType, String>>()

        override val effect: Flow<MessageEffect> = emptyFlow()
        override fun showToast(toastMsg: String) = Unit
        override fun showSnackBar(
            iconType: IconType,
            messageText: String,
            callToActionText: String?,
            onClickCTA: (() -> Unit)?,
        ) {
            snackBars += iconType to messageText
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
            dialogs += DialogCall(cantIgnore, onClickButton)
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

    private object FakeResourceHelper : ResourceHelper {
        override fun getString(resource: StringResource): String = ""
    }

    private object FakeTTIHelper : TTIHelper {
        override fun startTTITracking(page: TTIPage) = Unit
        override fun startTTITimeline(page: TTIPage, timelineCategory: TimelineCategory) = Unit
        override fun endTTITimeline(page: TTIPage, timelineCategory: TimelineCategory) = Unit
        override fun endTTITracking(page: TTIPage) = Unit
        override fun shotTTILogging(page: TTIPage) = Unit
        override fun addTTIMetaData(page: TTIPage, metadata: TTIMetaData, value: Any?) = Unit
    }
}
