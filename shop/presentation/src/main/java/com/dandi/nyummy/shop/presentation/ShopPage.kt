package com.dandi.nyummy.shop.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.shop.presentation.component.ShopHeader
import com.dandi.nyummy.shop.presentation.component.ShopItemDetailSheet

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
    ShopScreen(uiState = uiState, onIntent = viewModel::onIntent)
}

@Composable
private fun ShopScreen(
    uiState: ShopUIState,
    onIntent: (ShopIntent) -> Unit,
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
        // 탭 내용(추천 SDUI, 인테리어·방 배경 목록)은 탭 ViewModel과 함께 다음 단계에서 채운다.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        )
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
        ShopScreen(uiState = ShopUIState(balance = 1240, cartItemCount = 2), onIntent = {})
    }
}
