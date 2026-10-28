package com.dandi.nyummy.home.presentation.prepare

import com.dandi.nyummy.cat.domain.CatRepository
import com.dandi.nyummy.cat.domain.PrefetchCatAnimationsUseCase
import com.dandi.nyummy.cat.entity.CatAnimationSetVO
import com.dandi.nyummy.common.domain.helper.DeviceHelper
import com.dandi.nyummy.common.domain.helper.ImagePrefetchHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.navigation.NavRoute
import com.dandi.nyummy.common.domain.navigation.NavSignal
import com.dandi.nyummy.common.domain.navigation.Page
import com.dandi.nyummy.home.domain.HomePage
import com.dandi.nyummy.home.domain.HomeRepository
import com.dandi.nyummy.home.domain.PrefetchHomeNoticesUseCase
import com.dandi.nyummy.home.domain.PrefetchHomeSummaryUseCase
import com.dandi.nyummy.home.entity.HomeNoticeVO
import com.dandi.nyummy.home.entity.HomeSummaryVO
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class HomePrepareViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val navigationHelper = RecordingNavigationHelper()
    private val homeRepository = FakeHomeRepository()
    private val imagePrefetch = RecordingImagePrefetchHelper()

    /** 미리 받기가 끝나는 시점. 완료하기 전까지 미리 받기가 끝나지 않는다. */
    private val prefetchDone = CompletableDeferred<Unit>()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(prefetch: suspend () -> Boolean) = HomePrepareViewModel(
        prefetchHomeSummary = PrefetchHomeSummaryUseCase(homeRepository),
        prefetchCatAnimations = PrefetchCatAnimationsUseCase(
            repository = object : CatRepository {
                override suspend fun getAnimations(): CatAnimationSetVO = CatAnimationSetVO.empty
                override suspend fun refreshWeight(): Boolean = prefetch()
            },
        ),
        prefetchHomeNotices = PrefetchHomeNoticesUseCase(
            repository = homeRepository,
            deviceHelper = object : DeviceHelper {
                override val appVersionCode: Long = 8
                override val appVersionName: String = "2.1.3"
            },
            imagePrefetchHelper = imagePrefetch,
        ),
        navigationHelper = navigationHelper,
    )

    @Test
    fun `홈 요약은 기다리지 않고, 냐미 미리 받기가 끝나야 진행바를 채운 뒤 채움 애니메이션을 기다려 홈을 루트로 연다`() = runTest(testDispatcher) {
        val viewModel = viewModel { prefetchDone.await(); false }
        runCurrent()
        assertEquals(1, homeRepository.prefetchCount)
        assertFalse(viewModel.uiState.value.isComplete)

        prefetchDone.complete(Unit)
        runCurrent()
        assertTrue(viewModel.uiState.value.isComplete)
        assertTrue(navigationHelper.rootPages.isEmpty())

        advanceTimeBy(HomePrepareViewModel.COMPLETE_ANIMATION_WAIT_MS + 1)
        assertEquals(listOf<Page>(HomePage), navigationHelper.rootPages)
    }

    @Test
    fun `로그인을 거쳐 온 회원도 공지 사진을 받고, 다 받은 뒤에 홈으로 간다`() = runTest(testDispatcher) {
        homeRepository.noticeList = listOf(HomeNoticeVO(id = "a", title = "새단장", imageUrl = "https://cdn.example/a.webp"))
        val imagesDone = CompletableDeferred<Unit>()
        imagePrefetch.gate = imagesDone
        val viewModel = viewModel { false }

        runCurrent()
        assertEquals(listOf("https://cdn.example/a.webp"), imagePrefetch.imageUrls)
        assertFalse(viewModel.uiState.value.isComplete)

        imagesDone.complete(Unit)
        runCurrent()
        assertTrue(viewModel.uiState.value.isComplete)
        advanceTimeBy(HomePrepareViewModel.COMPLETE_ANIMATION_WAIT_MS + 1)
        assertEquals(listOf<Page>(HomePage), navigationHelper.rootPages)
    }

    @Test
    fun `공지 사진 받기가 끝나지 않아도 상한이 지나면 홈으로 간다`() = runTest(testDispatcher) {
        homeRepository.noticeList = listOf(HomeNoticeVO(id = "a", title = "새단장", imageUrl = "https://cdn.example/a.webp"))
        imagePrefetch.gate = CompletableDeferred()
        val viewModel = viewModel { false }

        advanceTimeBy(HomePrepareViewModel.PREFETCH_TIMEOUT_MS - 1)
        assertFalse(viewModel.uiState.value.isComplete)

        advanceTimeBy(HomePrepareViewModel.COMPLETE_ANIMATION_WAIT_MS + 2)
        assertTrue(viewModel.uiState.value.isComplete)
        assertEquals(listOf<Page>(HomePage), navigationHelper.rootPages)
    }

    @Test
    fun `미리 받기가 끝나지 않아도 상한이 지나면 홈으로 간다`() = runTest(testDispatcher) {
        val viewModel = viewModel { awaitCancellation() }

        advanceTimeBy(HomePrepareViewModel.PREFETCH_TIMEOUT_MS - 1)
        assertFalse(viewModel.uiState.value.isComplete)

        advanceTimeBy(HomePrepareViewModel.COMPLETE_ANIMATION_WAIT_MS + 2)
        assertTrue(viewModel.uiState.value.isComplete)
        assertEquals(listOf<Page>(HomePage), navigationHelper.rootPages)
    }
}

/** 홈 요약 미리 받기 호출을 센다. */
private class FakeHomeRepository : HomeRepository {
    var prefetchCount = 0
        private set

    override suspend fun getHomeSummary(): HomeSummaryVO = error("방 준비 화면은 조회하지 않는다")

    override fun prefetchHomeSummary() {
        prefetchCount++
    }

    var noticeList: List<HomeNoticeVO> = emptyList()

    override fun getNotices(): List<HomeNoticeVO> = noticeList
    override suspend fun getNoticeHiddenDate(): LocalDate? = null
    override suspend fun setNoticeHiddenDate(date: LocalDate) = Unit
    override fun isNoticeHandled(): Boolean = false
    override fun markNoticeHandled() = Unit
}

/** 미리 받은 그림 주소를 남긴다. [gate]를 주면 그것이 열릴 때까지 끝나지 않는다. */
private class RecordingImagePrefetchHelper : ImagePrefetchHelper {
    val imageUrls = mutableListOf<String>()
    var gate: CompletableDeferred<Unit>? = null

    override suspend fun prefetchSpriteSheets(urls: List<String>) = Unit
    override suspend fun prefetchImages(urls: List<String>) {
        imageUrls += urls
        gate?.await()
    }
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
    override fun navigateToExternalLink(url: String) = Unit
}
