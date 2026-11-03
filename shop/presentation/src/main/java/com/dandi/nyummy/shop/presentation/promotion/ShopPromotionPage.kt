package com.dandi.nyummy.shop.presentation.promotion

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dandi.nyummy.common.entity.richText.RichText
import com.dandi.nyummy.common.presentation.designsystem.component.NyummySpinner
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyStateAction
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyStateSurface
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyText
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTextButton
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTextButtonTone
import com.dandi.nyummy.common.presentation.designsystem.foundation.NyummyLoadMoreEffect
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.common.presentation.jank.JankScrollWatcher
import com.dandi.nyummy.shop.presentation.R
import com.dandi.nyummy.shop.presentation.component.ShopBanner
import com.dandi.nyummy.shop.presentation.component.ShopItemDetailSheet
import com.dandi.nyummy.shop.presentation.component.ShopPromotionItemRow
import com.dandi.nyummy.shop.presentation.component.ShopTopBar

/**
 * 프로모션 화면(피그마 `Shop · 30`·`30c`). 상단 바 제목은 배너 제목이고, 각진 배너 아래 "프로모션 상품" 줄마다
 * 가격과 "담기"가 있다. 끝난 프로모션이면 안내와 "상점으로 돌아가기"를 보인다.
 */
@Composable
fun ShopPromotionPage(
    promotionId: Long,
    viewModel: ShopPromotionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(promotionId) { viewModel.onIntent(ShopPromotionIntent.Enter(promotionId)) }
    LifecycleResumeEffect(Unit) {
        viewModel.onIntent(ShopPromotionIntent.ScreenResumed)
        onPauseOrDispose { }
    }
    val listState = rememberLazyListState()
    JankScrollWatcher(listState)
    ShopPromotionScreen(uiState = uiState, onIntent = viewModel::onIntent, listState = listState)
}

@Composable
private fun ShopPromotionScreen(
    uiState: ShopPromotionUIState,
    onIntent: (ShopPromotionIntent) -> Unit,
    listState: LazyListState = rememberLazyListState(),
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NyummyTheme.colors.bg.canvas),
    ) {
        val bannerTitle = uiState.banner?.title?.plainText().orEmpty()
        ShopTopBar(
            title = if (uiState.isEnded || bannerTitle.isEmpty()) stringResource(R.string.shop_promotion_title) else bannerTitle,
            onBack = { onIntent(ShopPromotionIntent.ClickBack) },
            cartItemCount = uiState.cartItemCount,
            onCartClick = if (uiState.isEnded) null else ({ onIntent(ShopPromotionIntent.ClickCart) }),
        )
        val bodyModifier = Modifier
            .fillMaxWidth()
            .weight(1f)
        when {
            uiState.isEnded -> Box(modifier = bodyModifier, contentAlignment = Alignment.Center) {
                NyummyStateSurface.Empty(
                    voice = stringResource(R.string.shop_promotion_ended_voice),
                    message = stringResource(R.string.shop_promotion_ended_body),
                    action = NyummyStateAction(stringResource(R.string.shop_promotion_go_to_shop)) {
                        onIntent(ShopPromotionIntent.ClickGoToShop)
                    },
                )
            }
            uiState.isLoadFailed -> Box(modifier = bodyModifier, contentAlignment = Alignment.Center) {
                NyummyStateSurface.Error(
                    voice = stringResource(R.string.shop_load_failed_voice),
                    message = stringResource(R.string.shop_load_failed_body),
                    retry = NyummyStateAction(stringResource(R.string.shop_retry)) { onIntent(ShopPromotionIntent.Retry) },
                )
            }
            uiState.isLoading -> Box(modifier = bodyModifier, contentAlignment = Alignment.Center) {
                NyummySpinner(contentDescription = stringResource(R.string.shop_loading))
            }
            else -> PromotionList(uiState = uiState, onIntent = onIntent, listState = listState, modifier = bodyModifier)
        }
    }
    uiState.detailItem?.let { item ->
        ShopItemDetailSheet(
            item = item,
            isAdding = item.productId in uiState.addingProductIds,
            onAdd = { onIntent(ShopPromotionIntent.AddToCart(item, fromSheet = true)) },
            onDismiss = { onIntent(ShopPromotionIntent.DismissDetail) },
        )
    }
}

@Composable
private fun PromotionList(
    uiState: ShopPromotionUIState,
    onIntent: (ShopPromotionIntent) -> Unit,
    listState: LazyListState,
    modifier: Modifier = Modifier,
) {
    NyummyLoadMoreEffect(listState = listState, itemCount = uiState.items.size) { onIntent(ShopPromotionIntent.LoadMore) }
    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = PaddingValues(bottom = NyummyTheme.spacing.s32),
    ) {
        uiState.banner?.let { banner ->
            item(key = "banner") { ShopBanner(banner = banner, onAction = {}) }
        }
        item(key = "title") {
            NyummyText(
                text = stringResource(R.string.shop_promotion_items),
                style = NyummyTheme.typography.titleS,
                modifier = Modifier.padding(
                    start = NyummyTheme.spacing.gutter,
                    end = NyummyTheme.spacing.gutter,
                    top = NyummyTheme.spacing.s24,
                    bottom = NyummyTheme.spacing.s8,
                ),
            )
        }
        items(uiState.items, key = { it.productId }) { item ->
            ShopPromotionItemRow(
                item = item,
                onClick = { onIntent(ShopPromotionIntent.ClickItem(item)) },
                onAdd = { onIntent(ShopPromotionIntent.AddToCart(item)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = NyummyTheme.spacing.gutter),
            )
        }
        when {
            uiState.isLoadingMore -> item(key = "loading-more") {
                Box(modifier = Modifier.fillMaxWidth().padding(NyummyTheme.spacing.s24), contentAlignment = Alignment.Center) {
                    NyummySpinner(contentDescription = stringResource(R.string.shop_loading))
                }
            }
            // 다음 묶음이 남았는데 자동 이어 받기가 멈추면(받은 묶음에 보일 상품이 없음·이어 받기 실패) 직접 더 받는다.
            uiState.hasNext -> item(key = "load-more") {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    NyummyTextButton(
                        text = stringResource(R.string.shop_load_more),
                        onClick = { onIntent(ShopPromotionIntent.LoadMore) },
                        tone = NyummyTextButtonTone.Neutral,
                    )
                }
            }
        }
    }
}

/** 서버 서식 제목의 글자만 한 줄로 잇는다(상단 바 제목). */
private fun RichText.plainText(): String =
    joinToString(separator = "") { it.text }.replace('\n', ' ').replace(Regex(" +"), " ").trim()
