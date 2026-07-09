package com.swm.dandi.meal.entity

/**
 * 이전 음식 선택 화면에 필요한 음식 후보 목록.
 *
 * @property previousFoods 최근성, 빈도, 서버 랭킹 기준으로 정렬된 이전 음식 후보.
 */
data class PreviousMealPageVO(
    val previousFoods: List<PreviousFoodVO> = emptyList(),
) {
    companion object {
        val empty: PreviousMealPageVO = PreviousMealPageVO()
    }
}

/**
 * 사용자가 과거에 기록한 음식 후보.
 *
 * `recordCountLabel`, `lastRecordedLabel` 같은 표시 문구는 VO에 두지 않고 presentation에서 만든다.
 *
 * @property foodHistoryId 이전 음식 후보를 식별하는 서버 id.
 * @property displayName 사용자에게 보여줄 음식 표시명.
 * @property iconImageUrl 이전 음식 아이콘 이미지 URL. 없으면 앱 기본 아이콘을 사용할 수 있다.
 * @property lastRecordedAt 마지막으로 이 음식을 기록한 시각.
 * @property recordCount 사용자가 이 음식을 기록한 누적 횟수.
 * @property rankScore 서버가 목록 정렬에 사용한 점수. 화면 문구로 직접 표시하지 않는다.
 */
data class PreviousFoodVO(
    val foodHistoryId: String = "",
    val displayName: String = "",
    val iconImageUrl: String = "",
    val lastRecordedAt: String = "",
    val recordCount: Int = 0,
    val rankScore: Double = 0.0,
) {
    companion object {
        val empty: PreviousFoodVO = PreviousFoodVO()
    }
}
