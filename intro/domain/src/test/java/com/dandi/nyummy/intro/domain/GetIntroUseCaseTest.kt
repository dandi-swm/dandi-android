package com.dandi.nyummy.intro.domain

import com.dandi.nyummy.auth.domain.LoginPage
import com.dandi.nyummy.common.domain.helper.DeviceHelper
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
import com.dandi.nyummy.intro.entity.VersionCheckVO
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
    fun `버전 통과 후 리프레시 토큰이 있으면 홈을 루트로 이동한다`() = runBlocking {
        val version = versionCheck(minimumVersionCode = 3)
        val useCase = buildUseCase(
            repository = FakeIntroRepository(hasRefreshToken = true),
            remoteConfigHelper = FakeRemoteConfigHelper(version),
            deviceHelper = FakeDeviceHelper(appVersionCode = 5),
        )

        val result = useCase()

        assertEquals(listOf<Page>(HomePage), navigationHelper.rootPages)
        assertTrue(messageHelper.oneButtonDialogs.isEmpty())
        assertEquals(Result.success(version), result)
    }

    @Test
    fun `버전 통과 후 리프레시 토큰이 없으면 로그인을 루트로 이동한다`() = runBlocking {
        val version = versionCheck(minimumVersionCode = 3)
        val useCase = buildUseCase(
            repository = FakeIntroRepository(hasRefreshToken = false),
            remoteConfigHelper = FakeRemoteConfigHelper(version),
            deviceHelper = FakeDeviceHelper(appVersionCode = 5),
        )

        val result = useCase()

        assertEquals(listOf<Page>(LoginPage), navigationHelper.rootPages)
        assertEquals(Result.success(version), result)
    }

    @Test
    fun `최소 버전 미만이면 닫을 수 없는 다이얼로그를 띄우고 이동하지 않으며 버튼 클릭 시 스토어 링크를 연다`() =
        runBlocking {
            val version = versionCheck(
                minimumVersionCode = 3,
                latestVersionUpdateLink = "https://play.google.com/store/apps/details?id=com.dandi.nyummy",
            )
            val useCase = buildUseCase(
                repository = FakeIntroRepository(hasRefreshToken = true),
                remoteConfigHelper = FakeRemoteConfigHelper(version),
                deviceHelper = FakeDeviceHelper(appVersionCode = 1),
            )

            val result = useCase()

            val dialog = messageHelper.oneButtonDialogs.single()
            assertTrue(dialog.cantIgnore)
            assertTrue(navigationHelper.rootPages.isEmpty())
            dialog.onClickButton?.invoke()
            assertEquals(listOf(version.latestVersionUpdateLink), navigationHelper.openedUrls)
            assertEquals(Result.success(version), result)
        }

    @Test
    fun `최소 버전 코드가 0이면 강제 업데이트를 걸지 않는다`() = runBlocking {
        val version = versionCheck(minimumVersionCode = 0)
        val useCase = buildUseCase(
            repository = FakeIntroRepository(hasRefreshToken = false),
            remoteConfigHelper = FakeRemoteConfigHelper(version),
            deviceHelper = FakeDeviceHelper(appVersionCode = 0),
        )

        useCase()

        assertTrue(messageHelper.oneButtonDialogs.isEmpty())
        assertEquals(listOf<Page>(LoginPage), navigationHelper.rootPages)
    }

    @Test
    fun `RemoteConfig 조회가 실패하면 실패 결과를 반환한다`() = runBlocking {
        val useCase = buildUseCase(
            repository = FakeIntroRepository(hasRefreshToken = false),
            remoteConfigHelper = ThrowingRemoteConfigHelper(),
            deviceHelper = FakeDeviceHelper(appVersionCode = 5),
        )

        val result = useCase()

        assertTrue(result.isFailure)
        assertTrue(navigationHelper.rootPages.isEmpty())
    }

    private fun versionCheck(
        minimumVersionCode: Long,
        latestVersionUpdateLink: String = "",
    ): VersionCheckVO = VersionCheckVO(
        minimumVersion = "",
        minimumVersionCode = minimumVersionCode,
        latestVersionUpdateLink = latestVersionUpdateLink,
        minimumVersionReleaseNote = "",
    )

    private fun buildUseCase(
        repository: IntroRepository,
        remoteConfigHelper: RemoteConfigHelper,
        deviceHelper: DeviceHelper,
    ): GetIntroUseCase = GetIntroUseCase(
        repository = repository,
        remoteConfigHelper = remoteConfigHelper,
        deviceHelper = deviceHelper,
        resourceHelper = FakeResourceHelper(),
        messageHelper = messageHelper,
        navigationHelper = navigationHelper,
        ttiHelper = FakeTTIHelper(),
    )

    private class FakeIntroRepository(
        private val hasRefreshToken: Boolean,
    ) : IntroRepository {
        override suspend fun hasRefreshToken(): Boolean = hasRefreshToken
    }

    private class FakeRemoteConfigHelper(
        private val version: VersionCheckVO,
    ) : RemoteConfigHelper {
        override suspend fun sync() = Unit
        override fun getVersionCheck(): VersionCheckVO = version
    }

    private class ThrowingRemoteConfigHelper : RemoteConfigHelper {
        override suspend fun sync() = Unit
        override fun getVersionCheck(): VersionCheckVO =
            throw IllegalStateException("remote config failed")
    }

    private class FakeDeviceHelper(
        override val appVersionCode: Long,
        override val appVersionName: String = "",
    ) : DeviceHelper

    private class RecordingNavigationHelper : NavigationHelper {
        val rootPages = mutableListOf<Page>()
        val openedUrls = mutableListOf<String>()
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

        override fun navigateToExternalLink(url: String) {
            openedUrls += url
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
