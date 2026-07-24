package com.dandi.nyummy.history.entity

import kotlinx.serialization.Serializable

/**
 * 하루에 기록된 식사 1건입니다.
 *
 * @property id 식사 기록 식별자
 * @property name 음식 이름 (예: "치킨 샐러드")
 * @property photoUrl 촬영 사진 URL. 백엔드 연동 전에는 빈 값입니다.
 * @property foodIconId 음식 픽셀 아이콘 식별자 (예: "salad")
 * @property recordedAtMillis 촬영 시각 epoch millis. 표시 포맷("08:10")은 presentation 에서 파생합니다.
 * @property calorieKcal 이 식사의 열량(kcal)
 * @property carbohydrateGram 이 식사의 탄수화물(g)
 * @property proteinGram 이 식사의 단백질(g)
 * @property fatGram 이 식사의 지방(g)
 * @property orderIndex 하루 안에서의 순서, 1부터 시작 ("첫 끼" 라벨용)
 */
@Serializable
data class MealHistoryVO(
    val id: String = "",
    val name: String = "",
    val photoUrl: String = "",
    val foodIconId: String = "",
    val recordedAtMillis: Long = 0L,
    val calorieKcal: Int = 0,
    val carbohydrateGram: Int = 0,
    val proteinGram: Int = 0,
    val fatGram: Int = 0,
    val orderIndex: Int = 0,
) {

    companion object {
        val empty = MealHistoryVO()
    }
}
