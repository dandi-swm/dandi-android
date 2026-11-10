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

    /** 바깥에서 상점에 다시 들어왔다. 인테리어·방 배경 탭의 칩·정렬을 처음으로 되돌리게 한다. */
    data object ConditionResetRequested : ShopReducerEvent
    data class DetailOpened(val item: ShopItemVO) : ShopReducerEvent
    data object DetailClosed : ShopReducerEvent
    data class AddStarted(val productId: Long) : ShopReducerEvent

    /** 담기 요청이 끝났다. [closeDetail]이면 상세 시트를 닫는다. */
    data class AddFinished(val productId: Long, val closeDetail: Boolean) : ShopReducerEvent

    /** 코인 충전 이벤트를 다시 읽었다. null이면 이벤트가 없어 "코인" 탭을 숨긴다. */
    data class CoinChargeTabChanged(val tab: ShopCoinChargeTab?) : ShopReducerEvent
}
