package com.dandi.nyummy.common.data.token

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.dandi.nyummy.common.data.BaseLocalDataSource
import kotlinx.coroutines.runBlocking

/**
 * DataStore 영속 + 메모리 캐시 기반 [TokenProvider] 구현.
 *
 * 동기 getter 는 OkHttp 워커 스레드에서 호출되는 전제 — 디스크 로드는 최초 1회만
 * [runBlocking] 으로 수행하고 이후는 캐시에서 읽는다.
 */
class TokenProviderImpl(
    dataStore: DataStore<Preferences>,
) : BaseLocalDataSource(dataStore), TokenProvider {

    @Volatile
    private var cache: CachedTokens? = null

    override val accessToken: String?
        get() = loadedCache().access

    override val refreshToken: String?
        get() = loadedCache().refresh

    override suspend fun update(access: String, refresh: String) {
        editAtomically { prefs ->
            prefs[KEY_ACCESS_TOKEN] = access
            prefs[KEY_REFRESH_TOKEN] = refresh
        }
        cache = CachedTokens(access = access, refresh = refresh)
    }

    override suspend fun clear() {
        editAtomically { prefs ->
            prefs.remove(KEY_ACCESS_TOKEN)
            prefs.remove(KEY_REFRESH_TOKEN)
        }
        cache = CachedTokens(access = null, refresh = null)
    }

    private fun loadedCache(): CachedTokens =
        cache ?: runBlocking {
            CachedTokens(
                access = read(KEY_ACCESS_TOKEN),
                refresh = read(KEY_REFRESH_TOKEN),
            )
        }.also { cache = it }

    private data class CachedTokens(
        val access: String?,
        val refresh: String?,
    )

    companion object {
        private val KEY_ACCESS_TOKEN = stringPreferencesKey("access_token")
        private val KEY_REFRESH_TOKEN = stringPreferencesKey("refresh_token")
    }
}
