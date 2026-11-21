package com.dandi.nyummy.cat.presentation

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.createBitmap
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme

/** 검증용 아이템 그림의 색. 디자인 토큰에서 가져온다(코드에 색 값을 두지 않는다). */
@Immutable
data class SampleArtColors(
    val main: Int,
    val gold: Int,
    val fabric: Int,
    val ink: Int,
    val light: Int,
) {
    companion object {
        @Composable
        fun fromTheme(): SampleArtColors {
            val c = NyummyTheme.colors
            return SampleArtColors(
                main = c.content.danger.toArgb(),
                gold = c.data.coin.toArgb(),
                fabric = c.bg.actionPrimary.toArgb(),
                ink = c.content.primary.toArgb(),
                light = c.bg.surface.toArgb(),
            )
        }
    }
}

/**
 * 착용 슬롯 검증용 임시 아이템 그림. 팀 정책상 아이템 원화는 디자이너가 만들므로(설계서 13절 A7),
 * 여기서는 슬롯 위치, 크기, 그리는 순서, 귀 처리를 확인할 수 있는 단순 도형만 코드로 그린다.
 * 그림 좌표는 아이템 크기(u)에 [PX_PER_UNIT]를 곱한 비트맵이다.
 * 외곽선 색은 아이템 데이터(`items.json`의 `lineColor`, 기준서 C9 공식 원화 외곽선)에서 받는다.
 */
object SampleAttachmentArt {
    const val PREFIX = "sample:"
    private const val PX_PER_UNIT = 8f
    private const val OUTLINE_UNITS = 1.6f

    fun draw(key: String, width: Float, height: Float, colors: SampleArtColors, lineColor: Int = colors.ink): Bitmap? {
        val w = (width * PX_PER_UNIT).toInt().coerceAtLeast(1)
        val h = (height * PX_PER_UNIT).toInt().coerceAtLeast(1)
        val bitmap = createBitmap(w, h)
        val canvas = Canvas(bitmap)
        val painter = Painter(canvas, w.toFloat(), h.toFloat(), colors, lineColor)
        when (key.removePrefix(PREFIX)) {
            "beanie" -> painter.beanie()
            "crown" -> painter.crown(front = true)
            "crown_back" -> painter.crown(front = false)
            "bell_collar" -> painter.collar()
            "vest" -> painter.vest()
            "glasses" -> painter.glasses()
            else -> return null
        }
        bitmap.setHasMipMap(true)
        return bitmap
    }

    private class Painter(val canvas: Canvas, val w: Float, val h: Float, val c: SampleArtColors, lineColor: Int) {
        private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = OUTLINE_UNITS * PX_PER_UNIT
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
            color = lineColor
        }
        private val inset = line.strokeWidth / 2f
        private val r = RectF()

        private fun shape(path: Path, color: Int, alpha: Int = 255) {
            fill.color = color
            fill.alpha = alpha
            canvas.drawPath(path, fill)
            canvas.drawPath(path, line)
        }

        /** 귀를 덮는 비니: 둥근 머리 덮개 + 접은 챙 + 방울 */
        fun beanie() {
            val brimTop = h * 0.68f
            val dome = Path().apply {
                moveTo(inset + w * 0.04f, brimTop)
                cubicTo(w * 0.06f, h * 0.12f, w * 0.94f, h * 0.12f, w - inset - w * 0.04f, brimTop)
                close()
            }
            shape(dome, c.main)
            r.set(inset, brimTop - h * 0.04f, w - inset, h - inset)
            shape(Path().apply { addRoundRect(r, h * 0.12f, h * 0.12f, Path.Direction.CW) }, c.light)
            val pom = h * 0.13f
            shape(Path().apply { addCircle(w / 2f, pom + inset, pom, Path.Direction.CW) }, c.light)
        }

        /** 귀 사이 왕관: 앞판은 뾰족한 테, 뒤판은 테 안쪽의 어두운 띠(귀 뒤에 그려진다) */
        fun crown(front: Boolean) {
            if (!front) {
                r.set(w * 0.12f, h * 0.35f, w * 0.88f, h * 0.85f)
                fill.color = c.ink
                fill.alpha = 150
                canvas.drawRect(r, fill)
                return
            }
            val p = Path().apply {
                moveTo(inset, h - inset)
                lineTo(inset, h * 0.3f)
                lineTo(w * 0.25f, h * 0.6f)
                lineTo(w * 0.5f, inset)
                lineTo(w * 0.75f, h * 0.6f)
                lineTo(w - inset, h * 0.3f)
                lineTo(w - inset, h - inset)
                close()
            }
            shape(p, c.gold)
            shape(Path().apply { addCircle(w / 2f, h * 0.72f, h * 0.12f, Path.Direction.CW) }, c.main)
        }

        /** 방울 목걸이: 목 띠 + 가운데 방울(방울은 띠보다 아래로 늘어진다) */
        fun collar() {
            r.set(inset, inset, w - inset, h * 0.45f)
            shape(Path().apply { addRoundRect(r, h * 0.2f, h * 0.2f, Path.Direction.CW) }, c.main)
            val bell = h * 0.27f
            shape(Path().apply { addCircle(w / 2f, h - bell - inset, bell, Path.Direction.CW) }, c.gold)
        }

        /** 조끼: 가슴을 덮는 사다리꼴, 앞발은 가리지 않는 길이, 단추 두 개 */
        fun vest() {
            val p = Path().apply {
                moveTo(w * 0.18f, inset)
                lineTo(w * 0.82f, inset)
                quadTo(w - inset, h * 0.5f, w * 0.9f, h - inset)
                lineTo(w * 0.1f, h - inset)
                quadTo(inset, h * 0.5f, w * 0.18f, inset)
                close()
            }
            shape(p, c.fabric)
            canvas.drawLine(w / 2f, inset, w / 2f, h - inset, line)
            for (y in floatArrayOf(0.4f, 0.72f)) {
                shape(Path().apply { addCircle(w / 2f + w * 0.06f, h * y, h * 0.07f, Path.Direction.CW) }, c.gold)
            }
        }

        /** 동그란 안경: 렌즈는 반투명(기준서 FACE_ITEM 렌즈 alpha 0.35 이하)이라 깜빡임과 웃는 눈이 비친다 */
        fun glasses() {
            val lens = h / 2f - inset
            for (cx in floatArrayOf(w * LENS_LEFT, w * (1f - LENS_LEFT))) {
                shape(Path().apply { addCircle(cx, h / 2f, lens, Path.Direction.CW) }, c.light, alpha = LENS_ALPHA)
            }
            canvas.drawLine(w * LENS_LEFT + lens, h / 2f, w * (1f - LENS_LEFT) - lens, h / 2f, line)
        }

        private companion object {
            const val LENS_ALPHA = 76 // 0.3

            /** 렌즈 가운데(폭 비율). NORMAL 두 눈 사이 21.7u를 안경 폭 34u에 맞춘 값 */
            const val LENS_LEFT = 0.18f
        }
    }
}
