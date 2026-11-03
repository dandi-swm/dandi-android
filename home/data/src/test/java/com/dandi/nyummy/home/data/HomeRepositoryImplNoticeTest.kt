package com.dandi.nyummy.home.data

import com.dandi.nyummy.common.data.preference.AppPreferenceProvider
import com.dandi.nyummy.common.domain.remoteconfig.RemoteConfigHelper
import com.dandi.nyummy.common.domain.remoteconfig.RemoteConfigKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Retrofit

class HomeRepositoryImplNoticeTest {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
    }

    private val remoteConfig = FakeRemoteConfigHelper()
    private val preferences = FakeAppPreferenceProvider()

    // 공지는 요약 API를 부르지 않는다. 데이터 소스는 만들기만 한다.
    private val apiService = Retrofit.Builder().baseUrl("http://localhost/").build().create(HomeApiService::class.java)

    private fun repository() = HomeRepositoryImpl(
        dataSource = HomeDataSource(apiService),
        ioScope = CoroutineScope(Dispatchers.Unconfined),
        remoteConfigHelper = remoteConfig,
        appPreferenceProvider = preferences,
        json = json,
    )

    @Test
    fun `원격 구성 JSON을 공지 목록으로 읽는다`() {
        remoteConfig.values[RemoteConfigKey.HomeNotice.key] = """{"notices":[{"id":"a","title":"새단장"},{"id":"b","title":"이벤트"}]}"""

        assertEquals(listOf("a", "b"), repository().getNotices().map { it.id })
    }

    @Test
    fun `값이 없거나 JSON이 깨졌으면 빈 목록이다`() {
        assertTrue(repository().getNotices().isEmpty())

        remoteConfig.values[RemoteConfigKey.HomeNotice.key] = """{"notices":[{"title":"닫히지 않은 JSON"""
        assertTrue(repository().getNotices().isEmpty())

        remoteConfig.values[RemoteConfigKey.HomeNotice.key] = """[1, 2, 3]"""
        assertTrue(repository().getNotices().isEmpty())
    }

    @Test
    fun `다시 보지 않기로 숨긴 공지를 기기에 쌓아 두고 읽는다`() = runBlocking {
        val repository = repository()
        assertTrue(repository.getHiddenNoticeKeys().isEmpty())

        repository.hideNotices(setOf("2026-10-renewal"))
        repository.hideNotices(setOf("2026-autumn-event"))

        assertEquals(setOf("2026-10-renewal", "2026-autumn-event"), preferences.hiddenIds)
        assertEquals(setOf("2026-10-renewal", "2026-autumn-event"), repository.getHiddenNoticeKeys())
    }

    @Test
    fun `이번 실행 다룸 표시는 저장소에만 남고 새로 만든 저장소는 모른다`() {
        val repository = repository()
        assertFalse(repository.isNoticeHandled())

        repository.markNoticeHandled()

        assertTrue(repository.isNoticeHandled())
        assertFalse(repository().isNoticeHandled())
    }

    private class FakeRemoteConfigHelper : RemoteConfigHelper {
        val values = mutableMapOf<String, Any>()

        override suspend fun sync() = Unit

        @Suppress("UNCHECKED_CAST")
        override fun <T> get(key: RemoteConfigKey<T>): T = (values[key.key] ?: key.defaultValue) as T
    }

    private class FakeAppPreferenceProvider : AppPreferenceProvider {
        var hiddenIds: Set<String> = emptySet()

        override suspend fun hasShownPermissionNotice(): Boolean = false
        override suspend fun markPermissionNoticeShown() = Unit
        override suspend fun isOnboardingIncomplete(): Boolean = false
        override suspend fun setOnboardingIncomplete(incomplete: Boolean) = Unit
        override suspend fun homeNoticeHiddenIds(): Set<String> = hiddenIds
        override suspend fun addHomeNoticeHiddenIds(ids: Set<String>) {
            hiddenIds = hiddenIds + ids
        }
    }
}
