package com.dandi.nyummy.shop.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import com.dandi.nyummy.common.entity.tag.TagTone
import com.dandi.nyummy.common.entity.tag.TagVO
import com.dandi.nyummy.common.presentation.R as CommonR
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyCoinPill
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyIconButton
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyIconButtonSize
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTabs
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyText
import com.dandi.nyummy.common.presentation.designsystem.foundation.nyummyClickable
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.shop.entity.ShopTab
import com.dandi.nyummy.shop.presentation.R
import com.dandi.nyummy.shop.presentation.ShopCoinChargeTab
import kotlinx.collections.immutable.toImmutableList
import java.text.NumberFormat
import java.util.Locale

/**
 * 상점 머리(피그마 `v2 / Shop / Header`). "상점" 제목, 장바구니 버튼(배지), 코인 칩("+" 없음)과 세 탭.
 * 탭 아래 내용은 탭마다 따로 그린다. [coinChargeTab]이 있으면 탭 줄 끝에 "코인" 탭(코인 충전 이벤트)을 더한다.
 * 이 탭은 누르면 [onCoinChargeTabClick]만 부르고 선택되지 않는다.
 */
@Composable
fun ShopHeader(
    balance: Int,
    cartItemCount: Int,
    selectedTab: ShopTab,
    onSelectTab: (ShopTab) -> Unit,
    onCartClick: () -> Unit,
    onCoinsClick: () -> Unit,
    modifier: Modifier = Modifier,
    coinChargeTab: ShopCoinChargeTab? = null,
    onCoinChargeTabClick: () -> Unit = {},
) {
    val coinLabel = stringResource(R.string.shop_tab_coin)
    val eventChip = TagVO(stringResource(R.string.shop_tab_event_badge), TagTone.PROMO)
    val tabLabels = (ShopTab.entries.map { stringResource(it.labelRes) } + listOfNotNull(coinLabel.takeIf { coinChargeTab != null }))
        .toImmutableList()
    val tabBadges = (ShopTab.entries.map { null } + listOfNotNull(coinChargeTab?.let { if (it.showsEventChip) eventChip else null }))
        .toImmutableList()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(NyummyTheme.colors.bg.surface)
            .padding(top = NyummyTheme.spacing.s8)
            .padding(horizontal = NyummyTheme.spacing.gutter),
        verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s8),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            NyummyText(
                text = stringResource(R.string.shop_title),
                style = NyummyTheme.typography.displayL,
                modifier = Modifier.weight(1f),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s4),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ShopCartButton(count = cartItemCount, onClick = onCartClick)
                NyummyCoinPill(
                    coins = NumberFormat.getNumberInstance(Locale.KOREA).format(balance),
                    modifier = Modifier.nyummyClickable(onClick = onCoinsClick),
                )
            }
        }
        NyummyTabs(
            tabs = tabLabels,
            selectedIndex = selectedTab.ordinal,
            onSelect = { index -> ShopTab.entries.getOrNull(index)?.let(onSelectTab) ?: onCoinChargeTabClick() },
            badges = tabBadges,
        )
    }
}

/** 장바구니 버튼(피그마 `v2 / Shop / Cart Button`). [count]는 상품 종류 수이고 0이면 배지가 없다. */
@Composable
fun ShopCartButton(count: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val description = if (count > 0) {
        stringResource(R.string.shop_cart_description_count, count)
    } else {
        stringResource(R.string.shop_cart_description)
    }
    NyummyIconButton(
        icon = CommonR.drawable.nyummy_ic_cart,
        contentDescription = description,
        onClick = onClick,
        modifier = modifier,
        size = NyummyIconButtonSize.M,
        badgeCount = count,
    )
}

private val ShopTab.labelRes: Int
    get() = when (this) {
        ShopTab.RECOMMEND -> R.string.shop_tab_recommend
        ShopTab.INTERIOR -> R.string.shop_tab_interior
        ShopTab.BACKGROUND -> R.string.shop_tab_background
    }


@Preview(showBackground = true, widthDp = 390)
@Composable
private fun ShopHeaderPreview() {
    NyummyTheme {
        Column {
            ShopHeader(1240, 2, ShopTab.RECOMMEND, {}, {}, {})
            ShopHeader(0, 0, ShopTab.INTERIOR, {}, {}, {})
            ShopHeader(1240, 0, ShopTab.RECOMMEND, {}, {}, {}, coinChargeTab = ShopCoinChargeTab(showsEventChip = true))
        }
    }
}
