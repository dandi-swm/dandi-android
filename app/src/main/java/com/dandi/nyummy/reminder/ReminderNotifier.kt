package com.dandi.nyummy.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.dandi.nyummy.R
import com.dandi.nyummy.main.presentation.MainActivity

/**
 * 냐미 리마인드 알림 표시기 (임시 구현).
 *
 * 무드별 냐미 이미지를 큰 아이콘/큰 그림으로 붙여 듀오링고식 감정 넛지를 만든다.
 * 탭하면 App Link 딥링크([DEFAULT_DEEP_LINK])로 MainActivity 에 진입해
 * 기존 콜드/웜 딥링크 플로우(synthetic stack)를 그대로 탄다.
 */
object ReminderNotifier {

    const val CHANNEL_ID = "nyami_reminder"
    private const val NOTIFICATION_ID = 1001
    private const val DEFAULT_DEEP_LINK = "https://www.dandi.com/meal/record"

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
     * 무드 알림을 표시한다. [title]/[body] 가 null 이면 무드별 기본 문구를 쓴다.
     * POST_NOTIFICATIONS 미허용(API 33+)이면 조용히 무시한다.
     */
    fun notify(
        context: Context,
        mood: NyamiMood,
        title: String? = null,
        body: String? = null,
        deepLink: String? = null,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        ensureChannel(context)

        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse(deepLink ?: DEFAULT_DEEP_LINK),
                context,
                MainActivity::class.java,
            ),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val moodBitmap = BitmapFactory.decodeResource(context.resources, mood.imageRes)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_nyami)
            .setLargeIcon(moodBitmap)
            .setContentTitle(title ?: context.getString(mood.notiTitleRes))
            .setContentText(body ?: context.getString(mood.notiBodyRes))
            .setStyle(
                NotificationCompat.BigPictureStyle()
                    .bigPicture(moodBitmap)
                    .bigLargeIcon(null as Bitmap?),
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }
}
