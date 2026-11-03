package com.dandi.nyummy.room.presentation.tryon

import com.dandi.nyummy.cat.domain.CatMotionPicker
import com.dandi.nyummy.cat.domain.GetCatAnimationsUseCase
import com.dandi.nyummy.room.domain.GetRoomUseCase
import com.dandi.nyummy.room.entity.RoomItemCategory
import com.dandi.nyummy.room.entity.RoomPlacementVO
import com.dandi.nyummy.room.entity.RoomTryOnItemVO
import com.dandi.nyummy.room.entity.RoomVO
import com.dandi.nyummy.room.presentation.FakeCatRepository
import com.dandi.nyummy.room.presentation.FakeResourceHelper
import com.dandi.nyummy.room.presentation.FakeRoomRepository
import com.dandi.nyummy.room.presentation.FakeTTIHelper
import com.dandi.nyummy.room.presentation.MyRoomLoadState
import com.dandi.nyummy.room.presentation.RecordingMessageHelper
import com.dandi.nyummy.room.presentation.RecordingNavigationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import java.io.IOException
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class RoomTryOnViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val repository = FakeRoomRepository()
    private val messageHelper = RecordingMessageHelper()

    /** 장바구니 순서 그대로: 발바닥 방석 2개, 플로어 조명, 민트 체크 방(배경), 판매 끝난 단풍 러그. */
    private val cartItems = listOf(
        item("7:0", itemId = 70, name = "발바닥 방석", category = "BED", price = 150),
        item("7:1", itemId = 70, name = "발바닥 방석", category = "BED", price = 150),
        item("8:0", itemId = 80, name = "플로어 조명", category = "ETC", price = 320),
        item("9:0", itemId = 90, name = "민트 체크 방", category = "BACKGROUND", price = 1_000),
        item("5:0", itemId = 50, name = "단풍 러그", category = "RUG", price = 400, isPurchasable = false),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        // 마이룸: 줄무늬 원목 방(100)에 호박 쿠션(1)과 스크래처 타워(2). 놓아보기에는 배경만 가져온다.
        repository.room = RoomVO(
            placements = listOf(
                RoomPlacementVO(roomItemId = 100, category = RoomItemCategory.BACKGROUND, itemId = 100),
                RoomPlacementVO(roomItemId = 1, itemId = 1, x = 10f, y = 20f, zIndex = 1),
                RoomPlacementVO(roomItemId = 2, itemId = 2, x = 80f, y = 25f, zIndex = 2),
            ),
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `들어오면 마이룸 배경만 가져오고 가진 아이템은 보이지 않는다`() = runTest(testDispatcher) {
        val state = openedViewModel().uiState.value

        assertEquals(MyRoomLoadState.LOADED, state.loadState)
        assertEquals(100L, state.room.background?.roomItemId)
        assertEquals("https://cdn.nyummy.co.kr/items/100.png", state.room.background?.imageUrl)
        assertEquals(100L, state.originalBackground?.roomItemId)
        assertTrue("호박 쿠션과 스크래처 타워는 보이지 않는다", state.room.items.isEmpty())
        assertFalse(state.canUndo)
        // 놓인 아이템만 쓰므로 가진 아이템 목록은 한 개씩만 받는다.
        assertEquals(1, repository.roomRequests)
    }

    @Test
    fun `칸은 수량만큼 배경이 먼저 그다음 받은 순서이고 살 수 없는 아이템도 보인다`() = runTest(testDispatcher) {
        val cells = openedViewModel().uiState.value.cells

        assertEquals(listOf("민트 체크 방", "발바닥 방석", "발바닥 방석", "플로어 조명", "단풍 러그"), cells.map { it.name })
        assertEquals(listOf(true, false, false, false, false), cells.map { it.isBackground })
        assertEquals(listOf(true, true, true, true, false), cells.map { it.isPurchasable })
        assertEquals(listOf(1_000, 150, 150, 320, 400), cells.map { it.price })
        // 방 무대 id는 서버 id와 겹치지 않게 음수이고 칸마다 다르다.
        assertTrue(cells.all { it.id < 0 })
        assertEquals(5, cells.map { it.id }.toSet().size)
        assertEquals("https://cdn.nyummy.co.kr/items/70.png", cells[1].imageUrl)
        assertTrue(cells.none { it.isPlaced })
    }

    @Test
    fun `칸을 누르면 화면이 정한 바닥 점에 놓고 고르며 다시 누르면 고르기만 한다`() = runTest(testDispatcher) {
        val viewModel = openedViewModel()
        val cushion = viewModel.cell("발바닥 방석")

        viewModel.onIntent(RoomTryOnIntent.ClickInteriorCell(cushion.id, x = 0.5f, y = 0.75f))

        var state = viewModel.uiState.value
        val placed = state.room.items.last()
        assertEquals(cushion.id, placed.roomItemId)
        assertEquals(0.5f, placed.x)
        assertEquals(0.75f, placed.y)
        assertEquals(cushion.id, state.selectedItemId)
        assertTrue(state.cells.first { it.id == cushion.id }.isPlaced)
        // 같은 이름의 다른 칸은 놓이지 않았다.
        assertEquals(1, state.cells.count { it.name == "발바닥 방석" && it.isPlaced })
        assertTrue(state.canUndo)

        viewModel.onIntent(RoomTryOnIntent.SelectItem(null))
        viewModel.onIntent(RoomTryOnIntent.ClickInteriorCell(cushion.id, x = 0.2f, y = 0.9f))

        state = viewModel.uiState.value
        assertEquals(cushion.id, state.selectedItemId)
        assertEquals(0.5f, state.room.items.last().x)
        assertEquals(1, state.undoStack.size)
    }

    @Test
    fun `판매 끝난 아이템도 놓아 볼 수 있다`() = runTest(testDispatcher) {
        val viewModel = openedViewModel()
        val rug = viewModel.cell("단풍 러그")

        viewModel.onIntent(RoomTryOnIntent.ClickInteriorCell(rug.id, x = 0.5f, y = 0.8f))

        assertEquals(rug.id, viewModel.uiState.value.room.items.last().roomItemId)
    }

    @Test
    fun `고른 아이템을 빼면 방에서 사라지고 칸의 배치됨이 없어진다`() = runTest(testDispatcher) {
        val viewModel = openedViewModel()
        val lamp = viewModel.cell("플로어 조명")
        viewModel.onIntent(RoomTryOnIntent.ClickInteriorCell(lamp.id, x = 0.3f, y = 0.6f))

        viewModel.onIntent(RoomTryOnIntent.PutSelectedAway)

        val state = viewModel.uiState.value
        assertTrue(state.room.items.none { it.roomItemId == lamp.id })
        assertFalse(state.cells.first { it.id == lamp.id }.isPlaced)
        assertNull(state.selectedItemId)
    }

    @Test
    fun `놓고 옮긴 뒤 되돌리기를 두 번 누르면 처음 방으로 돌아간다`() = runTest(testDispatcher) {
        val viewModel = openedViewModel()
        val initial = viewModel.uiState.value.room
        val cushion = viewModel.cell("발바닥 방석")
        viewModel.onIntent(RoomTryOnIntent.ClickInteriorCell(cushion.id, x = 0.5f, y = 0.75f))
        viewModel.onIntent(RoomTryOnIntent.MoveItem(cushion.id, x = 0.7f, y = 0.8f, scale = 1.2f))
        assertEquals(0.7f, viewModel.uiState.value.room.items.last().x)

        viewModel.onIntent(RoomTryOnIntent.Undo)
        viewModel.onIntent(RoomTryOnIntent.Undo)

        val state = viewModel.uiState.value
        assertEquals(initial, state.room)
        assertFalse(state.canUndo)
        assertNull(state.selectedItemId)
    }

    @Test
    fun `배경 칸을 누르면 배경이 바뀌고 다시 누르면 원래 배경으로 돌아간다`() = runTest(testDispatcher) {
        val viewModel = openedViewModel()
        val mint = viewModel.cell("민트 체크 방")

        viewModel.onIntent(RoomTryOnIntent.ClickBackgroundCell(mint.id))

        var state = viewModel.uiState.value
        assertEquals(mint.id, state.room.background?.roomItemId)
        assertEquals("https://cdn.nyummy.co.kr/items/90.png", state.room.background?.imageUrl)
        assertTrue(state.cells.first { it.id == mint.id }.isPlaced)
        assertTrue(state.canUndo)

        viewModel.onIntent(RoomTryOnIntent.ClickBackgroundCell(mint.id))

        state = viewModel.uiState.value
        assertEquals(100L, state.room.background?.roomItemId)
        assertFalse(state.cells.first { it.id == mint.id }.isPlaced)
        assertEquals(2, state.undoStack.size)
    }

    @Test
    fun `같은 칸을 다시 받으면 놓아 둔 모습과 되돌리기가 그대로다`() = runTest(testDispatcher) {
        val viewModel = openedViewModel()
        val cushion = viewModel.cell("발바닥 방석")
        val mint = viewModel.cell("민트 체크 방")
        viewModel.onIntent(RoomTryOnIntent.ClickInteriorCell(cushion.id, x = 0.5f, y = 0.75f))
        viewModel.onIntent(RoomTryOnIntent.ClickBackgroundCell(mint.id))
        val before = viewModel.uiState.value

        // 놓아보기를 닫았다 다시 열면 같은 담은 아이템이 다시 들어온다.
        viewModel.onIntent(RoomTryOnIntent.ItemsChanged(cartItems))

        val state = viewModel.uiState.value
        assertEquals(before.room, state.room)
        assertEquals(before.undoStack, state.undoStack)
        assertEquals(before.cells, state.cells)
    }

    @Test
    fun `장바구니에서 빠진 칸의 아이템과 배경은 방에서 빼고 되돌리기를 비운다`() = runTest(testDispatcher) {
        val viewModel = openedViewModel()
        val lamp = viewModel.cell("플로어 조명")
        val cushion = viewModel.cell("발바닥 방석")
        val mint = viewModel.cell("민트 체크 방")
        viewModel.onIntent(RoomTryOnIntent.ClickInteriorCell(cushion.id, x = 0.5f, y = 0.75f))
        viewModel.onIntent(RoomTryOnIntent.ClickBackgroundCell(mint.id))
        viewModel.onIntent(RoomTryOnIntent.ClickInteriorCell(lamp.id, x = 0.3f, y = 0.6f))

        // 장바구니에서 조명과 민트 체크 방을 뺐다.
        viewModel.onIntent(RoomTryOnIntent.ItemsChanged(cartItems.filterNot { it.name == "플로어 조명" || it.name == "민트 체크 방" }))

        val state = viewModel.uiState.value
        assertEquals(listOf(cushion.id), state.room.items.map { it.roomItemId })
        assertEquals(100L, state.room.background?.roomItemId)
        assertTrue(state.cells.none { it.name == "플로어 조명" })
        assertNull("빠진 조명을 고르고 있었으니 고르기를 푼다", state.selectedItemId)
        assertFalse(state.canUndo)
        // 남은 칸의 id는 그대로라 놓아 둔 방석이 이어진다.
        assertEquals(cushion.id, viewModel.cell("발바닥 방석").id)
    }

    @Test
    fun `놓았다 뺀 아이템을 장바구니에서 지우면 되돌리기로 되살아나지 않는다`() = runTest(testDispatcher) {
        val viewModel = openedViewModel()
        val lamp = viewModel.cell("플로어 조명")
        val cushion = viewModel.cell("발바닥 방석")
        viewModel.onIntent(RoomTryOnIntent.ClickInteriorCell(cushion.id, x = 0.5f, y = 0.75f))
        viewModel.onIntent(RoomTryOnIntent.ClickInteriorCell(lamp.id, x = 0.3f, y = 0.6f))
        viewModel.onIntent(RoomTryOnIntent.PutSelectedAway)
        assertTrue(viewModel.uiState.value.room.items.none { it.roomItemId == lamp.id })

        // 방에는 조명이 없지만 되돌리기 기록에는 있다. 장바구니에서 조명을 뺀다.
        viewModel.onIntent(RoomTryOnIntent.ItemsChanged(cartItems.filterNot { it.name == "플로어 조명" }))
        repeat(3) { viewModel.onIntent(RoomTryOnIntent.Undo) }

        val state = viewModel.uiState.value
        assertTrue("빠진 칸의 조명이 되살아나면 안 된다", state.room.items.none { it.roomItemId == lamp.id })
        assertFalse(state.canUndo)
        // 방은 그대로다.
        assertEquals(listOf(cushion.id), state.room.items.map { it.roomItemId })
    }

    @Test
    fun `원래 배경으로 돌린 뒤 그 배경을 장바구니에서 지우면 되돌리기로 되살아나지 않는다`() = runTest(testDispatcher) {
        val viewModel = openedViewModel()
        val mint = viewModel.cell("민트 체크 방")
        viewModel.onIntent(RoomTryOnIntent.ClickBackgroundCell(mint.id))
        viewModel.onIntent(RoomTryOnIntent.ClickBackgroundCell(mint.id))
        assertEquals(100L, viewModel.uiState.value.room.background?.roomItemId)

        viewModel.onIntent(RoomTryOnIntent.ItemsChanged(cartItems.filterNot { it.name == "민트 체크 방" }))
        viewModel.onIntent(RoomTryOnIntent.Undo)

        val state = viewModel.uiState.value
        assertEquals("빠진 칸의 민트 체크 방이 되살아나면 안 된다", 100L, state.room.background?.roomItemId)
        assertFalse(state.canUndo)
    }

    @Test
    fun `방과 되돌리기 기록에 없던 칸이 빠지면 되돌리기는 그대로다`() = runTest(testDispatcher) {
        val viewModel = openedViewModel()
        val cushion = viewModel.cell("발바닥 방석")
        viewModel.onIntent(RoomTryOnIntent.ClickInteriorCell(cushion.id, x = 0.5f, y = 0.75f))

        // 한 번도 놓지 않은 단풍 러그를 장바구니에서 뺀다.
        viewModel.onIntent(RoomTryOnIntent.ItemsChanged(cartItems.filterNot { it.name == "단풍 러그" }))

        assertTrue(viewModel.uiState.value.canUndo)
    }

    @Test
    fun `수량이 줄면 마지막 칸만 없어진다`() = runTest(testDispatcher) {
        val viewModel = openedViewModel()
        val second = viewModel.uiState.value.cells.filter { it.name == "발바닥 방석" }[1]
        viewModel.onIntent(RoomTryOnIntent.ClickInteriorCell(second.id, x = 0.5f, y = 0.75f))

        viewModel.onIntent(RoomTryOnIntent.ItemsChanged(cartItems.filterNot { it.key == "7:1" }))

        val state = viewModel.uiState.value
        assertEquals(1, state.cells.count { it.name == "발바닥 방석" })
        assertTrue(state.room.items.none { it.roomItemId == second.id })
    }

    @Test
    fun `방을 불러오지 못하면 실패를 보이고 다시 불러오면 들어온 직후가 된다`() = runTest(testDispatcher) {
        repository.loadError = IOException("끊김")
        val viewModel = openedViewModel()
        assertEquals(MyRoomLoadState.FAILED, viewModel.uiState.value.loadState)

        // 불러오기 전에는 칸을 눌러도 놓지 않는다.
        viewModel.onIntent(RoomTryOnIntent.ClickInteriorCell(viewModel.cell("발바닥 방석").id, x = 0.5f, y = 0.75f))
        assertTrue(viewModel.uiState.value.room.items.isEmpty())

        repository.loadError = null
        viewModel.onIntent(RoomTryOnIntent.RetryLoad)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(MyRoomLoadState.LOADED, state.loadState)
        assertEquals(100L, state.room.background?.roomItemId)
        assertEquals(5, state.cells.size)
    }

    @Test
    fun `냐미 애니메이션을 받으면 방에 냐미를 앉힌다`() = runTest(testDispatcher) {
        assertEquals(1, openedViewModel().uiState.value.catMotion?.playId)
    }

    /** 놓아보기를 처음 연 상태: 방을 읽고 담은 아이템을 받았다. */
    private fun TestScope.openedViewModel(): RoomTryOnViewModel {
        val viewModel = RoomTryOnViewModel(
            getRoom = GetRoomUseCase(repository, FakeResourceHelper, messageHelper, RecordingNavigationHelper(), FakeTTIHelper),
            getCatAnimations = GetCatAnimationsUseCase(
                FakeCatRepository(),
                FakeResourceHelper,
                messageHelper,
                RecordingNavigationHelper(),
                FakeTTIHelper,
            ),
            catMotionPicker = CatMotionPicker(Random(seed = 3)),
        )
        viewModel.onIntent(RoomTryOnIntent.ItemsChanged(cartItems))
        advanceUntilIdle()
        return viewModel
    }

    private fun RoomTryOnViewModel.cell(name: String): RoomTryOnCellUiModel = uiState.value.cells.first { it.name == name }

    private fun item(
        key: String,
        itemId: Long,
        name: String,
        category: String,
        price: Int,
        isPurchasable: Boolean = true,
    ) = RoomTryOnItemVO(
        key = key,
        itemId = itemId,
        name = name,
        category = RoomItemCategory(category),
        price = price,
        isPurchasable = isPurchasable,
    )
}
