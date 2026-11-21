package com.dandi.nyummy.cat.presentation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.test.platform.app.InstrumentationRegistry
import com.dandi.nyummy.cat.data.dto.CatAnimationSetDTO
import com.dandi.nyummy.cat.data.rig.CatRigParser
import com.dandi.nyummy.cat.domain.rig.CatRigAnimator
import com.dandi.nyummy.cat.domain.rig.CatRigModel
import com.dandi.nyummy.cat.entity.CatAnimationSetVO
import com.dandi.nyummy.cat.entity.rig.AttachmentItemVO
import com.dandi.nyummy.cat.entity.rig.CatRigCharacterVO
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.random.Random

/**
 * 리그를 정해진 시각에 멈춰 오프스크린 비트맵으로 그리는 도우미(실제 화면과 같은 [CatRigRenderer]).
 * 연속 프레임 캡처와 렌더 테스트가 쓴다. 결과 PNG는 앱 외부 파일 폴더 `cat-rigging-frames/`에 쓴다.
 */
object RigCapture {
    val BODIES = listOf("LEAN", "SLIM", "NORMAL", "CHUBBY", "PLUMP")
    const val CREAM = 0xFFF8F4EC.toInt() // 기준서 5-1 캡처 배경

    val context: Context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val json = Json { ignoreUnknownKeys = true }

    fun read(path: String): String = context.assets.open(path).bufferedReader().use { it.readText() }

    val items: List<AttachmentItemVO> by lazy { CatRigParser.items(read(CatRigParser.ITEMS_PATH)) }

    fun character(skin: String, body: String, itemIds: List<String> = emptyList()): CatRigCharacterVO {
        val spec = requireNotNull(CatRigParser.skin(read(CatRigParser.skinPath(skin))))
        val dir = CatRigParser.partDir(spec.partSet, body)
        val rig = requireNotNull(CatRigParser.bodyRig(read(dir + CatRigParser.RIG_FILE), dir))
        return CatRigCharacterVO(spec, rig, items.filter { it.id in itemIds }.associateBy { it.slot })
    }

    fun animations(body: String): CatAnimationSetVO =
        json.decodeFromString<CatAnimationSetDTO>(read("cats/$body/animations.json")).toVO(body, "")

    fun textures(character: CatRigCharacterVO): CatRigTextures = runBlocking {
        CatRigTextureLoader.load(context, character, SAMPLE_COLORS)
    }

    /** 앱 토큰과 같은 값(테스트는 Compose 테마 밖에서 그린다) */
    val SAMPLE_COLORS = SampleArtColors(
        main = 0xFFEA3348.toInt(),
        gold = 0xFFFBC15D.toInt(),
        fabric = 0xFF43855D.toInt(),
        ink = 0xFF262626.toInt(),
        light = 0xFFFFFFFF.toInt(),
    )

    /** 한 마리를 들고 다니며 시각을 정해 그린다. */
    class Rig(val character: CatRigCharacterVO, seed: Int = 7) {
        val model = CatRigModel(character)
        val animator = CatRigAnimator(model.skeleton, Random(seed))
        val textures = textures(character)
        private val renderer = CatRigRenderer()

        fun advance(seconds: Float) {
            var left = seconds
            while (left > 1e-6f) {
                val dt = minOf(left, 1f / 60f)
                animator.advance(dt)
                left -= dt
            }
            model.update(animator.pose, animator.time)
        }

        /** 지금 포즈를 [canvas]의 (x, y)에 한 변 [size]px 칸으로 그린다. */
        fun drawAt(canvas: Canvas, x: Float, y: Float, size: Float) {
            model.update(animator.pose, animator.time)
            renderer.draw(canvas, model.frame, textures, character.spec, character.bodyRig.palette, size / RIG_SIZE, x, y)
        }

        fun bitmap(size: Int, background: Int = CREAM): Bitmap {
            val b = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val c = Canvas(b)
            c.drawColor(background)
            drawAt(c, 0f, 0f, size.toFloat())
            return b
        }
    }

    /** 칸 [cell]px 여러 개를 가로로 잇는 시트. [label]이 있으면 칸 위에 적는다. */
    class Sheet(columns: Int, rows: Int, val cell: Int, private val labelHeight: Int = 22) {
        val bitmap: Bitmap = Bitmap.createBitmap(columns * cell, rows * (cell + labelHeight), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap).also { it.drawColor(CREAM) }
        private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.DKGRAY
            textSize = labelHeight * 0.7f
        }
        private val guide = Paint().apply {
            color = 0x553366CC
            strokeWidth = 1f
        }

        fun put(column: Int, row: Int, rig: Rig, label: String? = null, guides: Boolean = false) {
            val x = (column * cell).toFloat()
            val y = (row * (cell + labelHeight) + labelHeight).toFloat()
            if (label != null) canvas.drawText(label, x + 4f, y - 6f, text)
            if (guides) {
                val s = cell / RIG_SIZE
                canvas.drawLine(x, y + 118f * s, x + cell, y + 118f * s, guide)
                canvas.drawLine(x, y + 57f * s, x + cell, y + 57f * s, guide)
                canvas.drawLine(x + 64f * s, y, x + 64f * s, y + cell, guide)
            }
            rig.drawAt(canvas, x, y, cell.toFloat())
        }

        fun save(name: String): File = RigCapture.save(bitmap, name)
    }

    fun save(bitmap: Bitmap, name: String): File {
        val dir = File(context.getExternalFilesDir(null), "cat-rigging-frames").apply { mkdirs() }
        val file = File(dir, "$name.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return file
    }
}
