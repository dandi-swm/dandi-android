package com.dandi.nyummy.room.entity

import kotlinx.serialization.Serializable

/**
 * 마이룸 아이템 종류. 서버가 주는 종류 값(예 "BED")을 그대로 담아서, 서버에 종류가 늘면 보관함 탭도 따라 는다.
 * 앱이 따로 다루는 종류는 배경뿐이다. 배경은 방에 하나만 깔고, 나머지(인테리어)는 자유롭게 여러 개 놓는다.
 *
 * @property key 서버가 주는 종류 값. 보유 아이템 목록을 읽을 때 그대로 넘긴다.
 */
@Serializable
@JvmInline
value class RoomItemCategory(val key: String) {

    companion object {
        val BACKGROUND = RoomItemCategory("BACKGROUND")

        /** 종류 값이 오지 않은 아이템을 담는 기타. */
        val ETC = RoomItemCategory("ETC")

        fun from(raw: String?): RoomItemCategory = raw?.trim()?.takeIf { it.isNotEmpty() }?.let(::RoomItemCategory) ?: ETC
    }
}

/**
 * 가진 아이템 하나. 같은 아이템을 여러 개 샀으면 한 개씩 따로 온다([roomItemId]가 산 아이템 한 개를 가리킨다).
 *
 * @property itemId 아이템 종류 id. 화면이 CDN 아이템 경로에 붙여 그림 주소를 만든다. 없으면 null.
 * @property isEquipped 지금 방에 놓여 있다(배경이면 깔려 있다).
 */
data class OwnedRoomItemVO(
    val roomItemId: Long = 0L,
    val itemId: Long? = null,
    val name: String = "",
    val isEquipped: Boolean = false,
)

/**
 * 한 종류의 가진 아이템 한 묶음. 놓인 아이템이 먼저 오고, 같으면 최근에 산 순서다.
 *
 * @property categoryName 서버가 주는 종류 이름(예 "침대").
 * @property nextCursor 다음 묶음을 읽을 때 넘길 값. 마지막 묶음이면 null.
 */
data class OwnedRoomItemPageVO(
    val category: RoomItemCategory = RoomItemCategory.ETC,
    val categoryName: String = "",
    val items: List<OwnedRoomItemVO> = emptyList(),
    val nextCursor: Long? = null,
) {
    val hasNext: Boolean
        get() = nextCursor != null
}

/**
 * 방에 놓인 아이템 하나의 자리.
 *
 * 좌표와 크기는 iOS와 같은 규칙으로 저장한다.
 * - [x], [y]: 아이템 바닥 가운데 점의 위치. 배경 그림의 왼쪽 아래를 원점으로, 그림 전체 폭(x)과 높이(y) 대비
 *   퍼센트다. 늘 0~[MAX_POSITION] 사이이고, x는 오른쪽으로 y는 위로 커진다.
 *   화면마다 방이 잘리는 모양이 달라도 그림 위 같은 자리에 놓인다. 배경은 (0, 0)으로 둔다.
 * - [scale]: 기본 크기 대비 배율(1.0이 기본). 기본 크기는 아이템 그림의 긴 변이 방 폭의 일정 비율이 되는 크기다.
 * - [zIndex]: 작을수록 뒤에 그린다. 배경이 0이고 인테리어는 1부터 놓은 순서대로다.
 * - [itemId]: 아이템 종류 id. 화면이 CDN 아이템 경로에 붙여 그림 주소를 만든다. 없으면 null.
 */
@Serializable
data class RoomPlacementVO(
    val roomItemId: Long = 0L,
    val category: RoomItemCategory = RoomItemCategory.ETC,
    val itemId: Long? = null,
    val x: Float = 0f,
    val y: Float = 0f,
    val zIndex: Int = 0,
    val scale: Float = 1f,
) {
    companion object {
        /** [x], [y]의 최댓값(그림 오른쪽 끝, 위 끝). 최솟값은 0이다. */
        const val MAX_POSITION = 100f
    }
}

/**
 * 마이룸 화면을 처음 그리는 데 필요한 값.
 *
 * @property categories 종류별 가진 아이템의 첫 묶음. 가진 게 없어도 모든 종류가 빈 목록으로 오고, 이 순서가 보관함 탭 순서다.
 * @property placements 지금 방에 놓인 아이템(첫 묶음에 없는 아이템도 들어 있다). [RoomPlacementVO.zIndex] 오름차순.
 */
data class RoomVO(
    val categories: List<OwnedRoomItemPageVO> = emptyList(),
    val placements: List<RoomPlacementVO> = emptyList(),
)
