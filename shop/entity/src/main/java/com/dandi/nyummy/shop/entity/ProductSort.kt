package com.dandi.nyummy.shop.entity

/** 상품 목록 정렬. 이름이 서버 `sort` 값과 같다. 기본은 신상품순이다. */
enum class ProductSort {
    NEW,
    POPULAR,
    PRICE_ASC,
    PRICE_DESC,
    ;

    companion object {
        val Default: ProductSort = NEW

        /** 서버·링크 값으로 찾는다. 모르는 값이면 [Default]. */
        fun fromOrDefault(value: String?): ProductSort = entries.firstOrNull { it.name == value } ?: Default
    }
}
