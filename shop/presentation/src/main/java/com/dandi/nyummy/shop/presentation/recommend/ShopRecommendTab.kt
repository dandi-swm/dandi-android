package com.dandi.nyummy.shop.presentation.recommend

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dandi.nyummy.common.entity.richText.RichTextVO
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyPullToRefreshBox
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyRichText
import com.dandi.nyummy.common.presentation.designsystem.component.NyummySpinner
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyStateAction
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyStateSurface
import com.dandi.nyummy.common.presentation.designsystem.foundation.nyummyClickable
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.common.presentation.jank.JankScrollWatcher
import com.dandi.nyummy.shop.entity.SduiComponentVO
import com.dandi.nyummy.shop.entity.ShopActionVO
import com.dandi.nyummy.shop.entity.ShopBannerVO
import com.dandi.nyummy.shop.entity.ShopItemVO
import com.dandi.nyummy.shop.presentation.R
import com.dandi.nyummy.shop.presentation.component.PreviewPrice
import com.dandi.nyummy.shop.presentation.component.ShopHeroBanner
import com.dandi.nyummy.shop.presentation.component.ShopItemCard
import com.dandi.nyummy.shop.presentation.component.ShopItemRow
import com.dandi.nyummy.shop.presentation.component.ShopRankRow
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

/**
 * 추천 탭(서버 SDUI). 서버가 준 칸을 순서대로 그리고, 추천 탭 상품에는 담기 버튼이 없다(누르면 상세 시트).
 * 상품 누름과 서버 액션은 상점 화면으로 올리고, 어느 칸의 몇 번째를 눌렀는지는 추천 탭이 분석 이벤트로 남긴다.
 * 목록을 아래로 당기면 칸을 다시 읽고, [onRefresh]로 상점 화면에도 알린다(상단 바 값 새로 읽기).
 */
@Composable
fun ShopRecommendTab(
    onItemClick: (ShopItemVO) -> Unit,
    onAction: (ShopActionVO) -> Unit,
    modifier: Modifier = Modifier,
    onRefresh: () -> Unit = {},
    viewModel: ShopRecommendViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    JankScrollWatcher(listState)
    ShopRecommendContent(
        uiState = uiState,
        listState = listState,
        onItemClick = { componentIndex, position, item ->
            viewModel.onIntent(ShopRecommendIntent.ItemClicked(componentIndex, position, item.productId))
            onItemClick(item)
        },
        onBannerAction = { componentIndex, action ->
            viewModel.onIntent(ShopRecommendIntent.BannerClicked(componentIndex, action))
            onAction(action)
        },
        onAction = onAction,
        onRetry = { viewModel.onIntent(ShopRecommendIntent.Retry) },
        onRefresh = {
            viewModel.onIntent(ShopRecommendIntent.Refresh)
            onRefresh()
        },
        modifier = modifier,
    )
}

@Composable
private fun ShopRecommendContent(
    uiState: ShopRecommendUIState,
    onItemClick: (componentIndex: Int, position: Int, item: ShopItemVO) -> Unit,
    onBannerAction: (componentIndex: Int, action: ShopActionVO) -> Unit,
    onAction: (ShopActionVO) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    onRefresh: () -> Unit = {},
    listState: LazyListState = rememberLazyListState(),
) {
    when {
        uiState.isLoadFailed -> Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            NyummyStateSurface.Error(
                voice = stringResource(R.string.shop_load_failed_voice),
                message = stringResource(R.string.shop_load_failed_body),
                retry = NyummyStateAction(stringResource(R.string.shop_retry), onRetry),
            )
        }
        uiState.isLoading && uiState.components.isEmpty() ->
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                NyummySpinner(contentDescription = stringResource(R.string.shop_loading))
            }
        else -> NyummyPullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = onRefresh,
            modifier = modifier.fillMaxSize(),
        ) {
            val gutter = NyummyTheme.spacing.gutter
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = NyummyTheme.spacing.s16, bottom = NyummyTheme.spacing.s32),
            ) {
                uiState.components.forEachIndexed { index, component ->
                    sduiComponent(index, component, gutter, onItemClick, onBannerAction, onAction)
                }
            }
        }
    }
}

