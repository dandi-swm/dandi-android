package com.dandi.nyummy.home.data.dto

import com.dandi.nyummy.home.entity.HomeSummaryVO
import com.dandi.nyummy.room.entity.RoomItemCategory
import com.dandi.nyummy.room.entity.RoomPlacementVO
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * GET /api/v1/home 응답.
 *
 * @property user 사용자 지갑 정보
 * @property streak 연속 기록 현황
 * @property todayMealSummary 오늘의 식사 현황
 * @property hasUnreadInquiryAnswer 문의에 답장이 왔는데 아직 열어 보지 않은 것이 있다
 * @property equippedRoomItems 마이룸에 놓인 아이템(배경 포함). 마이룸 조회의 놓인 아이템과 같다.
 */
@Serializable
data class HomeDTO(
    val user: HomeUserDTO? = null,
    val streak: HomeStreakDTO? = null,
    val todayMealSummary: TodayMealSummaryDTO? = null,
    val hasUnreadInquiryAnswer: Boolean? = null,
    val equippedRoomItems: List<HomeEquippedRoomItemDTO>? = null,
) {
    fun toVO(): HomeSummaryVO = HomeSummaryVO(
        coinBalance = user?.coin ?: 0,
        streakDays = streak?.streakDays ?: 0,
        recordsUntilNextReward = streak?.recordsUntilNextReward ?: 0,
        todayRecordedCount = todayMealSummary?.todayRecordedCount ?: 0,
        todayCalorieKcal = todayMealSummary?.todayCurrentCalory ?: 0,
        goalCalorieKcal = todayMealSummary?.todayTargetCalory ?: 0,
        hasUnreadInquiryAnswer = hasUnreadInquiryAnswer == true,
        roomPlacements = equippedRoomItems.orEmpty().map { it.toVO() }.sortedBy { it.zIndex },
    )
}

/** @property coin 보유 코인 */
@Serializable
data class HomeUserDTO(
    val coin: Int? = null,
)

/**
 * @property streakDays 연속으로 기록한 일수
 * @property recordsUntilNextReward 다음 보상까지 남은 기록 횟수
 */
@Serializable
data class HomeStreakDTO(
    val streakDays: Int? = null,
    val recordsUntilNextReward: Int? = null,
)

/**
 * 서버 필드명은 calorie가 아니라 calory다.
 *
 * @property todayRecordedCount 오늘 기록한 끼니 수
 * @property todayCurrentCalory 오늘 섭취 열량(kcal)
 * @property todayTargetCalory 하루 목표 열량(kcal)
 */
@Serializable
data class TodayMealSummaryDTO(
    val todayRecordedCount: Int? = null,
    val todayCurrentCalory: Int? = null,
    val todayTargetCalory: Int? = null,
)

/**
 * 마이룸에 놓인 아이템 하나. 좌표와 크기 규칙은 [RoomPlacementVO]에 있다.
 * 좌표가 0~100을 벗어나 오면 그 안으로 맞춘다(그림 밖에 놓인 아이템이 없게).
 *
 * 서버 응답은 `zIndex`로 오지만 Swagger 스키마에는 `zindex`도 함께 적혀 있어서 둘 다 받는다.
 *
 * @property ownedRoomItemId 산 아이템 한 개의 id. 같은 아이템을 여러 개 사도 개마다 다르다. 앱은 roomItemId로 부른다.
 * @property itemId 아이템 종류 id. 그림은 주소 대신 이 id로 받는다(`items/{itemId}.png`).
 */
@Serializable
data class HomeEquippedRoomItemDTO(
    val ownedRoomItemId: Long? = null,
    val category: String? = null,
    val itemId: Long? = null,
    val x: Float? = null,
    val y: Float? = null,
    val zIndex: Int? = null,
    @SerialName("zindex") val lowercaseZIndex: Int? = null,
    val scale: Float? = null,
) {
    fun toVO(): RoomPlacementVO = RoomPlacementVO(
        roomItemId = ownedRoomItemId ?: 0L,
        category = RoomItemCategory.from(category),
        itemId = itemId,
        x = (x ?: 0f).coerceIn(0f, RoomPlacementVO.MAX_POSITION),
        y = (y ?: 0f).coerceIn(0f, RoomPlacementVO.MAX_POSITION),
        zIndex = zIndex ?: lowercaseZIndex ?: 0,
        scale = scale?.takeIf { it > 0f } ?: 1f,
    )
}
