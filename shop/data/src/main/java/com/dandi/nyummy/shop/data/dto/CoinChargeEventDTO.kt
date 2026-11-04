package com.dandi.nyummy.shop.data.dto

import com.dandi.nyummy.shop.domain.CoinChargePackages
import com.dandi.nyummy.shop.entity.CoinChargeEventVO
import com.dandi.nyummy.shop.entity.CoinChargeNoticeVO
import kotlinx.serialization.Serializable

/**
 * Remote Config `android_coin_charge_event` 값(JSON 문자열). A/B 그룹마다 다른 값이고, 작성법은
 * docs/remote-config-coin-charge-event.md를 본다. 묶음 가격은 앱에 고정돼 있어 이 값에 넣지 않는다([CoinChargePackages]).
 *
 * @property eventId 회차 구분값. 없으면 이벤트를 버린다.
 * @property group 가격 그룹(A 또는 B). 그 밖의 값이면 이벤트를 버린다(서버가 모르는 그룹을 거부한다).
 * @property notice 홈 공지. 없거나 제목·버튼 글자가 비었으면 공지 없이 충전 화면만 있다.
 */
@Serializable
data class CoinChargeEventDTO(
    val eventId: String? = null,
    val group: String? = null,
    val notice: CoinChargeNoticeDTO? = null,
) {
    /** 화면에 쓸 수 있는 이벤트로 바꾼다. 회차 구분값이 없거나 그룹이 A·B가 아니면 null(이벤트 없음)이다. */
    fun toVO(): CoinChargeEventVO? {
        val id = eventId?.trim().orEmpty()
        val groupName = CoinChargePackages.groupOf(group)
        if (id.isEmpty() || groupName == null) return null
        return CoinChargeEventVO(
            eventId = id,
            group = groupName,
            notice = notice?.toVO(),
            packages = CoinChargePackages.of(groupName),
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
