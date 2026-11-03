package com.dandi.nyummy.onboarding.domain

import com.dandi.nyummy.common.domain.helper.WidgetHelper
import com.dandi.nyummy.common.entity.meal.MealTimesVO

/** 기기 홈 화면 위젯에 넘긴 값을 부른 순서대로 남기는 가짜. [onCall]은 부르는 그 순간에 실행된다(순서 확인용). */
internal class RecordingWidgetHelper(
    private val onCall: (WidgetCall) -> Unit = {},
) : WidgetHelper {
    val calls = mutableListOf<WidgetCall>()

    override suspend fun onMealRecorded() = record(WidgetCall.MealRecorded)

    override suspend fun onHomeSummary(streakDays: Int, todayRecordedCount: Int) =
        record(WidgetCall.HomeSummary(streakDays, todayRecordedCount))

    override suspend fun onMealTimesSaved(mealTimes: MealTimesVO) = record(WidgetCall.MealTimesSaved(mealTimes))

    override suspend fun clear() = record(WidgetCall.Clear)

    private fun record(call: WidgetCall) {
        onCall(call)
        calls += call
    }
}

internal sealed interface WidgetCall {
    data object MealRecorded : WidgetCall
    data class HomeSummary(val streakDays: Int, val todayRecordedCount: Int) : WidgetCall
    data class MealTimesSaved(val mealTimes: MealTimesVO) : WidgetCall
    data object Clear : WidgetCall
}
