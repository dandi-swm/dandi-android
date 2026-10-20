package com.dandi.nyummy.room.domain

import com.dandi.nyummy.common.domain.base.BaseUseCase
import com.dandi.nyummy.common.domain.error.HttpResponseException
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.room.entity.RoomPlacementVO
import com.dandi.nyummy.tti.TTIHelper
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

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
) : BaseUseCase(resourceHelper, messageHelper, navigationHelper, ttiHelper) {

    suspend operator fun invoke(placements: List<RoomPlacementVO>): Result<Unit> = try {
        Result.success(repository.saveRoom(placements))
    } catch (e: HttpResponseException) {
        var itemGone = false
        handleHttpError<RoomErrorType>(
            e,
            onDomainError = { if (it == RoomErrorType.ROOM_ITEM_NOT_FOUND) itemGone = true },
        )
        Result.failure(if (itemGone) RoomItemGoneException() else e)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        // 네트워크 끊김 등 HTTP 응답 전 실패도 앱이 죽지 않게 실패로 돌려준다.
        Result.failure(e)
    }
}
