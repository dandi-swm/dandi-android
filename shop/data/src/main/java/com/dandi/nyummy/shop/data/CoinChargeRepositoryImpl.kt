package com.dandi.nyummy.shop.data

import com.dandi.nyummy.common.domain.remoteconfig.RemoteConfigHelper
import com.dandi.nyummy.common.domain.remoteconfig.RemoteConfigKey
import com.dandi.nyummy.shop.data.dto.CoinChargeEventDTO
import com.dandi.nyummy.shop.domain.CoinChargeRepository
import com.dandi.nyummy.shop.entity.CoinChargeEventVO
import com.dandi.nyummy.shop.entity.CoinChargeGrantVO
import kotlinx.serialization.json.Json

/**
 * [CoinChargeRepository] 구현. 이벤트는 원격 구성 JSON을 읽어 바꾸고(형식이 틀리면 이벤트 없음),
 * 받음 기록은 기기에, 지급은 [CoinChargeGrantDataSource]에 맡긴다.
 */
class CoinChargeRepositoryImpl(
    private val remoteConfigHelper: RemoteConfigHelper,
    private val json: Json,
    private val localDataSource: CoinChargeLocalDataSource,
    private val grantDataSource: CoinChargeGrantDataSource,
) : CoinChargeRepository {

    override fun getEvent(): CoinChargeEventVO? {
        val raw = remoteConfigHelper.get(RemoteConfigKey.CoinChargeEvent)
        if (raw.isBlank()) return null
        return runCatching { json.decodeFromString(CoinChargeEventDTO.serializer(), raw) }
            .getOrNull()
            ?.toVO()
    }

    override suspend fun isClaimed(eventId: String): Boolean = eventId in localDataSource.claimedEventIds()

    override suspend fun markClaimed(eventId: String) = localDataSource.addClaimedEventId(eventId)

    override suspend fun claim(event: CoinChargeEventVO, packageId: String): CoinChargeGrantVO =
        grantDataSource.grant(eventId = event.eventId, group = event.group, packageId = packageId)
            .toVO(fallbackCoins = event.grantCoins)
}

