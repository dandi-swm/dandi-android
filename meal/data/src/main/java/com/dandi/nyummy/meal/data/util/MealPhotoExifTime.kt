package com.dandi.nyummy.meal.data.util

import androidx.exifinterface.media.ExifInterface
import com.dandi.nyummy.common.entity.time.KstDateTime
import com.dandi.nyummy.common.entity.time.KstTime

/** EXIF 시각 포맷(`yyyy:MM:dd HH:mm:ss`). 날짜 구분자를 `-` 로 쓰는 기기도 허용한다. */
private val EXIF_DATE_TIME_REGEX = Regex("""(\d{4})[:-](\d{2})[:-](\d{2}) (\d{2}:\d{2}:\d{2})""")

/** EXIF 오프셋 포맷(`+09:00`). */
private val EXIF_OFFSET_REGEX = Regex("""[+-]\d{2}:\d{2}""")

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

/** 사진 EXIF 의 촬영 시각을 KST 벽시계로 읽는다. 촬영 시각 태그가 없거나 해석할 수 없으면 null. */
internal fun ExifInterface.takenAtKstOrNull(): KstDateTime? =
    EXIF_TAKEN_AT_TAGS.firstNotNullOfOrNull { (dateTimeTag, offsetTag) ->
        parseExifDateTimeToKstOrNull(getAttribute(dateTimeTag), getAttribute(offsetTag))
    }

/**
 * EXIF 시각 문자열과 오프셋을 KST 벽시계로 변환한다. 형식이 어긋나면 null.
 *
 * 오프셋이 있으면 그 순간을 KST 로 옮기고, 오프셋이 없거나 깨져 있으면
 * 이미 KST 로컬 시각인 것으로 간주한다([KstTime.parseIsoToEpochMillisOrNull] 과 같은 정책).
 */
internal fun parseExifDateTimeToKstOrNull(dateTime: String?, offset: String?): KstDateTime? {
    val match = EXIF_DATE_TIME_REGEX.matchEntire(dateTime?.trim().orEmpty()) ?: return null
    val (year, month, day, time) = match.destructured
    val offsetToken = offset?.trim()?.takeIf(EXIF_OFFSET_REGEX::matches).orEmpty()
    return KstTime.parseIsoToKstOrNull("$year-$month-${day}T$time$offsetToken")
}
