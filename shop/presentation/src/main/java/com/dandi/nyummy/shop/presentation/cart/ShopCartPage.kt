package com.dandi.nyummy.shop.presentation.cart

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dandi.nyummy.common.entity.tag.TagTone
import com.dandi.nyummy.common.entity.tag.TagVO
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyButton
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyButtonSize
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyButtonStyle
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyCheckbox
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyDivider
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyPose
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyPoseImage
import com.dandi.nyummy.common.presentation.designsystem.component.NyummySectionBand
import com.dandi.nyummy.common.presentation.designsystem.component.NyummySpinner
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyStateAction
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyStateSurface
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTag
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyText
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTextButton
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTextButtonSize
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTextButtonTone
import com.dandi.nyummy.common.presentation.designsystem.foundation.nyummyClickable
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.shop.entity.CartItemVO
import com.dandi.nyummy.shop.entity.CartSummaryVO
import com.dandi.nyummy.shop.presentation.R
import com.dandi.nyummy.shop.presentation.component.ShopItemCard
import com.dandi.nyummy.shop.presentation.component.ShopItemDetailSheet
import com.dandi.nyummy.shop.presentation.component.ShopItemImage
import com.dandi.nyummy.shop.presentation.component.ShopItemPrice
import com.dandi.nyummy.shop.presentation.component.ShopTopBar
import java.text.NumberFormat
import java.util.Locale
import com.dandi.nyummy.common.presentation.R as CommonR

/**
 * 장바구니 화면(피그마 `Shop · 20`·`20b`). 전체 선택, 장바구니 줄, 결제 정보, 하단 구매 바. 비어 있으면 안내와
 * 인기 인테리어 추천을 보인다.
 */
@Composable
fun ShopCartPage(viewModel: ShopCartViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.onIntent(ShopCartIntent.Enter) }
    ShopCartScreen(uiState = uiState, onIntent = viewModel::onIntent)
}

@Composable
private fun ShopCartScreen(uiState: ShopCartUIState, onIntent: (ShopCartIntent) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NyummyTheme.colors.bg.canvas),
    ) {
        ShopTopBar(title = stringResource(R.string.shop_cart_title), onBack = { onIntent(ShopCartIntent.ClickBack) })
        NyummyDivider()
        val bodyModifier = Modifier
            .fillMaxWidth()
            .weight(1f)
        when {
            uiState.isLoadFailed -> Box(modifier = bodyModifier, contentAlignment = Alignment.Center) {
                NyummyStateSurface.Error(
                    voice = stringResource(R.string.shop_load_failed_voice),
                    message = stringResource(R.string.shop_load_failed_body),
                    retry = NyummyStateAction(stringResource(R.string.shop_retry)) { onIntent(ShopCartIntent.Retry) },
                )
            }
            uiState.isLoading -> Box(modifier = bodyModifier, contentAlignment = Alignment.Center) {
                NyummySpinner(contentDescription = stringResource(R.string.shop_loading))
            }
            uiState.isEmpty -> EmptyCart(uiState = uiState, onIntent = onIntent, modifier = bodyModifier)
            else -> {
                CartList(uiState = uiState, onIntent = onIntent, modifier = bodyModifier)
                PurchaseBar(summary = uiState.summary, enabled = uiState.selectedItemCount > 0)
            }
        }
    }
    uiState.detailItem?.let { item ->
        ShopItemDetailSheet(
            item = item,
            isAdding = item.productId in uiState.addingProductIds,
            onAdd = { onIntent(ShopCartIntent.AddRecommendation(item)) },
            onDismiss = { onIntent(ShopCartIntent.DismissDetail) },
        )
    }
}

