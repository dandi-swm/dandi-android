package com.dandi.nyummy.meal.data.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import com.dandi.nyummy.meal.domain.MealPhotoInvalidException
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

/** 식사 사진 업로드 상한 (10MB). presigned 발급 전에 이 크기 이하로 맞춘다. */
internal const val MAX_MEAL_PHOTO_SIZE_BYTES = 10L * 1024 * 1024

private const val TAG = "MealPhoto"
private const val INITIAL_JPEG_QUALITY = 90
private const val MIN_JPEG_QUALITY = 50
private const val JPEG_QUALITY_STEP = 10
private const val DEFAULT_DECODE_BUDGET_BYTES = 64L * 1024 * 1024

/** 더 줄여도 음식 판별이 불가능해지는 하한. 이 아래로는 다운스케일하지 않는다. */
private const val MIN_DIMENSION_PX = 320

/** 압축 후 EXIF 를 다시 써넣으면 파일이 조금 커지므로, 압축 목표에서 미리 빼 두는 여유분. */
private const val EXIF_SIZE_MARGIN_BYTES = 64L * 1024

/**
 * 재압축 후 복원할 EXIF 태그 목록.
 *
 * 방향(orientation)은 압축 시 픽셀에 반영하므로 복사 대상에서 제외하고,
 * 이미지 크기 태그는 다운스케일로 달라질 수 있어 제외한다.
 */
private val EXIF_TAGS_TO_PRESERVE = listOf(
    ExifInterface.TAG_DATETIME,
    ExifInterface.TAG_DATETIME_ORIGINAL,
    ExifInterface.TAG_DATETIME_DIGITIZED,
    ExifInterface.TAG_OFFSET_TIME,
    ExifInterface.TAG_OFFSET_TIME_ORIGINAL,
    ExifInterface.TAG_OFFSET_TIME_DIGITIZED,
    ExifInterface.TAG_SUBSEC_TIME,
    ExifInterface.TAG_SUBSEC_TIME_ORIGINAL,
    ExifInterface.TAG_SUBSEC_TIME_DIGITIZED,
    ExifInterface.TAG_MAKE,
    ExifInterface.TAG_MODEL,
    ExifInterface.TAG_SOFTWARE,
    ExifInterface.TAG_EXPOSURE_TIME,
    ExifInterface.TAG_F_NUMBER,
    ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY,
    ExifInterface.TAG_FOCAL_LENGTH,
    ExifInterface.TAG_FLASH,
    ExifInterface.TAG_WHITE_BALANCE,
    ExifInterface.TAG_GPS_LATITUDE,
    ExifInterface.TAG_GPS_LATITUDE_REF,
    ExifInterface.TAG_GPS_LONGITUDE,
    ExifInterface.TAG_GPS_LONGITUDE_REF,
    ExifInterface.TAG_GPS_ALTITUDE,
    ExifInterface.TAG_GPS_ALTITUDE_REF,
    ExifInterface.TAG_GPS_TIMESTAMP,
    ExifInterface.TAG_GPS_DATESTAMP,
)

/**
 * 업로드 전에 촬영본 파일을 검증하고, [maxBytes] 를 넘으면 같은 경로에 재압축해 덮어쓴다.
 *
 * 검증 실패·압축 불가 시 [MealPhotoInvalidException] 을 던진다.
 */
internal fun prepareMealPhotoFile(photoPath: String, maxBytes: Long = MAX_MEAL_PHOTO_SIZE_BYTES) {
    val file = File(photoPath)
    if (!file.isFile || file.length() == 0L) {
        throw MealPhotoInvalidException("촬영한 사진을 찾지 못했어요. 다시 촬영해주세요")
    }
    ensureExifTimeMetadata(file)
    logExifMetadata(file)
    if (file.length() <= maxBytes) return
    compressIntoLimit(file, maxBytes)
    Log.d(TAG, "compressed to ${file.length()} bytes: ${file.name}")
}

private const val EXIF_DATE_TIME_PATTERN = "yyyy:MM:dd HH:mm:ss"

private val EXIF_DATE_TIME_TAGS = listOf(
    ExifInterface.TAG_DATETIME_ORIGINAL,
    ExifInterface.TAG_DATETIME,
    ExifInterface.TAG_DATETIME_DIGITIZED,
)

