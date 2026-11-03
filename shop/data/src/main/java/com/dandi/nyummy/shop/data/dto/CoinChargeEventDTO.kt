package com.dandi.nyummy.shop.data.dto

import com.dandi.nyummy.shop.entity.CoinChargeEventVO
import com.dandi.nyummy.shop.entity.CoinChargeGrantVO
import com.dandi.nyummy.shop.entity.CoinChargeNoticeVO
import com.dandi.nyummy.shop.entity.CoinChargePackageVO
import kotlinx.serialization.Serializable

/**
 * Remote Config `android_coin_charge_event` 값(JSON 문자열). A/B 그룹마다 다른 값이고, 작성법은
 * docs/remote-config-coin-charge-event.md를 본다.
 *
 * @property eventId 회차 구분값. 없으면 이벤트를 버린다.
 * @property group A/B 그룹 이름(A, B). 없으면 이벤트를 버린다(분석에서 그룹을 나눌 수 없어서).
 * @property grantCoins "결제하기"를 누르면 넣어 주는 코인 수. 0 이하면 이벤트를 버린다.
 * @property notice 홈 공지. 없거나 제목·버튼 글자가 비었으면 공지 없이 충전 화면만 있다.
 * @property packages 충전 화면의 묶음. 형식이 맞는 묶음이 하나도 없으면 이벤트를 버린다.
 */
@Serializable
data class CoinChargeEventDTO(
    val eventId: String? = null,
    val group: String? = null,
    val grantCoins: Int? = null,
    val notice: CoinChargeNoticeDTO? = null,
    val packages: List<CoinChargePackageDTO>? = null,
) {
    /** 화면에 쓸 수 있는 이벤트로 바꾼다. 필수 값이 빠졌으면 null(이벤트 없음)이다. */
    fun toVO(): CoinChargeEventVO? {
        val id = eventId?.trim().orEmpty()
        val groupName = group?.trim().orEmpty()
        val coins = grantCoins ?: 0
        val validPackages = packages.orEmpty().mapIndexedNotNull { index, pack -> pack.toVO(index) }
        if (id.isEmpty() || groupName.isEmpty() || coins <= 0 || validPackages.isEmpty()) return null
        return CoinChargeEventVO(
            eventId = id,
            group = groupName,
            grantCoins = coins,
            notice = notice?.toVO(),
            packages = validPackages,
        )
    }
}

/**
 * 홈 공지 한 장.
 *
 * @property badge 사진 위 배지 글자
 * @property imageUrl 10:9 사진 주소(https)
 * @property title 제목. 없으면 공지를 버린다.
 * @property body 설명
 * @property cta 버튼 글자. 없으면 공지를 버린다(이 공지는 충전 화면으로 가는 버튼이 목적이라서).
 */
@Serializable
data class CoinChargeNoticeDTO(
    val badge: String? = null,
    val imageUrl: String? = null,
    val title: String? = null,
    val body: String? = null,
    val cta: String? = null,
) {
    fun toVO(): CoinChargeNoticeVO? {
        val titleText = title?.trim().orEmpty()
        val ctaText = cta?.trim().orEmpty()
        if (titleText.isEmpty() || ctaText.isEmpty()) return null
        return CoinChargeNoticeVO(
            badge = badge?.trim().orEmpty(),
            imageUrl = imageUrl?.trim()?.takeIf { it.isNotEmpty() },
            title = titleText,
            body = body.orEmpty(),
            ctaText = ctaText,
        )
    }
}

/**
 * 충전 화면의 묶음 하나.
 *
 * @property id 묶음 구분값(coin_300 등). 없으면 묶음을 버린다.
 * @property coins 코인 수. 0 이하면 묶음을 버린다.
 * @property price 원 단위 가격. 0 이하면 묶음을 버린다.
 * @property imageId 묶음 그림 CDN 번호(1 동전 · 2 주머니 · 3 바구니). 없으면 순서대로 1, 2, 3이다.
 */
@Serializable
data class CoinChargePackageDTO(
    val id: String? = null,
    val coins: Int? = null,
    val price: Int? = null,
    val imageId: Long? = null,
) {
    fun toVO(index: Int): CoinChargePackageVO? {
        val packageId = id?.trim().orEmpty()
        val coinCount = coins ?: 0
        val won = price ?: 0
        if (packageId.isEmpty() || coinCount <= 0 || won <= 0) return null
        return CoinChargePackageVO(
            id = packageId,
            coins = coinCount,
            price = won,
            imageId = imageId?.takeIf { it > 0 } ?: (index + 1L).coerceAtMost(MAX_DEFAULT_IMAGE_ID),
        )
    }

    private companion object {
        /** CDN에 있는 묶음 그림 수. 묶음이 더 많으면 마지막 그림을 쓴다. */
        const val MAX_DEFAULT_IMAGE_ID = 3L
    }
}

/**
 * 이벤트 코인 지급 응답. 지급 API가 아직 없어 필드는 미확정이다(백엔드가 만들면 맞춘다).
 *
 * @property grantedCoins 받은 코인 수. 없으면 이벤트 값([CoinChargeEventDTO.grantCoins])을 쓴다.
 * @property balance 받은 뒤 내 코인
 */
@Serializable
data class CoinChargeGrantDTO(
    val grantedCoins: Int? = null,
    val balance: Int? = null,
) {
    fun toVO(fallbackCoins: Int): CoinChargeGrantVO = CoinChargeGrantVO(
        grantedCoins = grantedCoins ?: fallbackCoins,
        balance = balance,
    )
}