/** 칸 하나를 목록 항목으로 펼친다. 상품 줄은 줄마다 따로 항목이라 긴 목록도 보이는 만큼만 그린다. */
private fun LazyListScope.sduiComponent(
    index: Int,
    component: SduiComponentVO,
    gutterWidth: Dp,
    onItemClick: (componentIndex: Int, position: Int, item: ShopItemVO) -> Unit,
    onBannerAction: (componentIndex: Int, action: ShopActionVO) -> Unit,
    onAction: (ShopActionVO) -> Unit,
) {
    val gutter = Modifier.padding(horizontal = gutterWidth)
    when (component) {
        is SduiComponentVO.HeroBanner -> item(key = "hero-$index") {
            ShopHeroBanner(banners = component.banners.toImmutableList(), onAction = { onBannerAction(index, it) })
        }
        is SduiComponentVO.SectionHeader -> item(key = "header-$index") {
            ShopSectionHeader(header = component, onAction = onAction, isFirst = index == 0, modifier = gutter)
        }
        is SduiComponentVO.Ranking -> itemsIndexed(component.items, key = { _, item -> "rank-$index-${item.productId}" }) { rank, item ->
            ShopRankRow(rank = rank + 1, item = item, onClick = { onItemClick(index, rank, item) }, modifier = gutter.fillMaxWidth())
        }
        is SduiComponentVO.ItemList -> itemsIndexed(component.items, key = { _, item -> "list-$index-${item.productId}" }) { position, item ->
            ShopItemRow(item = item, onClick = { onItemClick(index, position, item) }, modifier = gutter.fillMaxWidth())
        }
        is SduiComponentVO.ItemGrid -> {
            val rows = component.items.chunked(GridColumns)
            itemsIndexed(rows, key = { row, _ -> "grid-$index-$row" }) { rowIndex, row ->
                Row(
                    modifier = gutter.fillMaxWidth().padding(vertical = NyummyTheme.spacing.s8),
                    horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s12),
                ) {
                    row.forEachIndexed { column, item ->
                        val position = rowIndex * GridColumns + column
                        ShopItemCard(item = item, onClick = { onItemClick(index, position, item) }, modifier = Modifier.weight(1f))
                    }
                    repeat(GridColumns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

/**
 * 칸 제목 줄(피그마 `v2 / Section / SectionHeader`). 위 32·아래 8 간격을 이 줄이 가진다. 오른쪽 글자는 서버 값이고,
 * 액션이 있으면 누를 수 있다("전체보기 >").
 * [isFirst]면(배너 없이 이 줄로 시작하면, 예: 첫 구매를 마친 사람) 위 간격을 빼서 배너가 있을 때처럼 탭 아래 목록 여백만 둔다.
 */
@Composable
private fun ShopSectionHeader(
    header: SduiComponentVO.SectionHeader,
    onAction: (ShopActionVO) -> Unit,
    isFirst: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = if (isFirst) 0.dp else NyummyTheme.spacing.s32, bottom = NyummyTheme.spacing.s8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NyummyRichText(richText = header.title, maxLines = 1, modifier = Modifier.weight(1f))
        if (header.trailing.isNotEmpty()) {
            val action = header.trailingAction
            NyummyRichText(
                richText = header.trailing,
                maxLines = 1,
                modifier = if (action != null) {
                    Modifier
                        .nyummyClickable(onClick = { onAction(action) })
                        .padding(NyummyTheme.spacing.s4)
                } else {
                    Modifier
                },
            )
        }
    }
}

private const val GridColumns = 3

@Preview(showBackground = true, widthDp = 390, heightDp = 900)
@Composable
private fun ShopRecommendContentPreview() {
    val item = ShopItemVO(productId = 1, itemId = 2, name = "세이지 캣휠", categoryName = "캣휠", price = PreviewPrice)
    NyummyTheme {
        ShopRecommendContent(
            uiState = ShopRecommendUIState(
                isLoading = false,
                components = persistentListOf(
                    SduiComponentVO.HeroBanner(
                        listOf(
                            ShopBannerVO(bannerId = 1, title = listOf(RichTextVO("신입 집사\n환영", "displayL")), price = PreviewPrice),
                            ShopBannerVO(bannerId = 2, title = listOf(RichTextVO("가을 모음", "displayL"))),
                        ),
                    ),
                    SduiComponentVO.SectionHeader(
                        title = listOf(RichTextVO("지금 인기 있는 가구", "titleS")),
                        trailing = listOf(RichTextVO("전체보기", "labelS", "contentSecondary"), RichTextVO(icon = "chevronRight")),
                        trailingAction = ShopActionVO.Navigate("https://link.nyummy.co.kr/shop/interior?sort=POPULAR"),
                    ),
                    SduiComponentVO.Ranking(List(4) { item.copy(productId = it.toLong()) }),
                    SduiComponentVO.ItemGrid(List(4) { item.copy(productId = 10L + it) }),
                ),
            ),
            onItemClick = { _, _, _ -> },
            onBannerAction = { _, _ -> },
            onAction = {},
            onRetry = {},
        )
    }
}
