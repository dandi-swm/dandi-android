package com.dandi.nyummy.shop.presentation

import com.dandi.nyummy.common.presentation.mvi.UiState
import com.dandi.nyummy.shop.entity.ProductSort
import com.dandi.nyummy.shop.entity.ShopItemVO
import com.dandi.nyummy.shop.entity.ShopTab
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

/**
 * 상점 화면 상태. 탭 안 목록은 탭마다 따로 들고, 여기에는 상단 바·선택 탭·상세 시트·담기 중인 상품만 둔다.
 *
 * @property balance 코인 잔액(상단 바)
 * @property cartItemCount 장바구니 상품 종류 수. 0이면 배지를 숨긴다
 * @property sortRequest 링크로 받은 정렬. 인테리어·방 배경 탭이 받아 정렬을 바꾼다
 * @property conditionResetId 인테리어·방 배경 탭의 칩·정렬을 처음으로 되돌리라는 요청 번호. 바깥에서 상점에 다시 들어올 때마다 오른다
 * @property detailItem 상세 시트에 보이는 상품. null이면 시트가 닫혀 있다
 * @property addingProductIds 담기 요청 중인 상품. 응답 전 같은 상품을 다시 담지 않는다
 * @property coinChargeTab 탭 줄 끝의 "코인" 탭(코인 충전 이벤트). 이벤트(A/B 실험 대상)가 없으면 null이라 탭이 없다
 */
data class ShopUIState(
    val balance: Int = 0,
    val cartItemCount: Int = 0,
    val selectedTab: ShopTab = ShopTab.RECOMMEND,
    val sortRequest: ShopSortRequest? = null,
    val conditionResetId: Int = 0,
    val detailItem: ShopItemVO? = null,
    val addingProductIds: ImmutableSet<Long> = persistentSetOf(),
    val coinChargeTab: ShopCoinChargeTab? = null,
) : UiState {
    companion object {
        val empty = ShopUIState()
    }
}

/**
 * 상점 탭 줄 끝의 "코인" 탭. 누르면 충전 화면으로 가는 버튼 탭이라 선택된 탭이 되지 않는다.
 *
 * @property showsEventChip "Event" 칩을 붙인다. 선물 코인을 이미 받았으면 false이고, 누르면 "준비 중인 기능이에요"만 뜬다
 */
data class ShopCoinChargeTab(val showsEventChip: Boolean = true)

/**
 * 링크(`/shop/interior?sort=POPULAR`)로 받은 정렬 요청. 같은 링크를 다시 눌러도 다시 적용되게 [id]를 올린다.
 */
data class ShopSortRequest(val tab: ShopTab, val sort: ProductSort, val id: Int)
