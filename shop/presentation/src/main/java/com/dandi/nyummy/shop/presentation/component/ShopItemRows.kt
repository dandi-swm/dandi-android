package com.dandi.nyummy.shop.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dandi.nyummy.common.entity.tag.TagTone
import com.dandi.nyummy.common.entity.tag.TagVO
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyButton
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyButtonSize
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyButtonStyle
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTag
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyText
import com.dandi.nyummy.common.presentation.designsystem.foundation.nyummyClickable
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.shop.entity.ShopItemVO
import com.dandi.nyummy.shop.presentation.R

/**
 * 상품 한 줄(피그마 `v2 / Shop / Item Row`). 그림 칸 + 이름(+태그)·분류 + 오른쪽 [trailing](기본은 가격).
 * 추천 탭 목록, 순위 줄, 프로모션 줄이 같이 쓴다.
 */
@Composable
fun ShopItemRow(
    item: ShopItemVO,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: @Composable RowScope.() -> Unit = { ShopItemPrice(item) },
) {
    Row(
        modifier = modifier
            .nyummyClickable(onClick = onClick)
            .padding(vertical = NyummyTheme.spacing.s8),
        horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(RowTileSize)
                .clip(RoundedCornerShape(NyummyTheme.radius.s))
                .background(NyummyTheme.colors.bg.surfaceSunken),
            contentAlignment = Alignment.Center,
        ) {
            ShopItemImage(itemId = item.itemId, modifier = Modifier.size(RowImageSize))
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s2),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s6),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NyummyText(
                    text = item.name,
                    style = NyummyTheme.typography.titleS,
                    maxLines = 1,
                    modifier = Modifier.weight(1f, fill = false),
                )
                shopItemTag(item)?.let { NyummyTag(tag = it) }
            }
            NyummyText(
                text = item.categoryName,
                style = NyummyTheme.typography.labelS,
                color = NyummyTheme.colors.content.tertiary,
                maxLines = 1,
            )
        }
        trailing()
    }
}

/** 인기 순위 줄(피그마 `v2 / Shop / Rank Row`). [rank]는 1부터이고, 1~3위는 브랜드 색 숫자다. */
@Composable
fun ShopRankRow(
    rank: Int,
    item: ShopItemVO,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s6),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NyummyText(
            text = rank.toString(),
            style = NyummyTheme.typography.numberL,
            color = if (rank <= HighlightedRanks) NyummyTheme.colors.content.brand else NyummyTheme.colors.content.tertiary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.width(RankWidth),
        )
        ShopItemRow(item = item, onClick = onClick, modifier = Modifier.weight(1f))
    }
}

/**
 * 프로모션 상품 줄. 오른쪽에 가격과 그 아래 "담기" 버튼(늘 1개)을 둔다. 보유한 방 배경은 담기 버튼이 없다.
 */
@Composable
fun ShopPromotionItemRow(
    item: ShopItemVO,
    onClick: () -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ShopItemRow(item = item, onClick = onClick, modifier = modifier) {
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s6),
        ) {
            ShopItemPrice(item)
            if (!(item.isBackground && item.isOwned)) {
                NyummyButton(
                    text = stringResource(R.string.shop_item_add),
                    onClick = onAdd,
                    style = NyummyButtonStyle.Tonal,
                    size = NyummyButtonSize.S,
                )
            }
        }
    }
}

private val RowTileSize = 64.dp
private val RowImageSize = 52.dp
private val RankWidth = 24.dp
private const val HighlightedRanks = 3

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun ShopItemRowsPreview() {
    NyummyTheme {
        val sofa = ShopItemVO(productId = 1, itemId = 1, name = "크림 소파", categoryName = "침대", price = PreviewPrice)
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = NyummyTheme.spacing.s20)) {
            ShopItemRow(item = sofa.copy(tag = TagVO("한정", TagTone.PROMO)), onClick = {}, modifier = Modifier.fillMaxWidth())
            ShopItemRow(item = sofa.copy(price = PreviewSalePrice), onClick = {}, modifier = Modifier.fillMaxWidth())
            ShopItemRow(item = sofa.copy(ownedCount = 1), onClick = {}, modifier = Modifier.fillMaxWidth())
            ShopRankRow(rank = 1, item = sofa, onClick = {}, modifier = Modifier.fillMaxWidth())
            ShopRankRow(rank = 4, item = sofa, onClick = {}, modifier = Modifier.fillMaxWidth())
            ShopPromotionItemRow(item = sofa.copy(price = PreviewSalePrice), onClick = {}, onAdd = {}, modifier = Modifier.fillMaxWidth())
        }
    }
}
