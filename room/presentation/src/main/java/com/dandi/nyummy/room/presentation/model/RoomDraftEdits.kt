package com.dandi.nyummy.room.presentation.model

import com.dandi.nyummy.common.presentation.image.NyummyCdn
import com.dandi.nyummy.common.presentation.room.RoomCardLayout
import com.dandi.nyummy.room.entity.RoomItemCategory
import com.dandi.nyummy.room.entity.RoomPlacementVO
import com.dandi.nyummy.room.presentation.RoomBackgroundUiModel
import com.dandi.nyummy.room.presentation.RoomDraft
import com.dandi.nyummy.room.presentation.RoomPlacementUiModel
import kotlinx.collections.immutable.toImmutableList

/*
 * 꾸미는 방(RoomDraft) 한 장면을 바꾸는 규칙. 마이룸과 놓아보기가 같이 쓴다.
 * 바뀐 방을 돌려주고, 바꿀 것이 없으면 null을 돌려준다. 되돌리기 스택은 쓰는 쪽이 쌓는다.
 */

/** [roomItemId] 아이템을 ([x], [y]) 바닥 점에 기본 크기로 놓는다. 맨 앞(나중에 그린 쪽)에 놓인다. 이미 놓였으면 null. */
internal fun RoomDraft.withItemPlaced(roomItemId: Long, imageUrl: String, x: Float, y: Float): RoomDraft? {
    if (items.any { it.roomItemId == roomItemId }) return null
    val placed = RoomPlacementUiModel(
        roomItemId = roomItemId,
        imageUrl = imageUrl,
        x = x.coerceIn(0f, 1f),
        y = y.coerceIn(0f, 1f),
        scale = 1f,
    )
    return copy(items = (items + placed).toImmutableList())
}

/**
 * [roomItemId] 아이템을 새 자리와 크기로 옮긴다. 옮기거나 크기를 바꾼 아이템은 맨 앞으로 온다(가장 최근에 만진 아이템이 위에 보인다).
 * 크기는 허용 범위로 자른다. 방에 없으면 null, 자리와 크기가 그대로면 이 방을 그대로 돌려준다.
 */
internal fun RoomDraft.withItemMoved(roomItemId: Long, x: Float, y: Float, scale: Float): RoomDraft? {
    val target = items.firstOrNull { it.roomItemId == roomItemId } ?: return null
    val moved = target.copy(x = x, y = y, scale = RoomCardLayout.clampItemScale(scale))
    if (moved == target) return this
    return copy(items = (items - target + moved).toImmutableList())
}

/** [roomItemId] 아이템을 맨 앞으로. 방에 없거나 이미 맨 앞이면 null. */
internal fun RoomDraft.withItemInFront(roomItemId: Long): RoomDraft? {
    val target = items.firstOrNull { it.roomItemId == roomItemId } ?: return null
    if (items.last() == target) return null
    return copy(items = (items - target + target).toImmutableList())
}

/** [roomItemId] 아이템을 방에서 뺀다. 방에 없으면 null. */
internal fun RoomDraft.withItemRemoved(roomItemId: Long): RoomDraft? {
    if (items.none { it.roomItemId == roomItemId }) return null
    return copy(items = items.filterNot { it.roomItemId == roomItemId }.toImmutableList())
}

/** 배경을 [background]로 바꾼다(null이면 기본 빈 방). 이미 같은 배경이면 null. */
internal fun RoomDraft.withBackground(background: RoomBackgroundUiModel?): RoomDraft? {
    if (this.background?.roomItemId == background?.roomItemId) return null
    return copy(background = background)
}

/**
 * 서버의 놓인 아이템을 꾸미는 방으로 바꾼다. 배경은 하나만 쓴다. 좌표는 저장값(그림 왼쪽 아래 기준 퍼센트)을
 * 그리는 좌표로 바꾸고, 좌표와 크기는 허용 범위로 맞춘다(범위를 벗어난 값이 와도 방 안에 보이게).
 */
internal fun List<RoomPlacementVO>.toRoomDraft(): RoomDraft {
    val background = firstOrNull { it.category == RoomItemCategory.BACKGROUND }
    return RoomDraft(
        background = background?.let { RoomBackgroundUiModel(it.roomItemId, it.itemId.toItemImageUrl()) },
        items = filter { it.category != RoomItemCategory.BACKGROUND }
            .sortedBy { it.zIndex }
            .map {
                val position = RoomCardLayout.fromSavedPosition(it.x, it.y)
                RoomPlacementUiModel(
                    roomItemId = it.roomItemId,
                    imageUrl = it.itemId.toItemImageUrl(),
                    x = position.x,
                    y = position.y,
                    scale = RoomCardLayout.clampItemScale(it.scale),
                )
            }
            .toImmutableList(),
    )
}

/** 아이템 그림 주소. 아이템 id를 CDN 아이템 경로에 붙인다. id가 없으면 빈 값(받지 못한 그림처럼 보인다). */
internal fun Long?.toItemImageUrl(): String = this?.let(NyummyCdn::itemUrl).orEmpty()
