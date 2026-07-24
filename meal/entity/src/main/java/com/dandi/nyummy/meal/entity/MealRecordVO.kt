package com.dandi.nyummy.meal.entity

import kotlinx.serialization.Serializable

/**
 * 식사 기록 한 건의 값 객체입니다.
 *
 * 기본 생성자 값이 곧 "아무것도 입력되지 않은 빈 기록"을 의미하며,
 * 식사 기록 화면의 초기 상태로 그대로 사용됩니다.
 *
 * [capturedAt] 은 서버가 관리하는 촬영 시각 timestamp 문자열로, 첨부된 사진의
 * 메타데이터에서 채워집니다. 타입 변환·정규화는 data 레이어의 DTO→VO 변환에서 처리합니다.
 */
@Serializable
data class MealRecordVO(
    val photoUri: String = "",
    val description: String = "",
    val capturedAt: String = "",
    val foodIconId: String = "",
) {
    val hasPhoto: Boolean get() = photoUri.isNotEmpty()

    val hasCapturedAt: Boolean get() = capturedAt.isNotEmpty()

    companion object {
        val empty = MealRecordVO()
    }
}
