package com.dandi.nyummy.shop.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.dandi.nyummy.common.entity.richText.RichTextVO
import com.dandi.nyummy.common.entity.tag.TagTone
import com.dandi.nyummy.common.entity.tag.TagVO
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyIconButton
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyIconButtonSize
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyIconButtonStyle
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyRichText
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTag
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyText
import com.dandi.nyummy.common.presentation.designsystem.foundation.nyummyClickable
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.common.presentation.image.NyummyCdn
import com.dandi.nyummy.shop.entity.ShopItemVO
import com.dandi.nyummy.shop.presentation.R
import com.dandi.nyummy.common.presentation.R as CommonR

/**
 * 인테리어 상품 카드(피그마 `v2 / Shop / Item Card`). 정사각 무대 위에 그림, 왼쪽 위 태그, 오른쪽 위 담기 버튼을 두고
 * 아래에 이름 한 줄과 서버 가격을 보인다. 너비는 부르는 쪽(3열 격자)이 정한다.
 *
 * @param onAdd 담기 버튼을 누를 때. null이면 담기 버튼을 그리지 않는다(추천 탭)
 */
@Composable
fun ShopItemCard(
    item: ShopItemVO,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onAdd: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.nyummyClickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s8),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(NyummyTheme.radius.m))
                .background(NyummyTheme.colors.bg.surfaceSunken),
        ) {
            ShopItemImage(
                itemId = item.itemId,
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxSize(ItemImageFraction),
            )
            // 태그와 담기 버튼을 한 줄에 둔다. 버튼 자리를 먼저 잡고 태그는 남은 너비 안에서 말줄임한다(좁은 카드·큰 글자).
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(NyummyTheme.spacing.s8),
                horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s4),
                verticalAlignment = Alignment.Top,
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    shopItemTag(item)?.let { tag -> NyummyTag(tag = tag) }
                }
                if (onAdd != null) {
                    ShopAddButton(onClick = onAdd)
                }
            }
        }
        ShopCardInfo(item)
    }
}

/**
 * 방 배경 상품 카드(피그마 `v2 / Shop / Background Card`). 배경과 같은 9:16 그림에 왼쪽 위 태그, 오른쪽 아래 담기
 * 버튼을 둔다. 보유한 방 배경은 "보유" 태그와 "보유 중"만 보이고 담기 버튼이 없다(한 사람이 하나만 가진다).
 */
@Composable
fun ShopBackgroundCard(
    item: ShopItemVO,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onAdd: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(NyummyTheme.radius.s)
    Column(
        modifier = modifier.nyummyClickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s8),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(BackgroundAspectRatio)
                .clip(shape)
                .border(NyummyTheme.borderWidth.hairline, NyummyTheme.colors.border.subtle, shape),
        ) {
            ShopItemImage(
                itemId = item.itemId,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            shopItemTag(item)?.let { tag ->
                NyummyTag(tag = tag, modifier = Modifier.align(Alignment.TopStart).padding(NyummyTheme.spacing.s6))
            }
            if (onAdd != null && !item.isOwned) {
                ShopAddButton(onClick = onAdd, modifier = Modifier.align(Alignment.BottomEnd).padding(NyummyTheme.spacing.s6))
            }
        }
        ShopCardInfo(item)
    }
}

/** 카드 아래 이름 한 줄과 가격. */
@Composable
private fun ShopCardInfo(item: ShopItemVO) {
    Column(verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s2)) {
        NyummyText(text = item.name, style = NyummyTheme.typography.labelM, maxLines = 1)
        // 카드는 좁아서 할인 가격(할인율·결제가·취소선 정가)이 한 줄에 다 안 들어가면 잘리지 않고 다음 줄로 넘긴다.
        ShopItemPrice(item, maxLines = CardPriceMaxLines)
    }
}

/**
 * 상품 가격 자리. 서버가 꾸민 가격을 그대로 그리고, 보유한 방 배경만 가격 대신 "보유 중"을 보인다.
 * 인테리어는 여러 개 가질 수 있어 보유해도 가격을 보인다.
 */
@Composable
fun ShopItemPrice(item: ShopItemVO, modifier: Modifier = Modifier, maxLines: Int = 1) {
    if (item.isBackground && item.isOwned) {
        NyummyText(
            text = stringResource(R.string.shop_item_owned_price),
            style = NyummyTheme.typography.labelS,
            color = NyummyTheme.colors.content.brand,
            modifier = modifier,
            maxLines = 1,
        )
    } else {
        NyummyRichText(richText = item.price, modifier = modifier, maxLines = maxLines)
    }
}

