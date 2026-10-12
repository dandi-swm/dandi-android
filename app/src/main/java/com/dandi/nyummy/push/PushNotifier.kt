package com.dandi.nyummy.push

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.dandi.nyummy.R
import com.dandi.nyummy.main.presentation.MainActivity

/**
 * 냐미 알림 표시기. 앱이 켜져 있을 때 받은 서버 푸시를 서버가 준 제목과 본문 그대로 띄운다.
 *
 * 탭하면 서버가 준 App Link(없으면 [DEFAULT_DEEP_LINK])로 MainActivity 에 진입해
 * 기존 콜드/웜 딥링크 플로우(synthetic stack)를 그대로 탄다.
 */
object PushNotifier {

    const val CHANNEL_ID = "nyami_reminder"
    private const val DEFAULT_NOTIFICATION_ID = 1001
    private const val DEFAULT_DEEP_LINK = "https://link.nyummy.co.kr/home"

    /** 알림 채널 등록. 앱 시작 시 1회 호출 (이미 있으면 no-op). */
    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.nyami_reminder_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.nyami_reminder_channel_description)
        }
        NotificationManagerCompat.from(context).createNotificationChannel(channel)
    }

    /**
     * 알림을 표시한다. 제목과 본문이 모두 없으면 보여 줄 게 없어 띄우지 않는다.
     * POST_NOTIFICATIONS 미허용(API 33+)이면 조용히 무시한다.
     *
     * @param notificationId 서버 알림 식별자. 같은 값이면 이전 알림을 교체하고, 다르면 따로 쌓는다.
     */
    fun notify(
        context: Context,
        title: String?,
        body: String?,
        deepLink: String? = null,
        notificationId: String? = null,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        if (title == null && body == null) return

        ensureChannel(context)

        // 알림마다 링크가 다르므로 requestCode 도 알림 ID 로 나눈다. 같으면 쌓인 알림들이 마지막 링크를 같이 쓴다.
        val id = notificationId?.hashCode() ?: DEFAULT_NOTIFICATION_ID
        val contentIntent = PendingIntent.getActivity(
            context,
            id,
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse(deepLink ?: DEFAULT_DEEP_LINK),
                context,
                MainActivity::class.java,
            ),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_nyami)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(id, notification)
    }
}
