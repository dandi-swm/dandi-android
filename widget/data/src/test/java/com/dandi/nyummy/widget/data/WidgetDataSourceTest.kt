package com.dandi.nyummy.widget.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.dandi.nyummy.common.data.token.TokenProvider
import com.dandi.nyummy.common.entity.meal.Meal
import com.dandi.nyummy.common.entity.meal.MealTimeVO
import com.dandi.nyummy.common.entity.meal.MealTimesVO
import com.dandi.nyummy.widget.entity.WidgetHomeHintVO
import com.dandi.nyummy.widget.entity.WidgetSnapshotVO
import com.dandi.nyummy.widget.entity.WidgetStage
import com.dandi.nyummy.widget.entity.WidgetVariantPickVO
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class WidgetDataSourceTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val dataStore by lazy {
        PreferenceDataStoreFactory.create(scope = scope) { folder.newFile("home_widget.preferences_pb") }
    }
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
    }
    private val dataSource by lazy { WidgetDataSource(dataStore, json) }

    @After
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun `남긴 적이 없으면 빈 값이다`() = runBlocking {
        assertEquals(WidgetSnapshotVO.empty, dataSource.read())
    }

    @Test
    fun `남긴 값을 그대로 다시 읽는다`() = runBlocking {
        val snapshot = WidgetSnapshotVO(
            lastRecordedAtMillis = 1_762_222_200_000L,
            homeHint = WidgetHomeHintVO(fetchedAtMillis = 1_762_222_300_000L, streakDays = 12, todayRecordedCount = 2),
            mealTimes = MealTimesVO.default
                .with(Meal.BREAKFAST, MealTimeVO(hour = 9, isSkipped = true))
                .with(Meal.LUNCH, MealTimeVO(hour = 13)),
            reunionRecordedAtMillis = 1_762_222_200_000L,
            variantPicks = mapOf(
                WidgetStage.NIGHT to WidgetVariantPickVO(episodeKey = "N:2026-11-04", chosen = 2, previous = 1),
                WidgetStage.WAITING to WidgetVariantPickVO(episodeKey = "W:2026-11-04:LUNCH", chosen = 0),
            ),
        )

        dataSource.update { snapshot }

        assertEquals(snapshot, dataSource.read())
    }

    @Test
    fun `바꾸기는 지금 남은 값에서 시작한다`() = runBlocking {
        dataSource.update { it.copy(lastRecordedAtMillis = 100L) }
        dataSource.update { it.copy(reunionRecordedAtMillis = 200L) }

        val read = dataSource.read()
        assertEquals(100L, read.lastRecordedAtMillis)
        assertEquals(200L, read.reunionRecordedAtMillis)
    }

    @Test
    fun `동시에 바꿔도 서로 덮어쓰지 않는다`() = runBlocking {
        (1..20).map { i ->
            async(Dispatchers.IO) {
                dataSource.update { current ->
                    val picks = current.variantPicks + (WidgetStage.entries[i % WidgetStage.entries.size] to WidgetVariantPickVO("k$i", i))
                    current.copy(lastRecordedAtMillis = current.lastRecordedAtMillis + 1, variantPicks = picks)
                }
            }
        }.awaitAll()

        assertEquals(20L, dataSource.read().lastRecordedAtMillis)
    }

    @Test
    fun `깨진 값은 빈 값으로 읽고 다음 쓰기로 고쳐진다`() = runBlocking {
        dataStore.edit { it[stringPreferencesKey("widget_snapshot")] = "{not json" }

        assertEquals(WidgetSnapshotVO.empty, dataSource.read())

        dataSource.update { it.copy(lastRecordedAtMillis = 5L) }
        assertEquals(5L, dataSource.read().lastRecordedAtMillis)
    }

    @Test
    fun `모르는 필드는 버리고 빠진 필드는 기본값으로 읽는다`() = runBlocking {
        dataStore.edit {
            it[stringPreferencesKey("widget_snapshot")] = """{"lastRecordedAtMillis":7,"futureField":"x"}"""
        }

        val read = dataSource.read()
        assertEquals(7L, read.lastRecordedAtMillis)
        assertEquals(MealTimesVO.default, read.mealTimes)
        assertTrue(read.homeHint.isEmpty)
    }

    @Test
    fun `모르는 상태의 고른 안은 버린다`() = runBlocking {
        dataStore.edit {
            it[stringPreferencesKey("widget_snapshot")] =
                """{"variantPicks":[{"stage":"GONE","episodeKey":"x","chosen":1},{"stage":"NIGHT","episodeKey":"N:1","chosen":2}]}"""
        }

        assertEquals(
            mapOf(WidgetStage.NIGHT to WidgetVariantPickVO(episodeKey = "N:1", chosen = 2)),
            dataSource.read().variantPicks,
        )
    }

    @Test
    fun `지우면 빈 값이다`() = runBlocking {
        dataSource.update { it.copy(lastRecordedAtMillis = 100L) }

        dataSource.clear()

        assertEquals(WidgetSnapshotVO.empty, dataSource.read())
    }

    @Test
    fun `로그인 토큰이 하나라도 있으면 로그인 상태다`() = runBlocking {
        assertTrue(WidgetRepositoryImpl(dataSource, FakeTokenProvider(access = "a", refresh = null)).isLoggedIn())
        assertTrue(WidgetRepositoryImpl(dataSource, FakeTokenProvider(access = null, refresh = "r")).isLoggedIn())
        assertFalse(WidgetRepositoryImpl(dataSource, FakeTokenProvider(access = "", refresh = " ")).isLoggedIn())
        assertFalse(WidgetRepositoryImpl(dataSource, FakeTokenProvider(access = null, refresh = null)).isLoggedIn())
    }

    private class FakeTokenProvider(access: String?, refresh: String?) : TokenProvider {
        override val accessToken: String? = access
        override val refreshToken: String? = refresh
        override suspend fun update(access: String, refresh: String) = Unit
        override suspend fun clear() = Unit
    }
}
