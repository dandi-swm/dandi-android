package com.dandi.nyummy.meal.data.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Build
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import com.dandi.nyummy.meal.domain.MealPhotoInvalidException
import com.dandi.nyummy.meal.entity.MealPhotoSource
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

/** 압축 후 EXIF 를 다시 써넣으면 파일이 조금 커지므로, 압축 목표에서 미리 빼 두는 여유분(APP1 최대 크기). */
private const val EXIF_SIZE_MARGIN_BYTES = 64L * 1024

/**
 * JPEG 가 아닌 사진(HEIC, PNG 등)을 JPEG 로 바꿀 때 옮겨 적는 EXIF 태그 목록.
 * JPEG 원본은 EXIF 세그먼트를 바이트 그대로 옮기므로 이 목록을 쓰지 않는다.
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
    ExifInterface.TAG_LENS_MAKE,
    ExifInterface.TAG_LENS_MODEL,
    ExifInterface.TAG_IMAGE_UNIQUE_ID,
    ExifInterface.TAG_IMAGE_DESCRIPTION,
    ExifInterface.TAG_ARTIST,
    ExifInterface.TAG_COPYRIGHT,
    ExifInterface.TAG_EXPOSURE_TIME,
    ExifInterface.TAG_EXPOSURE_PROGRAM,
    ExifInterface.TAG_EXPOSURE_MODE,
    ExifInterface.TAG_EXPOSURE_BIAS_VALUE,
    ExifInterface.TAG_F_NUMBER,
    ExifInterface.TAG_APERTURE_VALUE,
    ExifInterface.TAG_SHUTTER_SPEED_VALUE,
    ExifInterface.TAG_BRIGHTNESS_VALUE,
    ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY,
    ExifInterface.TAG_METERING_MODE,
    ExifInterface.TAG_FOCAL_LENGTH,
    ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM,
    ExifInterface.TAG_DIGITAL_ZOOM_RATIO,
    ExifInterface.TAG_SCENE_CAPTURE_TYPE,
    ExifInterface.TAG_FLASH,
    ExifInterface.TAG_WHITE_BALANCE,
    ExifInterface.TAG_COLOR_SPACE,
    ExifInterface.TAG_GPS_VERSION_ID,
    ExifInterface.TAG_GPS_LATITUDE,
    ExifInterface.TAG_GPS_LATITUDE_REF,
    ExifInterface.TAG_GPS_LONGITUDE,
    ExifInterface.TAG_GPS_LONGITUDE_REF,
    ExifInterface.TAG_GPS_ALTITUDE,
    ExifInterface.TAG_GPS_ALTITUDE_REF,
    ExifInterface.TAG_GPS_TIMESTAMP,
    ExifInterface.TAG_GPS_DATESTAMP,
    ExifInterface.TAG_GPS_PROCESSING_METHOD,
    ExifInterface.TAG_GPS_IMG_DIRECTION,
    ExifInterface.TAG_GPS_IMG_DIRECTION_REF,
)

/**
 * 업로드 전에 사진 파일을 검증하고, [maxBytes] 를 넘으면 같은 경로에 재압축해 덮어쓴다.
 *
 * 앱에서 찍은 사진([MealPhotoSource.CAMERA])은 EXIF 촬영 시각이 없을 때만 채워 넣는다.
 * 첨부한 사진([MealPhotoSource.GALLERY])은 EXIF 를 건드리지 않는다(없으면 없는 그대로 보낸다).
 * 재압축해도 원본 EXIF 는 남는다.
 *
 * 검증 실패·압축 불가 시 [MealPhotoInvalidException] 을 던진다.
 */
internal fun prepareMealPhotoFile(
    photoPath: String,
    source: MealPhotoSource,
    maxBytes: Long = MAX_MEAL_PHOTO_SIZE_BYTES,
) {
    val file = File(photoPath)
    if (!file.isFile || file.length() == 0L) {
        throw MealPhotoInvalidException("촬영한 사진을 찾지 못했어요. 다시 촬영해주세요")
    }
    if (source == MealPhotoSource.CAMERA) ensureCaptureExif(file)
    logExifMetadata(file)
    if (file.length() <= maxBytes) return
    compressIntoLimit(file, maxBytes)
    Log.d(TAG, "compressed to ${file.length()} bytes: ${file.name}")
}

