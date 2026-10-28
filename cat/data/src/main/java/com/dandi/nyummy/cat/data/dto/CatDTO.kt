package com.dandi.nyummy.cat.data.dto

import kotlinx.serialization.Serializable

/**
 * GET /api/v1/cats 응답. 앱은 체형만 쓴다.
 *
 * @property id 고양이 id
 * @property name 이름
 * @property weight 체형(LEAN, SLIM, NORMAL, CHUBBY, PLUMP)
 * @property weightName 체형 표시 이름
 * @property love 애정도
 * @property exp 경험치
 */
@Serializable
data class CatDTO(
    val id: Long? = null,
    val name: String? = null,
    val weight: String? = null,
    val weightName: String? = null,
    val love: Int? = null,
    val exp: Int? = null,
)
