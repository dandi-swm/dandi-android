package com.dandi.nyummy.shop.entity

/**
 * 상점 화면의 탭.
 *
 * @property linkSegment 앱 링크·라우트 인자에 쓰는 이름(`/shop/interior`의 `interior`). 추천 탭 링크는 `/shop`이고, `/shop/recommend`도 추천 탭으로 읽는다.
 */
enum class ShopTab(val linkSegment: String) {
    RECOMMEND("recommend"),
    INTERIOR("interior"),
    BACKGROUND("background"),
    ;

    companion object {
        /** 링크·라우트 인자 이름으로 찾는다. 모르는 이름이면 null. */
        fun fromLinkSegment(value: String?): ShopTab? = entries.firstOrNull { it.linkSegment == value }
    }
}
