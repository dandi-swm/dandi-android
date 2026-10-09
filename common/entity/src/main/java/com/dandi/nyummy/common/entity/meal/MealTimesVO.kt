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
 * @property isSkipped 이 끼니는 안 먹는다고 고른 상태. 알림을 보내지 않고, 다시 켤 때를 위해 시각은 남겨 둔다.
 */
data class MealTimeVO(
    val hour: Int = 0,
    val isSkipped: Boolean = false,
) {
    /** 자정부터 몇 분째인지. 저장할 때 쓴다. */
    val minuteOfDay: Int
        get() = hour * MinutesPerHour

    companion object {
        val empty: MealTimeVO = MealTimeVO()

        /**
         * 저장해 둔 "자정부터 몇 분째" 값을 시각으로 되돌린다. 범위를 벗어난 값은 하루 안으로 맞추고,
         * 분 단위로 저장했던 예전 값(12:30 등)은 그 시의 정시로 내린다.
         */
        fun ofMinuteOfDay(value: Int, isSkipped: Boolean = false): MealTimeVO =
            MealTimeVO(hour = value.coerceIn(0, MinutesPerDay - 1) / MinutesPerHour, isSkipped = isSkipped)

        private const val MinutesPerHour = 60
        private const val MinutesPerDay = 24 * MinutesPerHour
    }
}

/**
 * 평소 식사 시각. "밥 시간인데 아직 기록이 없네?" 알림을 보낼 기준이 된다.
 * 온보딩에서 처음 받고, 설정 화면과 식사 알림이 함께 쓰므로 common에 둔다.
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

    companion object {
        /** 기본값: 오전 8시, 오후 12시, 오후 6시. 그대로 넘어가도 되도록 미리 채워 둔다. */
        val DefaultBreakfast = MealTimeVO(hour = 8)
        val DefaultLunch = MealTimeVO(hour = 12)
        val DefaultDinner = MealTimeVO(hour = 18)

        val default: MealTimesVO = MealTimesVO()
    }
}