/**
 * 카드·줄의 태그 자리. 보유가 서버 태그보다 앞선다: 방 배경은 "보유", 인테리어는 "n개 보유". 보유하지 않았으면 서버가
 * 고른 태그(한정·할인·신규) 그대로, 없으면 null이다.
 */
@Composable
fun shopItemTag(item: ShopItemVO): TagVO? = when {
    item.isBackground && item.isOwned -> TagVO(stringResource(R.string.shop_item_owned), TagTone.SUCCESS)
    item.isOwned -> TagVO(stringResource(R.string.shop_item_owned_count, item.ownedCount), TagTone.SUCCESS)
    else -> item.tag
}

/** 장바구니에 1개 바로 담는 버튼(피그마 `v2 / Shop / Add Button`). 흰 원 위 브랜드 색 장바구니. */
@Composable
fun ShopAddButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    NyummyIconButton(
        icon = CommonR.drawable.nyummy_ic_cart,
        contentDescription = stringResource(R.string.shop_item_add_to_cart),
        onClick = onClick,
        modifier = modifier,
        style = NyummyIconButtonStyle.Outlined,
        size = NyummyIconButtonSize.Xs,
        tint = NyummyTheme.colors.content.brand,
    )
}

/** 상품 그림(`items/{itemId}.png`). 픽셀 그림이라 보간 없이 그린다. */
@Composable
fun ShopItemImage(
    itemId: Long,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    contentDescription: String? = null,
) {
    AsyncImage(
        model = NyummyCdn.itemUrl(itemId),
        contentDescription = contentDescription,
        contentScale = contentScale,
        alignment = if (contentScale == ContentScale.Crop) Alignment.BottomCenter else Alignment.Center,
        filterQuality = FilterQuality.None,
        modifier = modifier,
    )
}

/** 정사각 무대 안 그림 크기. 피그마에서 사방 11.11%를 띄운다. */
private const val ItemImageFraction = 0.7778f

/** 방 배경 그림 비율(9:16). */
private const val BackgroundAspectRatio = 9f / 16f

private const val CardPriceMaxLines = 2

internal val PreviewPrice = listOf(RichTextVO(icon = "coin"), RichTextVO("900", "numberS", "contentPrimary"))
internal val PreviewSalePrice = listOf(
    RichTextVO("20%", "numberXs", "contentDanger"),
    RichTextVO(icon = "coin"),
    RichTextVO("720", "numberXs", "contentPrimary"),
    RichTextVO("900", "labelS", "contentTertiary", strike = true),
)

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun ShopItemCardPreview() {
    NyummyTheme {
        Row(
            modifier = Modifier.padding(NyummyTheme.spacing.s16),
            horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s12),
        ) {
            val sofa = ShopItemVO(productId = 1, itemId = 1, name = "크림 소파", category = "BED", price = PreviewPrice)
            ShopItemCard(item = sofa, onClick = {}, onAdd = {}, modifier = Modifier.width(108.dp))
            ShopItemCard(
                item = sofa.copy(price = PreviewSalePrice, tag = TagVO("20%", TagTone.PROMO)),
                onClick = {},
                onAdd = {},
                modifier = Modifier.width(108.dp),
            )
            ShopItemCard(item = sofa.copy(ownedCount = 2, tag = TagVO("한정", TagTone.PROMO)), onClick = {}, modifier = Modifier.width(108.dp))
        }
    }
}

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun ShopBackgroundCardPreview() {
    NyummyTheme {
        Row(
            modifier = Modifier.padding(NyummyTheme.spacing.s16),
            horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s12),
        ) {
            val room = ShopItemVO(
                productId = 2,
                itemId = 2,
                name = "밤하늘 방",
                category = ShopItemVO.CATEGORY_BACKGROUND,
                price = PreviewPrice,
                tag = TagVO("한정", TagTone.PROMO),
            )
            ShopBackgroundCard(item = room, onClick = {}, onAdd = {}, modifier = Modifier.width(108.dp))
            ShopBackgroundCard(item = room.copy(ownedCount = 1), onClick = {}, onAdd = {}, modifier = Modifier.width(108.dp))
        }
    }
}
