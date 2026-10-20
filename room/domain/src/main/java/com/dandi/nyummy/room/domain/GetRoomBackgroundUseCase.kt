package com.dandi.nyummy.room.domain

import com.dandi.nyummy.room.entity.RoomItemCategory
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * 지금 방에 깔린 배경의 아이템 id. 식사 기록 연출처럼 방 그림을 장식으로만 쓰는 화면이 부른다.
 * 배경만 필요해서 가진 아이템은 한 개씩만 읽는다. 실패해도 안내하지 않고 null 을 돌려주며, 화면은 기본 빈 방을 그린다.
 */
class GetRoomBackgroundUseCase @Inject constructor(
    private val repository: RoomRepository,
) {

    suspend operator fun invoke(): Long? = try {
        repository.getRoom(size = 1).placements
            .firstOrNull { it.category == RoomItemCategory.BACKGROUND }
            ?.itemId
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }
}
