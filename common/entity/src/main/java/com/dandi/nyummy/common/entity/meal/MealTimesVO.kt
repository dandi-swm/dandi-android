package com.dandi.nyummy.common.entity.meal

/** 아침, 점심, 저녁 중 한 끼. */
enum class Meal {
    BREAKFAST,
    LUNCH,
    DINNER,
}

/**
 * 한 끼의 평소 식사 시각(KST). 정시 단위만 쓴다(시는 0~23).
 *
 * @property isSkipped 이 끼니는 안 먹는다고 고른 상태. 알림을 보내지 않는다.
 * 화면 안에서는 다시 켤 때를 위해 시각을 남겨 두지만, 서버에는 시각 없이(null) 저장된다.
 */
data class MealTimeVO(
    val hour: Int = 0,
    val isSkipped: Boolean = false,
) {
    companion object {
        val empty: MealTimeVO = MealTimeVO()
    }
}

/**
 * 평소 식사 시각. "밥 시간인데 아직 기록이 없네?" 알림을 보낼 기준이 된다.
 * 서버에 저장하고 알림도 서버가 보낸다. 온보딩에서 처음 받고 설정 화면에서 바꾸므로 common에 둔다.
 */
data class MealTimesVO(
    val breakfast: MealTimeVO = DefaultBreakfast,
    val lunch: MealTimeVO = DefaultLunch,
    val dinner: MealTimeVO = DefaultDinner,
) {
    operator fun get(meal: Meal): MealTimeVO = when (meal) {
        Meal.BREAKFAST -> breakfast
        Meal.LUNCH -> lunch
        Meal.DINNER -> dinner
    }

    fun with(meal: Meal, time: MealTimeVO): MealTimesVO = when (meal) {
        Meal.BREAKFAST -> copy(breakfast = time)
        Meal.LUNCH -> copy(lunch = time)
        Meal.DINNER -> copy(dinner = time)
    }

    /** 알림을 받을 끼니가 하나라도 있는지. 없으면 알림 권한을 묻지 않는다. */
    val hasAnyMeal: Boolean
        get() = Meal.entries.any { !get(it).isSkipped }

    /**
     * 먹는 끼니(건너뛴 끼니 제외)끼리 시각이 겹치는지. 서버는 겹치는 시각을 거절한다.
     * 같은 시각이면 식사 알림이 한 끼니만 나가기 때문이다.
     */
    val hasDuplicateHours: Boolean
        get() = Meal.entries.map(::get).filterNot { it.isSkipped }.map { it.hour }
            .let { hours -> hours.size != hours.toSet().size }

    companion object {
        /** 기본값: 오전 8시, 오후 12시, 오후 6시. 그대로 넘어가도 되도록 미리 채워 둔다. */
        val DefaultBreakfast = MealTimeVO(hour = 8)
        val DefaultLunch = MealTimeVO(hour = 12)
        val DefaultDinner = MealTimeVO(hour = 18)

        val default: MealTimesVO = MealTimesVO()
    }
}
