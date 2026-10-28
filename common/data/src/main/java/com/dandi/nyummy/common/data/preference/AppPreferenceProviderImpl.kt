package com.dandi.nyummy.common.data.preference

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.dandi.nyummy.common.data.BaseLocalDataSource

/**
 * DataStore Preferences 기반 [AppPreferenceProvider] 구현.
 *
 * TokenProviderImpl 과 달리 동기 메모리 캐시를 두지 않는다 — 그 캐시는 suspend 를
 * 쓸 수 없는 OkHttp Interceptor 전용 우회이며, 여기는 모든 접근이 suspend 다.
 */
class AppPreferenceProviderImpl(
    dataStore: DataStore<Preferences>,
) : BaseLocalDataSource(dataStore), AppPreferenceProvider {

    override suspend fun hasShownPermissionNotice(): Boolean =
        read(KEY_PERMISSION_NOTICE_SHOWN) ?: false

    override suspend fun markPermissionNoticeShown() {
        write(KEY_PERMISSION_NOTICE_SHOWN, true)
    }

    override suspend fun isOnboardingIncomplete(): Boolean =
        read(KEY_ONBOARDING_INCOMPLETE) ?: false

    override suspend fun setOnboardingIncomplete(incomplete: Boolean) {
        write(KEY_ONBOARDING_INCOMPLETE, incomplete)
    }

    override suspend fun homeNoticeHiddenDate(): String? = read(KEY_HOME_NOTICE_HIDDEN_DATE)

    override suspend fun setHomeNoticeHiddenDate(date: String) {
        write(KEY_HOME_NOTICE_HIDDEN_DATE, date)
    }

    companion object {
        private val KEY_PERMISSION_NOTICE_SHOWN = booleanPreferencesKey("permission_notice_shown")
        private val KEY_ONBOARDING_INCOMPLETE = booleanPreferencesKey("onboarding_incomplete")
        private val KEY_HOME_NOTICE_HIDDEN_DATE = stringPreferencesKey("home_notice_hidden_date")
    }
}
