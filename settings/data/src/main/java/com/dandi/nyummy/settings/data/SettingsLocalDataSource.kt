package com.dandi.nyummy.settings.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import com.dandi.nyummy.common.data.BaseLocalDataSource

/** 이 기기에만 두는 설정(배경음). 앱 전역 환경설정 파일을 같이 쓴다. */
class SettingsLocalDataSource(
    dataStore: DataStore<Preferences>,
) : BaseLocalDataSource(dataStore) {

    suspend fun isBgmEnabled(): Boolean = read(KEY_BGM_ENABLED) ?: true

    suspend fun setBgmEnabled(enabled: Boolean) {
        write(KEY_BGM_ENABLED, enabled)
    }

    private companion object {
        val KEY_BGM_ENABLED = booleanPreferencesKey("bgm_enabled")
    }
}
