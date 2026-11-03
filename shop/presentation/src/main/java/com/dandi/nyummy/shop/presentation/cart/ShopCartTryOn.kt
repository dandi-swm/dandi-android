package com.dandi.nyummy.shop.presentation.cart

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dandi.nyummy.common.presentation.R as CommonR
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyText
import com.dandi.nyummy.common.presentation.designsystem.foundation.nyummyClickable
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.room.entity.RoomItemCategory
import com.dandi.nyummy.room.entity.RoomTryOnItemVO
import com.dandi.nyummy.shop.entity.CartItemVO
import com.dandi.nyummy.shop.presentation.R
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

/**
 * 장바구니 위에 겹쳐 그릴 놓아보기 화면. 장바구니는 놓아보기를 모르고 이 자리만 두며, `main`이 마이룸의 놓아보기 화면을 꽂는다
 * (feature 화면끼리 서로 의존하지 않는다).
 *
 * - `items`: 담은 아이템 칸(수량만큼)
 * - `footer`: 놓아보기 시트 맨 아래에 그릴 장바구니 결제 바
 * - `onClose`: 놓아보기를 닫고 장바구니로 돌아간다
 */
typealias ShopTryOnContent = @Composable (
    items: ImmutableList<RoomTryOnItemVO>,
    footer: @Composable () -> Unit,
    onClose: () -> Unit,
) -> Unit

/**
 * 장바구니 항목을 놓아보기 칸으로 바꾼다. 체크 여부와 상관없이 모두, 수량만큼(최소 1) 펼치고 장바구니 순서를 그대로 둔다.
 * 칸 키는 "상품 id:순번"이라 수량이 줄면 마지막 칸부터 없어진다.
 */
internal fun List<CartItemVO>.toTryOnItems(): ImmutableList<RoomTryOnItemVO> = flatMap { item ->
    List(item.quantity.coerceAtLeast(1)) { index ->
        RoomTryOnItemVO(
            key = "${item.shopItem.productId}:$index",
            itemId = item.shopItem.itemId,
            name = item.shopItem.name,
            category = RoomItemCategory.from(item.shopItem.category),
            price = item.purchasePrice,
            isPurchasable = item.canPurchase,
        )
    }
}.toImmutableList()

/** 장바구니 맨 위 "내 방에 놓아 보기" 카드(피그마 `Shop · 20c`). 누르면 놓아보기가 장바구니 위를 덮는다. */
@Composable
internal fun TryOnEntryCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(NyummyTheme.radius.l)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(NyummyTheme.colors.bg.brandSubtle, shape)
            .nyummyClickable(onClick = onClick)
            .semantics(mergeDescendants = true) { role = Role.Button }
            .padding(start = EntryStartPadding, end = NyummyTheme.spacing.s12, top = NyummyTheme.spacing.s12, bottom = NyummyTheme.spacing.s12),
        horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(EntryIconCircle)
                .background(NyummyTheme.colors.bg.surface, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(CommonR.drawable.nyummy_nav_my_room),
                contentDescription = null,
                tint = NyummyTheme.colors.content.brand,
                modifier = Modifier.size(EntryIconSize),
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(EntryTextGap),
        ) {
            NyummyText(text = stringResource(R.string.shop_cart_try_on_title), style = NyummyTheme.typography.labelM, maxLines = 1)
            NyummyText(
                text = stringResource(R.string.shop_cart_try_on_body),
                style = NyummyTheme.typography.bodyS,
                color = NyummyTheme.colors.content.secondary,
                maxLines = 1,
            )
        }
        Icon(
            painter = painterResource(CommonR.drawable.nyummy_ic_chevron_right),
            contentDescription = null,
            tint = NyummyTheme.colors.content.tertiary,
            modifier = Modifier.size(EntryChevronSize),
        )
    }
}

private val EntryStartPadding = 14.dp
private val EntryIconCircle = 40.dp
private val EntryIconSize = 22.dp
private val EntryChevronSize = 20.dp
private val EntryTextGap = 2.dp

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun TryOnEntryCardPreview() {
    NyummyTheme {
        TryOnEntryCard(
            onClick = {},
            modifier = Modifier.padding(horizontal = NyummyTheme.spacing.gutter, vertical = NyummyTheme.spacing.s16),
        )
    }
}