private val EXIF_OFFSET_TIME_TAGS = listOf(
    ExifInterface.TAG_OFFSET_TIME_ORIGINAL,
    ExifInterface.TAG_OFFSET_TIME,
    ExifInterface.TAG_OFFSET_TIME_DIGITIZED,
)

/**
 * 촬영 시각·타임존 오프셋 EXIF 태그가 비어 있으면 채워 넣는다.
 *
 * 카메라 파이프라인이 촬영 시각은 대체로 기록하지만 타임존 오프셋(OFFSET_TIME_*)은
 * 누락하는 기기가 많다. 촬영은 방금 이 기기에서 일어났으므로, 파일 저장 시각과
 * 기기 타임존으로 빈 태그만 보충한다(이미 있는 값은 건드리지 않는다).
 */
private fun ensureExifTimeMetadata(file: File) {
    runCatching {
        val exif = ExifInterface(file)
        val captureMillis = file.lastModified().takeIf { it > 0 } ?: System.currentTimeMillis()
        val dateTime = formatExifDateTime(captureMillis)
        val utcOffset = formatUtcOffset(TimeZone.getDefault().getOffset(captureMillis))

        var changed = false
        EXIF_DATE_TIME_TAGS.forEach { tag ->
            if (exif.getAttribute(tag).isNullOrBlank()) {
                exif.setAttribute(tag, dateTime)
                changed = true
            }
        }
        EXIF_OFFSET_TIME_TAGS.forEach { tag ->
            if (exif.getAttribute(tag).isNullOrBlank()) {
                exif.setAttribute(tag, utcOffset)
                changed = true
            }
        }
        if (changed) {
            exif.saveAttributes()
            Log.d(TAG, "EXIF time filled (takenAt=$dateTime, offset=$utcOffset): ${file.name}")
        }
    }.getOrElse {
        Log.w(TAG, "EXIF time fill failed: ${file.name}", it)
        throw MealPhotoInvalidException("사진 촬영 정보를 저장하지 못했어요. 다시 촬영해주세요")
    }
}

/** epoch millis 를 EXIF 시각 포맷(`yyyy:MM:dd HH:mm:ss`, 기기 로컬 시각)으로 변환한다. */
private fun formatExifDateTime(epochMillis: Long): String =
    SimpleDateFormat(EXIF_DATE_TIME_PATTERN, Locale.US).format(Date(epochMillis))

/** 타임존 오프셋 millis 를 EXIF 오프셋 포맷(`+09:00`)으로 변환한다. */
private fun formatUtcOffset(offsetMillis: Int): String {
    val totalMinutes = offsetMillis / 60_000
    val sign = if (totalMinutes < 0) "-" else "+"
    val absMinutes = abs(totalMinutes)
    return String.format(Locale.US, "%s%02d:%02d", sign, absMinutes / 60, absMinutes % 60)
}

/** 촬영 직후 파일의 EXIF 메타데이터(촬영 시각·회전·크기 등)를 디버그 로그로 남긴다. */
private fun logExifMetadata(file: File) {
    runCatching {
        val exif = ExifInterface(file)
        Log.d(
            TAG,
            buildString {
                append("EXIF ${file.name} (${file.length()} bytes)")
                append(" | takenAt=${exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)}")
                append(" | offset=${exif.getAttribute(ExifInterface.TAG_OFFSET_TIME_ORIGINAL)}")
                append(" | rotation=${exif.rotationDegrees}")
                append(
                    " | size=${exif.getAttribute(ExifInterface.TAG_IMAGE_WIDTH)}" +
                        "x${exif.getAttribute(ExifInterface.TAG_IMAGE_LENGTH)}",
                )
                append(" | make=${exif.getAttribute(ExifInterface.TAG_MAKE)}")
                append(" | model=${exif.getAttribute(ExifInterface.TAG_MODEL)}")
                append(" | hasGps=${exif.latLong != null}")
            },
        )
    }.onFailure { Log.w(TAG, "EXIF read failed: ${file.name}", it) }
}

/**
 * JPEG 품질을 단계적으로 낮추고, 그래도 넘치면 해상도를 절반씩 줄여 [maxBytes] 이하로 만든다.
 * 재인코딩하면 EXIF 가 사라지므로 회전은 픽셀에 미리 반영하고, 나머지 메타데이터는
 * 압축 후 [EXIF_TAGS_TO_PRESERVE] 만큼 원본에서 복원한다.
 */
