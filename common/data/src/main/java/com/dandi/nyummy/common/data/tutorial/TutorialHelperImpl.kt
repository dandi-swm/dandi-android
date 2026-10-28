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
 * 남은 단계는 [TutorialStep] 이름 집합으로, 식사 튜토리얼 여부는 boolean 하나로 둔다. 모르는 이름은 버린다.
 */
class TutorialHelperImpl(
    dataStore: DataStore<Preferences>,
) : BaseLocalDataSource(dataStore), TutorialHelper {

    override suspend fun pendingSteps(): Set<TutorialStep> =
        read(KEY_PENDING_STEPS).orEmpty()
            .mapNotNull { name -> TutorialStep.entries.firstOrNull { it.name == name } }
            .toSet()

    override suspend fun startAfterMeal() {
        write(KEY_PENDING_STEPS, TutorialStep.entries.map { it.name }.toSet())
    }

    override suspend fun complete(step: TutorialStep) {
        editAtomically { prefs -> prefs[KEY_PENDING_STEPS] = prefs[KEY_PENDING_STEPS].orEmpty() - step.name }
    }

    override suspend fun skipToHealthStory() {
        write(KEY_PENDING_STEPS, setOf(TutorialStep.HEALTH_STORY.name))
    }

    override suspend fun finish() {
        remove(KEY_PENDING_STEPS)
    }

    override suspend fun clear() {
        editAtomically { prefs ->
            prefs.remove(KEY_PENDING_STEPS)
            prefs.remove(KEY_MEAL_REQUIRED)
        }
    }

    override suspend fun isMealTutorialRequired(): Boolean = read(KEY_MEAL_REQUIRED) == true

    override suspend fun setMealTutorialRequired(required: Boolean) {
        write(KEY_MEAL_REQUIRED, required)
    }

    private companion object {
        val KEY_PENDING_STEPS = stringSetPreferencesKey("tutorial_pending_steps")
        val KEY_MEAL_REQUIRED = booleanPreferencesKey("tutorial_meal_required")
    }
}
