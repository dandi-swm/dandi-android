package com.dandi.nyummy.common.data.preference

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.dandi.nyummy.common.data.BaseLocalDataSource
import com.dandi.nyummy.common.domain.helper.CompanionMemoryHelper
import kotlinx.coroutines.flow.Flow

/** 앱 전역 환경설정 DataStore("app_prefs")에 마지막 방문(epoch millis), 마지막 냐미 체형, 고양이 이름을 남긴다. */
class CompanionMemoryHelperImpl(
    dataStore: DataStore<Preferences>,
) : BaseLocalDataSource(dataStore), CompanionMemoryHelper {

    override suspend fun lastSeenAtMillis(): Long? = read(KEY_LAST_SEEN)

    override suspend fun setLastSeenAtMillis(millis: Long) {
        write(KEY_LAST_SEEN, millis)
    }

    override suspend fun lastCatWeight(): String? = read(KEY_LAST_CAT_WEIGHT)

    override suspend fun setLastCatWeight(weight: String) {
        write(KEY_LAST_CAT_WEIGHT, weight)
    }

    override fun catName(): Flow<String?> = observe(KEY_CAT_NAME)

    override suspend fun setCatName(name: String) {
        write(KEY_CAT_NAME, name)
    }

    override suspend fun clear() {
        remove(KEY_LAST_SEEN)
        remove(KEY_LAST_CAT_WEIGHT)
        remove(KEY_CAT_NAME)
    }

    private companion object {
        val KEY_LAST_SEEN = longPreferencesKey("companion_last_seen_at")
        val KEY_LAST_CAT_WEIGHT = stringPreferencesKey("companion_last_cat_weight")
        val KEY_CAT_NAME = stringPreferencesKey("companion_cat_name")
    }
}
