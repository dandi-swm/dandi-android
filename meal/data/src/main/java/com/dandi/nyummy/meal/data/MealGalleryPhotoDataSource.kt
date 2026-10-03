package com.dandi.nyummy.meal.data

import android.content.Context
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.dandi.nyummy.common.entity.time.KstDateTime
import com.dandi.nyummy.meal.data.util.ensureJpegMealPhotoFile
import com.dandi.nyummy.meal.data.util.takenAtKstOrNull
import com.dandi.nyummy.meal.domain.MealPhotoInvalidException
import java.io.File
import java.io.InputStream

/**
 * 갤러리에서 고른 사진(content URI)을 읽는 로컬 데이터소스.
 *
 * 선택기가 돌려준 URI 의 읽기 권한은 일시적이므로, 검증을 통과한 사진은 업로드 파이프라인이
 * 다루는 앱 캐시 파일로 곧바로 복사해 촬영본과 같은 경로 기반 흐름에 태운다.
 */
class MealGalleryPhotoDataSource(
    private val context: Context,
) {

    /** 사진 EXIF 의 촬영 시각을 KST 로 읽는다. 촬영 시각 정보가 없으면 null. */
    fun readTakenAt(photoUri: String): KstDateTime? =
        openPhoto(photoUri).use { input ->
            runCatching { ExifInterface(input).takenAtKstOrNull() }.getOrNull()
        }

    /** 사진을 앱 캐시에 JPEG 파일로 복사하고 그 절대 경로를 돌려준다. */
    fun copyToCache(photoUri: String): String {
        val file = File(context.cacheDir, "meal_gallery_${System.currentTimeMillis()}.jpeg")
        return runCatching {
            openPhoto(photoUri).use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            }
            ensureJpegMealPhotoFile(file)
            file.absolutePath
        }.getOrElse { cause ->
            file.delete()
            throw cause as? MealPhotoInvalidException
                ?: MealPhotoInvalidException(PHOTO_LOAD_FAILED_MESSAGE)
        }
    }

    private fun openPhoto(photoUri: String): InputStream =
        runCatching { context.contentResolver.openInputStream(Uri.parse(photoUri)) }.getOrNull()
            ?: throw MealPhotoInvalidException(PHOTO_LOAD_FAILED_MESSAGE)

    private companion object {
        const val PHOTO_LOAD_FAILED_MESSAGE = "사진을 불러오지 못했어요. 다시 선택해주세요"
    }
}