@Composable
private fun CartList(uiState: ShopCartUIState, onIntent: (ShopCartIntent) -> Unit, modifier: Modifier) {
    LazyColumn(modifier = modifier, contentPadding = PaddingValues(bottom = NyummyTheme.spacing.s32)) {
        item(key = "select-all") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = NyummyTheme.spacing.gutter, vertical = NyummyTheme.spacing.s12),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NyummyCheckbox(
                    checked = uiState.isAllSelected,
                    onCheckedChange = { onIntent(ShopCartIntent.ToggleSelectAll) },
                    enabled = uiState.items.any { it.canPurchase },
                )
                NyummyText(
                    text = stringResource(R.string.shop_cart_select_all, uiState.selectedItemCount, uiState.items.size),
                    style = NyummyTheme.typography.labelM,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = NyummyTheme.spacing.s8),
                )
                NyummyTextButton(
                    text = stringResource(R.string.shop_cart_delete_selected),
                    onClick = { onIntent(ShopCartIntent.DeleteSelected) },
                    tone = NyummyTextButtonTone.Neutral,
                    size = NyummyTextButtonSize.S,
                    enabled = uiState.selectedItemCount > 0,
                )
            }
        }
        item(key = "band-top") { NyummySectionBand() }
        items(uiState.items, key = { it.shopItem.productId }) { item ->
            CartRow(item = item, onIntent = onIntent)
        }
        item(key = "band-summary") { NyummySectionBand() }
        item(key = "summary") { CartSummary(summary = uiState.summary) }
    }
}

/** 장바구니 줄(피그마 `v2 / Shop / Cart Row`). 살 수 없으면 흐리게, 체크 없이, 서버 이유 문구를 빨갛게 보인다. */
@Composable
private fun CartRow(item: CartItemVO, onIntent: (ShopCartIntent) -> Unit) {
    val productId = item.shopItem.productId
    val dimmed = if (item.canPurchase) 1f else UnavailableAlpha
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = NyummyTheme.spacing.gutter, vertical = NyummyTheme.spacing.s16),
        horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s12),
    ) {
        NyummyCheckbox(
            checked = item.isSelected && item.canPurchase,
            onCheckedChange = { onIntent(ShopCartIntent.ToggleSelect(productId)) },
            enabled = item.canPurchase,
        )
        Box(
            modifier = Modifier
                .size(CartTileSize)
                .alpha(dimmed)
                .clip(RoundedCornerShape(NyummyTheme.radius.s))
                .background(NyummyTheme.colors.bg.surfaceSunken),
            contentAlignment = Alignment.Center,
        ) {
            ShopItemImage(itemId = item.shopItem.itemId, modifier = Modifier.size(CartImageSize))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s4)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s6),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    NyummyText(
                        text = item.shopItem.name,
                        style = NyummyTheme.typography.titleS,
                        maxLines = 1,
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .alpha(dimmed),
                    )
                    if (item.canPurchase && item.shopItem.isOwned) {
                        // 장바구니 줄은 방 배경도 "n개 보유"다(스펙 cart).
                        NyummyTag(TagVO(stringResource(R.string.shop_item_owned_count, item.shopItem.ownedCount), TagTone.SUCCESS))
                    }
                }
                Icon(
                    painter = painterResource(CommonR.drawable.nyummy_ic_x),
                    contentDescription = stringResource(R.string.shop_cart_remove),
                    tint = NyummyTheme.colors.content.tertiary,
                    modifier = Modifier
                        .size(NyummyTheme.size.iconM)
                        .nyummyClickable(onClick = { onIntent(ShopCartIntent.DeleteItem(productId)) }),
                )
            }
            NyummyText(
                text = item.shopItem.categoryName,
                style = NyummyTheme.typography.bodyS,
                color = NyummyTheme.colors.content.tertiary,
            )
            if (item.canPurchase) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    ShopItemPrice(item = item.shopItem, modifier = Modifier.weight(1f))
                    if (item.maxQuantity > 1) {
                        QuantityStepper(
                            quantity = item.quantity,
                            max = item.maxQuantity,
                            onChange = { onIntent(ShopCartIntent.ChangeQuantity(productId, it)) },
                        )
                    }
                }
            } else if (item.unavailableReason.isNotEmpty()) {
                NyummyText(
                    text = item.unavailableReason,
                    style = NyummyTheme.typography.bodyS,
                    color = NyummyTheme.colors.content.danger,
                )
            }
        }
    }
}

