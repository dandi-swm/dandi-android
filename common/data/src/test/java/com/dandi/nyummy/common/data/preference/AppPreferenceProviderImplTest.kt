package com.dandi.nyummy.common.data.preference

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class AppPreferenceProviderImplTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val provider = AppPreferenceProviderImpl(
        PreferenceDataStoreFactory.create(scope = scope) { folder.newFile("app_prefs.preferences_pb") },
    )

    @After
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun `숨긴 공지가 없으면 빈 집합이다`() = runBlocking {
        assertTrue(provider.homeNoticeHiddenIds().isEmpty())
    }

    @Test
    fun `숨긴 공지를 더하면 이전에 숨긴 공지와 합쳐 남는다`() = runBlocking {
        provider.addHomeNoticeHiddenIds(setOf("a"))
        provider.addHomeNoticeHiddenIds(setOf("b", "c"))

        assertEquals(setOf("a", "b", "c"), provider.homeNoticeHiddenIds())
    }

    @Test
    fun `여러 공지를 동시에 숨겨도 하나도 덮여 사라지지 않는다`() = runBlocking {
        val ids = (1..50).map { "notice-$it" }

        ids.map { id -> async(Dispatchers.IO) { provider.addHomeNoticeHiddenIds(setOf(id)) } }.awaitAll()

        assertEquals(ids.toSet(), provider.homeNoticeHiddenIds())
    }
}