/**
 * 갤러리에서 복사해 온 파일을 업로드 파이프라인이 기대하는 JPEG 로 맞춘다.
 *
 * 이미 JPEG 면 그대로 두고, HEIC·PNG 등은 같은 경로에 JPEG 로 다시 인코딩한다
 * (회전은 픽셀에 반영, 촬영 시각 등 EXIF 는 있으면 보존하고 없으면 새로 만들지 않는다). 디코드할 수 없으면
 * [MealPhotoInvalidException] 을 던진다.
 */
internal fun ensureJpegMealPhotoFile(file: File, maxBytes: Long = MAX_MEAL_PHOTO_SIZE_BYTES) {
    if (file.isJpeg()) return
    compressIntoLimit(file, maxBytes)
    Log.d(TAG, "re-encoded to JPEG (${file.length()} bytes): ${file.name}")
}

/** 파일 시그니처(SOI 마커 `FF D8`)로 JPEG 여부를 판별한다. */
private fun File.isJpeg(): Boolean =
    inputStream().use { it.read() == 0xFF && it.read() == 0xD8 }

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
 * 앱에서 찍은 사진에 EXIF 촬영 시각이 없으면 채워 넣는다.
 *
 * 카메라가 EXIF 를 쓰지 않는 기기가 있어서, 그런 경우에만 방금 이 기기에서 찍은 것으로
 * 촬영 시각, 타임존 오프셋, 제조사와 모델을 넣는다. 촬영 시각이 하나라도 있으면 기기가 EXIF 를
 * 제대로 쓴 것이므로 아무것도 바꾸지 않고 그대로 보낸다.
 */
