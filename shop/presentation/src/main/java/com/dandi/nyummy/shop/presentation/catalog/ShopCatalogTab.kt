package com.dandi.nyummy.shop.presentation.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyBottomSheet
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyChip
import com.dandi.nyummy.common.presentation.designsystem.component.NyummySheetTitle
import com.dandi.nyummy.common.presentation.designsystem.component.NyummySpinner
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyStateAction
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyStateSurface
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyText
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTextButton
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTextButtonTone
import com.dandi.nyummy.common.presentation.designsystem.foundation.NyummyLoadMoreEffect
import com.dandi.nyummy.common.presentation.designsystem.foundation.nyummyClickable
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.common.presentation.jank.JankScrollWatcher
import com.dandi.nyummy.shop.entity.ProductSort
import com.dandi.nyummy.shop.entity.ShopCategory
import com.dandi.nyummy.shop.entity.ShopItemVO
import com.dandi.nyummy.shop.entity.ShopTab
import com.dandi.nyummy.shop.presentation.R
import com.dandi.nyummy.shop.presentation.ShopSortRequest
import com.dandi.nyummy.shop.presentation.component.ShopBackgroundCard
import com.dandi.nyummy.shop.presentation.component.ShopItemCard
import com.dandi.nyummy.common.presentation.R as CommonR

/**
 * 인테리어·방 배경 탭(피그마 `Shop · 02`·`03`). 인테리어만 분류 칩이 있고, 두 탭 모두 보유 숨기기·정렬과 3열 격자다.
 * 카드의 담기 버튼은 1개를 바로 담고, 카드를 누르면 상세 시트가 열린다(둘 다 상점 화면이 처리한다).
 *
 * @param sortRequest 링크로 받은 정렬. 이 탭 것이면 정렬을 바꿔 처음부터 다시 읽는다
 * @param conditionResetId 칩·정렬을 처음으로 되돌리라는 요청 번호. 바뀐 뒤 이 탭이 처음 보일 때 되돌린다
 */
@Composable
fun ShopCatalogTab(
    tab: ShopTab,
    sortRequest: ShopSortRequest?,
    conditionResetId: Int,
    onItemClick: (ShopItemVO) -> Unit,
    onAdd: (ShopItemVO) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ShopCatalogViewModel = hiltViewModel(key = "shop-catalog-${tab.name}"),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(tab) { viewModel.onIntent(ShopCatalogIntent.Enter(tab)) }
    // 되돌리기가 링크 정렬보다 먼저다. 다시 들어오면서 링크 정렬도 받았으면 그 정렬이 남는다.
    LaunchedEffect(conditionResetId) { viewModel.onIntent(ShopCatalogIntent.ResetConditions(conditionResetId)) }
    LaunchedEffect(sortRequest) { sortRequest?.let { viewModel.onIntent(ShopCatalogIntent.ApplySortRequest(it)) } }
    val listState = rememberLazyListState()
    JankScrollWatcher(listState)
    ShopCatalogContent(
        uiState = uiState,
        listState = listState,
        onIntent = viewModel::onIntent,
        onItemClick = onItemClick,
        onAdd = onAdd,
        modifier = modifier,
    )
}

