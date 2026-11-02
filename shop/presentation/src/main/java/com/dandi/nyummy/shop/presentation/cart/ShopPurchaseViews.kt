package com.dandi.nyummy.shop.presentation.cart

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyBottomSheet
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyButton
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyButtonStyle
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyListRow
import com.dandi.nyummy.common.presentation.designsystem.component.NyummySheetTitle
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyText
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTextButton
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTextButtonTone
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.shop.entity.CartSummaryVO
import com.dandi.nyummy.shop.entity.ShopItemVO
import com.dandi.nyummy.shop.presentation.R
import com.dandi.nyummy.shop.presentation.component.ShopItemImage
import java.text.NumberFormat
import java.util.Locale
import com.dandi.nyummy.common.presentation.R as CommonR

/** 구매 확인 시트(피그마 `Shop · 21`). 산 상품 그림, 결제할 코인, 구매 후 남는 코인, 취소·구매하기. */
@Composable
internal fun PurchaseConfirmSheet(
    items: List<ShopItemVO>,
    summary: CartSummaryVO,
    isPurchasing: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    NyummyBottomSheet(onDismissRequest = onDismiss) {
        NyummySheetTitle(text = stringResource(R.string.shop_purchase_confirm_title, summary.quantity))
        ItemThumbnails(items = items, tileSize = SheetTileSize)
        Column(verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s12)) {
            SheetAmountRow(stringResource(R.string.shop_cart_total), summary.purchasePrice)
            SheetAmountRow(stringResource(R.string.shop_cart_balance_after), summary.balanceAfter)
        }
        NyummyText(
            text = stringResource(R.string.shop_purchase_no_refund),
            style = NyummyTheme.typography.bodyS,
            color = NyummyTheme.colors.content.tertiary,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s8)) {
            NyummyButton(
                text = stringResource(R.string.shop_purchase_cancel),
                onClick = onDismiss,
                style = NyummyButtonStyle.Neutral,
                modifier = Modifier.weight(CancelWeight),
            )
            NyummyButton(
                text = stringResource(R.string.shop_purchase_confirm),
                onClick = onConfirm,
                enabled = !isPurchasing,
                modifier = Modifier.weight(ConfirmWeight),
            )
        }
    }
}

