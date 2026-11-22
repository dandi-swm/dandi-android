package com.dandi.nyummy.room.domain

import com.dandi.nyummy.common.domain.base.BaseUseCase
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.common.domain.session.SessionExpiryHandler
import com.dandi.nyummy.room.entity.OwnedRoomItemPageVO
import com.dandi.nyummy.room.entity.RoomItemCategory
import com.dandi.nyummy.tti.TTIHelper
import javax.inject.Inject

/**
 * 한 종류의 가진 아이템 다음 묶음 조회(보관함을 끝까지 내렸을 때).
 * 실패는 화면이 지금 보관함을 그대로 두므로 공통 오류만 안내한다.
 */
class GetOwnedRoomItemsUseCase @Inject constructor(
    private val repository: RoomRepository,
    resourceHelper: ResourceHelper,
    messageHelper: MessageHelper,
    navigationHelper: NavigationHelper,
    ttiHelper: TTIHelper,
    sessionExpiryHandler: SessionExpiryHandler? = null,
) : BaseUseCase(resourceHelper, messageHelper, navigationHelper, ttiHelper, sessionExpiryHandler) {

    suspend operator fun invoke(
        category: RoomItemCategory,
        cursor: Long,
        size: Int = GetRoomUseCase.PAGE_SIZE,
    ): Result<OwnedRoomItemPageVO> =
        request(RoomErrorType.entries) { repository.getOwnedItems(category, cursor, size.coerceIn(1, GetRoomUseCase.MAX_PAGE_SIZE)) }
}
