package com.dandi.nyummy.shop.domain

import com.dandi.nyummy.shop.entity.CoinChargeCurrency
import com.dandi.nyummy.shop.entity.CoinChargePackageVO
import com.dandi.nyummy.shop.entity.CoinChargePriceVO

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
     * 원화 기준 가격 → 나라 통화 가격. 실제 결제 전이라 2026-11-14 환율(1달러 ≈ 1,332원, 100엔 ≈ 833원)로 바꾼 뒤
     * 깔끔한 숫자로 맞춰 고정했다. 엔은 환율 그대로(1원 = 0.12엔)이고, 달러는 앱 가격처럼 .99로 끝나게 맞추되
     * 그룹끼리 비율(A:B = 1:1.5)은 그대로 둔다.
     */
    private val LOCAL_PRICES: Map<Int, Map<CoinChargeCurrency, Long>> = mapOf(
        1000 to localPrice(won = 1000, yen = 120, cents = 79),
        5000 to localPrice(won = 5000, yen = 600, cents = 399),
        10000 to localPrice(won = 10000, yen = 1200, cents = 799),
        1500 to localPrice(won = 1500, yen = 180, cents = 119),
        7500 to localPrice(won = 7500, yen = 900, cents = 599),
        15000 to localPrice(won = 15000, yen = 1800, cents = 1199),
    )

    private fun localPrice(won: Long, yen: Long, cents: Long) = mapOf(
        CoinChargeCurrency.KRW to won,
        CoinChargeCurrency.JPY to yen,
        CoinChargeCurrency.USD to cents,
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

    /**
     * 기기 지역(없으면 언어)으로 화면 가격 통화를 고른다. 한국은 원, 일본은 엔, 그 밖은 달러다.
     *
     * @param country 지역 코드(ISO 3166, 예: KR). 비어 있을 수 있다.
     * @param language 언어 코드(ISO 639, 예: ko)
     */
    fun currencyOf(country: String, language: String): CoinChargeCurrency = when (country.uppercase()) {
        "KR" -> CoinChargeCurrency.KRW
        "JP" -> CoinChargeCurrency.JPY
        "" -> when (language.lowercase()) {
            "ko" -> CoinChargeCurrency.KRW
            "ja" -> CoinChargeCurrency.JPY
            else -> CoinChargeCurrency.USD
        }
        else -> CoinChargeCurrency.USD
    }

    /** [pack]의 [currency] 가격. 표에 없는 원화 가격이면 원화 그대로 보인다. */
    fun priceOf(pack: CoinChargePackageVO, currency: CoinChargeCurrency): CoinChargePriceVO {
        val amount = LOCAL_PRICES[pack.price]?.get(currency)
            ?: return CoinChargePriceVO(CoinChargeCurrency.KRW, pack.price.toLong())
        return CoinChargePriceVO(currency, amount)
    }
}
