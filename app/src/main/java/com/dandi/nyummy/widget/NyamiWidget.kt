package com.dandi.nyummy.widget

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.defaultWeight
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.unit.ColorProvider
import com.dandi.nyummy.common.presentation.R as CommonR
import com.dandi.nyummy.main.presentation.MainActivity
import com.dandi.nyummy.reminder.NyamiMood

/**
 * 냐미 무드 홈/잠금화면 위젯 (임시 구현).
 *
 * [NyamiWidgetStateStore] 스냅샷으로 무드 이미지 + 한 줄 메시지를 그리고,
 * 탭하면 식사 기록 딥링크로 앱을 연다. 30분 주기(updatePeriodMillis) + FCM 수신 시 갱신.
 *
 * 메시지는 Jua 서체 비트맵으로 그린다 — Glance(RemoteViews)는 커스텀 폰트 텍스트를
 * 지원하지 않아 비트맵 렌더링이 표준 우회다.
 *
 * TODO Glance 는 앱 Compose 테마(CompositionLocal)를 못 쓰므로 임시로 무드별 색을 직접 든다.
 *      디자인 시스템 위젯 토큰이 생기면 교체할 것.
 */
class NyamiWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = NyamiWidgetStateStore.snapshot(context)
        val message = snapshot.message ?: context.getString(snapshot.mood.widgetMessageRes)
        val messageBitmap = renderJuaMessage(context, message)
        val openAppIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse(MEAL_RECORD_DEEP_LINK),
            context,
            MainActivity::class.java,
        )
        provideContent {
            NyamiWidgetContent(
                mood = snapshot.mood,
                message = message,
                messageBitmap = messageBitmap,
                openAppIntent = openAppIntent,
            )
        }
    }

    private companion object {
        const val MEAL_RECORD_DEEP_LINK = "https://www.dandi.com/meal/record"
    }
}

@Composable
private fun NyamiWidgetContent(
    mood: NyamiMood,
    message: String,
    messageBitmap: Bitmap,
    openAppIntent: Intent,
) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(mood.widgetBackground))
            .cornerRadius(20.dp)
            .clickable(actionStartActivity(openAppIntent))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            provider = ImageProvider(mood.imageRes),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
        )
        Spacer(modifier = GlanceModifier.height(6.dp))
        Image(
            provider = ImageProvider(messageBitmap),
            contentDescription = message,
            contentScale = ContentScale.Fit,
            modifier = GlanceModifier.fillMaxWidth().height(18.dp),
        )
    }
}

/** 메시지를 Jua 서체 흰색 한 줄 비트맵으로 렌더링한다 (표시 크기의 2배로 그려 선명도 확보). */
private fun renderJuaMessage(context: Context, text: String): Bitmap {
    val density = context.resources.displayMetrics.density
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = ResourcesCompat.getFont(context, CommonR.font.jua_regular)
        textSize = 14f * density * 2f
        color = android.graphics.Color.WHITE
    }
    val metrics = paint.fontMetrics
    val padding = 4f * density
    val width = (paint.measureText(text) + padding * 2).toInt().coerceAtLeast(1)
    val height = (metrics.descent - metrics.ascent + padding * 2).toInt()
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    Canvas(bitmap).drawText(text, padding, padding - metrics.ascent, paint)
    return bitmap
}

/**
 * 무드 이미지의 실제 배경색과 동일한 값 (이미지에서 샘플링, 임시 하드코딩).
 * 이미지 여백과 위젯 배경이 한 덩어리 단색으로 보이도록 정확히 일치시켜야 한다.
 */
private val NyamiMood.widgetBackground: Color
    get() = when (this) {
        NyamiMood.HAPPY -> Color(0xFF6AB228)
        NyamiMood.SAD -> Color(0xFF7E90E6)
        NyamiMood.CRYING -> Color(0xFF31B0F1)
        NyamiMood.ANGRY -> Color(0xFFED5949)
    }
