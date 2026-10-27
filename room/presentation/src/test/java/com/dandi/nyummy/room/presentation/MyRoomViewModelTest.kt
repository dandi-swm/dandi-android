package com.dandi.nyummy.room.presentation

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
import com.dandi.nyummy.common.domain.helper.TutorialHelper
import com.dandi.nyummy.common.domain.message.IconType
import com.dandi.nyummy.common.domain.message.MessageEffect
import com.dandi.nyummy.common.domain.navigation.NavRoute
import com.dandi.nyummy.common.domain.navigation.NavSignal
import com.dandi.nyummy.common.domain.navigation.Page
import com.dandi.nyummy.common.entity.tutorial.TutorialStep
import com.dandi.nyummy.common.presentation.designsystem.component.NyummySpriteSource
import com.dandi.nyummy.common.presentation.room.RoomCardLayout
import com.dandi.nyummy.room.domain.GetOwnedRoomItemsUseCase
import com.dandi.nyummy.room.domain.GetRoomUseCase
import com.dandi.nyummy.room.domain.RoomRepository
import com.dandi.nyummy.room.domain.SaveRoomUseCase
import com.dandi.nyummy.room.entity.OwnedRoomItemPageVO
import com.dandi.nyummy.room.entity.OwnedRoomItemVO
import com.dandi.nyummy.room.entity.RoomItemCategory
import com.dandi.nyummy.room.entity.RoomPlacementVO
import com.dandi.nyummy.room.entity.RoomVO
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class MyRoomViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val repository = FakeRoomRepository()
    private val catRepository = FakeCatRepository()
    private val messageHelper = RecordingMessageHelper()
    private val navigationHelper = RecordingNavigationHelper()
    private val tutorialHelper = FakeTutorialHelper()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository.room = RoomVO(
            categories = listOf(
                page(RoomItemCategory.BACKGROUND, owned(100, "햇살 방"), owned(101, "민트 방"), name = "배경"),
                page(RoomItemCategory.BED, owned(1, "구름 방석"), nextCursor = 1, name = "침대"),
                page(RoomItemCategory.ETC, owned(2, "털실 공"), owned(3, "털실 공"), name = "기타"),
            ),
            placements = listOf(
                // 저장 좌표는 그림 왼쪽 아래 기준 퍼센트다. (50, 20)은 그리는 좌표로 (0.5, 0.8)이다.
                placed(1, zIndex = 2, x = 50f, y = 20f),
                RoomPlacementVO(roomItemId = 100, category = RoomItemCategory.BACKGROUND, itemId = 100),
                // 범위를 벗어난 좌표와 배율은 방 안으로 맞춘다.
                placed(2, zIndex = 1, x = 120f, y = 480f, scale = 9f),
            ),
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `방을 불러오면 종류마다 탭을 만들고 그리는 순서와 놓임 표시를 맞춘다`() = runTest(testDispatcher) {
        val state = loadedViewModel().uiState.value

        assertEquals(MyRoomLoadState.LOADED, state.loadState)
        assertEquals(100L, state.room.background?.roomItemId)
        assertEquals(listOf(2L, 1L), state.room.items.map { it.roomItemId })
        val outOfRange = state.room.items.first()
        // 100을 넘는 x는 오른쪽 끝, y는 위 끝이다.
        assertEquals(1f, outOfRange.x)
        assertEquals(0f, outOfRange.y)
        val inRange = state.room.items.last()
        assertEquals(0.5f, inRange.x, 0.001f)
        assertEquals(0.8f, inRange.y, 0.001f)
        assertEquals(RoomCardLayout.MaxItemScale, outOfRange.scale)
        // 그림은 아이템 id를 CDN 아이템 경로에 붙인 주소다.
        assertEquals("https://cdn.nyummy.co.kr/items/100.png", state.room.background?.imageUrl)
        assertEquals("https://cdn.nyummy.co.kr/items/1.png", inRange.imageUrl)
        // 탭은 서버가 주지 않은 종류까지 늘 세 개이고, 각 탭에는 방과 함께 받은 첫 묶음이 들어간다.
        assertEquals(RoomItemCategory.entries, state.inventoryTabs.map { it.category })
        assertEquals(listOf("배경", "침대", "기타"), state.inventoryTabs.map { it.name })
        assertEquals(listOf(null, 1L, null), state.inventoryTabs.map { it.nextCursor })
        assertEquals(listOf(true, false), state.itemsOf(RoomItemCategory.BACKGROUND).map { it.isPlaced })
        assertEquals(listOf(true), state.itemsOf(RoomItemCategory.BED).map { it.isPlaced })
        // 같은 아이템을 두 개 가지면 칸도 두 개다.
        assertEquals(listOf(2L, 3L), state.itemsOf(RoomItemCategory.ETC).map { it.roomItemId })
        assertEquals(listOf(true, false), state.itemsOf(RoomItemCategory.ETC).map { it.isPlaced })
        assertFalse(state.hasChanges)
        assertFalse(state.canSave)
    }

    @Test
    fun `놓이지 않은 아이템 칸을 누르면 화면이 정한 바닥 점에 놓고 고른다`() = runTest(testDispatcher) {
        val viewModel = loadedViewModel()

        viewModel.onIntent(MyRoomIntent.ClickInteriorItem(3, x = 0.45f, y = 0.7f))

        val state = viewModel.uiState.value
        val added = state.room.items.last()
        assertEquals(3L, added.roomItemId)
        assertEquals(0.45f, added.x)
        assertEquals(0.7f, added.y)
        assertEquals(1f, added.scale)
        assertEquals(3L, state.selectedItemId)
        assertTrue(state.itemsOf(RoomItemCategory.ETC).first { it.roomItemId == 3L }.isPlaced)
        assertTrue(state.canUndo)
        assertTrue(state.canSave)
    }

    @Test
    fun `놓인 아이템 칸을 누르면 방에서 고르기만 하고 되돌리기는 쌓지 않는다`() = runTest(testDispatcher) {
        val viewModel = loadedViewModel()

        viewModel.onIntent(MyRoomIntent.ClickInteriorItem(1, x = 0.45f, y = 0.7f))

        val state = viewModel.uiState.value
        assertEquals(1L, state.selectedItemId)
        assertFalse(state.canUndo)
        assertFalse(state.hasChanges)
    }

    @Test
    fun `옮기고 키운 아이템은 되돌리기로 한 단계 전으로 돌아간다`() = runTest(testDispatcher) {
        val viewModel = loadedViewModel()

        viewModel.onIntent(MyRoomIntent.MoveItem(1, x = 0.2f, y = 0.7f, scale = 5f))
        val moved = viewModel.uiState.value.room.items.first { it.roomItemId == 1L }
        assertEquals(0.2f, moved.x)
        assertEquals(RoomCardLayout.MaxItemScale, moved.scale)

        viewModel.onIntent(MyRoomIntent.Undo)

        val state = viewModel.uiState.value
        assertEquals(0.5f, state.room.items.first { it.roomItemId == 1L }.x, 0.001f)
        assertFalse(state.canUndo)
        assertFalse(state.hasChanges)
    }

    @Test
    fun `옮기거나 크기를 바꾼 아이템은 맨 앞으로 오고 되돌리면 원래 층으로 간다`() = runTest(testDispatcher) {
        val viewModel = loadedViewModel()
        // 불러온 순서는 2(뒤), 1(앞)이다.

        viewModel.onIntent(MyRoomIntent.MoveItem(2, x = 0.4f, y = 0.9f, scale = 2f))
        assertEquals(listOf(1L, 2L), viewModel.uiState.value.room.items.map { it.roomItemId })

        viewModel.onIntent(MyRoomIntent.Undo)
        assertEquals(listOf(2L, 1L), viewModel.uiState.value.room.items.map { it.roomItemId })
    }

    @Test
    fun `제자리에 놓으면 고르기만 하고 순서도 바뀐 것으로 보지 않는다`() = runTest(testDispatcher) {
        val viewModel = loadedViewModel()

        viewModel.onIntent(MyRoomIntent.MoveItem(2, x = 1f, y = 0f, scale = RoomCardLayout.MaxItemScale))

        val state = viewModel.uiState.value
        assertEquals(2L, state.selectedItemId)
        assertEquals(listOf(2L, 1L), state.room.items.map { it.roomItemId })
        assertFalse(state.canUndo)
    }

    @Test
    fun `고른 아이템을 맨 앞으로 보내고 보관함에 넣는다`() = runTest(testDispatcher) {
        val viewModel = loadedViewModel()
        viewModel.onIntent(MyRoomIntent.SelectItem(2))

        viewModel.onIntent(MyRoomIntent.BringSelectedToFront)
        assertEquals(listOf(1L, 2L), viewModel.uiState.value.room.items.map { it.roomItemId })

        viewModel.onIntent(MyRoomIntent.PutSelectedAway)
        val state = viewModel.uiState.value
        assertEquals(listOf(1L), state.room.items.map { it.roomItemId })
        assertNull(state.selectedItemId)
        assertFalse(state.itemsOf(RoomItemCategory.ETC).first { it.roomItemId == 2L }.isPlaced)
    }

    @Test
    fun `배경을 바꾸면 사용 중 표시가 옮겨 가고 같은 배경을 다시 누르면 그대로다`() = runTest(testDispatcher) {
        val viewModel = loadedViewModel()

        viewModel.onIntent(MyRoomIntent.ClickBackground(100))
        assertFalse(viewModel.uiState.value.canUndo)

        viewModel.onIntent(MyRoomIntent.ClickBackground(101))
        val state = viewModel.uiState.value
        assertEquals(101L, state.room.background?.roomItemId)
        assertEquals(listOf(false, true), state.itemsOf(RoomItemCategory.BACKGROUND).map { it.isPlaced })
    }

    @Test
    fun `저장하면 배경을 맨 뒤로 두고 그리는 순서대로 zIndex 를 매겨 보낸 뒤 홈으로 돌아간다`() = runTest(testDispatcher) {
        val viewModel = loadedViewModel()
        viewModel.onIntent(MyRoomIntent.ClickInteriorItem(3, x = 0.45f, y = 0.7f))

        viewModel.onIntent(MyRoomIntent.ClickSave)
        advanceUntilIdle()

        val saved = repository.saved.single()
        assertEquals(listOf(100L, 2L, 1L, 3L), saved.map { it.roomItemId })
        assertEquals(listOf(0, 1, 2, 3), saved.map { it.zIndex })
        // 그리는 좌표 (0.45, 0.7)은 그림 왼쪽 아래 기준 퍼센트 (45, 30)으로 보낸다.
        val added = saved.last()
        assertEquals(45f, added.x, 0.01f)
        assertEquals(30f, added.y, 0.01f)
        assertEquals(50f, saved[2].x, 0.01f)
        assertEquals(20f, saved[2].y, 0.01f)
        assertEquals(RoomItemCategory.BACKGROUND, saved.first().category)
        assertEquals(1, navigationHelper.backCount)
        assertEquals("방을 저장했어요", messageHelper.snackBars.single().messageText)
        assertFalse(viewModel.uiState.value.hasChanges)
    }

    @Test
    fun `가지고 있지 않은 아이템이 섞여 저장에 실패하면 안내하고 방을 다시 불러온다`() = runTest(testDispatcher) {
        val viewModel = loadedViewModel()
        viewModel.onIntent(MyRoomIntent.ClickInteriorItem(3, x = 0.45f, y = 0.7f))
        repository.saveError = httpException(404, "api.room.itemNotFound")

        viewModel.onIntent(MyRoomIntent.ClickSave)
        advanceUntilIdle()

        assertEquals(IconType.WARNING, messageHelper.snackBars.single().iconType)
        assertEquals(2, repository.roomRequests)
        assertEquals(0, navigationHelper.backCount)
        assertFalse(viewModel.uiState.value.hasChanges)
    }

    @Test
    fun `저장에 실패하면 꾸민 방을 그대로 두고 안내한다`() = runTest(testDispatcher) {
        val viewModel = loadedViewModel()
        viewModel.onIntent(MyRoomIntent.ClickInteriorItem(3, x = 0.45f, y = 0.7f))
        repository.saveError = httpException(400)

        viewModel.onIntent(MyRoomIntent.ClickSave)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(IconType.ERROR, messageHelper.snackBars.single().iconType)
        assertTrue(state.hasChanges)
        assertFalse(state.isSaving)
        assertEquals(0, navigationHelper.backCount)
    }

    @Test
    fun `저장하지 않은 변경이 있으면 나갈 때 저장할지 묻고 저장 안 함이면 그냥 나간다`() = runTest(testDispatcher) {
        val viewModel = loadedViewModel()
        viewModel.onIntent(MyRoomIntent.ClickInteriorItem(3, x = 0.45f, y = 0.7f))

        viewModel.onIntent(MyRoomIntent.ClickBack)

        val dialog = messageHelper.twoButtonDialogs.single()
        assertEquals("꾸민 방을 저장할까요?", dialog.descText)
        assertEquals(0, navigationHelper.backCount)
        dialog.onClickLeft?.invoke()
        assertEquals(1, navigationHelper.backCount)
        assertTrue(repository.saved.isEmpty())
    }

    @Test
    fun `저장할지 묻는 창에서 저장하기를 누르면 저장하고 나간다`() = runTest(testDispatcher) {
        val viewModel = loadedViewModel()
        viewModel.onIntent(MyRoomIntent.ClickInteriorItem(3, x = 0.45f, y = 0.7f))
        viewModel.onIntent(MyRoomIntent.ClickBack)

        messageHelper.twoButtonDialogs.single().onClickRight?.invoke()
        advanceUntilIdle()

        assertEquals(1, repository.saved.size)
        assertEquals(1, navigationHelper.backCount)
    }

    @Test
    fun `바뀐 것이 없으면 묻지 않고 나간다`() = runTest(testDispatcher) {
        val viewModel = loadedViewModel()

        viewModel.onIntent(MyRoomIntent.ClickBack)

        assertTrue(messageHelper.twoButtonDialogs.isEmpty())
        assertEquals(1, navigationHelper.backCount)
    }

    @Test
    fun `처음에는 첫 탭인 배경을 보고 탭을 누르면 그 종류로 바꾼다`() = runTest(testDispatcher) {
        val viewModel = loadedViewModel()
        assertEquals(RoomItemCategory.BACKGROUND, viewModel.uiState.value.selectedCategory)

        viewModel.onIntent(MyRoomIntent.SelectTab(RoomItemCategory.ETC))

        assertEquals(RoomItemCategory.ETC, viewModel.uiState.value.selectedCategory)
    }

    @Test
    fun `탭 끝에 닿으면 그 종류의 다음 묶음만 받아 그 탭 끝에 붙인다`() = runTest(testDispatcher) {
        val viewModel = loadedViewModel()
        // 묶음 사이에 겹친 칸(1)은 한 번만 둔다.
        repository.ownedPage = page(RoomItemCategory.BED, owned(4, "바구니 침대"), owned(1, "구름 방석"), nextCursor = 9)

        // 다음 묶음이 없는 종류는 요청하지 않는다.
        viewModel.onIntent(MyRoomIntent.ReachInventoryEnd(RoomItemCategory.ETC))
        // 받는 중에 또 끝에 닿아도 한 번만 요청한다.
        viewModel.onIntent(MyRoomIntent.ReachInventoryEnd(RoomItemCategory.BED))
        assertTrue(viewModel.uiState.value.tabOf(RoomItemCategory.BED)!!.isLoadingMore)
        viewModel.onIntent(MyRoomIntent.ReachInventoryEnd(RoomItemCategory.BED))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(listOf(Triple(RoomItemCategory.BED, 1L, GetRoomUseCase.PAGE_SIZE)), repository.ownedRequests)
        val bed = state.tabOf(RoomItemCategory.BED)!!
        assertEquals(listOf(1L, 4L), bed.items.map { it.roomItemId })
        assertEquals(listOf(true, false), bed.items.map { it.isPlaced })
        assertEquals(9L, bed.nextCursor)
        assertFalse(bed.isLoadingMore)
        assertEquals(listOf(2L, 3L), state.itemsOf(RoomItemCategory.ETC).map { it.roomItemId })

        // 마지막 묶음을 받으면 더 요청하지 않는다.
        repository.ownedPage = page(RoomItemCategory.BED, owned(5, "해먹"))
        viewModel.onIntent(MyRoomIntent.ReachInventoryEnd(RoomItemCategory.BED))
        advanceUntilIdle()
        viewModel.onIntent(MyRoomIntent.ReachInventoryEnd(RoomItemCategory.BED))
        advanceUntilIdle()

        assertEquals(listOf(1L, 9L), repository.ownedRequests.map { it.second })
        assertEquals(listOf(1L, 4L, 5L), viewModel.uiState.value.itemsOf(RoomItemCategory.BED).map { it.roomItemId })
        assertNull(viewModel.uiState.value.tabOf(RoomItemCategory.BED)!!.nextCursor)
    }

    @Test
    fun `다음 묶음을 받지 못하면 칸과 cursor 를 그대로 두고 다음에 다시 받는다`() = runTest(testDispatcher) {
        val viewModel = loadedViewModel()
        repository.ownedError = httpException(500)

        viewModel.onIntent(MyRoomIntent.ReachInventoryEnd(RoomItemCategory.BED))
        advanceUntilIdle()

        val bed = viewModel.uiState.value.tabOf(RoomItemCategory.BED)!!
        assertEquals(listOf(1L), bed.items.map { it.roomItemId })
        assertEquals(1L, bed.nextCursor)
        assertFalse(bed.isLoadingMore)

        repository.ownedError = null
        repository.ownedPage = page(RoomItemCategory.BED, owned(4, "바구니 침대"))
        viewModel.onIntent(MyRoomIntent.ReachInventoryEnd(RoomItemCategory.BED))
        advanceUntilIdle()

        assertEquals(listOf(1L, 1L), repository.ownedRequests.map { it.second })
        assertEquals(listOf(1L, 4L), viewModel.uiState.value.itemsOf(RoomItemCategory.BED).map { it.roomItemId })
    }

    @Test
    fun `방을 불러오지 못하면 다시 불러오기로 다시 시도한다`() = runTest(testDispatcher) {
        repository.loadError = httpException(500)
        val viewModel = createViewModel()
        advanceUntilIdle()
        assertEquals(MyRoomLoadState.FAILED, viewModel.uiState.value.loadState)

        repository.loadError = null
        viewModel.onIntent(MyRoomIntent.RetryLoad)
        advanceUntilIdle()

        assertEquals(MyRoomLoadState.LOADED, viewModel.uiState.value.loadState)
    }

    @Test
    fun `냐미는 마이룸 꾸미기 동작을 하고 끝나면 다음 동작으로 넘어간다`() = runTest(testDispatcher) {
        val viewModel = loadedViewModel()
        val first = viewModel.uiState.value.catMotion!!
        assertEquals(NyummySpriteSource.Url("playful-${first.group}"), first.clips.single().sheet.source)

        // 이미 지난 동작이 끝났다는 신호는 무시한다.
        viewModel.onIntent(MyRoomIntent.CatMotionFinished(first.playId - 1))
        assertEquals(first.playId, viewModel.uiState.value.catMotion?.playId)

        viewModel.onIntent(MyRoomIntent.CatMotionFinished(first.playId))
        val next = viewModel.uiState.value.catMotion!!
        assertEquals(first.playId + 1, next.playId)
        // 묶음이 둘이면 바로 전 묶음은 고르지 않는다.
        assertTrue(next.group != first.group)
    }

    @Test
    fun `냐미 애니메이션을 받지 못하면 냐미 없이 꾸민다`() = runTest(testDispatcher) {
        catRepository.error = httpException(500)

        val state = loadedViewModel().uiState.value

        assertNull(state.catMotion)
        assertEquals(MyRoomLoadState.LOADED, state.loadState)
    }

    @Test
    fun `튜토리얼 대상이 아니면 안내하지 않는다`() = runTest(testDispatcher) {
        val viewModel = loadedViewModel()

        assertNull(viewModel.uiState.value.tutorial)
    }

    @Test
    fun `놓지 않은 아이템이 있으면 그 탭으로 바꿔 칸을 가리키고 인테리어를 배경보다 먼저 고른다`() = runTest(testDispatcher) {
        tutorialHelper.pending = true

        val state = loadedViewModel().uiState.value

        // 놓이지 않은 칸은 배경 101과 기타 3이다.
        assertEquals(RoomItemCategory.ETC, state.selectedCategory)
        assertEquals(MyRoomTutorialUiState(MyRoomTutorialPhase.Place, itemId = 3), state.tutorial)
    }

    @Test
    fun `칸을 놓으면 저장을 가리키고 저장에 성공하면 돌아가기 전에 마이룸 안내를 마친다`() = runTest(testDispatcher) {
        tutorialHelper.pending = true
        val viewModel = loadedViewModel()

        viewModel.onIntent(MyRoomIntent.ClickInteriorItem(3, x = 0.5f, y = 0.8f))
        viewModel.onIntent(MyRoomIntent.TutorialTargetClicked)
        assertEquals(MyRoomTutorialPhase.Save, viewModel.uiState.value.tutorial?.phase)

        viewModel.onIntent(MyRoomIntent.ClickSave)
        viewModel.onIntent(MyRoomIntent.TutorialTargetClicked)
        assertEquals(MyRoomTutorialPhase.Saving, viewModel.uiState.value.tutorial?.phase)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.tutorial)
        assertEquals(setOf(TutorialStep.ROOM), tutorialHelper.done)
        assertEquals(1, navigationHelper.backCount)
    }

    @Test
    fun `안내대로 저장하지 못하면 실패 안내 뒤 저장을 다시 가리킨다`() = runTest(testDispatcher) {
        tutorialHelper.pending = true
        repository.saveError = httpException(400)
        val viewModel = loadedViewModel()

        viewModel.onIntent(MyRoomIntent.ClickInteriorItem(3, x = 0.5f, y = 0.8f))
        viewModel.onIntent(MyRoomIntent.TutorialTargetClicked)
        viewModel.onIntent(MyRoomIntent.ClickSave)
        viewModel.onIntent(MyRoomIntent.TutorialTargetClicked)
        advanceUntilIdle()

        assertEquals(IconType.ERROR, messageHelper.snackBars.single().iconType)
        assertEquals(MyRoomTutorialPhase.Save, viewModel.uiState.value.tutorial?.phase)
        assertTrue(tutorialHelper.done.isEmpty())
    }

    @Test
    fun `놓을 아이템이 없으면 꾸미기를 소개하고 끝까지 넘기면 마친다`() = runTest(testDispatcher) {
        tutorialHelper.pending = true
        repository.room = RoomVO(
            categories = listOf(page(RoomItemCategory.BACKGROUND, owned(100, "햇살 방"))),
            placements = listOf(RoomPlacementVO(roomItemId = 100, category = RoomItemCategory.BACKGROUND, itemId = 100)),
        )
        val viewModel = loadedViewModel()
        assertEquals(MyRoomTutorialUiState(MyRoomTutorialPhase.Intro), viewModel.uiState.value.tutorial)

        viewModel.onIntent(MyRoomIntent.TutorialAdvance)
        assertEquals(1, viewModel.uiState.value.tutorial?.introIndex)
        viewModel.onIntent(MyRoomIntent.TutorialAdvance)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.tutorial)
        assertEquals(setOf(TutorialStep.ROOM), tutorialHelper.done)
    }

    @Test
    fun `건너뛰면 남은 튜토리얼을 모두 끝낸다`() = runTest(testDispatcher) {
        tutorialHelper.pending = true
        val viewModel = loadedViewModel()

        viewModel.onIntent(MyRoomIntent.TutorialSkip)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.tutorial)
        assertFalse(tutorialHelper.pending)
    }

    private fun TestScope.loadedViewModel(): MyRoomViewModel {
        val viewModel = createViewModel()
        advanceUntilIdle()
        return viewModel
    }

    private fun createViewModel(): MyRoomViewModel {
        fun <T : Any> deps(build: (RoomRepository, ResourceHelper, MessageHelper, NavigationHelper, TTIHelper) -> T) =
            build(repository, FakeResourceHelper, messageHelper, navigationHelper, FakeTTIHelper)
        return MyRoomViewModel(
            getRoom = deps(::GetRoomUseCase),
            getOwnedRoomItems = deps(::GetOwnedRoomItemsUseCase),
            saveRoom = deps(::SaveRoomUseCase),
            getCatAnimations = GetCatAnimationsUseCase(catRepository, FakeResourceHelper, messageHelper, navigationHelper, FakeTTIHelper),
            catMotionPicker = CatMotionPicker(Random(seed = 3)),
            navigationHelper = navigationHelper,
            messageHelper = messageHelper,
            tutorialHelper = tutorialHelper,
        )
    }

    private fun owned(id: Long, name: String) = OwnedRoomItemVO(roomItemId = id, itemId = id, name = name)

    private fun page(category: RoomItemCategory, vararg items: OwnedRoomItemVO, nextCursor: Long? = null, name: String = "") =
        OwnedRoomItemPageVO(category = category, categoryName = name, items = items.toList(), nextCursor = nextCursor)

    private fun MyRoomUIState.itemsOf(category: RoomItemCategory): List<InventoryItemUiModel> = tabOf(category)!!.items

    private fun placed(id: Long, zIndex: Int, x: Float, y: Float, scale: Float = 1f) =
        RoomPlacementVO(roomItemId = id, itemId = id, x = x, y = y, zIndex = zIndex, scale = scale)
}

