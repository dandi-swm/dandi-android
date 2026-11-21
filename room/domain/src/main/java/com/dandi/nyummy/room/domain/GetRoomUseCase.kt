package com.dandi.nyummy.room.domain

import com.dandi.nyummy.common.domain.base.BaseUseCase
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.common.domain.session.SessionExpiryHandler
import com.dandi.nyummy.room.entity.RoomVO
import com.dandi.nyummy.tti.TTIHelper
import javax.inject.Inject

/**
 * 마이룸을 처음 그리는 데 필요한 값(종류별 가진 아이템 첫 묶음, 놓인 아이템) 조회.
 * 실패하면 공통 오류만 안내하고 실패를 돌려준다. 화면은 다시 불러오기를 보여 준다.
 */
class GetRoomUseCase @Inject constructor(
    private val repository: RoomRepository,
    resourceHelper: ResourceHelper,
    messageHelper: MessageHelper,
    navigationHelper: NavigationHelper,
    ttiHelper: TTIHelper,
    sessionExpiryHandler: SessionExpiryHandler? = null,
) : BaseUseCase(resourceHelper, messageHelper, navigationHelper, ttiHelper, sessionExpiryHandler) {

    suspend operator fun invoke(size: Int = PAGE_SIZE): Result<RoomVO> =
        request(RoomErrorType.entries) { repository.getRoom(size.coerceIn(1, MAX_PAGE_SIZE)) }

    companion object {
        /** 종류마다 처음 읽는 아이템 수. 보관함은 처음부터 거의 다 보이도록 서버 최대치로 읽는다. */
        const val PAGE_SIZE = 50

        /** 서버가 한 번에 주는 최대 아이템 수. */
        const val MAX_PAGE_SIZE = 50
    }
}
