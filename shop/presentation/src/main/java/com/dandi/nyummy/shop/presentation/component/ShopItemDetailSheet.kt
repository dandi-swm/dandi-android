package com.dandi.nyummy.shop.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyBottomSheet
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyButton
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTag
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyText
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.shop.entity.ShopItemVO
import com.dandi.nyummy.shop.presentation.R

/**
 * 상품 상세 시트(피그마 `Shop · 10 상품 상세`). 그림·보유 태그·이름·분류·1개 가격과 "장바구니에 담기" 버튼만 있다.
 * 수량 선택은 없고 늘 1개를 담는다. 보유한 방 배경은 더 가질 수 없어 버튼을 끈다.
 */
@Composable
fun ShopItemDetailSheet(
    item: ShopItemVO,
    isAdding: Boolean,
    onAdd: () -> Unit,
    onDismiss: () -> Unit,
) {
    NyummyBottomSheet(onDismissRequest = onDismiss) {
        ShopItemDetailContent(item = item, isAdding = isAdding, onAdd = onAdd)
    }
}

/** 시트 안 내용. 창 없이 그려 프리뷰에서 쓴다. */
@Composable
internal fun ColumnScope.ShopItemDetailContent(item: ShopItemVO, isAdding: Boolean, onAdd: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(StageHeight)
            .clip(RoundedCornerShape(NyummyTheme.radius.m))
            .background(NyummyTheme.colors.bg.surfaceSunken),
    ) {
        if (item.isBackground) {
            ShopItemImage(
                itemId = item.itemId,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(width = BackgroundImageWidth, height = StageHeight),
            )
        } else {
            ShopItemImage(itemId = item.itemId, modifier = Modifier.align(Alignment.Center).size(ImageSize))
        }
        shopItemTag(item)?.let { tag ->
            NyummyTag(tag = tag, modifier = Modifier.align(Alignment.TopStart).padding(NyummyTheme.spacing.s12))
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s2)) {
        NyummyText(text = item.name, style = NyummyTheme.typography.titleM)
        NyummyText(
            text = item.categoryName,
            style = NyummyTheme.typography.bodyS,
            color = NyummyTheme.colors.content.tertiary,
        )
    }
    ShopItemPrice(item)
    NyummyButton(
        text = stringResource(R.string.shop_item_add_to_cart),
        onClick = onAdd,
        enabled = !isAdding && !(item.isBackground && item.isOwned),
        modifier = Modifier.fillMaxWidth(),
    )
}

private val StageHeight = 220.dp
private val ImageSize = 176.dp

/** 방 배경은 9:16 그림을 무대 높이에 맞춘다. */
private val BackgroundImageWidth = StageHeight * 9f / 16f

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun ShopItemDetailContentPreview() {
    NyummyTheme {
        Column(
            modifier = Modifier.padding(NyummyTheme.spacing.gutter),
            verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s16),
        ) {
            ShopItemDetailContent(
                item = ShopItemVO(productId = 1, itemId = 2, name = "발바닥 방석", categoryName = "침대", price = PreviewPrice, ownedCount = 1),
                isAdding = false,
                onAdd = {},
            )
        }
    }
}