/** 메모리로 동작하는 튜토리얼 진행 기록. */
private class FakeTutorialHelper : TutorialHelper {
    var pending = false
    val done = mutableSetOf<TutorialStep>()

    override suspend fun isPending(step: TutorialStep) = pending && step !in done

    override suspend fun complete(step: TutorialStep) {
        done += step
    }

    override suspend fun start() {
        pending = true
        done.clear()
    }

    override suspend fun skipAll() {
        pending = false
    }

    override suspend fun clear() {
        pending = false
        done.clear()
    }
}

/** 마이룸 API를 흉내 내는 저장소. 불러오기와 저장 실패를 따로 정할 수 있다. */
private class FakeRoomRepository : RoomRepository {
    var room = RoomVO()
    var ownedPage = OwnedRoomItemPageVO()
    var ownedError: Exception? = null
    var loadError: Exception? = null
    var saveError: Exception? = null
    var roomRequests = 0
    val ownedRequests = mutableListOf<Triple<RoomItemCategory, Long?, Int>>()
    val saved = mutableListOf<List<RoomPlacementVO>>()

    override suspend fun getRoom(size: Int): RoomVO {
        roomRequests++
        loadError?.let { throw it }
        return room
    }

    override suspend fun getOwnedItems(category: RoomItemCategory, cursor: Long?, size: Int): OwnedRoomItemPageVO {
        ownedRequests += Triple(category, cursor, size)
        ownedError?.let { throw it }
        return ownedPage
    }

