package com.dandi.nyummy.shop.presentation.cart

import com.dandi.nyummy.room.entity.RoomItemCategory
import com.dandi.nyummy.shop.entity.CartItemVO
import com.dandi.nyummy.shop.presentation.shopItem
import org.junit.Assert.assertEquals
import org.junit.Test

class ShopCartTryOnTest {

    @Test
    fun `수량만큼 칸을 펼치고 장바구니 순서와 1개 결제가를 그대로 둔다`() {
        val items = listOf(
            CartItemVO(shopItem(7), quantity = 2, purchasePrice = 150, canPurchase = true, isSelected = true),
            CartItemVO(shopItem(9, category = "BACKGROUND"), quantity = 1, purchasePrice = 1_000, canPurchase = true, isSelected = true),
        ).toTryOnItems()

        assertEquals(listOf("7:0", "7:1", "9:0"), items.map { it.key })
        assertEquals(listOf(107L, 107L, 109L), items.map { it.itemId })
        assertEquals(listOf(150, 150, 1_000), items.map { it.price })
        assertEquals(RoomItemCategory("BED"), items[0].category)
        assertEquals(RoomItemCategory.BACKGROUND, items[2].category)
        assertEquals("상품 7", items[0].name)
    }

    @Test
    fun `체크를 풀었거나 살 수 없는 아이템도 칸으로 넣고 수량이 0이어도 한 칸은 둔다`() {
        val items = listOf(
            CartItemVO(shopItem(3), quantity = 1, purchasePrice = 200, canPurchase = true, isSelected = false),
            CartItemVO(shopItem(5, category = "RUG"), quantity = 0, purchasePrice = 400, canPurchase = false, isSelected = false),
        ).toTryOnItems()

        assertEquals(listOf("3:0", "5:0"), items.map { it.key })
        assertEquals(listOf(true, false), items.map { it.isPurchasable })
    }
}
