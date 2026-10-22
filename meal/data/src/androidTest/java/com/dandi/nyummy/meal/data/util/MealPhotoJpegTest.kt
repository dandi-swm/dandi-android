package com.dandi.nyummy.meal.data.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.math.abs

/** 갤러리에서 고른 JPEG 아닌 사진(PNG 등)을 JPEG로 바꾸는 규칙. 실제 디코더와 JPEG 인코더로 확인한다. */
@RunWith(AndroidJUnit4::class)
class MealPhotoJpegTest {

    private val cacheDir = InstrumentationRegistry.getInstrumentation().targetContext.cacheDir
    private val files = mutableListOf<File>()

    @After
    fun tearDown() {
        files.forEach { it.delete() }
    }

    @Test
    fun PNG_는_같은_크기의_JPEG_로_바뀌고_투명한_곳은_흰색이_된다() {
        val file = halfTransparentPng()

        ensureJpegMealPhotoFile(file)

        val bytes = file.readBytes()
        assertTrue("JPEG 시그니처가 아니다", bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte())
        val decoded = BitmapFactory.decodeFile(file.absolutePath)
        assertEquals(WIDTH, decoded.width)
        assertEquals(HEIGHT, decoded.height)
        assertTrue("불투명한 곳이 빨갛지 않다", decoded.getPixel(WIDTH / 4, HEIGHT / 2).isNear(Color.RED))
        assertTrue("투명한 곳이 희지 않다", decoded.getPixel(WIDTH * 3 / 4, HEIGHT / 2).isNear(Color.WHITE))
        decoded.recycle()
    }

    /** 왼쪽 절반은 불투명한 빨강, 오른쪽 절반은 투명한 PNG. */
    private fun halfTransparentPng(): File {
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(WIDTH * HEIGHT) { index -> if (index % WIDTH < WIDTH / 2) Color.RED else Color.TRANSPARENT }
        bitmap.setPixels(pixels, 0, WIDTH, 0, 0, WIDTH, HEIGHT)
        val file = File(cacheDir, "meal_jpeg_test_${files.size}.png").also(files::add)
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        return file
    }

    /** JPEG 손실을 감안해 채널마다 [TOLERANCE] 안이면 같은 색으로 본다. */
    private fun Int.isNear(expected: Int): Boolean =
        abs(Color.red(this) - Color.red(expected)) <= TOLERANCE &&
            abs(Color.green(this) - Color.green(expected)) <= TOLERANCE &&
            abs(Color.blue(this) - Color.blue(expected)) <= TOLERANCE

    private companion object {
        const val WIDTH = 400
        const val HEIGHT = 300
        const val TOLERANCE = 8
    }
}