/** 수량 −/+(피그마 `v2 / Shop / Stepper`). −는 1, +는 [max]에서 멈춘다. */
@Composable
private fun QuantityStepper(quantity: Int, max: Int, onChange: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .height(StepperHeight)
            .background(NyummyTheme.colors.bg.surface, RoundedCornerShape(NyummyTheme.radius.xs))
            .border(NyummyTheme.borderWidth.hairline, NyummyTheme.colors.border.default, RoundedCornerShape(NyummyTheme.radius.xs)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepperButton(
            icon = CommonR.drawable.nyummy_ic_minus,
            description = stringResource(R.string.shop_cart_decrease),
            enabled = quantity > 1,
            onClick = { onChange(quantity - 1) },
        )
        NyummyText(
            text = quantity.toString(),
            style = NyummyTheme.typography.numberXs,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(StepperValueWidth),
        )
        StepperButton(
            icon = CommonR.drawable.nyummy_ic_plus,
            description = stringResource(R.string.shop_cart_increase),
            enabled = quantity < max,
            onClick = { onChange(quantity + 1) },
        )
    }
}

@Composable
private fun StepperButton(icon: Int, description: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(StepperHeight)
            .nyummyClickable(onClick = onClick, enabled = enabled),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = description,
            tint = if (enabled) NyummyTheme.colors.content.primary else NyummyTheme.colors.content.disabled,
            modifier = Modifier.size(NyummyTheme.size.iconS),
        )
    }
}

/** 결제 정보(서버 요약 그대로). 할인은 베리 색 마이너스다. */
@Composable
private fun CartSummary(summary: CartSummaryVO) {
    Column(
        modifier = Modifier.padding(horizontal = NyummyTheme.spacing.gutter, vertical = NyummyTheme.spacing.s24),
        verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s12),
    ) {
        NyummyText(text = stringResource(R.string.shop_cart_summary_title), style = NyummyTheme.typography.titleS)
        SummaryRow(stringResource(R.string.shop_cart_list_price, summary.quantity), coins(summary.listPrice))
        SummaryRow(
            label = stringResource(R.string.shop_cart_discount),
            value = if (summary.discountPrice > 0) "-${coins(summary.discountPrice)}" else coins(0),
            valueColor = NyummyTheme.colors.content.promo,
        )
        NyummyDivider()
        SummaryRow(stringResource(R.string.shop_cart_total), coins(summary.purchasePrice), isTotal = true)
        SummaryRow(stringResource(R.string.shop_cart_balance), coins(summary.balance))
        SummaryRow(stringResource(R.string.shop_cart_balance_after), coins(summary.balanceAfter))
        NyummyText(
            text = stringResource(R.string.shop_cart_notice),
            style = NyummyTheme.typography.bodyS,
            color = NyummyTheme.colors.content.tertiary,
        )
    }
}

/** 결제 정보 한 줄(피그마 `v2 / Shop / Summary Row`). */
@Composable
private fun SummaryRow(
    label: String,
    value: String,
    isTotal: Boolean = false,
    valueColor: Color = NyummyTheme.colors.content.primary,
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        NyummyText(
            text = label,
            style = if (isTotal) NyummyTheme.typography.titleS else NyummyTheme.typography.bodyM,
            color = if (isTotal) NyummyTheme.colors.content.primary else NyummyTheme.colors.content.secondary,
            modifier = Modifier.weight(1f),
        )
        CoinAmount(
            value = value,
            iconSize = if (isTotal) TotalCoinSize else SummaryCoinSize,
            style = if (isTotal) NyummyTheme.typography.numberL else NyummyTheme.typography.numberXs,
            color = valueColor,
        )
    }
}

/** 하단 구매 바: 결제할 코인과 "n개 구매하기". 선택이 없으면 끈다. 구매 흐름은 다음 단계에서 붙인다. */
@Composable
private fun PurchaseBar(summary: CartSummaryVO, enabled: Boolean) {
    Column {
        NyummyDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(NyummyTheme.colors.bg.surface)
                .padding(horizontal = NyummyTheme.spacing.gutter, vertical = NyummyTheme.spacing.s8),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                NyummyText(
                    text = stringResource(R.string.shop_cart_total),
                    style = NyummyTheme.typography.labelXs,
                    color = NyummyTheme.colors.content.tertiary,
                )
                CoinAmount(value = coins(summary.purchasePrice), iconSize = TotalCoinSize, style = NyummyTheme.typography.numberL)
            }
            NyummyButton(
                text = stringResource(R.string.shop_cart_purchase, summary.quantity),
                onClick = {},
                enabled = enabled,
                size = NyummyButtonSize.L,
                modifier = Modifier.width(PurchaseButtonWidth),
            )
        }
    }
}

