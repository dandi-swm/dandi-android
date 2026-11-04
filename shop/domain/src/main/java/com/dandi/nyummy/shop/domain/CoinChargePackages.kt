package com.dandi.nyummy.shop.domain

import com.dandi.nyummy.shop.entity.CoinChargePackageVO

/**
 * 코인 충전 이벤트의 묶음표. 서버(선물 코인 받기 API)는 가격 그룹(헤더)과 묶음 번호(1~3)만 받으므로 가격은 앱에 고정한다.
 * 서버 기록(그룹·묶음 번호)과 화면 가격이 어긋나지 않게 이 표만 쓴다.
 */
object CoinChargePackages {
    const val GROUP_A = "A"
    const val GROUP_B = "B"

    private val COINS = listOf(300, 1500, 3000)
    private val PRICES = mapOf(
        GROUP_A to listOf(1000, 5000, 10000),
        GROUP_B to listOf(1500, 7500, 15000),
    )

    /**
     * 원격 구성의 그룹 이름을 서버가 받는 값(A, B)으로 맞춘다. 그 밖의 값은 null이다 — 서버가 모르는 그룹을 거부하고,
     * A 그룹 데이터에 섞이면 나중에 가려낼 수 없어서 이벤트가 없는 것으로 본다.
     */
    fun groupOf(raw: String?): String? = raw?.trim()?.uppercase()?.takeIf { it in PRICES }

    /** [group]의 묶음 3개(묶음 번호 1~3, 그림도 1~3). 모르는 그룹이면 빈 목록이다. */
    fun of(group: String): List<CoinChargePackageVO> = PRICES[group].orEmpty().mapIndexed { index, price ->
        CoinChargePackageVO(coinPackageId = index + 1L, coins = COINS[index], price = price, imageId = index + 1L)
    }
}