/** 코인 부족 시트(피그마 `Shop · 21b`). 모자란 코인, 결제할 코인, 내 코인, "식사 기록하기", 닫기. */
@Composable
internal fun InsufficientBalanceSheet(
    summary: CartSummaryVO,
    shortage: Int,
    onRecordMeal: () -> Unit,
    onDismiss: () -> Unit,
) {
    NyummyBottomSheet(onDismissRequest = onDismiss) {
        NyummySheetTitle(text = stringResource(R.string.shop_purchase_short_title, coins(shortage)))
        Column(verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s12)) {
            SheetAmountRow(stringResource(R.string.shop_cart_total), summary.purchasePrice)
            SheetAmountRow(stringResource(R.string.shop_cart_balance), summary.balance)
        }
        NyummyListRow(
            title = stringResource(R.string.shop_purchase_record_meal),
            subtitle = stringResource(R.string.shop_purchase_record_meal_body),
            leadingIcon = CommonR.drawable.nyummy_ic_camera,
            onClick = onRecordMeal,
        )
        NyummyButton(
            text = stringResource(R.string.shop_purchase_close),
            onClick = onDismiss,
            style = NyummyButtonStyle.Neutral,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** 구매 완료 화면(피그마 `Shop · 22`). 산 상품 그림, "아이템 n개를 샀어요", 남은 코인, 우편함 가기·상점 더 둘러보기. */
@Composable
internal fun PurchaseCompletionScreen(
    completion: ShopPurchaseCompletion,
    onGoToMailbox: () -> Unit,
    onMoreShopping: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NyummyTheme.colors.bg.canvas)
            .padding(horizontal = NyummyTheme.spacing.gutter, vertical = NyummyTheme.spacing.s24),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        ItemThumbnails(items = completion.items, tileSize = CompletionTileSize, centered = true)
        Spacer(Modifier.height(NyummyTheme.spacing.s32))
        NyummyText(
            text = stringResource(R.string.shop_purchase_done_title, completion.quantity),
            style = NyummyTheme.typography.displayL,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(NyummyTheme.spacing.s8))
        NyummyText(
            text = stringResource(R.string.shop_purchase_done_body),
            style = NyummyTheme.typography.bodyM,
            color = NyummyTheme.colors.content.secondary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(NyummyTheme.spacing.s16))
        Row(
            modifier = Modifier
                .background(NyummyTheme.colors.bg.actionNeutral, RoundedCornerShape(NyummyTheme.radius.full))
                .padding(horizontal = NyummyTheme.spacing.s12, vertical = NyummyTheme.spacing.s8),
            horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s6),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NyummyText(
                text = stringResource(R.string.shop_purchase_balance_left),
                style = NyummyTheme.typography.labelS,
                color = NyummyTheme.colors.content.tertiary,
            )
            CoinValue(value = completion.balance, iconSize = PillCoinSize)
        }
        Spacer(Modifier.weight(2f))
        NyummyButton(
            text = stringResource(R.string.shop_purchase_go_to_mailbox),
            onClick = onGoToMailbox,
            modifier = Modifier.fillMaxWidth(),
        )
        NyummyTextButton(
            text = stringResource(R.string.shop_purchase_more_shopping),
            onClick = onMoreShopping,
            tone = NyummyTextButtonTone.Neutral,
            modifier = Modifier.padding(top = NyummyTheme.spacing.s8),
        )
    }
}

/** 산(살) 상품 그림 줄. 방 배경은 칸을 채워 자른다. */
@Composable
private fun ItemThumbnails(items: List<ShopItemVO>, tileSize: Dp, centered: Boolean = false) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s12, if (centered) Alignment.CenterHorizontally else Alignment.Start),
    ) {
        items(items, key = { it.productId }) { item ->
            Box(
                modifier = Modifier
                    .size(tileSize)
                    .clip(RoundedCornerShape(NyummyTheme.radius.s))
                    .background(NyummyTheme.colors.bg.surfaceSunken),
                contentAlignment = Alignment.Center,
            ) {
                if (item.isBackground) {
                    ShopItemImage(itemId = item.itemId, contentScale = ContentScale.Crop, modifier = Modifier.size(tileSize * 0.6f, tileSize))
                } else {
                    ShopItemImage(itemId = item.itemId, modifier = Modifier.size(tileSize * ThumbnailImageFraction))
                }
            }
        }
    }
}

@Composable
private fun SheetAmountRow(label: String, value: Int) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        NyummyText(
            text = label,
            style = NyummyTheme.typography.bodyM,
            color = NyummyTheme.colors.content.secondary,
            modifier = Modifier.weight(1f),
        )
        CoinValue(value = value, iconSize = SheetCoinSize)
    }
}

@Composable
private fun CoinValue(value: Int, iconSize: Dp) {
    Row(horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s4), verticalAlignment = Alignment.CenterVertically) {
        Image(painter = painterResource(CommonR.drawable.nyummy_asset_coin), contentDescription = null, modifier = Modifier.size(iconSize))
        NyummyText(text = coins(value), style = NyummyTheme.typography.numberXs, maxLines = 1)
    }
}

private fun coins(value: Int): String = NumberFormat.getNumberInstance(Locale.KOREA).format(value)

private const val CancelWeight = 1f
private const val ConfirmWeight = 1.8f
private const val ThumbnailImageFraction = 0.75f
private val SheetTileSize = 64.dp
private val CompletionTileSize = 100.dp
private val SheetCoinSize = 18.dp
private val PillCoinSize = 22.dp