/** 빈 장바구니(피그마 `Shop · 20b`): 안내, "상점 구경하기", 인기 인테리어 추천(실패하면 숨김). */
@Composable
private fun EmptyCart(uiState: ShopCartUIState, onIntent: (ShopCartIntent) -> Unit, modifier: Modifier) {
    LazyColumn(modifier = modifier, contentPadding = PaddingValues(bottom = NyummyTheme.spacing.s32)) {
        item(key = "empty") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = NyummyTheme.spacing.s32),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s8),
            ) {
                NyummyPoseImage(pose = NyummyPose.Hello, size = NyummyTheme.size.characterM)
                NyummyText(text = stringResource(R.string.shop_cart_empty_title), style = NyummyTheme.typography.titleM)
                NyummyText(
                    text = stringResource(R.string.shop_cart_empty_body),
                    style = NyummyTheme.typography.bodyM,
                    color = NyummyTheme.colors.content.secondary,
                )
                Spacer(Modifier.height(NyummyTheme.spacing.s16))
                NyummyButton(
                    text = stringResource(R.string.shop_cart_browse),
                    onClick = { onIntent(ShopCartIntent.ClickBrowseShop) },
                    style = NyummyButtonStyle.Tonal,
                    modifier = Modifier.width(BrowseButtonWidth),
                )
            }
        }
        if (uiState.recommendations.isNotEmpty()) {
            item(key = "band") { NyummySectionBand() }
            item(key = "recommend-header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = NyummyTheme.spacing.gutter, end = NyummyTheme.spacing.s12, top = NyummyTheme.spacing.s16),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    NyummyText(
                        text = stringResource(R.string.shop_cart_recommend_title),
                        style = NyummyTheme.typography.titleM,
                        modifier = Modifier.weight(1f),
                    )
                    NyummyTextButton(
                        text = stringResource(R.string.shop_cart_see_all),
                        onClick = { onIntent(ShopCartIntent.ClickSeeAllRecommendations) },
                        tone = NyummyTextButtonTone.Neutral,
                        size = NyummyTextButtonSize.S,
                        trailingIcon = CommonR.drawable.nyummy_ic_chevron_right,
                    )
                }
            }
            items(uiState.recommendations.chunked(RecommendColumns), key = { row -> "recommend-${row.first().productId}" }) { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = NyummyTheme.spacing.gutter, vertical = NyummyTheme.spacing.s8),
                    horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s12),
                ) {
                    row.forEach { item ->
                        ShopItemCard(
                            item = item,
                            onClick = { onIntent(ShopCartIntent.ClickRecommendation(item)) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    repeat(RecommendColumns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun CoinAmount(
    value: String,
    iconSize: Dp,
    style: TextStyle,
    color: Color = NyummyTheme.colors.content.primary,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s4), verticalAlignment = Alignment.CenterVertically) {
        Image(painter = painterResource(CommonR.drawable.nyummy_asset_coin), contentDescription = null, modifier = Modifier.size(iconSize))
        NyummyText(text = value, style = style, color = color, maxLines = 1)
    }
}

private fun coins(value: Int): String = NumberFormat.getNumberInstance(Locale.KOREA).format(value)

private const val UnavailableAlpha = 0.45f
private const val RecommendColumns = 3
private val CartTileSize = 72.dp
private val CartImageSize = 60.dp
private val StepperHeight = 32.dp
private val StepperValueWidth = 28.dp
private val SummaryCoinSize = 18.dp
private val TotalCoinSize = 22.dp
private val PurchaseButtonWidth = 180.dp
private val BrowseButtonWidth = 200.dp
