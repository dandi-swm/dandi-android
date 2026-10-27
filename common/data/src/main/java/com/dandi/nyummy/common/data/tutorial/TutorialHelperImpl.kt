package com.dandi.nyummy.common.data.tutorial

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.dandi.nyummy.common.data.BaseLocalDataSource
import com.dandi.nyummy.common.domain.helper.TutorialHelper
import com.dandi.nyummy.common.entity.tutorial.TutorialStep

/**
 * 앱 전역 환경설정 DataStore("app_prefs")에 튜토리얼 진행 상태를 남기는 [TutorialHelper] 구현.
 * 뒤 안내 대상 여부와 식사 튜토리얼 여부는 boolean 하나씩, 마친 단계는 [TutorialStep] 이름 집합으로 둔다.
 */
class TutorialHelperImpl(
    dataStore: DataStore<Preferences>,
) : BaseLocalDataSource(dataStore), TutorialHelper {

    override suspend fun isPending(step: TutorialStep): Boolean {
        val prefs = readSnapshot()
        return prefs[KEY_PENDING] == true && step.name !in prefs[KEY_DONE_STEPS].orEmpty()
    }

    override suspend fun complete(step: TutorialStep) {
        editAtomically { prefs -> prefs[KEY_DONE_STEPS] = prefs[KEY_DONE_STEPS].orEmpty() + step.name }
    }

    override suspend fun start() {
        editAtomically { prefs ->
            prefs[KEY_PENDING] = true
            prefs.remove(KEY_DONE_STEPS)
        }
    }

    override suspend fun skipAll() {
        write(KEY_PENDING, false)
    }

    override suspend fun clear() {
        editAtomically { prefs ->
            prefs.remove(KEY_PENDING)
            prefs.remove(KEY_DONE_STEPS)
            prefs.remove(KEY_MEAL_REQUIRED)
        }
    }

    override suspend fun isMealTutorialRequired(): Boolean = readSnapshot()[KEY_MEAL_REQUIRED] == true

    override suspend fun setMealTutorialRequired(required: Boolean) {
        write(KEY_MEAL_REQUIRED, required)
    }

    private companion object {
        val KEY_PENDING = booleanPreferencesKey("tutorial_pending")
        val KEY_DONE_STEPS = stringSetPreferencesKey("tutorial_done_steps")
        val KEY_MEAL_REQUIRED = booleanPreferencesKey("tutorial_meal_required")
    }
}
