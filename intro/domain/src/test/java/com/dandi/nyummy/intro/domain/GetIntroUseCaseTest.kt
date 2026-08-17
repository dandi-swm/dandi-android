package com.dandi.nyummy.intro.domain

import com.dandi.nyummy.auth.domain.LoginPage
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
import org.junit.Test

class GetIntroUseCaseTest {

    @Test
    fun `리프레시 토큰이 있으면 홈을 루트로 이동한다`() = runBlocking {
        val navigationHelper = RecordingNavigationHelper()
        val useCase = buildUseCase(hasRefreshToken = true, navigationHelper = navigationHelper)

        useCase()

        assertEquals(listOf<Page>(HomePage), navigationHelper.rootPages)
    }

    @Test
    fun `리프레시 토큰이 없으면 로그인을 루트로 이동한다`() = runBlocking {
        val navigationHelper = RecordingNavigationHelper()
        val useCase = buildUseCase(hasRefreshToken = false, navigationHelper = navigationHelper)

        useCase()

        assertEquals(listOf<Page>(LoginPage), navigationHelper.rootPages)
    }

    private fun buildUseCase(
        hasRefreshToken: Boolean,
        navigationHelper: NavigationHelper,
    ): GetIntroUseCase = GetIntroUseCase(
        repository = FakeIntroRepository(hasRefreshToken),
        resourceHelper = FakeResourceHelper(),
        messageHelper = FakeMessageHelper(),
        navigationHelper = navigationHelper,
        ttiHelper = FakeTTIHelper(),
    )

    private class FakeIntroRepository(
        private val hasRefreshToken: Boolean,
    ) : IntroRepository {
        override suspend fun getIntro(): IntroVO = IntroVO.empty
        override suspend fun hasRefreshToken(): Boolean = hasRefreshToken
    }

    private class RecordingNavigationHelper : NavigationHelper {
        val rootPages = mutableListOf<Page>()

        override val navigationFlow: Flow<NavSignal> = emptyFlow()
        override fun navigateByRoute(route: NavRoute) = Unit
        override fun navigateTo(page: Page) = Unit
        override fun navigateDeepLink(route: NavRoute) = Unit
        override fun navigateToBack() = Unit
        override fun navigateToAsRoot(page: Page) {
            rootPages += page
        }

        override fun navigateToInitial() = Unit
    }

    private class FakeResourceHelper : ResourceHelper {
        override fun getString(resource: StringResource): String = ""
    }

    private class FakeMessageHelper : MessageHelper {
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

    private class FakeTTIHelper : TTIHelper {
        override fun startTTITracking(page: TTIPage) = Unit
        override fun startTTITimeline(page: TTIPage, timelineCategory: TimelineCategory) = Unit
        override fun endTTITimeline(page: TTIPage, timelineCategory: TimelineCategory) = Unit
        override fun endTTITracking(page: TTIPage) = Unit
        override fun shotTTILogging(page: TTIPage) = Unit
        override fun addTTIMetaData(page: TTIPage, metadata: TTIMetaData, value: Any?) = Unit
    }
}