    override suspend fun saveRoom(placements: List<RoomPlacementVO>) {
        saveError?.let { throw it }
        saved += placements
    }
}

/** 마이룸 꾸미기(PLAYFUL) 동작 묶음이 둘인 내 고양이 애니메이션. 시트 주소에 묶음 번호를 넣어 구분한다. */
private class FakeCatRepository : CatRepository {
    var error: Exception? = null

    override suspend fun prefetchAnimations(): CatAnimationSetVO? = null

    override suspend fun getAnimations(): CatAnimationSetVO {
        error?.let { throw it }
        return CatAnimationSetVO(
            weight = "NORMAL",
            animations = mapOf(
                CatState.PLAYFUL to CatAnimationVO(
                    state = CatState.PLAYFUL,
                    frame = SpriteFrameVO(width = 136, height = 136, framesPerRow = 4, durationMs = 100),
                    groups = listOf(
                        listOf(SpriteClipVO(url = "playful-0", frames = 8)),
                        listOf(SpriteClipVO(url = "playful-1", frames = 8)),
                    ),
                    lines = emptyList(),
                ),
            ),
        )
    }
}

private fun httpException(code: Int, errorCode: String? = null): HttpResponseException = HttpResponseException(
    status = HttpResponseStatus.create(code),
    rawCode = code,
    errorRequestUrl = "https://test/api/v1/rooms",
    msg = "Http Request Failed ($code)",
    cause = errorCode?.let(::Throwable),
)

