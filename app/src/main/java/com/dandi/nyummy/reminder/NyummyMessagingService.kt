package com.dandi.nyummy.reminder

import android.util.Log
import androidx.glance.appwidget.updateAll
import com.dandi.nyummy.widget.NyamiWidget
import com.dandi.nyummy.widget.NyamiWidgetStateStore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.runBlocking

/**
 * 냐미 리마인드 FCM 수신기 (임시 구현).
 *
 * 서버는 **data 메시지**로 보낸다(포그라운드/백그라운드 모두 onMessageReceived 로 수신).
 * 기대 payload:
 * ```json
 * {
 *   "data": {
 *     "mood": "ANGRY",                                  // HAPPY | SAD | CRYING | ANGRY
 *     "title": "냐미가 화났어요!!",                        // 생략 시 무드별 기본 문구
 *     "body": "지금 바로 식사를 기록해 주세요 🔥",           // 생략 시 무드별 기본 문구
 *     "deeplink": "https://www.dandi.com/meal/record"    // 생략 시 식사 기록 화면
 *   }
 * }
 * ```
 * notification 필드만 있는 메시지는 백그라운드에서 FCM 이 기본 채널/아이콘(manifest meta-data)로
 * 자동 표시하므로, 무드 이미지가 필요한 넛지는 반드시 data 로 보낸다.
 */
class NyummyMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        // TODO 서버 push 토큰 등록 API 연동 시 여기서 재등록한다.
        val rotated = FcmTokenStore.saveAndCheckRotated(this, token)
        Log.w(TAG, "FCM token ${if (rotated) "ROTATED" else "issued"}: $token")
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        val mood = NyamiMood.fromKeyOrNull(data["mood"])
            ?: NyamiMood.fromElapsed(NyamiWidgetStateStore.elapsedSinceRecordedOrNull(this))
        val title = data["title"] ?: message.notification?.title
        val body = data["body"] ?: message.notification?.body

        ReminderNotifier.notify(
            context = this,
            mood = mood,
            title = title,
            body = body,
            deepLink = data["deeplink"],
        )

        // 서버가 내려준 무드를 위젯에도 반영한다 (다음 식사 기록 전까지 유지).
        NyamiWidgetStateStore.savePushedMood(this, mood, data["body"])
        runBlocking { NyamiWidget().updateAll(this@NyummyMessagingService) }
    }

    private companion object {
        const val TAG = "[NyummyFcm]"
    }
}
