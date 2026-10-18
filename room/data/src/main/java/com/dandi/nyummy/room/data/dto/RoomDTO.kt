package com.dandi.nyummy.room.data.dto

import com.dandi.nyummy.room.entity.OwnedRoomItemPageVO
import com.dandi.nyummy.room.entity.OwnedRoomItemVO
import com.dandi.nyummy.room.entity.RoomItemCategory
import com.dandi.nyummy.room.entity.RoomPlacementVO
import com.dandi.nyummy.room.entity.RoomVO
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * GET /api/v1/rooms 응답.
 *
 * @property categories 종류별 가진 아이템의 첫 묶음. 가진 게 없어도 모든 종류가 오고 순서는 고정이다.
 * @property equippedRoomItems 지금 방에 놓인 아이템(첫 묶음에 없는 아이템도 들어 있다). zIndex 오름차순.
 */
@Serializable
data class RoomResponseDTO(
    val categories: List<RoomItemCategoryDTO>? = null,
    val equippedRoomItems: List<EquippedRoomItemDTO>? = null,
) {
    fun toVO(): RoomVO = RoomVO(
        categories = categories.orEmpty().map { it.toVO() },
        placements = equippedRoomItems.orEmpty().map { it.toVO() }.sortedBy { it.zIndex },
    )
}

/**
 * 한 종류의 가진 아이템 첫 묶음.
 *
 * @property categoryName 종류 이름(예 "침대").
 * @property nextCursor 다음 묶음을 읽을 때 cursor로 넘길 값. 마지막 묶음이면 null.
 * @property hasNext 다음 묶음이 있다. false면 [nextCursor]는 null이다.
 */
@Serializable
data class RoomItemCategoryDTO(
    val category: String? = null,
    val categoryName: String? = null,
    val ownedRoomItems: List<OwnedRoomItemDTO>? = null,
    val nextCursor: Long? = null,
    val hasNext: Boolean? = null,
) {
    fun toVO(): OwnedRoomItemPageVO = OwnedRoomItemPageVO(
        category = RoomItemCategory.from(category),
        categoryName = categoryName?.trim().orEmpty(),
        items = ownedRoomItems.orEmpty().map { it.toVO() },
        nextCursor = nextCursor.takeIf { hasNext == true },
    )
}

/**
 * GET /api/v1/rooms/owned-items 응답(한 종류의 가진 아이템 다음 묶음).
 */
@Serializable
data class OwnedRoomItemsResponseDTO(
    val ownedRoomItems: List<OwnedRoomItemDTO>? = null,
    val nextCursor: Long? = null,
    val hasNext: Boolean? = null,
) {
    fun toVO(category: RoomItemCategory): OwnedRoomItemPageVO = OwnedRoomItemPageVO(
        category = category,
        items = ownedRoomItems.orEmpty().map { it.toVO() },
        nextCursor = nextCursor.takeIf { hasNext == true },
    )
}

/**
 * 가진 아이템 하나. 같은 아이템을 여러 개 샀으면 한 개씩 따로 온다.
 *
 * @property isEquipped 지금 방에 놓여 있다.
 */
@Serializable
data class OwnedRoomItemDTO(
    val roomItemId: Long? = null,
    val name: String? = null,
    val imageUrl: String? = null,
    val isEquipped: Boolean? = null,
) {
    fun toVO(): OwnedRoomItemVO = OwnedRoomItemVO(
        roomItemId = roomItemId ?: 0L,
        name = name?.trim().orEmpty(),
        imageUrl = imageUrl.orEmpty(),
        isEquipped = isEquipped == true,
    )
}

/**
 * 방에 놓인 아이템 하나. 좌표와 크기 규칙은 [RoomPlacementVO]에 있다.
 * 좌표가 0~100을 벗어나 오면 그 안으로 맞춘다(그림 밖에 놓인 아이템이 없게).
 *
 * 서버 응답은 `zIndex`로 오지만 Swagger 스키마에는 `zindex`도 함께 적혀 있어서 둘 다 받는다.
 */
@Serializable
data class EquippedRoomItemDTO(
    val roomItemId: Long? = null,
    val category: String? = null,
    val imageUrl: String? = null,
    val x: Float? = null,
    val y: Float? = null,
    val zIndex: Int? = null,
    @SerialName("zindex") val lowercaseZIndex: Int? = null,
    val scale: Float? = null,
) {
    fun toVO(): RoomPlacementVO = RoomPlacementVO(
        roomItemId = roomItemId ?: 0L,
        category = RoomItemCategory.from(category),
        imageUrl = imageUrl.orEmpty(),
        x = (x ?: 0f).toPosition(),
        y = (y ?: 0f).toPosition(),
        zIndex = zIndex ?: lowercaseZIndex ?: 0,
        scale = scale?.takeIf { it > 0f } ?: 1f,
    )
}

/**
 * PUT /api/v1/rooms/equipped-items 요청. 성공은 본문 없는 204다.
 * 놓인 아이템 전체를 보낸다. 목록에 없는 아이템은 방에서 빠지고 마지막 좌표는 서버가 그대로 둔다.
 */
@Serializable
data class UpdateEquippedRoomItemsRequestDTO(
    val equippedRoomItems: List<EquippedRoomItemRequestDTO>? = null,
)

/**
 * 놓인 아이템 하나의 자리. Swagger 요청 스키마에 `zIndex`와 `zindex`가 함께 있는데, 응답과 같은 `zIndex`로 보낸다.
 *
 * @property x 바닥 가운데 점의 가로 위치(그림 왼쪽 아래 기준 퍼센트, 0~100). 넘치면 그 안으로 맞춰 보낸다.
 * @property y 바닥 가운데 점의 세로 위치(그림 왼쪽 아래 기준 퍼센트, 0~100). 넘치면 그 안으로 맞춰 보낸다.
 * @property scale 기본 크기 대비 배율. 0보다 커야 한다.
 */
@Serializable
data class EquippedRoomItemRequestDTO(
    val roomItemId: Long? = null,
    val x: Float? = null,
    val y: Float? = null,
    val zIndex: Int? = null,
    val scale: Float? = null,
) {
    companion object {
        fun from(placement: RoomPlacementVO) = EquippedRoomItemRequestDTO(
            roomItemId = placement.roomItemId,
            x = placement.x.toPosition(),
            y = placement.y.toPosition(),
            zIndex = placement.zIndex,
            scale = placement.scale,
        )
    }
}

/** 좌표를 0~100 퍼센트 안으로 맞춘다. */
private fun Float.toPosition(): Float = coerceIn(0f, RoomPlacementVO.MAX_POSITION)
