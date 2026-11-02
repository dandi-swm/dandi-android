package com.dandi.nyummy.shop.domain

import com.dandi.nyummy.common.domain.navigation.NavRoute
import com.dandi.nyummy.common.domain.navigation.Page
import com.dandi.nyummy.shop.entity.ProductSort
import com.dandi.nyummy.shop.entity.ShopTab

/**
 * 상점 탭 화면으로 이동하기 위한 네비게이션 정보입니다.
 *
 * [PATH]는 앱 내 이동, 백스택, 화면 등록, 딥링크에서 공통으로 사용하는 상점 화면의 식별자입니다.
 * 링크 `/shop/interior?sort=POPULAR`는 별칭으로 이 화면이 되고, 탭과 정렬은 [ARG_TAB]·[ARG_SORT] 인자로 온다.
 */
object ShopPage : Page {

    const val PATH = "/shop"

    /** 처음 열 탭. 값은 [ShopTab.linkSegment]다. 링크 별칭 `/shop/{tab}`의 경로 인자 이름과 같아야 한다. */
    const val ARG_TAB = "tab"

    /** 처음 정렬. 값은 [ProductSort] 이름이다. */
    const val ARG_SORT = "sort"

    override fun toRoute(): NavRoute = NavRoute(PATH)

    /** [tab] 탭을 [sort] 정렬로 여는 상점. */
    fun of(tab: ShopTab, sort: ProductSort? = null): Page = object : Page {
        override fun toRoute(): NavRoute = NavRoute(
            path = PATH,
            args = buildMap {
                put(ARG_TAB, tab.linkSegment)
                if (sort != null) put(ARG_SORT, sort.name)
            },
        )
    }
}

/** 프로모션 상품 모음 화면. 추천 탭 배너와 링크 `/shop/promotions/{promotionId}`로 연다. */
data class ShopPromotionPage(val promotionId: Long) : Page {

    override fun toRoute(): NavRoute = NavRoute(PATH, mapOf(ARG_PROMOTION_ID to promotionId.toString()))

    companion object {
        const val ARG_PROMOTION_ID = "promotionId"
        const val PATH = "/shop/promotions/{$ARG_PROMOTION_ID}"
    }
}

/** 장바구니 화면. 구매 확인·완료도 이 화면 안에서 보인다. */
object ShopCartPage : Page {

    const val PATH = "/shop/cart"

    override fun toRoute(): NavRoute = NavRoute(PATH)
}

/** 코인 내역 화면. 상점 코인 칩과 홈 지갑 버튼이 연다. */
object ShopCoinHistoryPage : Page {

    const val PATH = "/shop/coins"

    override fun toRoute(): NavRoute = NavRoute(PATH)
}
