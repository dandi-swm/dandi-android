package com.dandi.nyummy.home.presentation

import com.dandi.nyummy.cat.domain.CatBrain
import com.dandi.nyummy.cat.domain.CatLineRepository
import com.dandi.nyummy.cat.domain.CatRepository
import com.dandi.nyummy.cat.domain.GetCatAnimationsUseCase
import com.dandi.nyummy.cat.domain.GetCatLinesUseCase
import com.dandi.nyummy.cat.domain.RefreshCatWeightUseCase
import com.dandi.nyummy.cat.entity.CatAnimationSetVO
import com.dandi.nyummy.cat.entity.CatAnimationVO
import com.dandi.nyummy.cat.entity.CatLineVO
import com.dandi.nyummy.cat.entity.CatMotionVO
import com.dandi.nyummy.cat.entity.CatState
import com.dandi.nyummy.cat.entity.SpriteClipVO
import com.dandi.nyummy.cat.entity.SpriteFrameVO
import com.dandi.nyummy.common.domain.helper.CompanionMemoryHelper
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.common.domain.helper.StringResource
import com.dandi.nyummy.common.domain.helper.TutorialHelper
import com.dandi.nyummy.common.domain.message.IconType
import com.dandi.nyummy.common.domain.message.MessageEffect
import com.dandi.nyummy.common.domain.navigation.NavRoute
import com.dandi.nyummy.common.domain.navigation.NavSignal
import com.dandi.nyummy.common.domain.navigation.Page
import com.dandi.nyummy.common.domain.remoteconfig.RemoteConfigHelper
import com.dandi.nyummy.common.domain.remoteconfig.RemoteConfigKey
import com.dandi.nyummy.common.entity.tutorial.TutorialStep
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTutorialLesson
import com.dandi.nyummy.common.presentation.room.RoomCardLayout
import com.dandi.nyummy.home.domain.GetHomeSummaryUseCase
import com.dandi.nyummy.home.domain.HomeRepository
import com.dandi.nyummy.home.domain.tti.HomeTTIPage
import com.dandi.nyummy.home.entity.HomeNoticeVO
import com.dandi.nyummy.home.entity.HomeSummaryVO
import com.dandi.nyummy.meal.domain.MealRecordRepository
import com.dandi.nyummy.meal.domain.SubmitTutorialMealUseCase
import com.dandi.nyummy.meal.entity.CreatedMealVO
import com.dandi.nyummy.meal.entity.MealImageUploadVO
import com.dandi.nyummy.meal.entity.MealPhotoSource
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val homeRepository = FakeHomeRepository()
    private val catRepository = FakeCatRepository()
    private val companionMemory = FakeCompanionMemory()
    private val navigationHelper = RecordingNavigationHelper()
    private val tti = RecordingTTIHelper()
    private lateinit var viewModel: HomeViewModel

    /** 원격 구성 마이룸 플래그. 읽을 때마다 이 값을 돌려준다. */
    private var isMyRoomEnabled = false

    /** 원격 구성 우편함 아이템 플래그. 읽을 때마다 이 값을 돌려준다. */
    private var isMailEnabled = false

    private val tutorialHelper = FakeTutorialHelper()
    private val mealRepository = FakeMealRecordRepository()

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
            refreshCatWeight = RefreshCatWeightUseCase(
                repository = catRepository,
                resourceHelper = FakeResourceHelper,
                messageHelper = SilentMessageHelper,
                navigationHelper = navigationHelper,
                ttiHelper = FakeTTIHelper,
            ),
            catBrain = CatBrain(Random(seed = 1)),
            getCatLines = GetCatLinesUseCase(repository = object : CatLineRepository {
                override suspend fun getLines(): List<CatLineVO> = catLines
            }),
            companionMemory = companionMemory,
            ttiHelper = tti,
            remoteConfigHelper = object : RemoteConfigHelper {
                override suspend fun sync() = Unit

                @Suppress("UNCHECKED_CAST")
                override fun <T> get(key: RemoteConfigKey<T>): T = when (key) {
                    RemoteConfigKey.MyRoomEnabled -> isMyRoomEnabled as T
                    RemoteConfigKey.MailItemTabEnabled -> isMailEnabled as T
                    else -> key.defaultValue
                }
            },
            tutorialHelper = tutorialHelper,
            submitTutorialMeal = SubmitTutorialMealUseCase(
                repository = mealRepository,
                tutorialHelper = tutorialHelper,
                resourceHelper = FakeResourceHelper,
                messageHelper = SilentMessageHelper,
                navigationHelper = navigationHelper,
                ttiHelper = FakeTTIHelper,
            ),
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** 배경 하나와 아이템 셋(그중 하나는 아이템 id가 없다)이 놓인 방. */
    private val decoratedRoom = listOf(
        RoomPlacementVO(roomItemId = 101, category = RoomItemCategory("BED"), itemId = 2, x = 50f, y = 20f, zIndex = 3, scale = 9f),
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
    fun `처음 들어오면 인사 동작과 인사 대사를 보여 준다`() = runTest(testDispatcher) {
        companionMemory.lastSeen = null

        resumeWith(HomeSummaryVO(todayRecordedCount = 0))

        assertEquals(CatState.HOME_GREET, state.catState)
        assertEquals("인사", state.catLine)
        val motion = state.catMotion!!
        assertEquals(state.catPlayId, motion.playId)
        assertTrue(motion.sheetUrls.first().startsWith(sheetPrefix(CatState.HOME_GREET)))
    }

    @Test
    fun `방금 다녀갔으면 인사 없이 지금 분위기의 일상 동작을 한다`() = runTest(testDispatcher) {
        resumeWith(HomeSummaryVO(todayRecordedCount = 2))

        assertTrue(state.catState in moodStates)
        assertNotNull(state.catMotion)
    }

    @Test
    fun `식사를 기록하고 돌아오면 먹는 반응을 보여 주고, 끝나면 일상 동작으로 돌아간다`() = runTest(testDispatcher) {
        resumeWith(HomeSummaryVO(todayRecordedCount = 0))

        resumeWith(HomeSummaryVO(todayRecordedCount = 1))
        assertEquals(CatState.HOME_AFTER_MEAL, state.catState)
        assertEquals("잘 먹었다", state.catLine)

        // 먹는 반응 중에 다시 돌아와도(기록 수 그대로) 끊지 않는다.
        val playId = state.catPlayId
        resumeWith(HomeSummaryVO(todayRecordedCount = 1))
        assertEquals(CatState.HOME_AFTER_MEAL, state.catState)
        assertEquals(playId, state.catPlayId)

        viewModel.onIntent(HomeIntent.CatMotionFinished(playId))
        assertTrue(state.catState in moodStates)
        assertEquals(playId + 1, state.catPlayId)
    }

    @Test
    fun `늦게 도착한 이전 동작의 종료 신호는 무시한다`() = runTest(testDispatcher) {
        resumeWith(HomeSummaryVO(todayRecordedCount = 0))
        val stalePlayId = state.catPlayId
        resumeWith(HomeSummaryVO(todayRecordedCount = 1))
        val afterMeal = state

        viewModel.onIntent(HomeIntent.CatMotionFinished(stalePlayId))

        assertEquals(afterMeal, state)
    }

    @Test
    fun `냐미를 누르면 터치 반응을 하고, 대사가 아직 찍히는 중이면 대사는 그대로 둔다`() = runTest(testDispatcher) {
        resumeWith(HomeSummaryVO(todayRecordedCount = 2))
        val before = state

        viewModel.onIntent(HomeIntent.ClickCat)

        assertEquals(CatState.HOME_TAP, state.catState)
        assertEquals(before.catPlayId + 1, state.catPlayId)
        assertEquals(before.catLine, state.catLine)
    }

    @Test
    fun `대사가 다 찍힌 뒤 냐미를 누르면 터치 대사로 넘어간다`() = runTest(testDispatcher) {
        resumeWith(HomeSummaryVO(todayRecordedCount = 2))
        viewModel.onIntent(HomeIntent.CatLineRevealed(state.catLineId))

        viewModel.onIntent(HomeIntent.ClickCat)

        assertEquals(CatState.HOME_TAP, state.catState)
        assertEquals("간지러워", state.catLine)
        assertFalse(state.isCatLineRevealed)
    }

    @Test
    fun `애니메이션을 읽지 못하면 기본 냐미로 대신하고, 다음에 화면이 보일 때 다시 읽는다`() = runTest(testDispatcher) {
        catRepository.next = { CatAnimationSetVO.empty }
        resumeWith(HomeSummaryVO(todayRecordedCount = 0))

        assertTrue(state.isCatAnimationFailed)
        assertNull(state.catMotion)
        assertTrue(navigationHelper.pages.isEmpty())

        catRepository.next = { animationSet() }
        resumeWith(HomeSummaryVO(todayRecordedCount = 0))

        assertFalse(state.isCatAnimationFailed)
        assertNotNull(state.catMotion)
        assertEquals(2, catRepository.callCount)
    }

    @Test
    fun `애니메이션은 한 번 읽으면 체형이 그대로인 동안 다시 읽지 않는다`() = runTest(testDispatcher) {
        resumeWith(HomeSummaryVO(todayRecordedCount = 0))
        resumeWith(HomeSummaryVO(todayRecordedCount = 0))

        assertEquals(1, catRepository.callCount)
        assertEquals(2, catRepository.refreshCount)
    }

    @Test
    fun `체형이 바뀌었으면 새 체형 애니메이션을 다시 읽는다`() = runTest(testDispatcher) {
        resumeWith(HomeSummaryVO(todayRecordedCount = 0))
        catRepository.weightChanged = true

        resumeWith(HomeSummaryVO(todayRecordedCount = 0))

        assertEquals(2, catRepository.callCount)
    }

    @Test
    fun `홈을 떠나면 마지막 방문 시각을 남긴다`() = runTest(testDispatcher) {
        companionMemory.lastSeen = null

        viewModel.onIntent(HomeIntent.ScreenPaused)
        advanceUntilIdle()

        assertNotNull(companionMemory.lastSeen)
    }

    /** 분위기(낮·졸림·밤)로 고를 수 있는 상황. 테스트를 도는 시각에 따라 달라서 셋 중 하나인지만 본다. */
    private val moodStates = setOf(CatState.HOME_DAY, CatState.HOME_DROWSY, CatState.HOME_NIGHT)

    private val catLines = listOf(
        CatLineVO("인사", setOf("greet")),
        CatLineVO("잘 먹었다", setOf("after_meal")),
        CatLineVO("간지러워", setOf("tap")),
        CatLineVO("좋은 아침", setOf("morning")),
        CatLineVO("낮이다", setOf("day")),
        CatLineVO("졸려", setOf("night")),
    )

    private fun sheetPrefix(state: CatState) = "https://cdn/${state.name}_"

    private fun animation(state: CatState) = CatAnimationVO(
        state = state,
        frame = SpriteFrameVO(width = 136, height = 136, framesPerRow = 4, durationMs = 100),
        motions = (0..2).map { CatMotionVO(name = "${state.name}_$it", weight = 1.0, clips = listOf(SpriteClipVO(url = "${sheetPrefix(state)}$it.png", frames = 8))) },
    )

    private fun animationSet() = CatAnimationSetVO(
        weight = "NORMAL",
        animations = (CatState.entries - CatState.UNKNOWN - CatState.ROOM_IDLE).associateWith { animation(it) },
    )

    // region 첫 사용자 튜토리얼

    private val beforeTutorialMeal = HomeSummaryVO(isTutorialCompleted = false)
    private val afterTutorialMeal = HomeSummaryVO(isTutorialCompleted = true)

    @Test
    fun `튜토리얼을 마치지 않았으면 홈 소개를 띄우고 식사 기록 화면이 튜토리얼로 열리게 한다`() = runTest(testDispatcher) {
        resumeWith(beforeTutorialMeal)

        assertEquals(HomeTutorialStage.Intro, state.tutorial?.stage)
        assertEquals(0, state.tutorial?.index)
        assertTrue(tutorialHelper.mealRequired)
    }

    @Test
    fun `튜토리얼을 마친 사용자는 아무것도 띄우지 않는다`() = runTest(testDispatcher) {
        resumeWith(afterTutorialMeal)

        assertNull(state.tutorial)
        assertNull(state.healthStory)
        assertFalse(tutorialHelper.mealRequired)
    }

    @Test
    fun `소개 대사를 넘기면 카메라 버튼을 가리키고 그 뒤로는 넘어가지 않는다`() = runTest(testDispatcher) {
        resumeWith(beforeTutorialMeal)

        repeat(5) { viewModel.onIntent(HomeIntent.TutorialAdvance) }

        assertEquals(2, state.tutorial?.index)
        assertNotNull(state.tutorial?.line?.target)
    }

    @Test
    fun `식사 기록에서 기록하지 않고 돌아오면 카메라 버튼을 계속 가리킨다`() = runTest(testDispatcher) {
        resumeWith(beforeTutorialMeal)
        repeat(2) { viewModel.onIntent(HomeIntent.TutorialAdvance) }
        viewModel.onIntent(HomeIntent.TutorialTargetClicked)

        resumeWith(beforeTutorialMeal)

        assertEquals(HomeTutorialStage.Intro, state.tutorial?.stage)
        assertEquals(2, state.tutorial?.index)
    }

    @Test
    fun `튜토리얼 식사 뒤에는 우편 버튼을 가리키고 누르면 그 단계를 마친다`() = runTest(testDispatcher) {
        isMailEnabled = true
        tutorialHelper.startAfterMeal()
        resumeWith(afterTutorialMeal)

        assertEquals(HomeTutorialStage.MailPointer, state.tutorial?.stage)

        viewModel.onIntent(HomeIntent.TutorialTargetClicked)
        advanceUntilIdle()

        assertNull(state.tutorial)
        assertFalse(TutorialStep.HOME_MAIL in tutorialHelper.pending)
    }

    @Test
    fun `우편함 단계를 마치고 돌아오면 마이룸 탭을 가리킨다`() = runTest(testDispatcher) {
        isMailEnabled = true
        isMyRoomEnabled = true
        tutorialHelper.startAfterMeal()
        tutorialHelper.complete(TutorialStep.HOME_MAIL)
        tutorialHelper.complete(TutorialStep.MAILBOX)

        resumeWith(afterTutorialMeal)

        assertEquals(HomeTutorialStage.RoomTabPointer, state.tutorial?.stage)
    }

    @Test
    fun `우편함 기능이 꺼져 있으면 우편 단계를 마치고 마이룸 탭으로 간다`() = runTest(testDispatcher) {
        isMyRoomEnabled = true
        tutorialHelper.startAfterMeal()

        resumeWith(afterTutorialMeal)

        assertEquals(HomeTutorialStage.RoomTabPointer, state.tutorial?.stage)
        assertFalse(TutorialStep.MAILBOX in tutorialHelper.pending)
    }

    @Test
    fun `우편함과 마이룸 단계를 마치면 냐미 건강 이야기를 보이고 끝까지 넘기면 튜토리얼을 마친다`() = runTest(testDispatcher) {
        tutorialHelper.startAfterMeal()

        resumeWith(afterTutorialMeal)

        assertNull(state.tutorial)
        assertEquals(0, state.healthStory?.page)
        repeat(3) { viewModel.onIntent(HomeIntent.HealthStoryNext) }
        assertTrue(state.healthStory?.isFinish == true)

        viewModel.onIntent(HomeIntent.TutorialFinished)
        advanceUntilIdle()

        assertNull(state.healthStory)
        assertTrue(tutorialHelper.pending.isEmpty())
    }

    @Test
    fun `마이룸을 저장하고 돌아오면 방이 저장됐다고 알리고 넘기면 냐미 건강 이야기를 보인다`() = runTest(testDispatcher) {
        isMyRoomEnabled = true
        tutorialHelper.pending += setOf(TutorialStep.HOME_ROOM_SAVED, TutorialStep.HEALTH_STORY)

        resumeWith(afterTutorialMeal)

        assertEquals(HomeTutorialStage.RoomSaved, state.tutorial?.stage)
        assertNull(state.healthStory)

        viewModel.onIntent(HomeIntent.TutorialAdvance)
        advanceUntilIdle()

        assertNull(state.tutorial)
        assertEquals(0, state.healthStory?.page)
        assertEquals(setOf(TutorialStep.HEALTH_STORY), tutorialHelper.pending)
    }

    @Test
    fun `건너뛰기를 누르면 남은 안내와 함께 확인을 띄우고 계속 배우면 같은 단계로 돌아온다`() = runTest(testDispatcher) {
        isMailEnabled = true
        isMyRoomEnabled = true
        resumeWith(beforeTutorialMeal)
        viewModel.onIntent(HomeIntent.TutorialAdvance)

        viewModel.onIntent(HomeIntent.TutorialSkip)

        assertEquals(NyummyTutorialLesson.entries, state.tutorialSkipConfirm)

        viewModel.onIntent(HomeIntent.TutorialSkipCancelled)

        assertNull(state.tutorialSkipConfirm)
        assertEquals(1, state.tutorial?.index)
    }

    @Test
    fun `마이룸 탭 단계에서 건너뛰면 남은 안내는 꾸미기와 건강 이야기뿐이다`() = runTest(testDispatcher) {
        isMyRoomEnabled = true
        tutorialHelper.startAfterMeal()
        resumeWith(afterTutorialMeal)

        viewModel.onIntent(HomeIntent.TutorialSkip)

        assertEquals(listOf(NyummyTutorialLesson.DecorateRoom, NyummyTutorialLesson.CatHealth), state.tutorialSkipConfirm)
    }

    @Test
    fun `튜토리얼 식사 전에 건너뛰면 튜토리얼 식사를 대신 기록하고 냐미 건강 이야기만 남긴다`() = runTest(testDispatcher) {
        resumeWith(beforeTutorialMeal)
        viewModel.onIntent(HomeIntent.TutorialSkip)

        viewModel.onIntent(HomeIntent.TutorialSkipConfirmed)
        advanceUntilIdle()

        assertEquals(1, mealRepository.tutorialMealCount)
        assertNull(state.tutorial)
        assertNull(state.tutorialSkipConfirm)
        assertEquals(0, state.healthStory?.page)
        assertEquals(setOf(TutorialStep.HEALTH_STORY), tutorialHelper.pending)
    }

    @Test
    fun `튜토리얼 식사 뒤에 건너뛰면 튜토리얼 식사를 다시 부르지 않는다`() = runTest(testDispatcher) {
        isMyRoomEnabled = true
        tutorialHelper.startAfterMeal()
        resumeWith(afterTutorialMeal)
        viewModel.onIntent(HomeIntent.TutorialSkip)

        viewModel.onIntent(HomeIntent.TutorialSkipConfirmed)
        advanceUntilIdle()

        assertEquals(0, mealRepository.tutorialMealCount)
        assertEquals(setOf(TutorialStep.HEALTH_STORY), tutorialHelper.pending)
    }

    @Test
    fun `건너뛰기 뒤 튜토리얼 식사 기록에 실패해도 다음 홈에서 소개보다 건강 이야기를 먼저 보인다`() = runTest(testDispatcher) {
        mealRepository.tutorialMealError = IOException("network")
        resumeWith(beforeTutorialMeal)
        viewModel.onIntent(HomeIntent.TutorialSkip)
        viewModel.onIntent(HomeIntent.TutorialSkipConfirmed)
        advanceUntilIdle()

        resumeWith(beforeTutorialMeal)

        assertNull(state.tutorial)
        assertEquals(0, state.healthStory?.page)
    }

    private class FakeTutorialHelper : TutorialHelper {
        val pending = mutableSetOf<TutorialStep>()
        var mealRequired = false

        override suspend fun pendingSteps(): Set<TutorialStep> = pending.toSet()
        override suspend fun startAfterMeal() {
            pending.clear()
            pending += TutorialStep.entries
        }

        override suspend fun complete(step: TutorialStep) {
            pending -= step
        }

        override suspend fun skipToHealthStory() {
            pending.clear()
            pending += TutorialStep.HEALTH_STORY
        }

        override suspend fun finish() = pending.clear()
        override suspend fun clear() = pending.clear()
        override suspend fun isMealTutorialRequired(): Boolean = mealRequired
        override suspend fun setMealTutorialRequired(required: Boolean) {
            mealRequired = required
        }
    }

    private class FakeMealRecordRepository : MealRecordRepository {
        var tutorialMealCount = 0
            private set
        var tutorialMealError: Exception? = null

        override suspend fun readGalleryPhotoTakenAt(photoUri: String): LocalDateTime? = null
        override suspend fun importGalleryPhoto(photoUri: String) = photoUri
        override suspend fun prepareUploadImage(photoPath: String, source: MealPhotoSource) = Unit
        override suspend fun issueImageUploadUrl(photoPath: String) = MealImageUploadVO()
        override suspend fun uploadImage(uploadTarget: MealImageUploadVO, photoPath: String) = Unit
        override suspend fun createMeal(imageKey: String) = CreatedMealVO.empty
        override suspend fun createTutorialMeal() {
            tutorialMealCount++
            tutorialMealError?.let { throw it }
        }
    }

    // endregion

    /** 마지막 방문 시각. 기본은 방금이라 들어올 때 인사하지 않는다. */
    private class FakeCompanionMemory : CompanionMemoryHelper {
        var lastSeen: Long? = System.currentTimeMillis()

        override suspend fun lastSeenAtMillis(): Long? = lastSeen
        override suspend fun setLastSeenAtMillis(millis: Long) {
            lastSeen = millis
        }

        override suspend fun lastCatWeight(): String? = null
        override suspend fun setLastCatWeight(weight: String) = Unit
        override suspend fun clear() = Unit
    }

    private inner class FakeCatRepository : CatRepository {
        var next: () -> CatAnimationSetVO = { animationSet() }
        var weightChanged = false
        var callCount = 0
            private set
        var refreshCount = 0
            private set

        override suspend fun getAnimations(): CatAnimationSetVO {
            callCount++
            return next()
        }

        override suspend fun refreshWeight(): Boolean {
            refreshCount++
            return weightChanged
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

        override fun prefetchHomeSummary() = Unit

        var noticeList: List<HomeNoticeVO> = emptyList()
        var hiddenDate: LocalDate? = null
        var noticeHandled = false

        override fun getNotices(): List<HomeNoticeVO> = noticeList
        override suspend fun getNoticeHiddenDate(): LocalDate? = hiddenDate
        override suspend fun setNoticeHiddenDate(date: LocalDate) {
            hiddenDate = date
        }

        override fun isNoticeHandled(): Boolean = noticeHandled
        override fun markNoticeHandled() {
            noticeHandled = true
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