private fun compressIntoLimit(file: File, maxBytes: Long) {
    val originalExif = runCatching { ExifInterface(file) }.getOrNull()
    val rotationDegrees = originalExif?.rotationDegrees ?: 0
    val preservedAttributes = originalExif?.let { exif ->
        EXIF_TAGS_TO_PRESERVE.mapNotNull { tag -> exif.getAttribute(tag)?.let { tag to it } }
    }.orEmpty()

    val decoded = decodeSampledBitmap(file)
        ?: throw MealPhotoInvalidException("사진을 읽지 못했어요. 다시 촬영해주세요")
    var bitmap = decoded.rotatedBy(rotationDegrees)
    if (bitmap !== decoded) decoded.recycle()

    // EXIF 복원분이 더해져도 상한을 넘지 않도록 여유분을 뺀 크기를 목표로 압축한다.
    val targetBytes = maxBytes - EXIF_SIZE_MARGIN_BYTES
    try {
        var quality = INITIAL_JPEG_QUALITY
        var bytes = bitmap.toJpegBytes(quality)
        while (bytes.size > targetBytes && quality > MIN_JPEG_QUALITY) {
            quality -= JPEG_QUALITY_STEP
            bytes = bitmap.toJpegBytes(quality)
        }
        while (bytes.size > targetBytes && bitmap.width / 2 >= MIN_DIMENSION_PX && bitmap.height / 2 >= MIN_DIMENSION_PX) {
            val previous = bitmap
            bitmap = Bitmap.createScaledBitmap(previous, previous.width / 2, previous.height / 2, true)
            if (bitmap !== previous) previous.recycle()
            bytes = bitmap.toJpegBytes(MIN_JPEG_QUALITY)
        }
        if (bytes.size > targetBytes) {
            throw MealPhotoInvalidException("사진 용량을 줄이지 못했어요. 다시 촬영해주세요")
        }
        file.writeBytes(bytes)
        restoreExifMetadata(file, preservedAttributes)
    } finally {
        if (!bitmap.isRecycled) {
            bitmap.recycle()
        }
    }
}

private fun decodeSampledBitmap(
    file: File,
    decodeBudgetBytes: Long = DEFAULT_DECODE_BUDGET_BYTES,
): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.absolutePath, bounds)

    val width = bounds.outWidth
    val height = bounds.outHeight
    if (width <= 0 || height <= 0) return null

    val maxPixels = (decodeBudgetBytes / 4L).coerceAtLeast(1L)
    var sampleSize = 1
    while ((width.toLong() / sampleSize) * (height.toLong() / sampleSize) > maxPixels) {
        sampleSize *= 2
    }

    while (true) {
        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val bitmap = runCatching { BitmapFactory.decodeFile(file.absolutePath, options) }.getOrNull()
        if (bitmap != null) return bitmap
        if (sampleSize >= Int.MAX_VALUE / 2) return null
        sampleSize *= 2
    }
}

/**
 * 재압축으로 사라진 EXIF 메타데이터를 원본에서 읽어 둔 값으로 되살린다.
 * 복원 실패는 업로드를 막을 사유가 아니므로 경고 로그만 남긴다.
 */
private fun restoreExifMetadata(file: File, attributes: List<Pair<String, String>>) {
    if (attributes.isEmpty()) return
    runCatching {
        val exif = ExifInterface(file)
        attributes.forEach { (tag, value) -> exif.setAttribute(tag, value) }
        // 회전은 이미 픽셀에 반영됐으므로 방향 태그는 정상으로 고정한다.
        exif.setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL.toString())
        exif.saveAttributes()
        Log.d(TAG, "EXIF restored (${attributes.size} tags): ${file.name}")
    }.onFailure { Log.w(TAG, "EXIF restore failed: ${file.name}", it) }
}

private fun Bitmap.rotatedBy(degrees: Int): Bitmap {
    if (degrees == 0) return this
    val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}

private fun Bitmap.toJpegBytes(quality: Int): ByteArray =
    ByteArrayOutputStream().use { stream ->
        compress(Bitmap.CompressFormat.JPEG, quality, stream)
        stream.toByteArray()
    }
