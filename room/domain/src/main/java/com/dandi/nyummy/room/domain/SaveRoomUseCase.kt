package com.dandi.nyummy.room.domain

import com.dandi.nyummy.common.domain.base.BaseUseCase
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.common.domain.session.SessionExpiryHandler
import com.dandi.nyummy.room.entity.RoomPlacementVO
import com.dandi.nyummy.tti.TTIHelper
import javax.inject.Inject

/**
 * 꾸민 방 저장. 방에 놓인 아이템 전체를 [placements]로 바꾼다(배경 포함, 목록에 없는 아이템은 방에서 빠진다).
 *
 * 이제 가지고 있지 않은 아이템이 섞여 있으면 [RoomItemGoneException]으로 실패하고, 화면은 방을 다시 불러온다.
 * 그 밖의 실패는 공통 오류만 안내하고 실패를 돌려준다.
 */
class SaveRoomUseCase @Inject constructor(
    private val repository: RoomRepository,
    resourceHelper: ResourceHelper,
    messageHelper: MessageHelper,
    navigationHelper: NavigationHelper,
    ttiHelper: TTIHelper,
    sessionExpiryHandler: SessionExpiryHandler? = null,
) : BaseUseCase(resourceHelper, messageHelper, navigationHelper, ttiHelper, sessionExpiryHandler) {

    suspend operator fun invoke(placements: List<RoomPlacementVO>): Result<Unit> = request(
        RoomErrorType.entries,
        onBusinessError = { type, cause ->
            Result.failure(if (type == RoomErrorType.ROOM_ITEM_NOT_FOUND) RoomItemGoneException() else cause)
        },
    ) {
        repository.saveRoom(placements)
    }
}
