package com.dandi.nyummy.cat.presentation

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.core.graphics.withMatrix
import androidx.core.graphics.withTranslation
import com.dandi.nyummy.cat.domain.rig.CatDrawOp
import com.dandi.nyummy.cat.domain.rig.CatMeshOp
import com.dandi.nyummy.cat.domain.rig.CatRigFrame
import com.dandi.nyummy.cat.domain.rig.CatRigidOp
import com.dandi.nyummy.cat.domain.rig.CatShape
import com.dandi.nyummy.cat.domain.rig.CatShapeOp
import com.dandi.nyummy.cat.entity.rig.CatCharacterSpec
import com.dandi.nyummy.cat.entity.rig.ColorTransformVO
import com.dandi.nyummy.cat.entity.rig.PartMaterial
import com.dandi.nyummy.cat.entity.rig.RigPaletteVO

/**
 * 리그 프레임([CatRigFrame])을 Android Canvas에 그린다. 엔진(cat:domain)이 정한 순서와 좌표를 그대로 따른다.
 *
 * - 메시 파츠: `drawBitmapMesh`(API 18부터 하드웨어 가속). 꼭짓점은 실수 좌표 그대로라 반올림 떨림이 없다.
 * - 강체 파츠(눈, 입, 모자, 안경): 아핀 행렬을 걸고 `drawBitmap`. 눈꺼풀은 눈 좌표의 다각형으로 자른다.
 * - 코드 도형(감은 눈 선, 벌린 입, 그림자, Zzz): 원화에서 뽑은 색([RigPaletteVO])으로 그린다. 선 색(입, 그림자, Zzz)은
 *   털 색 변환([tinted])을 걸어 스킨 털 색에 맞춘다.
 * - 스킨 색: 재질별 [ColorTransformVO]를 `ColorMatrixColorFilter`로 칠한다(텍스처를 새로 만들지 않는다).
 *
 * 리그 좌표(136 x 136 u)는 [draw]의 배율로 화면 픽셀에 맞춘다. 그리는 동안 객체를 새로 만들지 않는다.
 */
