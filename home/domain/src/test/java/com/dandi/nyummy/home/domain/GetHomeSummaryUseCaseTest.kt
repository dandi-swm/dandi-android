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
    private val tti = RecordingTTIHelper()

    private fun useCase(repository: HomeRepository) = GetHomeSummaryUseCase(
        repository = repository,
        resourceHelper = FakeResourceHelper,
        messageHelper = messageHelper,
        navigationHelper = navigationHelper,
        ttiHelper = tti,
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

    @Test
    fun `요약 API 구간을 시작하고 응답을 받으면 끝낸다`() = runBlocking {
        useCase(FakeHomeRepository { HomeSummaryVO() })()

        assertEquals(listOf(API_START, API_END), tti.calls)
    }

    @Test
    fun `요약 API 가 실패해도 구간은 끝낸다`() = runBlocking {
        useCase(FakeHomeRepository { throw httpException(500) })()

        assertEquals(listOf(API_START, API_END), tti.calls)
    }

    @Test
    fun `요약 API 가 취소돼도 구간은 끝내고 취소는 그대로 던진다`() = runBlocking {
        val cancelled = runCatching { useCase(FakeHomeRepository { throw CancellationException("left home") })() }

        assertTrue(cancelled.exceptionOrNull() is CancellationException)
        assertEquals(listOf(API_START, API_END), tti.calls)
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

    /** 타임라인 시작과 끝을 부른 순서대로 기록한다. */
    private class RecordingTTIHelper : TTIHelper {
        val calls = mutableListOf<String>()

        override fun startTTITracking(page: TTIPage) = Unit
        override fun startTTITimeline(category: TimelineCategory) {
            calls += "start:${category.name}"
        }

        override fun endTTITimeline(category: TimelineCategory) {
            calls += "end:${category.name}"
        }

        override fun endTTITracking() = Unit
        override fun shotTTILogging() = Unit
        override fun addTTIMetaData(metadata: TTIMetaData, value: Any?) = Unit
    }

    private companion object {
        val API_START = "start:${TimelineCategory.API_RESPONSE_TIME.name}"
        val API_END = "end:${TimelineCategory.API_RESPONSE_TIME.name}"
    }
}
