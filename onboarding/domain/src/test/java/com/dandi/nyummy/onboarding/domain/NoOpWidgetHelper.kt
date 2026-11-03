package com.dandi.nyummy.onboarding.domain

import com.dandi.nyummy.common.domain.helper.WidgetHelper
import com.dandi.nyummy.common.entity.meal.MealTimesVO

/** 기기 홈 화면 위젯에 값을 넘기지 않는 가짜. 위젯 호출을 확인하지 않는 테스트에서 쓴다. */
internal object NoOpWidgetHelper : WidgetHelper {
    override suspend fun onMealRecorded() = Unit

    override suspend fun onHomeSummary(streakDays: Int, todayRecordedCount: Int) = Unit

    override suspend fun onMealTimesSaved(mealTimes: MealTimesVO) = Unit

    override suspend fun clear() = Unit
}
