package com.dandi.nyummy.room.domain

import com.dandi.nyummy.room.entity.OwnedRoomItemPageVO
import com.dandi.nyummy.room.entity.RoomItemCategory
import com.dandi.nyummy.room.entity.RoomPlacementVO
import com.dandi.nyummy.room.entity.RoomVO
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

class RoomUseCasesTest {

    private val repository = FakeRoomRepository()
    private val messageHelper = RecordingMessageHelper()
    private val navigationHelper = RecordingNavigationHelper()

    @Test
    fun `마이룸은 종류마다 서버 최대치까지 한 번에 읽는다`() = runBlocking {
        val room = RoomVO(placements = listOf(RoomPlacementVO(roomItemId = 1)))
        repository.room = room

        val result = getRoom()()

        assertEquals(listOf(GetRoomUseCase.MAX_PAGE_SIZE), repository.roomRequests)
        assertEquals(room, result.getOrNull())
    }

    @Test
    fun `마이룸 조회 중 서버 오류면 공통 안내를 한 번 띄우고 실패를 돌려준다`() = runBlocking {
        repository.error = httpException(500)

        val result = getRoom()()

        assertTrue(result.isFailure)
        assertEquals(1, messageHelper.dialogs.size)
    }

    @Test
    fun `마이룸 조회 중 네트워크 오류면 앱이 죽지 않고 실패를 돌려준다`() = runBlocking {
        repository.error = IOException("offline")

        assertTrue(getRoom()().isFailure)
    }

    @Test
    fun `가진 아이템 다음 묶음은 종류와 cursor를 넘기고 크기는 서버 최대치로 자른다`() = runBlocking {
        val page = OwnedRoomItemPageVO(category = RoomItemCategory("BED"), nextCursor = 30)
        repository.ownedPage = page

        val result = getOwnedItems()(RoomItemCategory("BED"), cursor = 12, size = 999)

        assertEquals(listOf(Triple(RoomItemCategory("BED"), 12L as Long?, GetRoomUseCase.MAX_PAGE_SIZE)), repository.ownedRequests)
        assertEquals(page, result.getOrNull())
    }

    @Test
    fun `꾸민 방을 저장하면 놓인 아이템 전체를 그대로 보낸다`() = runBlocking {
        val placements = listOf(
            RoomPlacementVO(roomItemId = 10, category = RoomItemCategory.BACKGROUND),
            RoomPlacementVO(roomItemId = 1, x = 0.5f, y = 0.8f, zIndex = 1, scale = 1.2f),
        )

        val result = saveRoom()(placements)

        assertTrue(result.isSuccess)
        assertEquals(listOf(placements), repository.saved)
    }

    @Test
    fun `가지고 있지 않은 아이템이 섞이면 공통 안내 없이 RoomItemGoneException 으로 실패한다`() = runBlocking {
        repository.error = httpException(404, "api.room.itemNotFound")

        val result = saveRoom()(emptyList())

        assertTrue(result.exceptionOrNull() is RoomItemGoneException)
        // 마이룸 code가 먼저 처리되어 404여도 공통 "준비 중" 안내가 뜨지 않는다.
        assertTrue(messageHelper.dialogs.isEmpty())
    }

    @Test
    fun `저장 중 서버 오류면 원래 예외로 실패하고 공통 안내를 띄운다`() = runBlocking {
        repository.error = httpException(500)

        val result = saveRoom()(emptyList())

        assertTrue(result.exceptionOrNull() !is RoomItemGoneException)
        assertEquals(1, messageHelper.dialogs.size)
    }

    @Test
    fun `마이룸 요청이 취소되면 실패로 바꾸지 않고 그대로 던진다`() = runBlocking {
        repository.error = CancellationException("left room")

        val calls: List<suspend () -> Result<*>> = listOf(
            { getRoom()() },
            { getOwnedItems()(RoomItemCategory.ETC, cursor = 1) },
            { saveRoom()(emptyList()) },
        )
        for (call in calls) {
            val thrown = runCatching { call() }.exceptionOrNull()
            assertTrue("취소가 전파되지 않았다: $thrown", thrown is CancellationException)
        }
    }

    private fun getRoom() = GetRoomUseCase(repository, FakeResourceHelper, messageHelper, navigationHelper, FakeTTIHelper)
    private fun getOwnedItems() = GetOwnedRoomItemsUseCase(repository, FakeResourceHelper, messageHelper, navigationHelper, FakeTTIHelper)
    private fun saveRoom() = SaveRoomUseCase(repository, FakeResourceHelper, messageHelper, navigationHelper, FakeTTIHelper)
}
