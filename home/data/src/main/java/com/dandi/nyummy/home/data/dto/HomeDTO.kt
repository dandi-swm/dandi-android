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
 * @property hasMailboxBadge 받을 수 있는 우편(받지 않았고 기한이 지나지 않은)이 하나라도 있다
 * @property equippedRoomItems 마이룸에 놓인 아이템(배경 포함). 마이룸 조회의 놓인 아이템과 같다.
 */
@Serializable
data class HomeDTO(
    val user: HomeUserDTO? = null,
    val streak: HomeStreakDTO? = null,
    val todayMealSummary: TodayMealSummaryDTO? = null,
    val hasMailboxBadge: Boolean? = null,
    val equippedRoomItems: List<HomeEquippedRoomItemDTO>? = null,
) {
    fun toVO(): HomeSummaryVO = HomeSummaryVO(
        coinBalance = user?.coin ?: 0,
        streakDays = streak?.streakDays ?: 0,
        recordsUntilNextReward = streak?.recordsUntilNextReward ?: 0,
        todayRecordedCount = todayMealSummary?.todayRecordedCount ?: 0,
        todayCalorieKcal = todayMealSummary?.todayCurrentCalory ?: 0,
        goalCalorieKcal = todayMealSummary?.todayTargetCalory ?: 0,
        todayAttemptCount = todayMealSummary?.todayAttemptCount ?: 0,
        todayMaxAttemptCount = todayMealSummary?.todayMaxAttemptCount ?: 0,
        hasMailboxBadge = hasMailboxBadge == true,
        roomPlacements = equippedRoomItems.orEmpty().map { it.toVO() }.sortedBy { it.zIndex },
        // 값이 없으면 마친 것으로 본다. 건너뛸 수 없는 튜토리얼에 기존 사용자를 가두지 않는다.
        isTutorialCompleted = user?.isTutorialCompleted ?: true,
    )
}

/**
 * @property coin 보유 코인
 * @property isTutorialCompleted 튜토리얼 식사를 기록했다(POST /api/v1/meals/tutorial 성공 뒤 true)
 */
@Serializable
data class HomeUserDTO(
    val coin: Int? = null,
    val isTutorialCompleted: Boolean? = null,
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
 * @property todayAttemptCount 오늘 식사 기록을 시도한 횟수(하루 한도에 세는 횟수)
 * @property todayMaxAttemptCount 하루에 시도할 수 있는 최대 횟수
 */
@Serializable
data class TodayMealSummaryDTO(
    val todayRecordedCount: Int? = null,
    val todayCurrentCalory: Int? = null,
    val todayTargetCalory: Int? = null,
    val todayAttemptCount: Int? = null,
    val todayMaxAttemptCount: Int? = null,
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
