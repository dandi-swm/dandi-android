package com.dandi.nyummy.settings.entity

import com.dandi.nyummy.auth.entity.Gender
import com.dandi.nyummy.common.entity.meal.MealTimesVO
import java.time.LocalDate
import java.time.Period

/**
 * 내 프로필(집사 정보). 서버는 성별·생년월일·키·몸무게로 하루 권장 칼로리를 계산한다.
 * 회원가입에서 처음 받지만 서버가 비워 둘 수 있어, 비어 있으면 null이다.
 *
 * @property nickname 집사 이름.
 * @property birth 생년월일. 시각이 없는 날짜라 타임존을 바꾸지 않는다.
 * @property mealTimes 평소 식사 시각. 같은 프로필에 들어 있어 함께 읽지만, 바꾸는 곳은 설정의 식사 시간이다.
 */
data class MyProfileVO(
    val nickname: String = "",
    val gender: Gender? = null,
    val birth: LocalDate? = null,
    val heightCm: Int? = null,
    val weightKg: Int? = null,
    val mealTimes: MealTimesVO = MealTimesVO.default,
) {
    /** [today] 기준 만 나이. 생년월일을 모르면 null이다. */
    fun ageOn(today: LocalDate): Int? = birth?.let { Period.between(it, today).years.coerceAtLeast(0) }

    companion object {
        val empty = MyProfileVO()
    }
}

/**
 * 내 프로필에서 바꿀 항목. null인 항목은 그대로 둔다.
 * 서버에도 바꾼 항목만 보낸다. 마이페이지는 시트 하나에서 항목 하나씩 바꾼다.
 */
data class MyProfileChange(
    val nickname: String? = null,
    val gender: Gender? = null,
    val birth: LocalDate? = null,
    val heightCm: Int? = null,
    val weightKg: Int? = null,
) {
    /** 바꾸는 항목이 하나도 없다. */
    val isEmpty: Boolean
        get() = this == MyProfileChange()

    /** [profile]에 이 변경을 적용한다. */
    fun applyTo(profile: MyProfileVO): MyProfileVO = profile.copy(
        nickname = nickname ?: profile.nickname,
        gender = gender ?: profile.gender,
        birth = birth ?: profile.birth,
        heightCm = heightCm ?: profile.heightCm,
        weightKg = weightKg ?: profile.weightKg,
    )
}
