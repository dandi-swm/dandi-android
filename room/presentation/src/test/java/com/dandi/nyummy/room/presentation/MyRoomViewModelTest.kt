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
import com.dandi.nyummy.common.domain.message.IconType
import com.dandi.nyummy.common.domain.message.MessageEffect
import com.dandi.nyummy.common.domain.navigation.NavRoute
import com.dandi.nyummy.common.domain.navigation.NavSignal
import com.dandi.nyummy.common.domain.navigation.Page
import com.dandi.nyummy.common.presentation.designsystem.component.NyummySpriteSource
import com.dandi.nyummy.room.domain.GetOwnedRoomItemsUseCase
import com.dandi.nyummy.room.domain.GetRoomUseCase
import com.dandi.nyummy.room.domain.RoomRepository
import com.dandi.nyummy.room.domain.SaveRoomUseCase
import com.dandi.nyummy.room.entity.OwnedRoomItemPageVO
import com.dandi.nyummy.room.entity.OwnedRoomItemVO
import com.dandi.nyummy.room.entity.RoomItemCategory
import com.dandi.nyummy.room.entity.RoomPlacementVO
import com.dandi.nyummy.room.entity.RoomVO
import com.dandi.nyummy.room.presentation.model.RoomGeometry
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

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository.room = RoomVO(
            categories = listOf(
                page(RoomItemCategory.BACKGROUND, owned(100, "햇살 방"), owned(101, "민트 방")),
                page(RoomItemCategory.BED, owned(1, "구름 방석"), nextCursor = 1),
                page(RoomItemCategory.ETC, owned(2, "털실 공"), owned(3, "털실 공")),
            ),
            placements = listOf(
                placed(1, zIndex = 2, x = 0.5f, y = 0.8f),
                RoomPlacementVO(roomItemId = 100, category = RoomItemCategory.BACKGROUND, imageUrl = "bg100"),
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
    fun `방을 불러오면 배경과 인테리어를 나누고 그리는 순서와 놓임 표시를 맞춘다`() = runTest(testDispatcher) {
        val state = loadedViewModel().uiState.value

        assertEquals(MyRoomLoadState.LOADED, state.loadState)
        assertEquals(100L, state.room.background?.roomItemId)
        assertEquals(listOf(2L, 1L), state.room.items.map { it.roomItemId })
        val outOfRange = state.room.items.first()
        assertEquals(1f, outOfRange.x)
        assertEquals(1f, outOfRange.y)
        assertEquals(RoomGeometry.MAX_SCALE, outOfRange.scale)
        // 같은 아이템을 두 개 가지면 칸도 두 개다.
        assertEquals(listOf(1L, 2L, 3L), state.interiorItems.map { it.roomItemId })
        assertEquals(listOf(true, true, false), state.interiorItems.map { it.isPlaced })
        assertEquals(listOf(true, false), state.backgrounds.map { it.isPlaced })
        assertFalse(state.hasChanges)
        assertFalse(state.canSave)
    }

    @Test
    fun `놓이지 않은 아이템 칸을 누르면 방 가운데에 놓고 고른다`() = runTest(testDispatcher) {
        val viewModel = loadedViewModel()

        viewModel.onIntent(MyRoomIntent.ClickInteriorItem(3))

        val state = viewModel.uiState.value
        val added = state.room.items.last()
        assertEquals(3L, added.roomItemId)
        assertEquals(RoomGeometry.NEW_ITEM_X, added.x)
        assertEquals(RoomGeometry.NEW_ITEM_Y, added.y)
        assertEquals(1f, added.scale)
        assertEquals(3L, state.selectedItemId)
        assertTrue(state.interiorItems.first { it.roomItemId == 3L }.isPlaced)
        assertTrue(state.canUndo)
        assertTrue(state.canSave)
    }

    @Test
    fun `놓인 아이템 칸을 누르면 방에서 고르기만 하고 되돌리기는 쌓지 않는다`() = runTest(testDispatcher) {
        val viewModel = loadedViewModel()

        viewModel.onIntent(MyRoomIntent.ClickInteriorItem(1))

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
        assertEquals(RoomGeometry.MAX_SCALE, moved.scale)

        viewModel.onIntent(MyRoomIntent.Undo)

        val state = viewModel.uiState.value
        assertEquals(0.5f, state.room.items.first { it.roomItemId == 1L }.x)
        assertFalse(state.canUndo)
        assertFalse(state.hasChanges)
    }

    @Test
    fun `제자리에 놓으면 고르기만 하고 바뀐 것으로 보지 않는다`() = runTest(testDispatcher) {
        val viewModel = loadedViewModel()

        viewModel.onIntent(MyRoomIntent.MoveItem(1, x = 0.5f, y = 0.8f, scale = 1f))

        assertEquals(1L, viewModel.uiState.value.selectedItemId)
        assertFalse(viewModel.uiState.value.canUndo)
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
        assertFalse(state.interiorItems.first { it.roomItemId == 2L }.isPlaced)
    }

    @Test
    fun `배경을 바꾸면 사용 중 표시가 옮겨 가고 같은 배경을 다시 누르면 그대로다`() = runTest(testDispatcher) {
        val viewModel = loadedViewModel()

        viewModel.onIntent(MyRoomIntent.ClickBackground(100))
        assertFalse(viewModel.uiState.value.canUndo)

        viewModel.onIntent(MyRoomIntent.ClickBackground(101))
        val state = viewModel.uiState.value
        assertEquals(101L, state.room.background?.roomItemId)
        assertEquals(listOf(false, true), state.backgrounds.map { it.isPlaced })
    }

    @Test
    fun `저장하면 배경을 맨 뒤로 두고 그리는 순서대로 zIndex 를 매겨 보낸 뒤 홈으로 돌아간다`() = runTest(testDispatcher) {
        val viewModel = loadedViewModel()
        viewModel.onIntent(MyRoomIntent.ClickInteriorItem(3))

        viewModel.onIntent(MyRoomIntent.ClickSave)
        advanceUntilIdle()

        val saved = repository.saved.single()
        assertEquals(listOf(100L, 2L, 1L, 3L), saved.map { it.roomItemId })
        assertEquals(listOf(0, 1, 2, 3), saved.map { it.zIndex })
        assertEquals(RoomItemCategory.BACKGROUND, saved.first().category)
        assertEquals(1, navigationHelper.backCount)
        assertEquals("방을 저장했어요", messageHelper.snackBars.single().messageText)
        assertFalse(viewModel.uiState.value.hasChanges)
    }

    @Test
    fun `가지고 있지 않은 아이템이 섞여 저장에 실패하면 안내하고 방을 다시 불러온다`() = runTest(testDispatcher) {
        val viewModel = loadedViewModel()
        viewModel.onIntent(MyRoomIntent.ClickInteriorItem(3))
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
        viewModel.onIntent(MyRoomIntent.ClickInteriorItem(3))
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
        viewModel.onIntent(MyRoomIntent.ClickInteriorItem(3))

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
        viewModel.onIntent(MyRoomIntent.ClickInteriorItem(3))
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
    fun `보관함 끝에 닿으면 남은 종류의 다음 묶음을 같은 종류 뒤에 붙인다`() = runTest(testDispatcher) {
        val viewModel = loadedViewModel()
        repository.ownedPage = page(RoomItemCategory.BED, owned(4, "바구니 침대"))

        viewModel.onIntent(MyRoomIntent.ReachInventoryEnd)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(listOf(Triple(RoomItemCategory.BED, 1L, GetRoomUseCase.PAGE_SIZE)), repository.ownedRequests)
        assertEquals(listOf(1L, 4L, 2L, 3L), state.interiorItems.map { it.roomItemId })
        assertTrue(state.nextCursors.isEmpty())
        assertFalse(state.isLoadingMore)

        viewModel.onIntent(MyRoomIntent.ReachInventoryEnd)
        advanceUntilIdle()
        assertEquals(1, repository.ownedRequests.size)
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
        )
    }

    private fun owned(id: Long, name: String) = OwnedRoomItemVO(roomItemId = id, name = name, imageUrl = "img$id")

    private fun page(category: RoomItemCategory, vararg items: OwnedRoomItemVO, nextCursor: Long? = null) =
        OwnedRoomItemPageVO(category = category, items = items.toList(), nextCursor = nextCursor)

    private fun placed(id: Long, zIndex: Int, x: Float, y: Float, scale: Float = 1f) =
        RoomPlacementVO(roomItemId = id, imageUrl = "img$id", x = x, y = y, zIndex = zIndex, scale = scale)
}

/** 마이룸 API를 흉내 내는 저장소. 불러오기와 저장 실패를 따로 정할 수 있다. */
private class FakeRoomRepository : RoomRepository {
    var room = RoomVO()
    var ownedPage = OwnedRoomItemPageVO()
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
