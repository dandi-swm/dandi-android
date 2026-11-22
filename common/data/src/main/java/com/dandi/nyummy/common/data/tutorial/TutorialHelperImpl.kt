package com.dandi.nyummy.common.data.tutorial

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.dandi.nyummy.common.data.BaseLocalDataSource
import com.dandi.nyummy.common.domain.helper.TutorialHelper
import com.dandi.nyummy.common.entity.tutorial.TutorialStep
import kotlin.coroutines.cancellation.CancellationException

/**
 * 앱 전역 환경설정 DataStore("app_prefs")에 튜토리얼 진행 상태를 남기는 [TutorialHelper] 구현.
 * 남은 단계는 [TutorialStep] 이름 집합으로, 식사 튜토리얼 여부는 boolean 하나로 둔다. 모르는 이름은 버린다.
 *
 * 쓰기가 실패해도(저장 공간 부족, 파일 손상 등) 예외를 던지지 않고 로그만 남긴다. 화면들이 `viewModelScope.launch`에서
 * 바로 부르므로 예외가 나가면 앱이 꺼진다. 튜토리얼 기록은 실패해도 화면 흐름을 이어 가면 되고,
 * 최악은 마친 안내가 다음에 한 번 더 뜨는 것이다. 코루틴 취소는 다시 던진다. 읽기 실패는 [BaseLocalDataSource]가 빈 값으로 돌린다.
 */
class TutorialHelperImpl(
    dataStore: DataStore<Preferences>,
) : BaseLocalDataSource(dataStore), TutorialHelper {

    override suspend fun pendingSteps(): Set<TutorialStep> =
        read(KEY_PENDING_STEPS).orEmpty()
            .mapNotNull { name -> TutorialStep.entries.firstOrNull { it.name == name } }
            .toSet()

    override suspend fun startAfterMeal() = safely("startAfterMeal") {
        write(KEY_PENDING_STEPS, TutorialStep.entries.map { it.name }.toSet())
    }

    override suspend fun complete(step: TutorialStep) = safely("complete($step)") {
        editAtomically { prefs -> prefs[KEY_PENDING_STEPS] = prefs[KEY_PENDING_STEPS].orEmpty() - step.name }
    }

    override suspend fun skipToHealthStory() = safely("skipToHealthStory") {
        write(KEY_PENDING_STEPS, setOf(TutorialStep.HEALTH_STORY.name))
    }

    override suspend fun finish() = safely("finish") {
        remove(KEY_PENDING_STEPS)
    }

    override suspend fun clear() = safely("clear") {
        editAtomically { prefs ->
            prefs.remove(KEY_PENDING_STEPS)
            prefs.remove(KEY_MEAL_REQUIRED)
        }
    }

    override suspend fun isMealTutorialRequired(): Boolean = read(KEY_MEAL_REQUIRED) == true

    override suspend fun setMealTutorialRequired(required: Boolean) = safely("setMealTutorialRequired") {
        write(KEY_MEAL_REQUIRED, required)
    }

    /** 튜토리얼 기록 쓰기. 실패하면 로그만 남기고 넘어간다. 취소는 다시 던진다. */
    private suspend inline fun safely(action: String, block: () -> Unit) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "튜토리얼 기록 실패: $action", e)
        }
    }

    private companion object {
        const val TAG = "TutorialHelper"
        val KEY_PENDING_STEPS = stringSetPreferencesKey("tutorial_pending_steps")
        val KEY_MEAL_REQUIRED = booleanPreferencesKey("tutorial_meal_required")
    }
}
