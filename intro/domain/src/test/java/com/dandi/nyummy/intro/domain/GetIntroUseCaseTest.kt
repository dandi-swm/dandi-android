package com.dandi.nyummy.intro.domain

import com.dandi.nyummy.auth.domain.LoginPage
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
import com.dandi.nyummy.home.domain.HomePage
import com.dandi.nyummy.intro.entity.IntroVO
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

class GetIntroUseCaseTest {

    private val navigationHelper = RecordingNavigationHelper()
    private val messageHelper = RecordingMessageHelper()

    @Test
    fun `리프레시 토큰이 있으면 홈을 루트로 이동한다`() = runBlocking {
        val useCase = buildUseCase(FakeIntroRepository(hasRefreshToken = true))

        val result = useCase()

        assertEquals(listOf<Page>(HomePage), navigationHelper.rootPages)
        assertEquals(Result.success(IntroVO.empty), result)
    }

    @Test
    fun `리프레시 토큰이 없으면 로그인을 루트로 이동한다`() = runBlocking {
        val useCase = buildUseCase(FakeIntroRepository(hasRefreshToken = false))

        val result = useCase()

        assertEquals(listOf<Page>(LoginPage), navigationHelper.rootPages)
        assertEquals(Result.success(IntroVO.empty), result)
    }

    @Test
    fun `강제 업데이트 에러면 닫을 수 없는 다이얼로그를 띄우고 버튼 클릭 시 뒤로 이동한다`() = runBlocking {
        val exception = httpException(400, errorCode = "api.intro.requiredForceUpdate")
        val useCase = buildUseCase(ThrowingIntroRepository(exception))

        val result = useCase()

        val dialog = messageHelper.oneButtonDialogs.single()
        assertTrue(dialog.cantIgnore)
        assertEquals(IntroErrorType.REQUIRED_FORCE_UPDATE.errorMsg, dialog.descText)
        dialog.onClickButton?.invoke()
        assertEquals(1, navigationHelper.backCount)
        assertTrue(result.isFailure)
    }

    @Test
    fun `401 이면 세션 만료 다이얼로그를 띄우고 버튼 클릭 시 초기 화면으로 이동한다`() = runBlocking {
        val useCase = buildUseCase(ThrowingIntroRepository(httpException(401)))

        val result = useCase()

        val dialog = messageHelper.oneButtonDialogs.single()
        assertTrue(dialog.cantIgnore)
        dialog.onClickButton?.invoke()
        assertEquals(1, navigationHelper.initialCount)
        assertTrue(result.isFailure)
    }

    @Test
    fun `5xx 이면 일시 오류 다이얼로그를 띄운다`() = runBlocking {
        val useCase = buildUseCase(ThrowingIntroRepository(httpException(500)))

        val result = useCase()

        assertEquals("A temporary error occurred.", messageHelper.oneButtonDialogs.single().titleText)
        assertTrue(result.isFailure)
    }

    @Test
    fun `매칭되는 에러 타입이 없으면 다이얼로그도 네비게이션도 없다`() = runBlocking {
        val useCase = buildUseCase(ThrowingIntroRepository(httpException(403)))

        val result = useCase()

        assertTrue(messageHelper.oneButtonDialogs.isEmpty())
        assertTrue(navigationHelper.rootPages.isEmpty())
        assertTrue(result.isFailure)
    }

    private fun buildUseCase(repository: IntroRepository): GetIntroUseCase = GetIntroUseCase(
        repository = repository,
        resourceHelper = FakeResourceHelper(),
        messageHelper = messageHelper,
        navigationHelper = navigationHelper,
        ttiHelper = FakeTTIHelper(),
    )

    private fun httpException(code: Int, errorCode: String? = null): HttpResponseException =
        HttpResponseException(
            status = HttpResponseStatus.create(code),
            rawCode = code,
            errorRequestUrl = "https://test/intro",
            msg = "Http Request Failed ($code)",
            cause = errorCode?.let(::Throwable),
        )

    private class FakeIntroRepository(
        private val hasRefreshToken: Boolean,
    ) : IntroRepository {
        override suspend fun getIntro(): IntroVO = IntroVO.empty
        override suspend fun hasRefreshToken(): Boolean = hasRefreshToken
    }

    /** getIntro 주석 해제 전까지 hasRefreshToken 에서 예외를 던져 에러 경로를 검증한다. */
    private class ThrowingIntroRepository(
        private val exception: HttpResponseException,
    ) : IntroRepository {
        override suspend fun getIntro(): IntroVO = throw exception
        override suspend fun hasRefreshToken(): Boolean = throw exception
    }

    private class RecordingNavigationHelper : NavigationHelper {
        val rootPages = mutableListOf<Page>()
        var backCount = 0
        var initialCount = 0

        override val navigationFlow: Flow<NavSignal> = emptyFlow()
        override fun navigateByRoute(route: NavRoute) = Unit
        override fun navigateTo(page: Page) = Unit
        override fun navigateDeepLink(route: NavRoute) = Unit
        override fun navigateToBack() {
            backCount++
        }

        override fun navigateToAsRoot(page: Page) {
            rootPages += page
        }

        override fun navigateToInitial() {
            initialCount++
        }
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

        val oneButtonDialogs = mutableListOf<OneButtonDialogCall>()

        override val effect: Flow<MessageEffect> = emptyFlow()
        override fun showToast(toastMsg: String) = Unit
        override fun showSnackBar(
            iconType: IconType,
            messageText: String,
            callToActionText: String?,
            onClickCTA: (() -> Unit)?,
        ) = Unit

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
