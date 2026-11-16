package com.dandi.nyummy.meal.entity

/**
 * 식사를 기록한 직후 서버가 사진에서 음식을 찾은 결과입니다. 기록 화면이 다음에 보여 줄 화면을 정합니다.
 */
enum class MealRecognition {
    /** 음식을 찾았다. 찾은 음식 목록을 사용자가 확인한다. */
    CONFIRMING,

    /** 사진이 음식이 아니다. */
    NOT_FOOD,

    /** 기한 안에 찾지 못했다. 서버가 이어서 찾고, 끝나면 알림으로 알려 준다. */
    RECOGNIZING,
}
