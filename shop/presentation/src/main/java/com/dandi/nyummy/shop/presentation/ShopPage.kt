package com.dandi.nyummy.shop.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.shop.entity.ShopTab
import com.dandi.nyummy.shop.presentation.component.ShopHeader
import com.dandi.nyummy.shop.presentation.component.ShopItemDetailSheet
import com.dandi.nyummy.shop.presentation.recommend.ShopRecommendTab

/**
 * 상점 화면. 상단 머리(상단 바·탭) 아래에 선택한 탭 내용을 그리고, 상품을 누르면 상세 시트를 띄운다.
 *
 * @param tab 처음 열 탭(링크·라우트 인자 `tab`). 없으면 추천 탭
 * @param sort 처음 정렬(링크·라우트 인자 `sort`)
 */
@Composable
fun ShopPage(
    tab: String? = null,
    sort: String? = null,
    viewModel: ShopViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(tab, sort) {
        viewModel.onIntent(ShopIntent.Enter(tab, sort))
    }
    LifecycleResumeEffect(Unit) {
        viewModel.onIntent(ShopIntent.ScreenResumed)
        onPauseOrDispose { }
    }
    ShopScreen(uiState = uiState, onIntent = viewModel::onIntent) { tab, tabModifier ->
        ShopTabContent(tab = tab, onIntent = viewModel::onIntent, modifier = tabModifier)
    }
}

/** 탭 하나의 내용. 탭마다 자기 ViewModel을 쓰므로 미리보기에서는 [ShopScreen]에 다른 내용을 넣는다. */
@Composable
private fun ShopTabContent(tab: ShopTab, onIntent: (ShopIntent) -> Unit, modifier: Modifier) {
    when (tab) {
        ShopTab.RECOMMEND -> ShopRecommendTab(
            onItemClick = { onIntent(ShopIntent.ClickItem(it)) },
            onAction = { onIntent(ShopIntent.ClickAction(it)) },
            modifier = modifier,
        )
        // 인테리어·방 배경 목록은 탭 ViewModel과 함께 다음 단계에서 채운다.
        ShopTab.INTERIOR, ShopTab.BACKGROUND -> Box(modifier = modifier)
    }
}

/**
 * @param tabContent 고른 탭의 내용. 탭마다 저장 상태(스크롤 위치·배너 쪽)를 따로 보관해, 다른 탭에 갔다 와도 보던 자리로 돌아온다
 */
@Composable
private fun ShopScreen(
    uiState: ShopUIState,
    onIntent: (ShopIntent) -> Unit,
    tabContent: @Composable (tab: ShopTab, modifier: Modifier) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NyummyTheme.colors.bg.canvas),
    ) {
        ShopHeader(
            balance = uiState.balance,
            cartItemCount = uiState.cartItemCount,
            selectedTab = uiState.selectedTab,
            onSelectTab = { onIntent(ShopIntent.SelectTab(it)) },
            onCartClick = { onIntent(ShopIntent.ClickCart) },
            onCoinsClick = { onIntent(ShopIntent.ClickCoins) },
        )
        val tabModifier = Modifier
            .fillMaxWidth()
            .weight(1f)
        val tabStateHolder = rememberSaveableStateHolder()
        tabStateHolder.SaveableStateProvider(uiState.selectedTab.name) {
            tabContent(uiState.selectedTab, tabModifier)
        }
    }
    uiState.detailItem?.let { item ->
        ShopItemDetailSheet(
            item = item,
            isAdding = item.productId in uiState.addingProductIds,
            onAdd = { onIntent(ShopIntent.AddToCart(item, fromSheet = true)) },
            onDismiss = { onIntent(ShopIntent.DismissDetail) },
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 767)
@Composable
private fun ShopScreenPreview() {
    NyummyTheme {
        ShopScreen(uiState = ShopUIState(balance = 1240, cartItemCount = 2), onIntent = {}) { _, tabModifier ->
            Box(modifier = tabModifier)
        }
    }
}
