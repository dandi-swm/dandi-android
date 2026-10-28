package com.dandi.nyummy.home.domain

import com.dandi.nyummy.common.domain.helper.DeviceHelper
import com.dandi.nyummy.common.domain.helper.ImagePrefetchHelper
import com.dandi.nyummy.common.entity.time.DeviceTime
import com.dandi.nyummy.home.entity.HomeNoticeVO
import com.dandi.nyummy.home.entity.HomeSummaryVO
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.time.LocalDate

class HomeNoticeUseCaseTest {

    private val now = 1_793_200_000_000L
    private val today = DeviceTime.today(now)
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
    fun `오늘 하루 숨겼으면 고르지 않고 다음 날에는 다시 고른다`() = runBlocking {
        repository.noticeList = listOf(notice("a"))
        repository.hiddenDate = today

        assertTrue(getNotices()(now).isEmpty())

        repository.noticeHandled = false
        repository.hiddenDate = today.minusDays(1)
        assertEquals(listOf("a"), getNotices()(now).map { it.id })
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
    fun `숨긴 날을 읽지 못하면 숨기지 않은 것으로 본다`() = runBlocking {
        repository.noticeList = listOf(notice("a"))
        repository.failHiddenDate = true

        assertEquals(listOf("a"), getNotices()(now).map { it.id })
    }

    @Test
    fun `그냥 닫으면 이번 실행만 닫고 날짜는 남기지 않는다`() = runBlocking {
        close()(hideToday = false, nowMillis = now)

        assertTrue(repository.noticeHandled)
        assertNull(repository.hiddenDate)
    }

    @Test
    fun `오늘 하루 보지 않기로 닫으면 오늘 기기 날짜를 남긴다`() = runBlocking {
        close()(hideToday = true, nowMillis = now)

        assertTrue(repository.noticeHandled)
        assertEquals(today, repository.hiddenDate)
    }

    @Test
    fun `날짜를 쓰지 못해도 이번 실행은 닫힌다`() = runBlocking {
        repository.failHiddenDate = true

        close()(hideToday = true, nowMillis = now)

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
    fun `이번 실행에 이미 다뤘거나 오늘 숨겼거나 사진이 없으면 받지 않는다`() = runBlocking {
        repository.noticeList = listOf(notice("a").copy(imageUrl = "https://cdn/a.webp"))
        repository.noticeHandled = true
        prefetch()(now)

        repository.noticeHandled = false
        repository.hiddenDate = today
        prefetch()(now)

        repository.hiddenDate = null
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
        var hiddenDate: LocalDate? = null
        var noticeHandled = false
        var failHiddenDate = false

        override suspend fun getHomeSummary(): HomeSummaryVO = HomeSummaryVO.empty
        override fun prefetchHomeSummary() = Unit
        override fun getNotices(): List<HomeNoticeVO> = noticeList
        override suspend fun getNoticeHiddenDate(): LocalDate? {
            if (failHiddenDate) throw IOException("datastore")
            return hiddenDate
        }

        override suspend fun setNoticeHiddenDate(date: LocalDate) {
            if (failHiddenDate) throw IOException("datastore")
            hiddenDate = date
        }

        override fun isNoticeHandled(): Boolean = noticeHandled
        override fun markNoticeHandled() {
            noticeHandled = true
        }
    }
}
