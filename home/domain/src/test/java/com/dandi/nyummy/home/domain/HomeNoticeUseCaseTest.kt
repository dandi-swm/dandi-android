package com.dandi.nyummy.home.domain

import com.dandi.nyummy.common.domain.helper.DeviceHelper
import com.dandi.nyummy.common.domain.helper.ImagePrefetchHelper
import com.dandi.nyummy.home.entity.HomeNoticeVO
import com.dandi.nyummy.home.entity.HomeSummaryVO
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class HomeNoticeUseCaseTest {

    private val now = 1_793_200_000_000L
    private val repository = FakeHomeRepository()
    private val device = FakeDeviceHelper(appVersionCode = 210)

    private fun getNotices() = GetHomeNoticesUseCase(repository, device)
    private fun close() = CloseHomeNoticeUseCase(repository)
    private val images = RecordingImagePrefetchHelper()
    private fun prefetch() = PrefetchHomeNoticesUseCase(repository, device, images)

    private fun notice(
        id: String,
        start: Long? = null,
        end: Long? = null,
        minVersionCode: Long? = null,
    ) = HomeNoticeVO(id = id, title = id, startAtMillis = start, endAtMillis = end, minVersionCode = minVersionCode)

    @Test
    fun `기간 앞, 안, 뒤 중 기간 안의 공지만 JSON 순서대로 고른다`() = runBlocking {
        repository.noticeList = listOf(
            notice("before", start = now + 1),
            notice("inside", start = now - 1_000, end = now + 1_000),
            notice("after", end = now - 1),
            notice("edges", start = now, end = now),
        )

        assertEquals(listOf("inside", "edges"), getNotices()(now).map { it.id })
    }

    @Test
    fun `기간과 최소 버전을 적지 않은 공지는 조건 없이 고른다`() = runBlocking {
        repository.noticeList = listOf(notice("free"))

        assertEquals(listOf("free"), getNotices()(now).map { it.id })
    }

    @Test
    fun `앱 versionCode가 최소 버전보다 낮으면 고르지 않는다`() = runBlocking {
        repository.noticeList = listOf(notice("same", minVersionCode = 210), notice("higher", minVersionCode = 211))

        assertEquals(listOf("same"), getNotices()(now).map { it.id })
    }

    @Test
    fun `다시 보지 않기로 숨긴 공지는 날이 바뀌어도 고르지 않고 새 공지는 고른다`() = runBlocking {
        repository.noticeList = listOf(notice("a"), notice("b"))
        repository.hiddenKeys = mutableSetOf("a")

        assertEquals(listOf("b"), getNotices()(now).map { it.id })

        repository.noticeHandled = false
        val nextWeek = now + 7L * 24 * 60 * 60 * 1000
        assertEquals(listOf("b"), getNotices()(nextWeek).map { it.id })
    }

    @Test
    fun `구분값이 빈 공지는 제목으로 알아본다`() = runBlocking {
        repository.noticeList = listOf(HomeNoticeVO(id = "", title = "가을 이벤트"), notice("b"))
        repository.hiddenKeys = mutableSetOf("가을 이벤트")

        assertEquals(listOf("b"), getNotices()(now).map { it.id })
    }

    @Test
    fun `한 실행에 한 번만 고른다`() = runBlocking {
        repository.noticeList = listOf(notice("a"))

        assertEquals(listOf("a"), getNotices()(now).map { it.id })
        assertTrue(repository.noticeHandled)
        assertTrue(getNotices()(now).isEmpty())
    }

    @Test
    fun `보일 공지가 없어도 이번 실행은 다룬 것으로 표시한다`() = runBlocking {
        assertTrue(getNotices()(now).isEmpty())
        assertTrue(repository.noticeHandled)
    }

    @Test
    fun `숨긴 공지를 읽지 못하면 아무것도 숨기지 않은 것으로 본다`() = runBlocking {
        repository.noticeList = listOf(notice("a"))
        repository.failHiddenKeys = true

        assertEquals(listOf("a"), getNotices()(now).map { it.id })
    }

    @Test
    fun `그냥 닫으면 이번 실행만 닫고 숨긴 공지는 남기지 않는다`() = runBlocking {
        close()()

        assertTrue(repository.noticeHandled)
        assertTrue(repository.hiddenKeys.isEmpty())
    }

    @Test
    fun `다시 보지 않기로 닫으면 그 공지들을 이미 숨긴 공지에 더해 남긴다`() = runBlocking {
        repository.hiddenKeys = mutableSetOf("old")

        close()(hideKeys = setOf("a", "b"))

        assertTrue(repository.noticeHandled)
        assertEquals(setOf("old", "a", "b"), repository.hiddenKeys)
    }

    @Test
    fun `숨긴 공지를 쓰지 못해도 이번 실행은 닫힌다`() = runBlocking {
        repository.failHiddenKeys = true

        close()(hideKeys = setOf("a"))

        assertTrue(repository.noticeHandled)
    }

    @Test
    fun `미리 받기는 보일 공지의 사진만 한 번씩 받고 이번 실행 다룸은 표시하지 않는다`() = runBlocking {
        repository.noticeList = listOf(
            notice("a").copy(imageUrl = "https://cdn/a.webp"),
            notice("b").copy(imageUrl = "https://cdn/a.webp"),
            notice("c"),
            notice("expired", end = now - 1).copy(imageUrl = "https://cdn/x.webp"),
            notice("new-app", minVersionCode = 211).copy(imageUrl = "https://cdn/y.webp"),
        )

        prefetch()(now)

        assertEquals(listOf(listOf("https://cdn/a.webp")), images.calls)
        assertFalse(repository.noticeHandled)
    }

    @Test
    fun `이번 실행에 이미 다뤘거나 다시 보지 않기로 숨겼거나 사진이 없으면 받지 않는다`() = runBlocking {
        repository.noticeList = listOf(notice("a").copy(imageUrl = "https://cdn/a.webp"))
        repository.noticeHandled = true
        prefetch()(now)

        repository.noticeHandled = false
        repository.hiddenKeys = mutableSetOf("a")
        prefetch()(now)

        repository.hiddenKeys = mutableSetOf()
        repository.noticeList = listOf(notice("no-image"))
        prefetch()(now)

        assertTrue(images.calls.isEmpty())
    }

    private class RecordingImagePrefetchHelper : ImagePrefetchHelper {
        val calls = mutableListOf<List<String>>()

        override suspend fun prefetchSpriteSheets(urls: List<String>) = Unit
        override suspend fun prefetchImages(urls: List<String>) {
            calls += urls
        }
    }

    private class FakeDeviceHelper(override val appVersionCode: Long) : DeviceHelper {
        override val appVersionName: String = "1.0"
    }

    private class FakeHomeRepository : HomeRepository {
        var noticeList: List<HomeNoticeVO> = emptyList()
        var hiddenKeys: MutableSet<String> = mutableSetOf()
        var noticeHandled = false
        var failHiddenKeys = false

        override suspend fun getHomeSummary(): HomeSummaryVO = HomeSummaryVO.empty
        override fun prefetchHomeSummary() = Unit
        override fun getNotices(): List<HomeNoticeVO> = noticeList
        override suspend fun getHiddenNoticeKeys(): Set<String> {
            if (failHiddenKeys) throw IOException("datastore")
            return hiddenKeys.toSet()
        }

        override suspend fun hideNotices(keys: Set<String>) {
            if (failHiddenKeys) throw IOException("datastore")
            hiddenKeys += keys
        }

        override fun isNoticeHandled(): Boolean = noticeHandled
        override fun markNoticeHandled() {
            noticeHandled = true
        }
    }
}
