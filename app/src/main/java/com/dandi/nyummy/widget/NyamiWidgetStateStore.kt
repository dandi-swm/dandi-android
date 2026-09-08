package com.dandi.nyummy.widget

import android.content.Context
import androidx.core.content.edit
import com.dandi.nyummy.reminder.NyamiMood

/** 위젯이 그릴 냐미 상태 스냅샷. [message] 는 무드 기본 문구 대신 쓸 서버 문구(없으면 null). */
data class NyamiWidgetSnapshot(
    val mood: NyamiMood,
    val message: String?,
)

/**
 * 냐미 위젯 상태 저장소 (임시 구현 — SharedPreferences).
 *
 * 우선순위:
 * 1. 마지막 식사 기록 이후 FCM 으로 내려온 무드([savePushedMood]) — 서버 판단을 그대로 보여준다.
 * 2. 마지막 기록 경과 시간 기반 계산([NyamiMood.fromElapsed]).
 *
 * 식사 기록 제출이 연동되면 완료 지점에서 [markRecorded] 를 호출해 냐미를 다시 행복하게 만든다.
 */
object NyamiWidgetStateStore {

    private const val PREFS_NAME = "nyami_widget_state"
    private const val KEY_LAST_RECORDED_AT = "last_recorded_at"
    private const val KEY_PUSHED_MOOD = "pushed_mood"
    private const val KEY_PUSHED_MESSAGE = "pushed_message"
    private const val KEY_PUSHED_AT = "pushed_at"

    fun snapshot(context: Context, nowMillis: Long = System.currentTimeMillis()): NyamiWidgetSnapshot {
        val prefs = prefs(context)
        val lastRecordedAt = prefs.getLong(KEY_LAST_RECORDED_AT, 0L)
        val pushedAt = prefs.getLong(KEY_PUSHED_AT, 0L)
        val pushedMood = NyamiMood.fromKeyOrNull(prefs.getString(KEY_PUSHED_MOOD, null))

        if (pushedMood != null && pushedAt > lastRecordedAt) {
            return NyamiWidgetSnapshot(pushedMood, prefs.getString(KEY_PUSHED_MESSAGE, null))
        }
        val elapsed = if (lastRecordedAt > 0L) nowMillis - lastRecordedAt else null
        return NyamiWidgetSnapshot(NyamiMood.fromElapsed(elapsed), message = null)
    }

    /** 마지막 기록 후 경과 시간(ms). 기록이 없으면 null. */
    fun elapsedSinceRecordedOrNull(context: Context): Long? {
        val lastRecordedAt = prefs(context).getLong(KEY_LAST_RECORDED_AT, 0L)
        return if (lastRecordedAt > 0L) System.currentTimeMillis() - lastRecordedAt else null
    }

    /** FCM 이 내려준 무드를 저장한다. 다음 [markRecorded] 전까지 위젯에 우선 적용된다. */
    fun savePushedMood(context: Context, mood: NyamiMood, message: String?) {
        prefs(context).edit {
            putString(KEY_PUSHED_MOOD, mood.name)
            putString(KEY_PUSHED_MESSAGE, message)
            putLong(KEY_PUSHED_AT, System.currentTimeMillis())
        }
    }

    /** 식사 기록 완료 시각을 남기고 푸시 무드를 무효화한다. 제출 플로우 연동 지점에서 호출할 것. */
    fun markRecorded(context: Context, nowMillis: Long = System.currentTimeMillis()) {
        prefs(context).edit {
            putLong(KEY_LAST_RECORDED_AT, nowMillis)
            remove(KEY_PUSHED_MOOD)
            remove(KEY_PUSHED_MESSAGE)
            remove(KEY_PUSHED_AT)
        }
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
