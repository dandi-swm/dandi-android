package com.dandi.nyummy.meal.data.util

import androidx.exifinterface.media.ExifInterface
import com.dandi.nyummy.common.entity.time.DeviceTime
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeParseException

/** EXIF 시각 포맷(`yyyy:MM:dd HH:mm:ss`). 날짜 구분자를 `-` 로 쓰는 기기도 허용한다. */
private val EXIF_DATE_TIME_REGEX = Regex("""(\d{4})[:-](\d{2})[:-](\d{2}) (\d{2}:\d{2}:\d{2})""")

/** EXIF 오프셋 포맷(`+09:00`). */
private val EXIF_OFFSET_REGEX = Regex("""[+-](?:[01]\d|2[0-3]):[0-5]\d""")

/**
 * 촬영 시각으로 볼 태그와 그 오프셋 태그. 앞에 있을수록 우선한다.
 *
 * `DATETIME` 은 파일 변경 시각이라 편집하면 바뀌므로, 실제 촬영 시각인 `ORIGINAL` 계열이
 * 없을 때만 마지막으로 참고한다.
 */
private val EXIF_TAKEN_AT_TAGS = listOf(
    ExifInterface.TAG_DATETIME_ORIGINAL to ExifInterface.TAG_OFFSET_TIME_ORIGINAL,
    ExifInterface.TAG_DATETIME_DIGITIZED to ExifInterface.TAG_OFFSET_TIME_DIGITIZED,
    ExifInterface.TAG_DATETIME to ExifInterface.TAG_OFFSET_TIME,
)

/** 사진 EXIF 의 촬영 시각을 기기 타임존 벽시계로 읽는다. 촬영 시각 태그가 없거나 해석할 수 없으면 null. */
internal fun ExifInterface.takenAtOrNull(zone: ZoneId = DeviceTime.zone()): LocalDateTime? =
    EXIF_TAKEN_AT_TAGS.firstNotNullOfOrNull { (dateTimeTag, offsetTag) ->
        parseExifDateTimeOrNull(getAttribute(dateTimeTag), getAttribute(offsetTag), zone)
    }

/**
 * EXIF 시각 문자열과 오프셋을 [zone] 의 벽시계로 변환한다. 형식이 어긋나면 null.
 *
 * 오프셋이 있으면 그 순간을 [zone] 으로 옮기고, 오프셋이 없거나 깨져 있으면
 * 찍은 곳의 벽시계를 그대로 적은 것이라 이미 [zone] 의 로컬 시각인 것으로 간주한다.
 */
internal fun parseExifDateTimeOrNull(
    dateTime: String?,
    offset: String?,
    zone: ZoneId = DeviceTime.zone(),
): LocalDateTime? {
    val match = EXIF_DATE_TIME_REGEX.matchEntire(dateTime?.trim().orEmpty()) ?: return null
    val (year, month, day, time) = match.destructured
    val local = "$year-$month-${day}T$time"
    val offsetToken = offset?.trim()?.takeIf(EXIF_OFFSET_REGEX::matches)
    return try {
        if (offsetToken == null) {
            LocalDateTime.parse(local)
        } else {
            OffsetDateTime.parse(local + offsetToken).atZoneSameInstant(zone).toLocalDateTime()
        }
    } catch (_: DateTimeParseException) {
        null
    }
}
