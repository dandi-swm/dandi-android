package com.dandi.nyummy.shop.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.dandi.nyummy.common.data.BaseLocalDataSource
import com.dandi.nyummy.shop.data.dto.CoinChargeGrantDTO
import kotlinx.coroutines.delay

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

/**
 * 이벤트 코인 지급(서버). 지급 API는 백엔드가 만들 예정이라 지금은 [PendingCoinChargeGrantDataSource]가 대신한다.
 * API가 생기면 ApiService를 붙인 구현으로 바꾸고, 같은 사용자의 두 번째 요청은 서버가
 * `ShopErrorType.COIN_CHARGE_ALREADY_CLAIMED` code로 막는다.
 */
interface CoinChargeGrantDataSource {
    /** [eventId] 회차 코인을 넣는다. [group]과 [packageId]는 서버 기록용이다. */
    suspend fun grant(eventId: String, group: String, packageId: String): CoinChargeGrantDTO
}

/**
 * 지급 API가 생기기 전 임시 구현. 개발 빌드는 받은 것처럼 응답해 화면 흐름을 끝까지 확인할 수 있게 하고(실제 코인은 늘지 않는다),
 * release 빌드는 실패해서 사용자에게 받지 않은 코인을 받았다고 보이지 않게 한다.
 */
class PendingCoinChargeGrantDataSource(
    private val isReleaseBuild: Boolean,
) : CoinChargeGrantDataSource {

    override suspend fun grant(eventId: String, group: String, packageId: String): CoinChargeGrantDTO {
        // TODO(백엔드): 이벤트 코인 지급 API가 생기면 ApiService 구현으로 바꾼다.
        check(!isReleaseBuild) { "코인 충전 이벤트 지급 API가 아직 없다" }
        delay(FAKE_LATENCY_MILLIS)
        return CoinChargeGrantDTO()
    }

    private companion object {
        /** 버튼이 잠깐 막히는 모습을 볼 수 있게 서버 응답처럼 기다린다. */
        const val FAKE_LATENCY_MILLIS = 500L
    }
}
