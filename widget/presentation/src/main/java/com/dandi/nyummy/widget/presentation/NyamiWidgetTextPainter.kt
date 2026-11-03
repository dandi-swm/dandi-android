package com.dandi.nyummy.widget.presentation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.text.StaticLayout
import android.text.TextPaint
import android.util.TypedValue
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.createBitmap
import com.dandi.nyummy.common.presentation.designsystem.foundation.AccentGlyphCoverage
import kotlin.math.ceil
import kotlin.math.max
import com.dandi.nyummy.common.presentation.R as CommonR

/**
 * 위젯 문구를 까투리로 비트맵에 그린다.
 *
 * 런처는 위젯을 제한된 컨텍스트로 그려서, 레이아웃 XML에 지정한 앱 글꼴(`android:fontFamily="@font/…"`)을
 * 불러오지 않고 기기 글꼴로 보인다(2026-11-03 에뮬레이터 확인, design.md D9). 그래서 문구는 앱이 직접 그려 넣는다.
 * 문장에 까투리에 없는 글자가 있으면 앱 화면과 같이 그 문장만 Pretendard Medium으로 그린다.
 * 크기·줄 높이는 피그마 `Widget / Card`(제목 32/36, 설명 17/21, 둘 사이 4, 스트릭 15/19)를 따른다.
 */
internal object NyamiWidgetTextPainter {

    private const val TITLE_SIZE_SP = 32f
    private const val TITLE_LINE_SP = 36f
    private const val SUB_SIZE_SP = 17f
    private const val SUB_LINE_SP = 21f
    private const val TITLE_SUB_GAP_DP = 4f
    private const val STREAK_SIZE_SP = 15f
    private const val STREAK_LINE_SP = 19f

    /** 제목과 설명을 한 장에 그린다. */
    fun copy(context: Context, title: String, sub: String): Bitmap {
        val titleLayout = layout(context, title, sp(context, TITLE_SIZE_SP), sp(context, TITLE_LINE_SP))
        val subLayout = layout(context, sub, sp(context, SUB_SIZE_SP), sp(context, SUB_LINE_SP))
        val gap = dp(context, TITLE_SUB_GAP_DP)
        val width = max(titleLayout.width, subLayout.width)
        val height = titleLayout.height + gap + subLayout.height
        return draw(context, width, height) { canvas ->
            titleLayout.draw(canvas)
            canvas.translate(0f, (titleLayout.height + gap).toFloat())
            subLayout.draw(canvas)
        }
    }

    /** 스트릭 줄의 "12일"을 그린다. */
    fun streak(context: Context, text: String): Bitmap {
        val textLayout = layout(context, text, sp(context, STREAK_SIZE_SP), sp(context, STREAK_LINE_SP))
        return draw(context, textLayout.width, textLayout.height) { canvas -> textLayout.draw(canvas) }
    }

    private fun draw(context: Context, width: Int, height: Int, block: (Canvas) -> Unit): Bitmap =
        createBitmap(width.coerceAtLeast(1), height.coerceAtLeast(1)).apply {
            density = context.resources.displayMetrics.densityDpi
            block(Canvas(this))
        }

    private fun layout(context: Context, text: String, sizePx: Float, lineHeightPx: Float): StaticLayout {
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = sizePx
            typeface = typefaceFor(context, text)
        }
        val width = text.split('\n').maxOf { ceil(paint.measureText(it)).toInt() }.coerceAtLeast(1)
        val metrics = paint.fontMetrics
        val extra = lineHeightPx - (metrics.descent - metrics.ascent)
        return StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setIncludePad(false)
            .setLineSpacing(extra, 1f)
            .build()
    }

    private fun typefaceFor(context: Context, text: String): Typeface? {
        val coverage = AccentGlyphCoverage.of(context)
        val fontRes = if (coverage == null || coverage.covers(text)) {
            CommonR.font.andong_katuri_regular
        } else {
            CommonR.font.pretendard_medium
        }
        return ResourcesCompat.getFont(context, fontRes)
    }

    private fun sp(context: Context, value: Float): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value, context.resources.displayMetrics)

    private fun dp(context: Context, value: Float): Int =
        ceil(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, context.resources.displayMetrics)).toInt()
}
