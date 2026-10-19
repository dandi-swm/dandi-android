package com.dandi.nyummy.home.presentation

import com.dandi.nyummy.common.presentation.image.NyummyCdn
import com.dandi.nyummy.common.presentation.room.RoomCardLayout
import com.dandi.nyummy.common.presentation.room.RoomSceneItem
import com.dandi.nyummy.room.entity.RoomItemCategory
import com.dandi.nyummy.room.entity.RoomPlacementVO
import kotlinx.collections.immutable.toImmutableList

/**
 * 서버의 놓인 아이템을 고양이방 카드에 그릴 꾸민 방으로 바꾼다. 마이룸 편집 화면과 같은 규칙이다.
 * 배경은 하나만 쓰고, 좌표는 저장값(그림 왼쪽 아래 기준 퍼센트)을 그리는 좌표로, 크기는 허용 범위로 맞춘다.
 * 그림은 아이템 id를 CDN 아이템 경로에 붙여 받고, id가 없는 아이템은 그릴 수 없어서 뺀다.
 */
internal fun List<RoomPlacementVO>.toHomeRoom(): HomeRoomUiModel {
    val background = firstOrNull { it.category == RoomItemCategory.BACKGROUND }
    return HomeRoomUiModel(
        backgroundUrl = background?.itemId?.let(NyummyCdn::itemUrl),
        items = filter { it.category != RoomItemCategory.BACKGROUND }
            .sortedBy { it.zIndex }
            .mapNotNull { placement ->
                val itemId = placement.itemId ?: return@mapNotNull null
                val position = RoomCardLayout.fromSavedPosition(placement.x, placement.y)
                RoomSceneItem(
                    roomItemId = placement.roomItemId,
                    imageUrl = NyummyCdn.itemUrl(itemId),
                    x = position.x,
                    y = position.y,
                    scale = RoomCardLayout.clampItemScale(placement.scale),
                )
            }
            .toImmutableList(),
    )
}
