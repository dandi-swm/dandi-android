package com.dandi.nyummy.shop.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.dandi.nyummy.common.data.BaseLocalDataSource

/**
 * 코인 충전 이벤트 받음 기록(기기). 받은 회차의 구분값을 모아 둔다. 계정이 아니라 기기 단위라 로그아웃해도 남는다.
 * 두 번째 지급은 서버가 막으므로, 이 기록은 공지를 다시 띄우지 않고 시트 대신 안내를 띄우는 데만 쓴다.
 */
class CoinChargeLocalDataSource(
    dataStore: DataStore<Preferences>,
) : BaseLocalDataSource(dataStore) {

    suspend fun claimedEventIds(): Set<String> = read(KEY_CLAIMED_EVENT_IDS).orEmpty()

    suspend fun addClaimedEventId(eventId: String) {
        // 읽기와 쓰기를 한 트랜잭션에서 해야 동시에 남긴 다른 회차가 덮여 사라지지 않는다.
        editAtomically { prefs -> prefs[KEY_CLAIMED_EVENT_IDS] = prefs[KEY_CLAIMED_EVENT_IDS].orEmpty() + eventId }
    }

    private companion object {
        val KEY_CLAIMED_EVENT_IDS = stringSetPreferencesKey("coin_charge_claimed_event_ids")
    }
}
