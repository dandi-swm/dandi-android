package com.dandi.nyummy.settings.data.image

import kotlin.math.max
import kotlin.math.roundToInt

/**
 * 문의 첨부 사진의 긴 변 상한(px). 흔한 세로 스크린샷(1080×2400, 1440×3200)의 글자가 읽히는 크기이고,
 * JPEG로 저장하면 서버 상한(10MB)보다 한참 작다.
 */
internal const val INQUIRY_IMAGE_MAX_LONG_SIDE_PX = 2560

/**
 * 디코드할 때 쓸 inSampleSize(2의 거듭제곱). 줄인 뒤에도 긴 변이 [maxLongSide] 이상이 되는 가장 큰 값이라,
 * 마지막 축소([inquiryImageTargetSize])는 늘 줄이는 방향이다.
 */
internal fun inquiryImageSampleSize(width: Int, height: Int, maxLongSide: Int = INQUIRY_IMAGE_MAX_LONG_SIDE_PX): Int {
    val longSide = max(width, height)
    var sampleSize = 1
    while (longSide / (sampleSize * 2) >= maxLongSide) sampleSize *= 2
    return sampleSize
}

/** 긴 변이 [maxLongSide]를 넘으면 비율을 지켜 줄인 크기. 넘지 않으면 그대로. */
internal fun inquiryImageTargetSize(
    width: Int,
    height: Int,
    maxLongSide: Int = INQUIRY_IMAGE_MAX_LONG_SIDE_PX,
): Pair<Int, Int> {
    val longSide = max(width, height)
    if (longSide <= maxLongSide) return width to height
    val scale = maxLongSide.toFloat() / longSide
    return (width * scale).roundToInt().coerceAtLeast(1) to (height * scale).roundToInt().coerceAtLeast(1)
}
