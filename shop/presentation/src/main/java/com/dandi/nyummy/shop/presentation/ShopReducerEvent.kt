package com.dandi.nyummy.shop.presentation

import com.dandi.nyummy.common.presentation.mvi.ReducerEvent
import com.dandi.nyummy.shop.entity.ProductSort
import com.dandi.nyummy.shop.entity.ShopItemVO
import com.dandi.nyummy.shop.entity.ShopSummaryVO
import com.dandi.nyummy.shop.entity.ShopTab

/** 상점 화면 상태를 변이시키는 내부 이벤트. */
sealed interface ShopReducerEvent : ReducerEvent {
    data class SummaryChanged(val summary: ShopSummaryVO) : ShopReducerEvent
    data class TabSelected(val tab: ShopTab) : ShopReducerEvent
    data class SortRequested(val tab: ShopTab, val sort: ProductSort) : ShopReducerEvent
    data class DetailOpened(val item: ShopItemVO) : ShopReducerEvent
    data object DetailClosed : ShopReducerEvent
    data class AddStarted(val productId: Long) : ShopReducerEvent

    /** 담기 요청이 끝났다. [closeDetail]이면 상세 시트를 닫는다. */
    data class AddFinished(val productId: Long, val closeDetail: Boolean) : ShopReducerEvent
}
