package com.dandi.nyummy.shop.data.dto

import com.dandi.nyummy.shop.entity.CoinChargeGrantVO
import kotlinx.serialization.Serializable

/**
 * 선물 코인 받기 요청(`POST /api/v1/coins/gifts`). 가격 그룹은 헤더 `X-Coin-Package-Variant`로 따로 보낸다.
 *
 * @property coinPackageId 화면의 묶음 번호(1~3). 1~3 밖이면 `api.coin.coinPackageNotFound`(404)다.
 */
@Serializable
data class CoinGiftRequestDTO(
    val coinPackageId: Long,
)

/**
 * 선물 코인 받기 응답.
 *
 * @property coinQuantity 넣어 준 코인 수(묶음·그룹과 상관없이 3000)
 * @property balance 받은 뒤 내 코인
 */
@Serializable
data class CoinGiftDTO(
    val coinQuantity: Int? = null,
    val balance: Int? = null,
) {
    fun toVO(): CoinChargeGrantVO = CoinChargeGrantVO(
        grantedCoins = coinQuantity ?: 0,
        balance = balance,
    )
}