@Composable
private fun ShopCatalogContent(
    uiState: ShopCatalogUIState,
    onIntent: (ShopCatalogIntent) -> Unit,
    onItemClick: (ShopItemVO) -> Unit,
    onAdd: (ShopItemVO) -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
) {
    val rows = uiState.visibleItems.chunked(GridColumns)
    NyummyLoadMoreEffect(listState = listState, itemCount = rows.size) { onIntent(ShopCatalogIntent.LoadMore) }
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = NyummyTheme.spacing.s16, bottom = NyummyTheme.spacing.s32),
    ) {
        if (uiState.tab == ShopTab.INTERIOR) {
            item(key = "chips") {
                CategoryChips(selected = uiState.category, onSelect = { onIntent(ShopCatalogIntent.SelectCategory(it)) })
            }
        }
        item(key = "meta") {
            CatalogMeta(
                hideOwned = uiState.hideOwned,
                sort = uiState.sort,
                onToggleHideOwned = { onIntent(ShopCatalogIntent.ToggleHideOwned) },
                onSortClick = { onIntent(ShopCatalogIntent.OpenSortSheet) },
                modifier = Modifier.padding(top = if (uiState.tab == ShopTab.INTERIOR) NyummyTheme.spacing.s12 else 0.dp),
            )
        }
        when {
            uiState.isLoadFailed -> item(key = "failed") {
                NyummyStateSurface.Error(
                    voice = stringResource(R.string.shop_load_failed_voice),
                    message = stringResource(R.string.shop_load_failed_body),
                    retry = NyummyStateAction(stringResource(R.string.shop_retry)) { onIntent(ShopCatalogIntent.Retry) },
                    modifier = Modifier.padding(top = NyummyTheme.spacing.s32),
                )
            }
            uiState.isLoading -> item(key = "loading") { CatalogSpinner() }
            uiState.visibleItems.isEmpty() && !uiState.hasNext -> item(key = "empty") {
                NyummyStateSurface.Empty(
                    voice = stringResource(R.string.shop_catalog_empty_voice),
                    message = stringResource(R.string.shop_catalog_empty_body),
                    modifier = Modifier.padding(top = NyummyTheme.spacing.s32),
                )
            }
            else -> {
                itemsIndexed(rows, key = { index, _ -> "row-$index" }) { _, row ->
                    CatalogRow(tab = uiState.tab, row = row, onItemClick = onItemClick, onAdd = onAdd)
                }
                when {
                    uiState.isLoadingMore -> item(key = "loading-more") { CatalogSpinner() }
                    // 끝에 닿아도 이어 받지 못한 경우(보유 숨기기로 새로 보인 상품이 없거나 이어 받기 실패) 직접 더 받는다.
                    uiState.hasNext -> item(key = "load-more") {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            NyummyTextButton(
                                text = stringResource(R.string.shop_load_more),
                                onClick = { onIntent(ShopCatalogIntent.LoadMore) },
                                tone = NyummyTextButtonTone.Neutral,
                            )
                        }
                    }
                }
            }
        }
    }
    if (uiState.isSortSheetOpen) {
        SortSheet(
            selected = uiState.sort,
            onSelect = { onIntent(ShopCatalogIntent.SelectSort(it)) },
            onDismiss = { onIntent(ShopCatalogIntent.DismissSortSheet) },
        )
    }
}

/** 분류 칩 줄. "전체"(null) + 서버 분류 5종, 가로로 넘친다. */
@Composable
private fun CategoryChips(selected: ShopCategory?, onSelect: (ShopCategory?) -> Unit) {
    val options = listOf<ShopCategory?>(null) + ShopCategory.entries
    LazyRow(
        contentPadding = PaddingValues(horizontal = NyummyTheme.spacing.gutter),
        horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s8),
    ) {
        items(options, key = { it?.name ?: "ALL" }) { category ->
            NyummyChip(
                text = stringResource(category.labelRes),
                selected = category == selected,
                onClick = { onSelect(category) },
            )
        }
    }
}

/** 오른쪽 정렬: "(✓) 보유 숨기기"와 "신상품순 ⌄". 원형 체크는 체크박스처럼 켜지면 초록, 꺼지면 회색으로 채운다. */
@Composable
private fun CatalogMeta(
    hideOwned: Boolean,
    sort: ProductSort,
    onToggleHideOwned: () -> Unit,
    onSortClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val secondary = NyummyTheme.colors.content.secondary
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = NyummyTheme.spacing.gutter),
        horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s12, Alignment.End),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val hideColor = if (hideOwned) NyummyTheme.colors.content.brand else secondary
        Row(
            modifier = Modifier
                .semantics { toggleableState = ToggleableState(hideOwned) }
                .nyummyClickable(onClick = onToggleHideOwned, role = Role.Checkbox),
            horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(CommonR.drawable.nyummy_ic_check_circle_fill),
                contentDescription = null,
                tint = if (hideOwned) NyummyTheme.colors.bg.actionPrimary else NyummyTheme.colors.border.strong,
                modifier = Modifier.size(MetaCheckSize),
            )
            NyummyText(text = stringResource(R.string.shop_catalog_hide_owned), style = NyummyTheme.typography.labelM, color = hideColor)
        }
        Row(
            modifier = Modifier.nyummyClickable(onClick = onSortClick),
            horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NyummyText(text = stringResource(sort.labelRes), style = NyummyTheme.typography.labelM, color = secondary)
            Icon(
                painter = painterResource(CommonR.drawable.nyummy_ic_chevron_down),
                contentDescription = null,
                tint = secondary,
                modifier = Modifier.size(NyummyTheme.size.iconS),
            )
        }
    }
}

