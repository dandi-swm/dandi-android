package com.dandi.nyummy.cat.data.dto

import kotlinx.serialization.Serializable

/**
 * PATCH /api/v1/cats 요청. 바꾸는 항목만 담고, 빠진 항목은 서버가 그대로 둔다. 지금은 이름만 바꿀 수 있다.
 *
 * @property name 새 이름. 8자 이하(String 길이, 이모지는 2자 이상)이고 공백만으로는 보낼 수 없다.
 */
@Serializable
data class UpdateCatRequestDTO(
    val name: String? = null,
)
