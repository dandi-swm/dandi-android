package com.dandi.nyummy.widget.presentation

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context

/**
 * 다음에 상태가 바뀌는 순간 위젯을 다시 그리도록 알람 하나를 건다.
 * 화면이 꺼져 있으면 켜질 때 전달되는 비깨우기 알람이라 정확한 알람 권한이 필요 없다.
 * `set()`은 시스템이 오차를 30분 넘게 잡아서(에뮬레이터에서 +35분 확인), 오차 10분짜리 [AlarmManager.setWindow]를 쓴다.
 */
internal object NyamiWidgetScheduler {

    private const val REQUEST_CODE = 0
    // 경계 순간 바로 뒤에 그리도록 조금 늦춘다.
    private const val SLACK_MILLIS = 1_000L
    private const val WINDOW_MILLIS = 10 * 60 * 1_000L

    fun schedule(context: Context, atMillis: Long) {
        if (atMillis <= 0L) return
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        alarmManager.setWindow(AlarmManager.RTC, atMillis + SLACK_MILLIS, WINDOW_MILLIS, refreshPendingIntent(context))
    }

    fun cancel(context: Context) {
        context.getSystemService(AlarmManager::class.java)?.cancel(refreshPendingIntent(context))
    }

    private fun refreshPendingIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_CODE,
        NyamiWidgetReceiver.refreshIntent(context),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
}
