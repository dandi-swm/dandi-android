package com.dandi.nyummy.shop.presentation

import com.dandi.nyummy.common.presentation.mvi.MviIntent
import com.dandi.nyummy.shop.entity.ShopActionVO
import com.dandi.nyummy.shop.entity.ShopItemVO
import com.dandi.nyummy.shop.entity.ShopTab

/** 상점 화면에서 발생하는 사용자 입력. */
sealed interface ShopIntent : MviIntent {
    /** 화면에 들어왔다. [tab]·[sort]는 링크·라우트 인자(`tab`·`sort`) 그대로다. 한 번만 반영한다. */
    data class Enter(val tab: String?, val sort: String?) : ShopIntent

    /** 화면이 다시 보였다. 상단 바 값을 새로 읽는다. */
    data object ScreenResumed : ShopIntent

    data class SelectTab(val tab: ShopTab) : ShopIntent
    data object ClickCart : ShopIntent
    data object ClickCoins : ShopIntent

    /** 서버 SDUI 액션(배너, 칸 제목 오른쪽). */
    data class ClickAction(val action: ShopActionVO) : ShopIntent

    /** 상품 카드·줄을 눌렀다. 상세 시트를 연다. */
    data class ClickItem(val item: ShopItemVO) : ShopIntent
    data object DismissDetail : ShopIntent

    /** 1개 담기. [fromSheet]면 담은 뒤 시트를 닫는다. */
    data class AddToCart(val item: ShopItemVO, val fromSheet: Boolean = false) : ShopIntent
}
