package com.dandi.nyummy.room.domain

import com.dandi.nyummy.common.domain.base.BaseUseCase
import com.dandi.nyummy.common.domain.error.HttpResponseException
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.room.entity.OwnedRoomItemPageVO
import com.dandi.nyummy.room.entity.RoomItemCategory
import com.dandi.nyummy.tti.TTIHelper
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

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
) : BaseUseCase(resourceHelper, messageHelper, navigationHelper, ttiHelper) {

    suspend operator fun invoke(
        category: RoomItemCategory,
        cursor: Long,
        size: Int = GetRoomUseCase.PAGE_SIZE,
    ): Result<OwnedRoomItemPageVO> = try {
        Result.success(repository.getOwnedItems(category, cursor, size.coerceIn(1, GetRoomUseCase.MAX_PAGE_SIZE)))
    } catch (e: HttpResponseException) {
        handleHttpError<RoomErrorType>(e, onDomainError = {})
        Result.failure(e)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        // 네트워크 끊김 등 HTTP 응답 전 실패도 앱이 죽지 않게 실패로 돌려준다.
        Result.failure(e)
    }
}
