package com.dandi.nyummy.onboarding.entity

/**
 * 사용자가 온보딩에서 이름을 지어 준 고양이.
 *
 * @property id 서버가 발급한 고양이 식별자
 * @property name 사용자가 지어 준 이름
 */
data class CatVO(
    val id: Long = 0L,
    val name: String = "",
) {
    companion object {
        val empty: CatVO = CatVO()
    }
}
