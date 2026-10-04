package com.dandi.nyummy.onboarding.data.dto

import com.dandi.nyummy.onboarding.entity.CatVO
import kotlinx.serialization.Serializable

/**
 * 고양이 이름 등록 요청입니다.
 *
 * @property name 사용자가 지어 준 고양이 이름 (앞뒤 공백 제거, 1~10자)
 */
@Serializable
data class CatRegisterRequestDTO(
    val name: String,
)

/**
 * 고양이 정보 응답입니다.
 *
 * @property catId 고양이 식별자
 * @property name 고양이 이름
 * @property createdAt 등록 시각 (ISO-8601, 화면에서 아직 쓰지 않아 VO 로 옮기지 않는다)
 */
@Serializable
data class CatDTO(
    val catId: Long? = null,
    val name: String? = null,
    val createdAt: String? = null,
) {
    fun toVO(): CatVO = CatVO(
        id = catId ?: 0L,
        name = name.orEmpty(),
    )
}
