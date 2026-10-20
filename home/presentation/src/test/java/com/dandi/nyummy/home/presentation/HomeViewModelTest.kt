package com.dandi.nyummy.home.presentation

import com.dandi.nyummy.cat.domain.CatMotionPicker
import com.dandi.nyummy.cat.domain.CatRepository
import com.dandi.nyummy.cat.domain.GetCatAnimationsUseCase
import com.dandi.nyummy.cat.entity.CatAnimationSetVO
import com.dandi.nyummy.cat.entity.CatAnimationVO
import com.dandi.nyummy.cat.entity.CatState
import com.dandi.nyummy.cat.entity.SpriteClipVO
import com.dandi.nyummy.cat.entity.SpriteFrameVO
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
import com.dandi.nyummy.common.domain.remoteconfig.RemoteConfigHelper
import com.dandi.nyummy.common.domain.remoteconfig.RemoteConfigKey
import com.dandi.nyummy.common.presentation.designsystem.component.NyummySpriteSource
import com.dandi.nyummy.common.presentation.room.RoomCardLayout
import com.dandi.nyummy.home.domain.GetHomeSummaryUseCase
import com.dandi.nyummy.home.domain.HomeRepository
import com.dandi.nyummy.home.domain.tti.HomeTTIPage
import com.dandi.nyummy.home.entity.HomeSummaryVO
import com.dandi.nyummy.room.entity.RoomItemCategory
import com.dandi.nyummy.room.entity.RoomPlacementVO
import com.dandi.nyummy.tti.TTIHelper
import com.dandi.nyummy.tti.TTIMetaData
import com.dandi.nyummy.tti.TTIPage
import com.dandi.nyummy.tti.TimelineCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val homeRepository = FakeHomeRepository()
    private val catRepository = FakeCatRepository()
    private val navigationHelper = RecordingNavigationHelper()
    private val tti = RecordingTTIHelper()
    private lateinit var viewModel: HomeViewModel

    /** 원격 구성 마이룸 플래그. 읽을 때마다 이 값을 돌려준다. */
    private var isMyRoomEnabled = false

    private val state get() = viewModel.uiState.value

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = HomeViewModel(
            navigationHelper = navigationHelper,
            messageHelper = SilentMessageHelper,
            getHomeSummary = GetHomeSummaryUseCase(
                repository = homeRepository,
                resourceHelper = FakeResourceHelper,
                messageHelper = SilentMessageHelper,
                navigationHelper = navigationHelper,
                ttiHelper = FakeTTIHelper,
            ),
            getCatAnimations = GetCatAnimationsUseCase(
                repository = catRepository,
                resourceHelper = FakeResourceHelper,
                messageHelper = SilentMessageHelper,
                navigationHelper = navigationHelper,
                ttiHelper = FakeTTIHelper,
            ),
            catMotionPicker = CatMotionPicker(Random(seed = 1)),
            ttiHelper = tti,
            remoteConfigHelper = object : RemoteConfigHelper {
                override suspend fun sync() = Unit

                @Suppress("UNCHECKED_CAST")
                override fun <T> get(key: RemoteConfigKey<T>): T =
                    if (key == RemoteConfigKey.MyRoomEnabled) isMyRoomEnabled as T else key.defaultValue
            },
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** 배경 하나와 아이템 셋(그중 하나는 아이템 id가 없다)이 놓인 방. */
    private val decoratedRoom = listOf(
        RoomPlacementVO(roomItemId = 101, category = RoomItemCategory.BED, itemId = 2, x = 50f, y = 20f, zIndex = 3, scale = 9f),
        RoomPlacementVO(roomItemId = 108, category = RoomItemCategory.BACKGROUND, itemId = 1),
        RoomPlacementVO(roomItemId = 104, category = RoomItemCategory.ETC, itemId = 3, x = 30f, y = 10f, zIndex = 1),
        RoomPlacementVO(roomItemId = 105, category = RoomItemCategory.ETC, itemId = null, x = 60f, y = 10f, zIndex = 2),
    )

    private fun TestScope.resumeWith(summary: HomeSummaryVO) {
        homeRepository.next = { summary }
        viewModel.onIntent(HomeIntent.ScreenResumed)
        advanceUntilIdle()
    }

    @Test
    fun `마이룸 플래그가 켜져 있으면 홈 요약의 놓인 아이템으로 고양이방 카드에 꾸민 방을 그린다`() = runTest(testDispatcher) {
        isMyRoomEnabled = true

        resumeWith(HomeSummaryVO(roomPlacements = decoratedRoom))

        val room = viewModel.uiState.value.room!!
        assertEquals("https://cdn.nyummy.co.kr/items/1.png", room.backgroundUrl)
        // 배경을 뺀 아이템을 zIndex 순서로, 저장 좌표를 그리는 좌표로 바꿔 그린다. id가 없는 아이템은 그릴 수 없어서 뺀다.
        assertEquals(listOf(104L, 101L), room.items.map { it.roomItemId })
        val bed = room.items.last()
        assertEquals("https://cdn.nyummy.co.kr/items/2.png", bed.imageUrl)
        assertEquals(0.5f, bed.x, 0.001f)
        assertEquals(0.8f, bed.y, 0.001f)
        assertEquals(RoomCardLayout.MaxItemScale, bed.scale)
    }

    @Test
    fun `마이룸 플래그가 꺼져 있으면 꾸민 방 없이 기본 빈 방만 그린다`() = runTest(testDispatcher) {
        resumeWith(HomeSummaryVO(roomPlacements = decoratedRoom))

        assertNull(viewModel.uiState.value.room)
    }

    @Test
    fun `홈에 들어오면 홈 TTI 를 시작한다`() {
        assertEquals("track:${HomeTTIPage.pageName}", tti.calls.first())
    }

    @Test
    fun `요약을 받았어도 화면이 그렸다고 알리기 전에는 홈 TTI 를 끝내지 않는다`() = runTest(testDispatcher) {
        resumeWith(HomeSummaryVO(coinBalance = 100))
        viewModel.onIntent(HomeIntent.CatShown)

        assertFalse(TTI_END in tti.calls)
        assertFalse(TTI_SHOT in tti.calls)
    }

    @Test
    fun `냐미만 그려지면 이미지 구간만 끝내고 홈 TTI 는 기다린다`() = runTest(testDispatcher) {
        viewModel.onIntent(HomeIntent.CatShown)

        assertTrue(IMAGE_END in tti.calls)
        assertFalse(TTI_END in tti.calls)
    }

    @Test
    fun `요약과 냐미가 둘 다 그려지면 홈 TTI 를 끝내고 바로 보낸다`() = runTest(testDispatcher) {
        resumeWith(HomeSummaryVO(coinBalance = 100))
        viewModel.onIntent(HomeIntent.SummaryShown)
        viewModel.onIntent(HomeIntent.CatShown)

        assertEquals(listOf(TTI_END, TTI_SHOT), tti.calls.filter { it == TTI_END || it == TTI_SHOT })
    }

    @Test
    fun `그렸다는 신호가 여러 번 와도 홈 TTI 는 한 번만 보낸다`() = runTest(testDispatcher) {
        resumeWith(HomeSummaryVO(coinBalance = 100))
        viewModel.onIntent(HomeIntent.CatShown)
        viewModel.onIntent(HomeIntent.SummaryShown)
        viewModel.onIntent(HomeIntent.CatShown)
        viewModel.onIntent(HomeIntent.SummaryShown)

        assertEquals(1, tti.calls.count { it == IMAGE_END })
        assertEquals(1, tti.calls.count { it == TTI_END })
        assertEquals(1, tti.calls.count { it == TTI_SHOT })
    }

    @Test
    fun `화면이 보일 때마다 홈 요약을 다시 읽는다`() = runTest(testDispatcher) {
        resumeWith(HomeSummaryVO(coinBalance = 100, todayRecordedCount = 0))
        assertEquals(100, state.summary.coinBalance)
        assertFalse(state.hasRecordedToday)

        // 식사를 기록하고 돌아온 경우
        resumeWith(HomeSummaryVO(coinBalance = 110, todayRecordedCount = 1))

        assertEquals(2, homeRepository.callCount)
        assertEquals(110, state.summary.coinBalance)
        assertTrue(state.hasRecordedToday)
    }

    @Test
    fun `요약을 읽지 못하면 직전 값을 그대로 둔다`() = runTest(testDispatcher) {
        resumeWith(HomeSummaryVO(coinBalance = 100))

        homeRepository.next = { throw IOException("offline") }
        viewModel.onIntent(HomeIntent.ScreenResumed)
        advanceUntilIdle()

        assertEquals(100, state.summary.coinBalance)
    }

    @Test
    fun `오늘 기록이 없으면 기다리는 냐미를 보여 주고 대사도 그 상태에서 고른다`() = runTest(testDispatcher) {
        resumeWith(HomeSummaryVO(todayRecordedCount = 0))

        val motion = state.catMotion!!
        assertEquals(sheetUrls(CatState.CONFLICTED_CAUTIOUS), motion.sheetUrls)
        assertEquals(
            listOf(NyummySpriteSource.Url(sheetUrl(CatState.CONFLICTED_CAUTIOUS, state.catGroup))),
            motion.clips.map { it.sheet.source },
        )
        assertEquals(state.catPlayId, motion.playId)
        assertTrue(state.catLine in lines(CatState.CONFLICTED_CAUTIOUS))
        assertTrue(motion.restMillis in CatMotionPicker.MIN_REST_MILLIS..CatMotionPicker.MAX_REST_MILLIS)
    }

    @Test
    fun `처음 들어왔을 때 오늘 기록이 있으면 먹는 반응 없이 바로 여유로운 냐미다`() = runTest(testDispatcher) {
        resumeWith(HomeSummaryVO(todayRecordedCount = 2))

        assertEquals(CatState.RELAXED, state.catState)
    }

    @Test
    fun `식사를 기록하고 돌아오면 먹는 반응을 보여 주고, 끝나면 여유로 돌아간다`() = runTest(testDispatcher) {
        resumeWith(HomeSummaryVO(todayRecordedCount = 0))

        resumeWith(HomeSummaryVO(todayRecordedCount = 1))
        assertEquals(CatState.CONTENT, state.catState)
        assertTrue(state.catLine in lines(CatState.CONTENT))

        // 먹는 반응 중에 다시 돌아와도(기록 수 그대로) 끊지 않는다.
        val playId = state.catPlayId
        resumeWith(HomeSummaryVO(todayRecordedCount = 1))
        assertEquals(CatState.CONTENT, state.catState)
        assertEquals(playId, state.catPlayId)

        viewModel.onIntent(HomeIntent.CatMotionFinished(playId))
        assertEquals(CatState.RELAXED, state.catState)
        assertTrue(state.catLine in lines(CatState.RELAXED))
    }

    @Test
    fun `동작이 끝나면 같은 상태의 다른 동작으로 넘어가고 대사는 그대로 둔다`() = runTest(testDispatcher) {
        resumeWith(HomeSummaryVO(todayRecordedCount = 0))
        val before = state

        viewModel.onIntent(HomeIntent.CatMotionFinished(before.catPlayId))

        assertEquals(CatState.CONFLICTED_CAUTIOUS, state.catState)
        assertNotEquals(before.catGroup, state.catGroup)
        assertEquals(before.catPlayId + 1, state.catPlayId)
        assertEquals(before.catLine, state.catLine)
    }

    @Test
    fun `늦게 도착한 이전 동작의 종료 신호는 무시한다`() = runTest(testDispatcher) {
        resumeWith(HomeSummaryVO(todayRecordedCount = 0))
        val stalePlayId = state.catPlayId
        resumeWith(HomeSummaryVO(todayRecordedCount = 1))
        val content = state

        viewModel.onIntent(HomeIntent.CatMotionFinished(stalePlayId))

        assertEquals(content, state)
    }

    @Test
    fun `냐미를 누르면 같은 상태의 다른 동작과 새 대사를 보여 준다`() = runTest(testDispatcher) {
        resumeWith(HomeSummaryVO(todayRecordedCount = 2))
        val before = state

        viewModel.onIntent(HomeIntent.ClickCat)

        assertEquals(CatState.RELAXED, state.catState)
        assertNotEquals(before.catGroup, state.catGroup)
        assertNotEquals(before.catLine, state.catLine)
        assertTrue(state.catLine in lines(CatState.RELAXED))
    }

    @Test
    fun `고른 상태가 응답에 없으면 기본 인사 동작과 대사를 쓴다`() = runTest(testDispatcher) {
        catRepository.next = { animationSet(CatState.FRIENDLY) }

        resumeWith(HomeSummaryVO(todayRecordedCount = 0))

        assertEquals(sheetUrls(CatState.FRIENDLY), state.catMotion?.sheetUrls)
        assertTrue(state.catLine in lines(CatState.FRIENDLY))
    }

    @Test
    fun `고양이가 없으면 기본 냐미로 대신하고, 다음에 화면이 보일 때 다시 받는다`() = runTest(testDispatcher) {
        catRepository.next = { throw notFound() }
        resumeWith(HomeSummaryVO(todayRecordedCount = 0))

        assertTrue(state.isCatAnimationFailed)
        assertNull(state.catMotion)
        assertNull(state.catLine)
        assertTrue(navigationHelper.pages.isEmpty())

        catRepository.next = { animationSet(CatState.CONFLICTED_CAUTIOUS) }
        resumeWith(HomeSummaryVO(todayRecordedCount = 0))

        assertFalse(state.isCatAnimationFailed)
        assertEquals(sheetUrls(CatState.CONFLICTED_CAUTIOUS), state.catMotion?.sheetUrls)
        assertEquals(2, catRepository.callCount)
    }

    @Test
    fun `애니메이션은 한 번 받으면 화면에 돌아올 때 다시 받지 않는다`() = runTest(testDispatcher) {
        resumeWith(HomeSummaryVO(todayRecordedCount = 0))
        resumeWith(HomeSummaryVO(todayRecordedCount = 0))

        assertEquals(1, catRepository.callCount)
    }

    private fun lines(state: CatState) = listOf("${state.name} 대사 1", "${state.name} 대사 2", "${state.name} 대사 3")

    private fun sheetUrl(state: CatState, group: Int) = "https://cdn/${state.name}_$group.png"

    private fun sheetUrls(state: CatState) = (0..2).map { sheetUrl(state, it) }

    private fun animation(state: CatState) = CatAnimationVO(
        state = state,
        frame = SpriteFrameVO(width = 136, height = 136, framesPerRow = 4, durationMs = 100),
        groups = (0..2).map { listOf(SpriteClipVO(url = sheetUrl(state, it), frames = 8)) },
        lines = lines(state),
    )

    private fun animationSet(vararg states: CatState) =
        CatAnimationSetVO(weight = "NORMAL", animations = states.associateWith { animation(it) })

    private fun notFound() = HttpResponseException(
        status = HttpResponseStatus.NotFound,
        rawCode = 404,
        errorRequestUrl = "https://test/api/v1/cats/animations",
        cause = Throwable("api.cat.notFound"),
    )

    private inner class FakeCatRepository : CatRepository {
        var next: () -> CatAnimationSetVO = {
            animationSet(CatState.CONFLICTED_CAUTIOUS, CatState.RELAXED, CatState.CONTENT, CatState.FRIENDLY)
        }
        var callCount = 0
            private set

        override suspend fun getAnimations(): CatAnimationSetVO {
            callCount++
            return next()
        }
    }

    private class FakeHomeRepository : HomeRepository {
        var next: () -> HomeSummaryVO = { HomeSummaryVO.empty }
        var callCount = 0
            private set

        override suspend fun getHomeSummary(): HomeSummaryVO {
            callCount++
            return next()
        }
    }

    private class RecordingNavigationHelper : NavigationHelper {
        val pages = mutableListOf<Page>()

        override val navigationFlow: Flow<NavSignal> = emptyFlow()
        override fun navigateByRoute(route: NavRoute) = Unit
        override fun navigateTo(page: Page) {
            pages += page
        }

        override fun navigateDeepLink(route: NavRoute) = Unit
        override fun navigateToBack() = Unit
        override fun navigateToAsRoot(page: Page) = Unit
        override fun navigateToInitial() = Unit
        override fun navigateToExternalLink(url: String) = Unit
    }

    private object SilentMessageHelper : MessageHelper {
        override val effect: Flow<MessageEffect> = emptyFlow()
        override fun showToast(toastMsg: String) = Unit
        override fun showToast(messageRes: Int) = Unit
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

    private object FakeResourceHelper : ResourceHelper {
        override fun getString(resource: StringResource): String = ""
    }

    private object FakeTTIHelper : TTIHelper {
        override fun startTTITracking(page: TTIPage) = Unit
        override fun startTTITimeline(category: TimelineCategory) = Unit
        override fun endTTITimeline(category: TimelineCategory) = Unit
        override fun endTTITracking() = Unit
        override fun shotTTILogging() = Unit
        override fun addTTIMetaData(metadata: TTIMetaData, value: Any?) = Unit
    }

    /** ViewModel 이 부른 TTI 호출을 순서대로 기록한다. UseCase 들은 [FakeTTIHelper] 를 써서 섞이지 않는다. */
    private class RecordingTTIHelper : TTIHelper {
        val calls = mutableListOf<String>()

        override fun startTTITracking(page: TTIPage) {
            calls += "track:${page.pageName}"
        }

        override fun startTTITimeline(category: TimelineCategory) {
            calls += "start:${category.name}"
        }

        override fun endTTITimeline(category: TimelineCategory) {
            calls += "end:${category.name}"
        }

        override fun endTTITracking() {
            calls += TTI_END
        }

        override fun shotTTILogging() {
            calls += TTI_SHOT
        }

        override fun addTTIMetaData(metadata: TTIMetaData, value: Any?) = Unit
    }

    private companion object {
        const val TTI_END = "endTracking"
        const val TTI_SHOT = "shot"
        val IMAGE_END = "end:${TimelineCategory.IMAGE_LOADED_TIME.name}"
    }
}
