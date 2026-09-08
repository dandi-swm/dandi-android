package com.dandi.nyummy

import android.app.Application
import android.util.Log
import com.dandi.nyummy.reminder.FcmTokenStore
import com.dandi.nyummy.reminder.ReminderNotifier
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class DandiApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // 냐미 리마인드 알림 채널을 미리 등록한다 (FCM 백그라운드 자동 표시 대비).
        ReminderNotifier.ensureChannel(this)

        // TODO 서버 push 토큰 등록 API 연동 전까지, 테스트 발송용 토큰을 로그로만 확인한다.
        // 토큰은 재설치·데이터 삭제 때만 바뀐다. 회전 시 로그에 ROTATED 로 표시된다.
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            val rotated = FcmTokenStore.saveAndCheckRotated(this, token)
            Log.d("[NyummyFcm]", "FCM token${if (rotated) " (ROTATED)" else ""}: $token")
        }
    }
}
