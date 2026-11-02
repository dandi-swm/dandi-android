package com.dandi.nyummy.shop.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyCoinPill
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTabs
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyText
import com.dandi.nyummy.common.presentation.designsystem.foundation.nyummyClickable
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.shop.entity.ShopTab
import com.dandi.nyummy.shop.presentation.R
import kotlinx.collections.immutable.toImmutableList
import java.text.NumberFormat
import java.util.Locale
import com.dandi.nyummy.common.presentation.R as CommonR

/**
 * 상점 머리(피그마 `v2 / Shop / Header`). "상점" 제목, 장바구니 버튼(배지), 코인 칩("+" 없음)과 세 탭.
 * 탭 아래 내용은 탭마다 따로 그린다.
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
) {
    val tabLabels = ShopTab.entries.map { stringResource(it.labelRes) }.toImmutableList()
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
            onSelect = { onSelectTab(ShopTab.entries[it]) },
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
    Box(
        modifier = modifier
            .size(CartButtonSize)
            .semantics { contentDescription = description }
            .nyummyClickable(onClick = onClick),
    ) {
        Icon(
            painter = painterResource(CommonR.drawable.nyummy_ic_cart),
            contentDescription = null,
            tint = NyummyTheme.colors.content.primary,
            modifier = Modifier
                .align(Alignment.Center)
                .size(NyummyTheme.size.iconL),
        )
        if (count > 0) {
            val shape = RoundedCornerShape(NyummyTheme.radius.full)
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = BadgeOffsetX, y = BadgeOffsetY)
                    .height(BadgeSize)
                    .defaultMinSize(minWidth = BadgeSize)
                    .background(NyummyTheme.colors.bg.actionDanger, shape)
                    .border(NyummyTheme.borderWidth.bold, NyummyTheme.colors.bg.surface, shape)
                    .padding(horizontal = NyummyTheme.spacing.s4),
                contentAlignment = Alignment.Center,
            ) {
                NyummyText(
                    text = count.toString(),
                    style = NyummyTheme.typography.labelXs,
                    color = NyummyTheme.colors.content.onAction,
                    maxLines = 1,
                )
            }
        }
    }
}

private val ShopTab.labelRes: Int
    get() = when (this) {
        ShopTab.RECOMMEND -> R.string.shop_tab_recommend
        ShopTab.INTERIOR -> R.string.shop_tab_interior
        ShopTab.BACKGROUND -> R.string.shop_tab_background
    }

private val CartButtonSize = 40.dp
private val BadgeSize = 18.dp
private val BadgeOffsetX = 22.dp
private val BadgeOffsetY = 3.dp

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun ShopHeaderPreview() {
    NyummyTheme {
        Column {
            ShopHeader(1240, 2, ShopTab.RECOMMEND, {}, {}, {})
            ShopHeader(0, 0, ShopTab.INTERIOR, {}, {}, {})
        }
    }
}
