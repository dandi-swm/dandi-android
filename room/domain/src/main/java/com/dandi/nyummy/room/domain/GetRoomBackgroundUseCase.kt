package com.dandi.nyummy.room.domain

import com.dandi.nyummy.common.domain.error.runSuspendCatching
import com.dandi.nyummy.room.entity.RoomItemCategory
import javax.inject.Inject

/**
 * 지금 방에 깔린 배경의 아이템 id. 식사 기록 연출처럼 방 그림을 장식으로만 쓰는 화면이 부른다.
 * 배경만 필요해서 가진 아이템은 한 개씩만 읽는다. 실패해도 안내하지 않고 null 을 돌려주며, 화면은 기본 빈 방을 그린다.
 */
class GetRoomBackgroundUseCase @Inject constructor(
    private val repository: RoomRepository,
) {

    // 로그인 만료도 알리지 않는다. 식사 기록 연출 중에만 부르고, 연출 뒤 홈의 조회가 로그인 만료를 안내한다.
    suspend operator fun invoke(): Long? = runSuspendCatching {
        repository.getRoom(size = 1).placements
            .firstOrNull { it.category == RoomItemCategory.BACKGROUND }
            ?.itemId
    }.getOrNull()
}
