package com.dandi.nyummy.meal.data.util

import android.graphics.Bitmap
import androidx.exifinterface.media.ExifInterface
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.dandi.nyummy.meal.entity.MealPhotoSource
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.random.Random

/** 업로드 전 사진 준비에서 EXIF 를 채우거나 남기는 규칙. 실제 JPEG 인코더와 ExifInterface 로 확인한다. */
@RunWith(AndroidJUnit4::class)
class MealPhotoExifTest {

    private val cacheDir = InstrumentationRegistry.getInstrumentation().targetContext.cacheDir
    private val files = mutableListOf<File>()

    @After
    fun tearDown() {
        files.forEach { it.delete() }
    }

    @Test
    fun 찍은_사진에_촬영_시각이_없으면_채워_넣는다() {
        val file = jpegFile()

        prepareMealPhotoFile(file.absolutePath, MealPhotoSource.CAMERA)

        val exif = ExifInterface(file)
        assertNotNull(exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL))
        assertNotNull(exif.getAttribute(ExifInterface.TAG_OFFSET_TIME_ORIGINAL))
        assertNotNull(exif.getAttribute(ExifInterface.TAG_MODEL))
    }

    @Test
    fun 찍은_사진에_촬영_시각이_있으면_파일을_그대로_둔다() {
        val file = jpegFile { setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, TAKEN_AT) }
        val before = file.readBytes()

        prepareMealPhotoFile(file.absolutePath, MealPhotoSource.CAMERA)

        assertArrayEquals(before, file.readBytes())
        assertNull(ExifInterface(file).getAttribute(ExifInterface.TAG_OFFSET_TIME_ORIGINAL))
    }

    @Test
    fun 첨부한_사진에_EXIF_가_없으면_채우지_않는다() {
        val file = jpegFile()
        val before = file.readBytes()

        prepareMealPhotoFile(file.absolutePath, MealPhotoSource.GALLERY)

        assertArrayEquals(before, file.readBytes())
        assertNull(ExifInterface(file).getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL))
    }

    @Test
    fun 크기를_줄여도_원본_EXIF_를_그대로_남긴다() {
        val file = jpegFile {
            setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, TAKEN_AT)
            setAttribute(ExifInterface.TAG_OFFSET_TIME_ORIGINAL, "+09:00")
            setAttribute(ExifInterface.TAG_USER_COMMENT, "nyummy")
            setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_90.toString())
            setLatLong(37.5665, 126.9780)
        }
        val originalSize = file.length()

        prepareMealPhotoFile(file.absolutePath, MealPhotoSource.GALLERY, maxBytes = SMALL_LIMIT)

        assertTrue(file.length() in 1..SMALL_LIMIT)
        assertTrue(file.length() < originalSize)
        val exif = ExifInterface(file)
        assertEquals(TAKEN_AT, exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL))
        assertEquals("+09:00", exif.getAttribute(ExifInterface.TAG_OFFSET_TIME_ORIGINAL))
        assertEquals("nyummy", exif.getAttribute(ExifInterface.TAG_USER_COMMENT))
        // 픽셀을 돌리지 않으므로 방향 태그도 그대로다.
        assertEquals(90, exif.rotationDegrees)
        assertNotNull(exif.latLong)
    }

    @Test
    fun EXIF_가_없던_사진은_줄여도_EXIF_가_생기지_않는다() {
        val file = jpegFile()

        prepareMealPhotoFile(file.absolutePath, MealPhotoSource.GALLERY, maxBytes = SMALL_LIMIT)

        assertTrue(file.length() <= SMALL_LIMIT)
        assertNull(file.inputStream().buffered().use(::readJpegExifSegment))
    }

    /** 압축이 잘 안 되도록 잡음으로 채운 JPEG. [exif] 로 태그를 넣는다. */
    private fun jpegFile(exif: (ExifInterface.() -> Unit)? = null): File {
        val bitmap = Bitmap.createBitmap(SIDE, SIDE, Bitmap.Config.ARGB_8888)
        val random = Random(SEED)
        bitmap.setPixels(IntArray(SIDE * SIDE) { random.nextInt() or 0xFF000000.toInt() }, 0, SIDE, 0, 0, SIDE, SIDE)
        val file = File(cacheDir, "meal_exif_test_${files.size}.jpeg").also(files::add)
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it) }
        bitmap.recycle()
        if (exif != null) ExifInterface(file).apply(exif).saveAttributes()
        return file
    }

    private companion object {
        const val SIDE = 1200
        const val SEED = 7
        const val SMALL_LIMIT = 400L * 1024
        const val TAKEN_AT = "2026:10:09 12:30:00"
    }
}
