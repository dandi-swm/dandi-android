package com.dandi.nyummy.meal.entity

/**
 * 서버가 사진에서 찾았거나 사용자가 이름으로 추가한 음식 한 줄입니다.
 *
 * @property mealFoodId 음식 줄 ID. 사용자가 새로 추가해 아직 서버에 없는 줄은 [NEW_ID]입니다.
 * @property name 음식 이름
 */
data class FoundFoodVO(
    val mealFoodId: Long = NEW_ID,
    val name: String = "",
) {
    /** 사용자가 추가해 아직 서버에 없는 줄인지 여부. */
    val isNew: Boolean
        get() = mealFoodId == NEW_ID

    companion object {
        /** 아직 서버에 없는 줄의 ID. 서버 ID는 1부터 시작한다. */
        const val NEW_ID = 0L
    }
}