class CatRigRenderer {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)
    private val shapePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val matrix = Matrix()
    private val values = FloatArray(9)
    private val rect = RectF()
    private val path = Path()
    private val filters = HashMap<ColorTransformVO, ColorMatrixColorFilter>()

    /**
     * [frame]을 [canvas]에 그린다. 리그 좌표 x에 [scale]을 곱한 뒤 [offsetX], [offsetY]를 더한 자리에 그린다.
     *
     * @param textures 파츠와 아이템 그림. 아직 없는 그림은 건너뛴다.
     * @param spec 스킨(재질별 색 변환)
     * @param palette 원화 색(코드 도형용)
     */
    fun draw(
        canvas: Canvas,
        frame: CatRigFrame,
        textures: CatRigTextures,
        spec: CatCharacterSpec,
        palette: RigPaletteVO,
        scale: Float,
        offsetX: Float = 0f,
        offsetY: Float = 0f,
    ) {
        canvas.withTranslation(offsetX, offsetY) {
            scale(scale, scale)
            drawOps(this, frame, textures, spec, palette)
        }
    }

    private fun drawOps(canvas: Canvas, frame: CatRigFrame, textures: CatRigTextures, spec: CatCharacterSpec, palette: RigPaletteVO) {
        for (op in frame.ops) {
            if (op.alpha <= MIN_ALPHA) continue
            when (op) {
                is CatMeshOp -> drawMesh(canvas, op, textures, spec)
                is CatRigidOp -> drawRigid(canvas, op, textures, spec)
                is CatShapeOp -> drawShape(canvas, op, spec, palette)
            }
        }
    }

    private fun prepare(op: CatDrawOp, spec: CatCharacterSpec) {
        paint.alpha = (op.alpha.coerceIn(0f, 1f) * 255f + 0.5f).toInt()
        val material = op.material
        paint.colorFilter = if (material == null) null else filterOf(spec.colorOf(material))
    }

    private fun drawMesh(canvas: Canvas, op: CatMeshOp, textures: CatRigTextures, spec: CatCharacterSpec) {
        val bitmap = textures[op.texture] ?: return
        prepare(op, spec)
        val clip = op.clip
        if (clip != null) {
            canvas.save()
            canvas.clipPath(polygon(clip))
        }
        canvas.drawBitmapMesh(bitmap, op.cols, op.rows, op.vertices, 0, null, 0, paint)
        if (clip != null) canvas.restore()
    }

    private fun drawRigid(canvas: Canvas, op: CatRigidOp, textures: CatRigTextures, spec: CatCharacterSpec) {
        val bitmap = textures[op.texture] ?: return
        prepare(op, spec)
        canvas.withMatrix(affine(op.matrix)) {
            op.clip?.let { clipPath(polygon(it)) }
            rect.set(op.left, op.top, op.left + op.width, op.top + op.height)
            drawBitmap(bitmap, null, rect, paint)
        }
    }

    private fun drawShape(canvas: Canvas, op: CatShapeOp, spec: CatCharacterSpec, palette: RigPaletteVO) {
        val p = op.params
        val line = lineColor(palette.line, spec.colorOf(PartMaterial.FUR))
        canvas.withMatrix(affine(op.matrix)) {
            path.reset()
            when (op.shape) {
                CatShape.CLOSED_EYE -> {
                    path.moveTo(p[0] - p[2], p[1])
                    path.quadTo(p[0], p[1] + p[3] * 2f, p[0] + p[2], p[1])
                    stroke(palette.pupil, op.alpha, p[4])
                    canvas.drawPath(path, shapePaint)
                }
                CatShape.SMILE_EYE -> {
                    path.moveTo(p[0] - p[2], p[1] + p[3] * 0.5f)
                    path.quadTo(p[0], p[1] - p[3] * 1.5f, p[0] + p[2], p[1] + p[3] * 0.5f)
                    stroke(palette.pupil, op.alpha, p[4])
                    canvas.drawPath(path, shapePaint)
                }
                CatShape.OPEN_MOUTH -> {
                    fill(line, op.alpha)
                    rect.set(p[0] - p[2] / 2f, p[1], p[0] + p[2] / 2f, p[1] + p[3])
                    canvas.drawOval(rect, shapePaint)
                    fill(palette.blush, op.alpha)
                    rect.set(p[0] - p[2] * 0.3f, p[1] + p[3] * 0.45f, p[0] + p[2] * 0.3f, p[1] + p[3] * 0.92f)
                    canvas.drawOval(rect, shapePaint)
                }
                CatShape.POUT_MOUTH -> {
                    path.moveTo(p[0] - p[2], p[1] + p[3])
                    path.quadTo(p[0], p[1] - p[3], p[0] + p[2], p[1] + p[3])
                    stroke(line, op.alpha, p[4])
                    canvas.drawPath(path, shapePaint)
                }
                CatShape.BLUSH -> {
                    fill(palette.blush, op.alpha)
                    canvas.drawCircle(p[0], p[1], p[4], shapePaint)
                    canvas.drawCircle(p[2], p[3], p[4], shapePaint)
                }
                CatShape.SHADOW -> {
                    fill(line, op.alpha)
                    rect.set(p[0] - p[2], p[1] - p[3], p[0] + p[2], p[1] + p[3])
                    canvas.drawOval(rect, shapePaint)
                }
                CatShape.ZZZ -> drawZzz(canvas, p, line, op.alpha)
            }
        }
    }

    /** 머리 위로 떠오르며 커지고 흐려지는 Z 세 개. 위상(p[3])이 시간에 따라 이어져 끊기지 않는다. */
    private fun drawZzz(canvas: Canvas, p: FloatArray, color: Int, alpha: Float) {
        for (n in 0 until ZZZ_COUNT) {
            val phase = (p[3] + n.toFloat() / ZZZ_COUNT) % 1f
            // 나타날 때와 사라질 때 서서히(위상 끝에서 0)
            val fade = minOf(phase / ZZZ_FADE, (1f - phase) / ZZZ_FADE, 1f)
            val size = p[2] * (0.55f + 0.45f * phase)
            val x = p[0] + phase * p[2] * 1.2f
            val y = p[1] - phase * p[2] * 3f
            path.reset()
            path.moveTo(x, y)
            path.lineTo(x + size, y)
            path.lineTo(x, y + size)
            path.lineTo(x + size, y + size)
            stroke(color, alpha * fade, size * ZZZ_STROKE)
            canvas.drawPath(path, shapePaint)
        }
    }

    private var lastLine = 0
    private var lastFur: ColorTransformVO? = null
    private var lastTinted = 0

    /** 털 색 변환을 건 선 색. 스킨이 바뀔 때만 다시 계산한다(프레임마다 할당하지 않게). */
    private fun lineColor(line: Int, fur: ColorTransformVO): Int {
        if (line != lastLine || fur != lastFur) {
            lastLine = line
            lastFur = fur
            lastTinted = tinted(line, fur)
        }
        return lastTinted
    }

    private fun stroke(color: Int, alpha: Float, width: Float) {
        shapePaint.style = Paint.Style.STROKE
        shapePaint.strokeCap = Paint.Cap.ROUND
        shapePaint.strokeJoin = Paint.Join.ROUND
        shapePaint.strokeWidth = width
        shapePaint.color = withAlpha(color, alpha)
    }

    private fun fill(color: Int, alpha: Float) {
        shapePaint.style = Paint.Style.FILL
        shapePaint.color = withAlpha(color, alpha)
    }

    private fun withAlpha(color: Int, alpha: Float): Int {
        val a = ((color ushr 24) * alpha.coerceIn(0f, 1f) + 0.5f).toInt()
        return (a shl 24) or (color and 0x00FFFFFF)
    }

    private fun affine(m: FloatArray): Matrix {
        values[0] = m[0]; values[1] = m[2]; values[2] = m[4]
        values[3] = m[1]; values[4] = m[3]; values[5] = m[5]
        values[6] = 0f; values[7] = 0f; values[8] = 1f
        matrix.setValues(values)
        return matrix
    }

    private fun polygon(points: FloatArray): Path {
        path.reset()
        path.moveTo(points[0], points[1])
        var i = 2
        while (i + 1 < points.size) {
            path.lineTo(points[i], points[i + 1])
            i += 2
        }
        path.close()
        return path
    }

    private fun filterOf(t: ColorTransformVO): ColorMatrixColorFilter? {
        if (t.isIdentity) return null
        return filters.getOrPut(t) { ColorMatrixColorFilter(colorMatrix(t)) }
    }

    companion object {
        private const val MIN_ALPHA = 0.002f
        private const val ZZZ_COUNT = 3
        private const val ZZZ_FADE = 0.2f
        private const val ZZZ_STROKE = 0.16f

        /** 채도 조절 뒤 곱하고 더하는 색 행렬(더하기는 0..1 값을 0..255로). */
        fun colorMatrix(t: ColorTransformVO): ColorMatrix {
            val m = ColorMatrix()
            m.setSaturation(t.saturation)
            val scale = ColorMatrix(
                floatArrayOf(
                    t.multiplyR, 0f, 0f, 0f, t.addR * 255f,
                    0f, t.multiplyG, 0f, 0f, t.addG * 255f,
                    0f, 0f, t.multiplyB, 0f, t.addB * 255f,
                    0f, 0f, 0f, 1f, 0f,
                ),
            )
            m.postConcat(scale)
            return m
        }

        /** 원화 색 하나에 재질 색 변환을 걸어 코드 도형이 털 색을 따라가게 한다. */
        fun tinted(color: Int, t: ColorTransformVO): Int {
            if (t.isIdentity) return color
            val m = colorMatrix(t).array
            val r = (color shr 16) and 0xFF
            val g = (color shr 8) and 0xFF
            val b = color and 0xFF
            fun ch(row: Int) = (m[row * 5] * r + m[row * 5 + 1] * g + m[row * 5 + 2] * b + m[row * 5 + 4]).toInt().coerceIn(0, 255)
            return (color and 0xFF000000.toInt()) or (ch(0) shl 16) or (ch(1) shl 8) or ch(2)
        }
    }
}

/** 파츠와 아이템 그림. 키는 [CatDrawOp.texture]와 같다. */
class CatRigTextures(private val bitmaps: Map<String, Bitmap>) {
    operator fun get(key: String): Bitmap? = bitmaps[key]
    val keys: Set<String> get() = bitmaps.keys
}
