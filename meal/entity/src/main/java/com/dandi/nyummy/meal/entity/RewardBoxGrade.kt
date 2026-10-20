package com.dandi.nyummy.meal.entity

/**
 * 식사를 기록하면 우편함으로 오는 보상 상자의 등급입니다.
 *
 * 서버 응답에 등급이 없거나 모르는 값이면 [NONE]이고, 이때는 상자 연출을 하지 않습니다.
 */
enum class RewardBoxGrade {
    NONE,
    BRONZE,
    SILVER,
    GOLD,
    ;

    companion object {
        /** 서버 문자열(BRONZE/SILVER/GOLD)을 등급으로 바꿉니다. 모르는 값은 [NONE]입니다. */
        fun from(value: String?): RewardBoxGrade =
            entries.firstOrNull { it != NONE && it.name == value } ?: NONE
    }
}