private fun ensureCaptureExif(file: File) {
    runCatching {
        val exif = ExifInterface(file)
        if (EXIF_DATE_TIME_TAGS.any { !exif.getAttribute(it).isNullOrBlank() }) return
        val captureMillis = file.lastModified().takeIf { it > 0 } ?: System.currentTimeMillis()
        val dateTime = formatExifDateTime(captureMillis)
        val utcOffset = formatUtcOffset(TimeZone.getDefault().getOffset(captureMillis))
        EXIF_DATE_TIME_TAGS.forEach { tag -> exif.setAttribute(tag, dateTime) }
        EXIF_OFFSET_TIME_TAGS.forEach { tag -> exif.setAttribute(tag, utcOffset) }
        if (exif.getAttribute(ExifInterface.TAG_MAKE).isNullOrBlank()) {
            exif.setAttribute(ExifInterface.TAG_MAKE, Build.MANUFACTURER)
        }
        if (exif.getAttribute(ExifInterface.TAG_MODEL).isNullOrBlank()) {
            exif.setAttribute(ExifInterface.TAG_MODEL, Build.MODEL)
        }
        exif.saveAttributes()
        Log.d(TAG, "EXIF filled (takenAt=$dateTime, offset=$utcOffset): ${file.name}")
    }.getOrElse {
        Log.w(TAG, "EXIF fill failed: ${file.name}", it)
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
 *
 * 재인코딩하면 EXIF 가 사라지므로 원본 EXIF 를 되살린다.
 * - JPEG 원본: EXIF 세그먼트를 바이트 그대로 옮긴다. 픽셀은 저장된 방향 그대로 두고 방향 태그도 그대로 남긴다.
 * - 그 밖의 원본(HEIC 등): 회전은 픽셀에 반영하고 [EXIF_TAGS_TO_PRESERVE] 를 옮겨 적는다.
 * EXIF 가 없던 사진은 EXIF 없이 저장한다.
 */
private fun compressIntoLimit(file: File, maxBytes: Long) {
    val exifSegment = if (file.isJpeg()) file.inputStream().buffered().use(::readJpegExifSegment) else null
    if (exifSegment != null) {
        compressKeepingExifSegment(file, maxBytes, exifSegment)
    } else {
        compressCopyingExifTags(file, maxBytes)
    }
}

private fun compressKeepingExifSegment(file: File, maxBytes: Long, exifSegment: ByteArray) {
    val bitmap = decodeSampledBitmap(file)
        ?: throw MealPhotoInvalidException("사진을 읽지 못했어요. 다시 촬영해주세요")
    val original = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        .also { BitmapFactory.decodeFile(file.absolutePath, it) }
    val (bytes, encoded) = encodeIntoLimit(bitmap, maxBytes - exifSegment.size)
    try {
        file.writeBytes(insertJpegExifSegment(bytes, exifSegment))
        if (encoded.width != original.outWidth || encoded.height != original.outHeight) {
            updateExifDimensions(file, encoded.width, encoded.height)
        }
        Log.d(TAG, "EXIF segment kept (${exifSegment.size} bytes): ${file.name}")
    } finally {
        if (!encoded.isRecycled) encoded.recycle()
    }
}

private fun compressCopyingExifTags(file: File, maxBytes: Long) {
    val originalExif = runCatching { ExifInterface(file) }.getOrNull()
    val rotationDegrees = originalExif?.rotationDegrees ?: 0
    val preservedAttributes = originalExif?.let { exif ->
        EXIF_TAGS_TO_PRESERVE.mapNotNull { tag -> exif.getAttribute(tag)?.let { tag to it } }
    }.orEmpty()

    val decoded = decodeSampledBitmap(file)
        ?: throw MealPhotoInvalidException("사진을 읽지 못했어요. 다시 촬영해주세요")
    val rotated = decoded.rotatedBy(rotationDegrees)
    if (rotated !== decoded) decoded.recycle()

    // EXIF 복원분이 더해져도 상한을 넘지 않도록 여유분을 뺀 크기를 목표로 압축한다.
    val (bytes, encoded) = encodeIntoLimit(rotated, maxBytes - EXIF_SIZE_MARGIN_BYTES)
    try {
        file.writeBytes(bytes)
        restoreExifMetadata(file, preservedAttributes)
    } finally {
        if (!encoded.isRecycled) encoded.recycle()
    }
}

/**
 * [targetBytes] 이하가 될 때까지 품질을 낮추고 해상도를 줄인다. 인코딩한 바이트와 마지막 비트맵을 돌려준다.
 * 줄여도 넘치면 [bitmap] 을 정리하고 [MealPhotoInvalidException] 을 던진다.
 */
private fun encodeIntoLimit(bitmap: Bitmap, targetBytes: Long): Pair<ByteArray, Bitmap> {
    var current = bitmap
    try {
        var quality = INITIAL_JPEG_QUALITY
        var bytes = current.toJpegBytes(quality)
        while (bytes.size > targetBytes && quality > MIN_JPEG_QUALITY) {
            quality -= JPEG_QUALITY_STEP
            bytes = current.toJpegBytes(quality)
        }
        while (bytes.size > targetBytes && current.width / 2 >= MIN_DIMENSION_PX && current.height / 2 >= MIN_DIMENSION_PX) {
            val previous = current
            current = Bitmap.createScaledBitmap(previous, previous.width / 2, previous.height / 2, true)
            if (current !== previous) previous.recycle()
            bytes = current.toJpegBytes(MIN_JPEG_QUALITY)
        }
        if (bytes.size > targetBytes) {
            throw MealPhotoInvalidException("사진 용량을 줄이지 못했어요. 다시 촬영해주세요")
        }
        return bytes to current
    } catch (e: Throwable) {
        if (!current.isRecycled) current.recycle()
        throw e
    }
}

/**
 * 해상도를 줄였으면 EXIF 의 이미지 크기 태그를 새 크기로 고친다.
 * 실패해도 옮겨 둔 EXIF 는 그대로 남으므로 경고 로그만 남긴다.
 */
private fun updateExifDimensions(file: File, width: Int, height: Int) {
    runCatching {
        val exif = ExifInterface(file)
        exif.setAttribute(ExifInterface.TAG_PIXEL_X_DIMENSION, width.toString())
        exif.setAttribute(ExifInterface.TAG_PIXEL_Y_DIMENSION, height.toString())
        exif.saveAttributes()
    }.onFailure { Log.w(TAG, "EXIF size update failed: ${file.name}", it) }
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
