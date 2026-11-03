package com.dandi.nyummy.room.entity

/**
 * 놓아보기(사기 전에 내 방에 놓아 보기) 시트의 칸 하나. 아직 갖지 않은 아이템이라 [RoomPlacementVO]처럼 서버 아이템 id가 없다.
 * 같은 아이템을 여러 개 놓아 볼 수 있게 수량만큼 칸이 따로 온다.
 *
 * @property key 칸 하나를 가리키는 값. 같은 칸이면 다시 받아도 같다(예 "상품 id:순번").
 * @property itemId 아이템 종류 id. 화면이 CDN 아이템 경로에 붙여 그림 주소를 만든다.
 * @property category 아이템 종류. 배경이면 방 배경을 바꿔 보고, 아니면 방에 놓아 본다.
 * @property price 1개 결제가(코인).
 * @property isPurchasable 지금 살 수 있다. 살 수 없어도 놓아 볼 수는 있다.
 */
data class RoomTryOnItemVO(
    val key: String = "",
    val itemId: Long = 0L,
    val name: String = "",
    val category: RoomItemCategory = RoomItemCategory.ETC,
    val price: Int = 0,
    val isPurchasable: Boolean = true,
)
