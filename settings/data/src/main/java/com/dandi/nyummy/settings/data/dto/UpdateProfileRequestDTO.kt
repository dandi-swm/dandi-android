package com.dandi.nyummy.settings.data.dto

import com.dandi.nyummy.settings.entity.MyProfileChange
import kotlinx.serialization.Serializable

/**
 * 내 프로필 변경 요청(`PATCH /api/v1/users/me`)입니다. 바꾸는 항목만 담고, 빠진 항목은 서버가 그대로 둔다.
 * 그래서 한 번 넣은 값은 비울 수 없다.
 *
 * @property nickname 집사 이름. 공백만으로는 보낼 수 없다(최대 100자).
 * @property birth 생년월일(`yyyy-MM-dd`). 시각이 없는 날짜라 타임존을 바꾸지 않는다.
 * @property gender 성별(`MALE`/`FEMALE`/`OTHER`)
 * @property height 키(cm)
 * @property weight 몸무게(kg)
 */
@Serializable
data class UpdateProfileRequestDTO(
    val nickname: String? = null,
    val birth: String? = null,
    val gender: String? = null,
    val height: Int? = null,
    val weight: Int? = null,
) {
    companion object {
        fun from(change: MyProfileChange) = UpdateProfileRequestDTO(
            nickname = change.nickname,
            birth = change.birth?.toString(),
            gender = change.gender?.name,
            height = change.heightCm,
            weight = change.weightKg,
        )
    }
}