private class RecordingMessageHelper : MessageHelper {
    data class SnackBarCall(
        val iconType: IconType,
        val messageText: String,
        val callToActionText: String?,
        val onClickCTA: (() -> Unit)?,
    )

    data class DialogCall(val descText: String, val cantIgnore: Boolean, val onClickButton: (() -> Unit)?)

    data class TwoButtonDialogCall(
        val descText: String,
        val leftButtonText: String,
        val onClickLeft: (() -> Unit)?,
        val rightButtonText: String,
        val onClickRight: (() -> Unit)?,
    )

    val snackBars = mutableListOf<SnackBarCall>()
    val twoButtonDialogs = mutableListOf<TwoButtonDialogCall>()
    val dialogs = mutableListOf<DialogCall>()

    override val effect: Flow<MessageEffect> = emptyFlow()
    override fun showToast(toastMsg: String) = Unit
    override fun showToast(messageRes: Int) = Unit
    override fun showSnackBar(
        iconType: IconType,
        messageText: String,
        callToActionText: String?,
        onClickCTA: (() -> Unit)?,
    ) {
        snackBars += SnackBarCall(iconType, messageText, callToActionText, onClickCTA)
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
        dialogs += DialogCall(descText, cantIgnore, onClickButton)
    }

    override fun showTwoButtonDialog(
        titleText: String?,
        descText: String,
        cantIgnore: Boolean,
        leftButtonText: String,
        onClickLeftButton: (() -> Unit)?,
        rightButtonText: String,
        onClickRightButton: (() -> Unit)?,
    ) {
        twoButtonDialogs += TwoButtonDialogCall(descText, leftButtonText, onClickLeftButton, rightButtonText, onClickRightButton)
    }
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