/** 격자 한 줄(3열). 줄 사이 20, 첫 줄 위 12. */
@Composable
private fun CatalogRow(
    tab: ShopTab,
    row: List<ShopItemVO>,
    onItemClick: (ShopItemVO) -> Unit,
    onAdd: (ShopItemVO) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = NyummyTheme.spacing.gutter)
            .padding(top = NyummyTheme.spacing.s12, bottom = NyummyTheme.spacing.s8),
        horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s12),
    ) {
        row.forEach { item ->
            val cardModifier = Modifier.weight(1f)
            if (tab == ShopTab.BACKGROUND) {
                ShopBackgroundCard(item = item, onClick = { onItemClick(item) }, onAdd = { onAdd(item) }, modifier = cardModifier)
            } else {
                ShopItemCard(item = item, onClick = { onItemClick(item) }, onAdd = { onAdd(item) }, modifier = cardModifier)
            }
        }
        repeat(GridColumns - row.size) { Spacer(Modifier.weight(1f)) }
    }
}

@Composable
private fun CatalogSpinner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = NyummyTheme.spacing.s24),
        contentAlignment = Alignment.Center,
    ) {
        NyummySpinner(contentDescription = stringResource(R.string.shop_loading))
    }
}

/** 정렬 시트(피그마 `Shop · 02b`). 고른 정렬은 브랜드 색 굵은 글자와 체크다. */
@Composable
private fun SortSheet(selected: ProductSort, onSelect: (ProductSort) -> Unit, onDismiss: () -> Unit) {
    // 시트 기본 간격(16) 대신 제목 아래 8, 옵션끼리는 붙인다(옵션 줄 높이 52).
    NyummyBottomSheet(onDismissRequest = onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s8)) {
            NyummySheetTitle(text = stringResource(R.string.shop_sort_title))
            Column { SortOptions(selected = selected, onSelect = onSelect) }
        }
    }
}

@Composable
private fun SortOptions(selected: ProductSort, onSelect: (ProductSort) -> Unit) {
    ProductSort.entries.forEach { sort ->
        val isSelected = sort == selected
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(SortOptionHeight)
                .semantics { this.selected = isSelected }
                .nyummyClickable(onClick = { onSelect(sort) }, role = Role.RadioButton),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NyummyText(
                text = stringResource(sort.labelRes),
                style = if (isSelected) NyummyTheme.typography.labelM else NyummyTheme.typography.bodyL,
                color = if (isSelected) NyummyTheme.colors.content.brand else NyummyTheme.colors.content.primary,
                modifier = Modifier.weight(1f),
            )
            if (isSelected) {
                Icon(
                    painter = painterResource(CommonR.drawable.nyummy_ic_check),
                    contentDescription = null,
                    tint = NyummyTheme.colors.content.brand,
                    modifier = Modifier.size(NyummyTheme.size.iconL),
                )
            }
        }
    }
}

private val ShopCategory?.labelRes: Int
    get() = when (this) {
        null -> R.string.shop_category_all
        ShopCategory.LIMITED -> R.string.shop_category_limited
        ShopCategory.BED -> R.string.shop_category_bed
        ShopCategory.CAT_WHEEL -> R.string.shop_category_cat_wheel
        ShopCategory.RUG -> R.string.shop_category_rug
        ShopCategory.ETC -> R.string.shop_category_etc
    }

private val ProductSort.labelRes: Int
    get() = when (this) {
        ProductSort.NEW -> R.string.shop_sort_new
        ProductSort.POPULAR -> R.string.shop_sort_popular
        ProductSort.PRICE_ASC -> R.string.shop_sort_price_asc
        ProductSort.PRICE_DESC -> R.string.shop_sort_price_desc
    }

private const val GridColumns = 3
private val MetaCheckSize = 18.dp
private val SortOptionHeight = 52.dp
