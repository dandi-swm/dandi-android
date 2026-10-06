package com.dandi.nyummy.common.data.preference

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import com.dandi.nyummy.common.data.BaseLocalDataSource
import com.dandi.nyummy.common.entity.meal.MealTimeVO
import com.dandi.nyummy.common.entity.meal.MealTimesVO

/**
 * DataStore Preferences 기반 [AppPreferenceProvider] 구현.
 *
 * TokenProviderImpl 과 달리 동기 메모리 캐시를 두지 않는다 — 그 캐시는 suspend 를
 * 쓸 수 없는 OkHttp Interceptor 전용 우회이며, 여기는 모든 접근이 suspend 다.
 */
class AppPreferenceProviderImpl(
    dataStore: DataStore<Preferences>,
) : BaseLocalDataSource(dataStore), AppPreferenceProvider {

    override suspend fun hasShownPermissionNotice(): Boolean =
        read(KEY_PERMISSION_NOTICE_SHOWN) ?: false

    override suspend fun markPermissionNoticeShown() {
        write(KEY_PERMISSION_NOTICE_SHOWN, true)
    }

    override suspend fun isOnboardingIncomplete(): Boolean =
        read(KEY_ONBOARDING_INCOMPLETE) ?: false

    override suspend fun setOnboardingIncomplete(incomplete: Boolean) {
        write(KEY_ONBOARDING_INCOMPLETE, incomplete)
    }

    override suspend fun getMealTimes(): MealTimesVO? {
        val breakfast = readMealTime(KEY_BREAKFAST_MINUTE, KEY_BREAKFAST_SKIPPED) ?: return null
        val lunch = readMealTime(KEY_LUNCH_MINUTE, KEY_LUNCH_SKIPPED) ?: return null
        val dinner = readMealTime(KEY_DINNER_MINUTE, KEY_DINNER_SKIPPED) ?: return null
        return MealTimesVO(breakfast = breakfast, lunch = lunch, dinner = dinner)
    }

    override suspend fun setMealTimes(mealTimes: MealTimesVO) {
        // 세 끼가 함께 바뀌어야 하므로 한 트랜잭션으로 쓴다.
        editAtomically { prefs ->
            prefs[KEY_BREAKFAST_MINUTE] = mealTimes.breakfast.minuteOfDay
            prefs[KEY_BREAKFAST_SKIPPED] = mealTimes.breakfast.isSkipped
            prefs[KEY_LUNCH_MINUTE] = mealTimes.lunch.minuteOfDay
            prefs[KEY_LUNCH_SKIPPED] = mealTimes.lunch.isSkipped
            prefs[KEY_DINNER_MINUTE] = mealTimes.dinner.minuteOfDay
            prefs[KEY_DINNER_SKIPPED] = mealTimes.dinner.isSkipped
        }
    }

    private suspend fun readMealTime(
        minuteKey: Preferences.Key<Int>,
        skippedKey: Preferences.Key<Boolean>,
    ): MealTimeVO? {
        val minuteOfDay = read(minuteKey) ?: return null
        return MealTimeVO.ofMinuteOfDay(minuteOfDay, isSkipped = read(skippedKey) ?: false)
    }

    companion object {
        private val KEY_PERMISSION_NOTICE_SHOWN = booleanPreferencesKey("permission_notice_shown")
        private val KEY_ONBOARDING_INCOMPLETE = booleanPreferencesKey("onboarding_incomplete")

        // 평소 식사 시각: 자정부터 몇 분째인지와 "안 먹어요" 여부.
        private val KEY_BREAKFAST_MINUTE = intPreferencesKey("meal_time_breakfast_minute")
        private val KEY_BREAKFAST_SKIPPED = booleanPreferencesKey("meal_time_breakfast_skipped")
        private val KEY_LUNCH_MINUTE = intPreferencesKey("meal_time_lunch_minute")
        private val KEY_LUNCH_SKIPPED = booleanPreferencesKey("meal_time_lunch_skipped")
        private val KEY_DINNER_MINUTE = intPreferencesKey("meal_time_dinner_minute")
        private val KEY_DINNER_SKIPPED = booleanPreferencesKey("meal_time_dinner_skipped")
    }
}
